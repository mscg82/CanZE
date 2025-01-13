package lu.fisch.canze.actors.smoother;

import java.time.Instant;

public interface Smoother {

    void append(double value, Instant occurredAt);

    default void append(double value)
    {
        append(value, Instant.now());
    }

    double smoothValue();

    void clear();

}
