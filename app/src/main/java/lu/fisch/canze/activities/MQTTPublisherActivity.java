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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import lu.fisch.canze.R;
import lu.fisch.canze.actors.Field;
import lu.fisch.canze.classes.Sid;
import lu.fisch.canze.interfaces.DebugListener;
import lu.fisch.canze.interfaces.FieldListener;
import lu.fisch.canze.mqtt.MqttValuePusher;

// If you want to monitor changes, you must add a FieldListener to the fields.
// For the simple activity, the easiest way is to implement it in the activity itself.
public class MQTTPublisherActivity extends CanzeActivity implements FieldListener, DebugListener {

    private final String[] conditioning_Status = MainActivity.getStringList(MainActivity.isPh2() ? R.array.list_ConditioningStatusPh2
            : R.array.list_ConditioningStatus);
    private final String[] climate_Status = MainActivity.getStringList(MainActivity.isPh2() ? R.array.list_ClimateStatusPh2
            : R.array.list_ClimateStatus);

    private MqttValuePusher mqttPusher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mqtt);

        if (mqttPusher != null) {
            mqttPusher.close();
        }
        mqttPusher = new MqttValuePusher(UUID.randomUUID().toString(),
                connected -> runOnUiThread(() -> {
                    int stringIndex = connected ?
                            R.string.default_debug_mqtt_connected :
                            R.string.default_debug_mqtt_disconnected;
                    ((TextView) findViewById(R.id.textMqtt)).setText(MainActivity.getStringSingle(stringIndex));
                }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttPusher != null) {
            mqttPusher.close();
            mqttPusher = null;
        }
    }

    @Override
    protected void initListenerAndPropelFields() {
        if (MainActivity.mqttEnabled) {
            findViewById(R.id.MQTTDebug).setVisibility(View.VISIBLE);
        }
        else {
            findViewById(R.id.MQTTDebug).setVisibility(View.GONE);
        }

        mqttPusher.connectAndThen(() -> runOnUiThread(super::initListenerAndPropelFields));
    }

    protected void initListeners() {
        MainActivity.getInstance().setDebugListener(this);

        addField(Sid.EngineFanSpeed, 0);
        if (MainActivity.isPh2()) {
            addField(Sid.ThermalComfortPower, 0);
        } else {
            findViewById(R.id.textLabel_climatePower).setVisibility(View.GONE);
        }
        addField(Sid.BatteryConditioningMode, 0);
        addField(Sid.ClimaLoopMode, 0);
        addField(Sid.CompressorRPM, 5000);
        addField(Sid.HvTemp, 5000);
        addField(Sid.AvailableChargingPower, 5000);
        addField(Sid.DcPowerIn, 5000);
        addField(Sid.UserSoC, 10000);
        addField(Sid.RealSoC, 10000);
        addField(Sid.DisplaySOC, 10000);
        addField(Sid.GroundResistance, 0);
        addField(Sid.AvailableEnergy, 5000);
        addField(Sid.HvKilometers, 5000);
        addField(Sid.TractionBatteryVoltage, 5000);
        if (MainActivity.mqttTestEnabled) {
            addField(Sid.TestField1, 0);
        }
    }


    // This is the event fired as soon as this the registered fields are
    // getting updated by the corresponding reader class.
    @Override
    public void onFieldUpdateEvent(final Field field) {
        // the update has to be done in a separate thread
        // otherwise the UI will not be repainted
        final AtomicLong lastMqttUpdate = new AtomicLong(Long.MIN_VALUE);
        runOnUiThread(() -> {
            // get the text field
            switch (field.getSID()) {
                case Sid.EngineFanSpeed:
                    setNumericValueFromField(findViewById(R.id.text_EFS), "%.1f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.ThermalComfortPower:
                    setNumericValueFromField(findViewById(R.id.text_ClimatePower), "%.1f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.BatteryConditioningMode: {
                    String setValue = setTextValueFromField(findViewById(R.id.text_HCM), conditioning_Status, field);
                    mqttPusher.pushValue(field.getSID(), setValue);
                    break;
                }

                case Sid.ClimaLoopMode: {
                    String setValue = setTextValueFromField(findViewById(R.id.text_CLM), climate_Status, field);
                    mqttPusher.pushValue(field.getSID(), setValue);
                    break;
                }

                case Sid.CompressorRPM:
                    setNumericValueFromField(findViewById(R.id.text_CRPM), "%.0f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.HvTemp:
                    setNumericValueFromField(findViewById(R.id.text_battTemp), "%.1f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.AvailableChargingPower:
                    setNumericValueFromField(findViewById(R.id.text_chgPwr), "%.1f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.DcPowerIn:
                    setNumericValueFromField(findViewById(R.id.text_dcPwr), "%.1f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.UserSoC:
                    setNumericValueFromField(findViewById(R.id.text_usable_soc), "%.1f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.RealSoC:
                    setNumericValueFromField(findViewById(R.id.text_real_soc), "%.1f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.DisplaySOC:
                    setNumericValueFromField(findViewById(R.id.text_disp_soc), "%.1f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.GroundResistance:
                    setNumericValueFromField(findViewById(R.id.text_ground), "%.0f", field);
                    if (field.getValue() > 0.5) {
                        mqttPusher.pushValue(field.getSID(), field.getValue());
                    }
                    break;

                case Sid.AvailableEnergy:
                    setNumericValueFromField(findViewById(R.id.text_energy), "%.2f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.HvKilometers:
                    setNumericValueFromField(findViewById(R.id.text_HKM), "%.2f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.TractionBatteryVoltage:
                    setNumericValueFromField(findViewById(R.id.text_volt), "%.2f", field);
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;

                case Sid.TestField1:
                    mqttPusher.pushValue(field.getSID(), field.getValue());
                    break;
            }
        });

    }

    private void setNumericValueFromField(TextView tv, String format, Field field) {
        if (tv != null) {
            tv.setText(String.format(Locale.getDefault(), format, field.getValue()));
        }
    }

    private String setTextValueFromField(TextView tv, String[] values, Field field) {
        int value = (int) field.getValue();
        String valueStr;
        if (tv != null && values != null && value >= 0 && value < values.length) {
            valueStr = values[value];
            tv.setText(valueStr);
        } else {
            valueStr = null;
        }
        return valueStr;
    }

}