package lu.fisch.canze.mqtt;

import android.util.Log;

import com.google.android.gms.common.util.concurrent.NumberedThreadFactory;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import lu.fisch.canze.activities.MainActivity;

public class MqttValuePusher implements AutoCloseable {

    private final AtomicReference<IMqttClient> mqttClient;
    private final ExecutorService asyncExecutor;

    public MqttValuePusher(String publisherId) {
        this(publisherId, MainActivity.mqttConnectionUri, MainActivity.mqttConnectionUsername, MainActivity.mqttConnectionPassword);
    }

    public MqttValuePusher(String publisherId, String url, String username, char[] password) {
        this.mqttClient = new AtomicReference<>(null);
        NumberedThreadFactory threadFactory = new NumberedThreadFactory("mqtt-pool-");
        this.asyncExecutor = Executors.newSingleThreadExecutor(threadFactory);

        if (MainActivity.mqttEnabled) {
            this.asyncExecutor.submit(() -> {
                try {
                    IMqttClient mqttClient1 = new MqttClient(url, publisherId, new MemoryPersistence());

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
                    mqttClient1.connect(options);

                    closeClient(this.mqttClient.getAndSet(mqttClient1));
                } catch (Exception e) {
                    Log.e(MainActivity.TAG, "Failed to connect to MQTT broker", e);
                }
            });
        }
    }

    public void pushValue(String sid, double value) {
        pushValue(sid, String.format(Locale.ENGLISH, "%.3f", value));
    }

    public void pushValue(String sid, String value) {
        this.asyncExecutor.submit(() -> {
            try {
                IMqttClient mqttClient = this.mqttClient.get();
                if (mqttClient != null && mqttClient.isConnected()) {
                    MqttMessage msg = new MqttMessage(value.getBytes(StandardCharsets.UTF_8));
                    msg.setQos(0);
                    mqttClient.publish("zoe/obd/" + sid, msg);
                }
            } catch (Exception e) {
                Log.e(MainActivity.TAG, "Failed to send message to MQTT broker for sid " + sid, e);
            }
        });
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
