package lu.fisch.canze.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttPersistable;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class InMemoryPersistence implements MqttClientPersistence {

    private final AtomicReference<Map<String, MqttPersistable>> data = new AtomicReference<>(null);

    public void close() throws MqttPersistenceException {
        final Map<String, MqttPersistable> currentData = data.getAndSet(null);
        currentData.clear();
    }

    public Enumeration<String> keys() throws MqttPersistenceException {
        return runIfOpen(currentData -> //
                new IteratorBackedEnumeration<>(currentData.keySet().iterator()));
    }

    public MqttPersistable get(String key) throws MqttPersistenceException {
        return runIfOpen(currentData -> currentData.get(key));
    }

    public void open(String clientId, String serverURI) throws MqttPersistenceException {
        if (!this.data.compareAndSet(null, new ConcurrentHashMap<>())) {
            throw new MqttPersistenceException(new IllegalStateException("InMemoryPersistence was already open"));
        }
    }

    public void put(String key, MqttPersistable persistable) throws MqttPersistenceException {
        runIfOpenNoResult(currentData -> currentData.put(key, persistable));
    }

    public void remove(String key) throws MqttPersistenceException {
        runIfOpenNoResult(currentData -> currentData.remove(key));
    }

    public void clear() throws MqttPersistenceException {
        runIfOpenNoResult(Map::clear);
    }

    public boolean containsKey(String key) throws MqttPersistenceException {
        return runIfOpen(currentData -> currentData.containsKey(key));
    }

    private void runIfOpenNoResult(Consumer<Map<String, MqttPersistable>> block) throws MqttPersistenceException {
        runIfOpen(currentData -> {
            block.accept(currentData);
            return null;
        });
    }

    private <R> R runIfOpen(Function<Map<String, MqttPersistable>, R> block) throws MqttPersistenceException {
        final Object[] returnValue = new Object[1];
        final Map<String, MqttPersistable> data = this.data.getAndUpdate(currentData -> {
            if (currentData != null) {
                returnValue[0] = block.apply(currentData);
            }
            return currentData;
        });
        if (data == null) {
            throw new MqttPersistenceException(new IllegalStateException("InMemoryPersistence was not open"));
        }

        //noinspection unchecked
        return (R) returnValue[0];
    }

    private static class IteratorBackedEnumeration<T> implements Enumeration<T> {
        private final Iterator<T> keysIterator;

        public IteratorBackedEnumeration(Iterator<T> keysIterator) {
            this.keysIterator = keysIterator;
        }

        @Override
        public boolean hasMoreElements() {
            return keysIterator.hasNext();
        }

        @Override
        public T nextElement() {
            return keysIterator.next();
        }
    }
}