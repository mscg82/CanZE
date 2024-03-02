/*
    CanZE
    Take a closer look at your ZE car

    Copyright (C) 2015 - The CanZE Team
    http://canze.fisch.lu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or any
    later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package lu.fisch.canze.activities;

import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

import lu.fisch.canze.R;
import lu.fisch.canze.actors.Field;
import lu.fisch.canze.classes.Sid;
import lu.fisch.canze.interfaces.DebugListener;
import lu.fisch.canze.interfaces.FieldListener;

// If you want to monitor changes, you must add a FieldListener to the fields.
// For the simple activity, the easiest way is to implement it in the activity itself.
public class DrivingAdvancedActivity extends CanzeActivity implements FieldListener, DebugListener {

    private final String[] cooling_Status = MainActivity.getStringList(R.array.list_CoolingStatus);
    private final String[] conditioning_Status = MainActivity.getStringList(MainActivity.isPh2() ? R.array.list_ConditioningStatusPh2
            : R.array.list_ConditioningStatus);
    private final String[] climate_Status = MainActivity.getStringList(MainActivity.isPh2() ? R.array.list_ClimateStatusPh2
            : R.array.list_ClimateStatus);

    private final Deque<Double> realSpeeds;
    private final Deque<Double> dcPwrs;

    public DrivingAdvancedActivity() {
        int maxValues = 10;
        this.realSpeeds = new ArrayDeque<>(maxValues);
        this.dcPwrs = new ArrayDeque<>(maxValues);
        for (int i = 1; i <= maxValues; i++) {
            realSpeeds.add(Double.NEGATIVE_INFINITY);
            dcPwrs.add(Double.NEGATIVE_INFINITY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving_advanced);

        TextView tv = findViewById(R.id.label_instant_consumption);
        String baseLabel = MainActivity.getStringSingle(R.string.label_InstantConsumption);
        String unit = MainActivity.getStringSingle(MainActivity.milesMode ? R.string.unit_ConsumptionMiAlt : R.string.label_kWh100km);
        tv.setText(String.format("%s (%s)", baseLabel, unit));
    }

    protected void initListeners() {
        MainActivity.getInstance().setDebugListener(this);
        addField(Sid.EngineFanSpeed, 0);
        addField(Sid.HvCoolingState, 0);
        addField(Sid.HvEvaporationTemp, 10000);
        addField(Sid.Pressure, 1000);
        addField(Sid.BatteryConditioningMode, 0);
        addField(Sid.ClimaLoopMode, 0);
        addField(Sid.DcPowerOut, 0);
        addField(Sid.RealSpeed, 0);

        TextView tv = findViewById(R.id.textLabel_climatePower);
        if (MainActivity.isPh2()) {
            addField(Sid.ThermalComfortPower, 0);
            tv.setText(getResources().getString(R.string.label_ThermalComfortPower));
        } else {
            tv.setText(getResources().getString(R.string.label_DcPwr));
        }
    }


    // This is the event fired as soon as this the registered fields are
    // getting updated by the corresponding reader class.
    @Override
    public void onFieldUpdateEvent(final Field field) {
        // the update has to be done in a separate thread
        // otherwise the UI will not be repainted
        runOnUiThread(() -> {
            String fieldId = field.getSID();
            TextView tv = null;
            int value;

            // get the text field
            switch (fieldId) {

                case Sid.RealSpeed:
                    realSpeeds.removeFirst();
                    realSpeeds.addLast(field.getValue());
                    showInstantConsumption();
                    break;

                case Sid.EngineFanSpeed:
                    tv = findViewById(R.id.text_EFS);
                    break;

                case Sid.DcPowerOut: {
                    tv = findViewById(R.id.text_ClimatePower);
                    dcPwrs.removeFirst();
                    dcPwrs.addLast(field.getValue());
                    showInstantConsumption();
                    break;
                }

                case Sid.ThermalComfortPower:
                    tv = findViewById(R.id.text_ClimatePower);
                    break;

                case Sid.HvCoolingState:
                    value = (int) field.getValue();
                    tv = findViewById(R.id.text_HCS);
                    if (tv != null && cooling_Status != null && value >= 0 && value < cooling_Status.length)
                        tv.setText(cooling_Status[value]);
                    tv = null;
                    break;

                case Sid.HvEvaporationTemp:
                    tv = findViewById(R.id.text_HET);
                    break;

                case Sid.Pressure:
                    tv = findViewById(R.id.text_PRE);
                    break;

                case Sid.BatteryConditioningMode:
                    value = (int) field.getValue();
                    tv = findViewById(R.id.text_HCM);
                    if (tv != null && conditioning_Status != null && value >= 0 && value < conditioning_Status.length)
                        tv.setText(conditioning_Status[value]);
                    tv = null;
                    break;

                case Sid.ClimaLoopMode:
                    value = (int) field.getValue();
                    tv = findViewById(R.id.text_CLM);
                    if (tv != null && climate_Status != null && value >= 0 && value < climate_Status.length)
                        tv.setText(climate_Status[value]);
                    tv = null;
                    break;
            }
            // set regular new content, all exceptions handled above
            if (tv != null) {
                tv.setText(String.format(Locale.getDefault(), "%.1f", field.getValue()));
            }
        });

    }

    private void showInstantConsumption() {
        double realSpeed = averageValues(realSpeeds);
        double dcPwr = averageValues(dcPwrs);

        String instantConsumption;
        if (!MainActivity.milesMode && !Double.isNaN(realSpeed) && realSpeed > 5 && !Double.isNaN(dcPwr)) {
            instantConsumption = String.format(Locale.getDefault(), "%.1f", 100.0 * dcPwr / realSpeed);
        } else if (MainActivity.milesMode && !Double.isNaN(dcPwr) && dcPwr != 0) {
            instantConsumption = String.format(Locale.getDefault(), "%.2f", realSpeed / dcPwr);
        } else {
            instantConsumption = MainActivity.getStringSingle(R.string.default_Dash);
        }

        TextView instantConsumptionText = findViewById(R.id.text_instant_consumption);
        instantConsumptionText.setText(instantConsumption);
    }

    private static double averageValues(Deque<Double> values) {
        int validValues = 0;
        double sum = 0.0;
        for (Double val : values) {
            if (!Double.isInfinite(val)) {
                sum += val;
                validValues++;
            }
        }
        return validValues != 0 ? sum / validValues : sum;
    }

}