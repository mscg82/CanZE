package lu.fisch.canze.actors.integrator;

import java.util.ArrayList;
import java.util.List;

import lu.fisch.canze.classes.TimePoint;

public class IntegratorUtils {

    private IntegratorUtils() {
        throw new UnsupportedOperationException("Constructor of util class " + IntegratorUtils.class + " cannot be called");
    }

    public static long[] buildCommonTimeline(List<TimePoint> dataset1, List<TimePoint> dataset2) {
        if (dataset1.isEmpty() && dataset2.isEmpty()) {
            return new long[0];
        }

        int size1 = dataset1.size();
        int size2 = dataset2.size();

        long[] results = new long[size1 + size2];

        int resultsIndex = 0;
        int idx1 = 0;
        int idx2 = 0;

        while (idx1 < size1 && idx2 < size2) {
            long v1 = dataset1.get(idx1).date;
            long v2 = dataset2.get(idx2).date;
            if (v1 == v2) {
                results[resultsIndex] = v1;
                idx1++;
                idx2++;
            } else if (v1 < v2) {
                results[resultsIndex] = v1;
                idx1++;
            } else {
                results[resultsIndex] = v2;
                idx2++;
            }
            resultsIndex++;
        }

        while (idx1 < size1) {
            long v1 = dataset1.get(idx1).date;
            results[resultsIndex] = v1;
            idx1++;
            resultsIndex++;
        }

        while (idx2 < size2) {
            long v2 = dataset2.get(idx2).date;
            results[resultsIndex] = v2;
            idx2++;
            resultsIndex++;
        }

        if (resultsIndex != results.length) {
            long[] trimmedResults = new long[resultsIndex];
            System.arraycopy(results, 0, trimmedResults, 0, resultsIndex);
            return trimmedResults;
        }
        return results;
    }

    public static List<TimePoint> extend(List<TimePoint> dataset, long[] targetTimes) {
        int datasetSize = dataset.size();
        if (targetTimes.length < datasetSize) {
            throw new IllegalArgumentException("Target times length (" + targetTimes.length +
                    ") must be greater than dataset length (" + datasetSize + ")");
        }

        List<TimePoint> extended = new ArrayList<>(targetTimes.length);
        int dataIdx = 0;
        int timesIdx = 0;
        while (dataIdx < datasetSize && timesIdx < targetTimes.length) {
            TimePoint timePoint = dataset.get(dataIdx);
            long targetTime = targetTimes[timesIdx];
            if (timePoint.date == targetTime) {
                extended.add(timePoint);
                dataIdx++;
                timesIdx++;
            } else if (timePoint.date < targetTime) {
                throw new IllegalStateException("Dataset cannot contain times that are not in the target times");
            } else {
                extended.add(null);
                timesIdx++;
            }
        }

        while (timesIdx < targetTimes.length) {
            extended.add(null);
            timesIdx++;
        }

        return extended;
    }
}
