package lu.fisch.canze.mqtt;

import android.util.Log;

import com.google.android.gms.common.util.concurrent.NumberedThreadFactory;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import lu.fisch.canze.activities.MainActivity;

public class MqttValuePusher implements AutoCloseable {

    private static final long NO_DISCONNECTION_FOUND = Long.MIN_VALUE;

    private final AtomicReference<IMqttClient> mqttClient;
    private final ExecutorService asyncExecutor;
    private final Runnable connectionAction;
    private final AtomicBoolean connecting;
    private final AtomicLong lastDisconnection;

    public MqttValuePusher(String publisherId) {
        this(publisherId, MainActivity.mqttConnectionUri, MainActivity.mqttConnectionUsername, MainActivity.mqttConnectionPassword);
    }

    public MqttValuePusher(String publisherId, String url, String username, char[] password) {
        this.lastDisconnection = new AtomicLong(NO_DISCONNECTION_FOUND);
        this.connecting = new AtomicBoolean(false);
        this.mqttClient = new AtomicReference<>(null);
        NumberedThreadFactory threadFactory = new NumberedThreadFactory("mqtt-pool-");
        this.asyncExecutor = Executors.newSingleThreadExecutor(threadFactory);
        this.connectionAction = () -> {
            try {
                if (this.connecting.compareAndSet(false, true)) {
                    IMqttClient internalMqttClient = new MqttClient(url, publisherId, new InMemoryPersistence());

                    MqttConnectOptions options = new MqttConnectOptions();
                    if (username != null && !username.trim().isEmpty()) {
                        options.setUserName(username);
                    }
                    if (password != null) {
                        options.setPassword(password);
                    }
                    options.setAutomaticReconnect(true);
                    options.setCleanSession(true);
                    options.setConnectionTimeout(10);
                    internalMqttClient.connect(options);

                    this.lastDisconnection.set(NO_DISCONNECTION_FOUND);

                    closeClient(this.mqttClient.getAndSet(internalMqttClient));
                }
                else {
                    Log.d(MainActivity.TAG, "Another thread is connecting to MQTT broker");
                }
            } catch (Exception e) {
                Log.e(MainActivity.TAG, "Failed to connect to MQTT broker", e);
            } finally {
                this.connecting.set(false);
            }
        };


    }

    public void connect() {
        connectAndThen(null);
    }

    public boolean isConnected()
    {
        IMqttClient mqttClient = this.mqttClient.get();
        if (mqttClient == null || !mqttClient.isConnected()) {
            this.lastDisconnection.compareAndSet(NO_DISCONNECTION_FOUND, System.currentTimeMillis());
            return false;
        }

        this.lastDisconnection.set(NO_DISCONNECTION_FOUND);
        return true;
    }

    public void connectAndThen(Runnable additionalAction) {
        if (MainActivity.mqttEnabled) {
            this.asyncExecutor.submit(() -> {
                this.connectionAction.run();
                if (additionalAction != null) {
                    additionalAction.run();
                }
            });
        }
    }

    public void pushValue(String sid, double value) {
        if (Double.isNaN(value)) {
            return;
        }

        pushValue(sid, String.format(Locale.ENGLISH, "%.3f", value));
    }

    public void pushValue(String sid, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        try {
            this.asyncExecutor.submit(() -> {
                try {
                    IMqttClient mqttClient = this.mqttClient.get();
                    if (mqttClient != null) {
                        if (isConnected()) {
                            MqttMessage msg = new MqttMessage(value.getBytes(StandardCharsets.UTF_8));
                            msg.setQos(0);
                            mqttClient.publish("zoe/obd/" + sid, msg);
                        } else {
                            long now = System.currentTimeMillis();
                            if (now - this.lastDisconnection.get() >= 30_000L) {
                                this.lastDisconnection.set(NO_DISCONNECTION_FOUND);
                                this.connectionAction.run();
                            } else {
                                mqttClient.reconnect();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Failed to send message to MQTT broker for sid " + sid, e);
                }
            });
        } catch (RejectedExecutionException e) {
            Log.e(MainActivity.TAG, "Failed to enqueue push action for sid " + sid, e);
        }
    }

    @Override
    public void close() {
        closeClient(this.mqttClient.getAndSet(null));
        this.asyncExecutor.shutdownNow();
    }

    private void closeClient(IMqttClient client) {
        String publisherId = client == null ? "<>" : client.getClientId();
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            Log.w(MainActivity.TAG, "An error occurred while closing MQTT client with publisher id " + publisherId, e);
        }
    }
}
