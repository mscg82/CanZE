package lu.fisch.canze.actors.smoother;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;

public class AverageSmootherTest {

    @Test
    public void assertEmptyAverage_isNan()
    {
        Smoother avgSmoother = AverageSmoother.withMaxValues(10);
        assertTrue("Empty average should be NaN", Double.isNaN(avgSmoother.smoothValue()));
    }

    @Test
    public void assertSingleValueAverage_isCorrect()
    {
        Smoother avgSmoother = AverageSmoother.withMaxValues(10);
        avgSmoother.append(1.3);
        double expectedAvg = 1.3;
        assertTrue("Expected value is " + expectedAvg, Math.abs(avgSmoother.smoothValue() - expectedAvg) < 0.0001);
    }

    @Test
    public void assertMultipleValuesAverageWithoutExpiryNoOverflow_isCorrect()
    {
        Smoother avgSmoother = AverageSmoother.withMaxValues(10);
        avgSmoother.append(1.3);
        avgSmoother.append(3.5);
        double expectedAvg = 2.4;
        assertTrue("Expected value is " + expectedAvg, Math.abs(avgSmoother.smoothValue() - expectedAvg) < 0.0001);
    }

    @Test
    public void assertMultipleValuesAverageWithoutExpiryWithOverflow_isCorrect()
    {
        Smoother avgSmoother = AverageSmoother.withMaxValues(4);
        avgSmoother.append(1.3);
        avgSmoother.append(3.5);
        avgSmoother.append(2.0);
        avgSmoother.append(4.5);
        avgSmoother.append(0.4);
        double expectedAvg = 2.6;
        assertTrue("Expected value is " + expectedAvg, Math.abs(avgSmoother.smoothValue() - expectedAvg) < 0.0001);
    }

    @Test
    public void assertMultipleValuesAverageWithExpiryNoOverflow_isCorrect()
    {
        Smoother avgSmoother = AverageSmoother.withMaxValuesAndValidity(10, Duration.ofMinutes(5));
        avgSmoother.append(1.3, Instant.now().minus(Duration.ofSeconds(10)));
        avgSmoother.append(3.5, Instant.now().minus(Duration.ofSeconds(5)));
        avgSmoother.append(2.0, Instant.now().minus(Duration.ofSeconds(3)));
        avgSmoother.append(4.5, Instant.now().minus(Duration.ofMinutes(10)));
        avgSmoother.append(0.4, Instant.now().minus(Duration.ofMinutes(5)));
        double expectedAvg = 1.8;
        assertTrue("Expected value is " + expectedAvg, Math.abs(avgSmoother.smoothValue() - expectedAvg) < 0.0001);
    }

    @Test
    public void assertMultipleValuesAverageWithExpiryWithOverflow_isCorrect()
    {
        Smoother avgSmoother = AverageSmoother.withMaxValuesAndValidity(3, Duration.ofMinutes(5));
        avgSmoother.append(1.3, Instant.now().minus(Duration.ofSeconds(10)));
        avgSmoother.append(3.6, Instant.now().minus(Duration.ofSeconds(5)));
        avgSmoother.append(2.0, Instant.now().minus(Duration.ofSeconds(3)));
        avgSmoother.append(4.5, Instant.now().minus(Duration.ofMinutes(10)));
        avgSmoother.append(0.4, Instant.now().minus(Duration.ofMinutes(5)));
        double expectedAvg = 2.0;
        assertTrue("Expected value is " + expectedAvg, Math.abs(avgSmoother.smoothValue() - expectedAvg) < 0.0001);
    }

}
