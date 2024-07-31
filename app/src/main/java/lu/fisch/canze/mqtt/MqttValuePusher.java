package lu.fisch.canze.mqtt;

import android.util.Log;

import com.google.android.gms.common.util.concurrent.NumberedThreadFactory;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import lu.fisch.canze.activities.MainActivity;

public class MqttValuePusher implements AutoCloseable {

    private final IMqttClient mqttClient;
    private final ScheduledExecutorService asyncExecutor;
    private final Runnable connectionAction;
    private final AtomicReference<Future<?>> reconnectAction;
    private final Consumer<Boolean> mqttConnectionListener;

    public MqttValuePusher(String publisherId, Consumer<Boolean> mqttConnectionListener) {
        this(publisherId,
                MainActivity.mqttConnectionUri, MainActivity.mqttConnectionUsername, MainActivity.mqttConnectionPassword,
                mqttConnectionListener);
    }

    public MqttValuePusher(String publisherId, String url, String username, char[] password,
                           Consumer<Boolean> mqttConnectionListener) {
        IMqttClient mqttClient;
        try {
            mqttClient = new MqttClient(url, publisherId, new InMemoryPersistence());
        } catch (Exception e) {
            mqttClient = null;
            Log.w(MainActivity.TAG, "Failed to initialize MQTT client", e);
        }
        this.mqttClient = mqttClient;
        setCallback();
        NumberedThreadFactory threadFactory = new NumberedThreadFactory("mqtt-pool-");
        this.asyncExecutor = Executors.newScheduledThreadPool(1, threadFactory);

        this.connectionAction = () -> {
            try {
                if (this.mqttClient != null && !this.mqttClient.isConnected()) {
                    MqttConnectOptions options = getMqttConnectOptions(username, password);
                    this.mqttClient.connect(options);
                }
            } catch (Exception e) {
                Log.e(MainActivity.TAG, "Failed to connect to MQTT broker", e);
            }
        };

        this.reconnectAction = new AtomicReference<>(null);
        this.mqttConnectionListener = mqttConnectionListener;
    }

    private MqttConnectOptions getMqttConnectOptions(String username, char[] password) {
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
        return options;
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public void connect() {
        connectAndThen(null);
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
                    if (isConnected()) {
                        MqttMessage msg = new MqttMessage(value.getBytes(StandardCharsets.UTF_8));
                        msg.setQos(0);
                        mqttClient.publish("zoe/obd/" + sid, msg);
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
        closeClient();
        this.asyncExecutor.shutdownNow();
    }

    private void closeClient() {
        String publisherId = mqttClient == null ? "<>" : mqttClient.getClientId();
        try {
            if (mqttClient != null) {
                try {
                    mqttClient.disconnect();
                } finally {
                    mqttClient.close();
                }
            }
        } catch (Exception e) {
            Log.w(MainActivity.TAG, "An error occurred while closing MQTT client with publisher id " + publisherId, e);
        }
    }

    private void setCallback() {
        if (this.mqttClient != null) {
            this.mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Future<?> actionToStop = reconnectAction.getAndSet(null);
                    if (actionToStop != null) {
                        actionToStop.cancel(true);
                    }
                    if (mqttConnectionListener != null) {
                        mqttConnectionListener.accept(true);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    reconnectAction.updateAndGet(oldAction -> {
                        if (oldAction != null) {
                            return oldAction;
                        }

                        return asyncExecutor.schedule(this::reconnect, 30L, TimeUnit.SECONDS);
                    });
                    if (mqttConnectionListener != null) {
                        mqttConnectionListener.accept(false);
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // NOTHING TO DO
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // NOTHING TO DO
                }

                private void reconnect() {
                    try {
                        mqttClient.reconnect();
                    } catch (Exception e) {
                        Log.w(MainActivity.TAG, "Failed to reconnect to MQTT broker", e);

                        if (!Thread.currentThread().isInterrupted()) {
                            Future<?> oldAction = reconnectAction.getAndSet(asyncExecutor.schedule(this::reconnect, 30L, TimeUnit.SECONDS));
                            if (oldAction != null) {
                                oldAction.cancel(true);
                            }
                        }
                    }
                }
            });
        }
    }
}
