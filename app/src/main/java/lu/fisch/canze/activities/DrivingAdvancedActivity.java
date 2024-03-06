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
import android.view.View;
import android.widget.TextView;

import java.util.Locale;
import java.util.Optional;
import java.util.function.DoubleFunction;

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
        addField(Sid.InstantConsumptionByAverage, 0);
        addField(Sid.GPS_Altitude, 5000);

        if (MainActivity.isPh2()) {
            addField(Sid.ThermalComfortPower, 0);
        } else {
            findViewById(R.id.textLabel_climatePower).setVisibility(View.GONE);
        }
    }


    // This is the event fired as soon as this the registered fields are
    // getting updated by the corresponding reader class.
    @Override
    public void onFieldUpdateEvent(final Field field) {
        // the update has to be done in a separate thread
        // otherwise the UI will not be repainted
        runOnUiThread(() -> {
            // get the text field
            switch (field.getSID()) {

                case Sid.EngineFanSpeed:
                    setNumericValueFromField(findViewById(R.id.text_EFS), field);
                    break;

                case Sid.InstantConsumptionByAverage:
                    setNumericValueFromField(findViewById(R.id.text_instant_consumption),
                            MainActivity.milesMode ? "%.2f" : "%.1f",
                            field,
                            val -> Double.isNaN(val) ?
                                    Optional.of(MainActivity.getStringSingle(R.string.default_Dash)) :
                                    Optional.empty());
                    break;

                case Sid.ThermalComfortPower:
                    setNumericValueFromField(findViewById(R.id.text_ClimatePower), field);
                    break;

                case Sid.HvCoolingState:
                    setTextValueFromField(findViewById(R.id.text_HCS), cooling_Status, field);
                    break;

                case Sid.HvEvaporationTemp:
                    setNumericValueFromField(findViewById(R.id.text_HET), field);
                    break;

                case Sid.Pressure:
                    setNumericValueFromField(findViewById(R.id.text_PRE), field);
                    break;

                case Sid.BatteryConditioningMode:
                    setTextValueFromField(findViewById(R.id.text_HCM), conditioning_Status, field);
                    break;

                case Sid.ClimaLoopMode:
                    setTextValueFromField(findViewById(R.id.text_CLM), climate_Status, field);
                    break;

                case Sid.GPS_Altitude:
                    setNumericValueFromField(findViewById(R.id.text_altitude), field);
                    break;
            }
        });

    }

    private void setNumericValueFromField(TextView tv, Field field) {
        setNumericValueFromField(tv, "%.1f", field, val -> Optional.empty());
    }

    private void setNumericValueFromField(TextView tv, String format, Field field, DoubleFunction<Optional<String>> nanHandler) {
        if (tv != null) {
            tv.setText(nanHandler.apply(field.getValue())
                    .orElseGet(() -> String.format(Locale.getDefault(), format, field.getValue())));
        }
    }

    private void setTextValueFromField(TextView tv, String[] values, Field field) {
        int value = (int) field.getValue();
        if (tv != null && values != null && value >= 0 && value < values.length) {
            tv.setText(values[value]);
        }
    }

}