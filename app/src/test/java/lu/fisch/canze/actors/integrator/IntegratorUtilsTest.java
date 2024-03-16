package lu.fisch.canze.actors.integrator;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import lu.fisch.canze.classes.TimePoint;

public class IntegratorUtilsTest {

    @Test
    public void emptySetsShouldProduceEmptyTimeset() {
        assertArrayEquals(new long[0], IntegratorUtils.buildCommonTimeline(Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    public void oneEmptySetShouldProduceOtherTimeset() {
        assertArrayEquals(new long[]{1, 2, 3}, IntegratorUtils.buildCommonTimeline(
                Arrays.asList(new TimePoint(1, 0.0), new TimePoint(2, 0.0), new TimePoint(3, 0.0)),
                Collections.emptyList()));

        assertArrayEquals(new long[]{1, 2, 3}, IntegratorUtils.buildCommonTimeline(
                Collections.emptyList(),
                Arrays.asList(new TimePoint(1, 0.0), new TimePoint(2, 0.0), new TimePoint(3, 0.0))));
    }

    @Test
    public void nonOverlappingSetsShouldProduceUnionTimeset() {
        assertArrayEquals(new long[]{1, 2, 3, 10, 20, 30}, IntegratorUtils.buildCommonTimeline(
                Arrays.asList(new TimePoint(1, 0.0), new TimePoint(2, 0.0), new TimePoint(3, 0.0)),
                Arrays.asList(new TimePoint(10, 0.0), new TimePoint(20, 0.0), new TimePoint(30, 0.0))));

        assertArrayEquals(new long[]{1, 2, 3, 10, 20, 30}, IntegratorUtils.buildCommonTimeline(
                Arrays.asList(new TimePoint(10, 0.0), new TimePoint(20, 0.0), new TimePoint(30, 0.0)),
                Arrays.asList(new TimePoint(1, 0.0), new TimePoint(2, 0.0), new TimePoint(3, 0.0))));
    }

    @Test
    public void setsOverlappingOnlyOnExtremeShouldProduceUnionTimesetMinusOnePoint() {
        assertArrayEquals(new long[]{1, 2, 3, 10, 20, 30}, IntegratorUtils.buildCommonTimeline(
                Arrays.asList(new TimePoint(1, 0.0), new TimePoint(2, 0.0), new TimePoint(3, 0.0)),
                Arrays.asList(new TimePoint(3, 0.0), new TimePoint(10, 0.0), new TimePoint(20, 0.0), new TimePoint(30, 0.0))));

        assertArrayEquals(new long[]{1, 2, 3, 10, 20, 30}, IntegratorUtils.buildCommonTimeline(
                Arrays.asList(new TimePoint(3, 0.0), new TimePoint(10, 0.0), new TimePoint(20, 0.0), new TimePoint(30, 0.0)),
                Arrays.asList(new TimePoint(1, 0.0), new TimePoint(2, 0.0), new TimePoint(3, 0.0))));
    }

    @Test
    public void overlappingNonConflictingSetsProduceUnionTimeset() {
        assertArrayEquals(new long[]{1, 3, 4, 5, 8, 9, 10}, IntegratorUtils.buildCommonTimeline(
                Arrays.asList(new TimePoint(1, 0.0), new TimePoint(5, 0.0), new TimePoint(10, 0.0)),
                Arrays.asList(new TimePoint(3, 0.0), new TimePoint(4, 0.0), new TimePoint(8, 0.0), new TimePoint(9, 0.0))));

        assertArrayEquals(new long[]{1, 3, 4, 5, 8, 9, 10}, IntegratorUtils.buildCommonTimeline(
                Arrays.asList(new TimePoint(3, 0.0), new TimePoint(4, 0.0), new TimePoint(8, 0.0), new TimePoint(9, 0.0)),
                Arrays.asList(new TimePoint(1, 0.0), new TimePoint(5, 0.0), new TimePoint(10, 0.0))));
    }

    @Test
    public void overlappingConflictingSetsProduceUnionTimesetWithoutDuplicates() {
        assertArrayEquals(new long[]{1, 3, 4, 5, 8, 9, 10}, IntegratorUtils.buildCommonTimeline(
                Arrays.asList(new TimePoint(1, 0.0), new TimePoint(3, 0.0), new TimePoint(5, 0.0), new TimePoint(9, 0.0), new TimePoint(10, 0.0)),
                Arrays.asList(new TimePoint(3, 0.0), new TimePoint(4, 0.0), new TimePoint(8, 0.0), new TimePoint(9, 0.0))));

        assertArrayEquals(new long[]{1, 3, 4, 5, 8, 9, 10}, IntegratorUtils.buildCommonTimeline(
                Arrays.asList(new TimePoint(3, 0.0), new TimePoint(4, 0.0), new TimePoint(8, 0.0), new TimePoint(9, 0.0)),
                Arrays.asList(new TimePoint(1, 0.0), new TimePoint(3, 0.0), new TimePoint(5, 0.0), new TimePoint(9, 0.0), new TimePoint(10, 0.0))));
    }

}
