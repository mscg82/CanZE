package lu.fisch.canze.actors.smoother;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

public class AverageSmoother implements Smoother {

    private final Deque<Data> queue;

    private final int maxValues;

    private final Duration maxDataValidity;

    public static AverageSmoother withMaxValues(int maxValues)
    {
        return new AverageSmoother(maxValues, null);
    }

    public static AverageSmoother withMaxValidity(Duration maxDataValidity)
    {
        return new AverageSmoother(Integer.MAX_VALUE, maxDataValidity);
    }

    public static AverageSmoother withMaxValuesAndValidity(int maxValues, Duration maxDataValidity)
    {
        return new AverageSmoother(maxValues, maxDataValidity);
    }

    private AverageSmoother(int maxValues, Duration maxDataValidity) {
        this.queue = new ArrayDeque<>(Math.min(maxValues, 1_000));
        this.maxValues = maxValues;
        this.maxDataValidity = maxDataValidity;
    }

    @Override
    public void append(double value, Instant occurredAt) {
        if (maxDataValidity != null) {
            Instant minValidInstant = Instant.now().minus(maxDataValidity);
            queue.removeIf(data -> data.getWhen().compareTo(minValidInstant) < 0);
        }
        while (queue.size() >= maxValues) {
            queue.removeFirst();
        }
        queue.addLast(new Data(value, occurredAt));
    }

    @Override
    public double smoothValue() {
        return queue.stream()
                .mapToDouble(Data::getValue)
                .average()
                .orElse(Double.NaN);
    }

    @Override
    public void clear() {
        queue.clear();
    }

    private final static class Data
    {
        private final double value;

        private final Instant when;

        private Data(double value, Instant when) {
            this.when = when;
            this.value = value;
        }

        private Instant getWhen() {
            return when;
        }

        private double getValue() {
            return value;
        }
    }

}
