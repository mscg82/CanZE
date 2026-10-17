package lu.fisch.canze.actors;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

public class FieldsTest {

    @Test
    public void smoothAround445() {
        double[] energies = new double[501];
        for (int i = 0; i < energies.length; i++) {
            energies[i] = 3.0 * (i / (double) (energies.length - 1)) + 43;
        }

        double[] socs = new double[energies.length];
        for (int i = 0; i < energies.length; i++) {
            socs[i] = Fields.computeSoCFromEnergyAndTemperature(energies[i], 25.0);
        }

        printValuesForPlotting(energies, socs);

        assertSmoothness(socs, energies);
    }

    @Test
    public void smoothAround265() {
        double[] energies = new double[501];
        for (int i = 0; i < energies.length; i++) {
            energies[i] = 3.0 * (i / (double) (energies.length - 1)) + 25;
        }

        double[] socs = new double[energies.length];
        for (int i = 0; i < energies.length; i++) {
            socs[i] = Fields.computeSoCFromEnergyAndTemperature(energies[i], 25.0);
        }

        printValuesForPlotting(energies, socs);

        assertSmoothness(socs, energies);
    }

    @Test
    public void test_estimationThroughSoH_shouldGiveMeaningfulResults() {
        double computedSoc = Fields.computeSoCFromEnergyAndTemperatureAndSoh(35.0, 22.0, 0.9458);
        Assert.assertTrue("Expected value was around 71.5, but was " + computedSoc,
                Math.abs(71.5 - computedSoc) < 0.02);

        computedSoc = Fields.computeSoCFromEnergyAndTemperatureAndSoh(34.01, 24.75, 0.9457);
        Assert.assertTrue("Expected value was around 69, but was " + computedSoc,
                Math.abs(69 - computedSoc) < 0.02);
    }

    private void printValuesForPlotting(double[] energies, double[] socs) {
        String energiesStr = Arrays.stream(energies)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(" ", "e = [", "];"));
        System.out.println(energiesStr);

        String socsStr = Arrays.stream(socs)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(" ", "s = [", "];"));
        System.out.println(socsStr);
    }

    private void assertSmoothness(double[] socs, double[] energies) {
        for (int i = 1; i < socs.length; i++) {
            double delta = socs[i] - socs[i - 1];
            Assert.assertTrue(String.format("SoC is not smooth around values %s and %s. Delta is %s.", energies[i - 1], energies[i], delta),
                    delta < 0.05);
        }
    }

}
