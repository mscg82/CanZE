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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving_advanced);
    }

    protected void initListeners() {
        MainActivity.getInstance().setDebugListener(this);
        addField(Sid.EngineFanSpeed, 0);
        addField(Sid.HvCoolingState, 0);
        addField(Sid.HvEvaporationTemp, 10000);
        addField(Sid.Pressure, 1000);
        addField(Sid.BatteryConditioningMode, 0);
        addField(Sid.ClimaLoopMode, 0);

        TextView tv = findViewById(R.id.textLabel_climatePower);
        if (MainActivity.isPh2()) {
            addField(Sid.ThermalComfortPower, 0);
            tv.setText(getResources().getString(R.string.label_ThermalComfortPower));
        } else {
            addField(Sid.DcPowerOut, 0);
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

                case Sid.EngineFanSpeed:
                    tv = findViewById(R.id.text_EFS);
                    break;

                case Sid.DcPowerOut:
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

}