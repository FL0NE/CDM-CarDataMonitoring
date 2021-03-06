package com.tsue.dsa.tsue.obd;

import android.app.Activity;
import android.media.MediaPlayer;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.anastr.speedviewlib.base.Speedometer;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.tsue.dsa.tsue.R;
import com.tsue.dsa.tsue.Setting;
import com.tsue.dsa.tsue.SettingsManager;
import com.tsue.dsa.tsue.model.ModeOptionValues;
import com.tsue.dsa.tsue.model.ModeOptions;
import com.tsue.dsa.tsue.model.Modes;
import com.tsue.dsa.tsue.model.Unit;
import com.tsue.dsa.tsue.utils.DataManager;
import com.tsue.dsa.tsue.utils.OnDataChangedListener;

import java.util.ArrayList;
import java.util.List;

/**
 * MyOBDCommand is a OBDCommand.
 * It can be used to send a Request via Bluetooth to a OBD Interface.
 * Created by abr
 */

public class MyOBDCommand extends ObdCommand {
    private final ProgressBar progressbarUpdate;
    private final Activity activity;
    private double resultA = 0;
    private double resultB = 0;
    private ModifierCalculator modifierCalculator;
    private TextView textViewUpdate;
    private ModeOptions option;
    private List<OnDataChangedListener> listener = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private Toast toast;
    Unit unit = null;

//    /**
//     * Constructs a OBDCommand.
//     * @param obdCommand the hexadecimal request, which will be send to the OBDDevice.
//     */
//    public MyOBDCommand(String obdCommand) {
//        super(obdCommand);
//    }

    /**
     * Constructs a OBDCommand.
     *
     * @param mode   the Mode of the request
     * @param option the mode option of the request
     */
    public MyOBDCommand(Activity activity, Modes mode, ModeOptions option, ModifierCalculator modifier, TextView textViewUpdate, ProgressBar progressbarUpdate) {
        super((mode.getValue()) + " " + option.getValue());
        this.modifierCalculator = modifier;
        this.textViewUpdate = textViewUpdate;
        this.option = option;
        this.progressbarUpdate = progressbarUpdate;
        this.activity = activity;
        mediaPlayer = MediaPlayer.create(activity, R.raw.warning2_3);

    }

    public MyOBDCommand(Activity activity, Modes mode, ModeOptions option, ModifierCalculator modifier, TextView textView) {
        this(activity, mode, option, modifier, textView, null);
    }

//    /**
//     * Constructs a OBDCommand.
//     * @param mode the Mode of the request
//     * @param option the mode option of the request
//     * @param unit the Unit of the return value. Will be added to the formatted result.
//     */
//    public MyOBDCommand(Modes mode, ModeOptions option, Unit unit) {
//        super((mode.getValue())+" "+ option.getValue());
//        this.unit = unit;
//    }

    @Override
    protected void performCalculations() {
        resultA = buffer.get(2);
        resultB = buffer.size() >= 3 ? buffer.get(3) : 0;
    }

    @Override
    public String getFormattedResult() {
        if (unit != null) {
            return resultA + unit.getSymbol();
        }
        return resultA + "";
    }

    @Override
    public String getCalculatedResult() {
        return Math.round(modifierCalculator.calculateModifier(resultA, resultB)) + "";
    }

    private int getCalculatedPercentage() {
        double modifier = modifierCalculator.calculateModifier(resultA, resultB);
        double finalResult = 0;
        Integer currentValue = 1;
        if (modifier != 0.0) {
//            int resultInt = Integer.valueOf(resultA);
//            double resultModified = resultInt * modifier;
//            double rounded = Math.round(resultModified);
//            int roundedToInt = (int) rounded;
            finalResult = modifier;


            for (ModeOptionValues value : ModeOptionValues.values()) {
                if (value.getOption() == option) {
                    currentValue = value.getValue();
                }
            }
        }
        return (int) ((finalResult / currentValue) * 100);
    }

    @Override
    public String getName() {
        return AvailableCommandNames.SPEED.getValue();
    }

    /**
     * Sets the return value of the getCalculatedResult to the text view, given in the constructor.
     */
    public void updateUI() {
        if(toast != null) {
            toast.cancel();
        }

//        if (mediaPlayer.isPlaying()) {
//            mediaPlayer.stop();
//        }
        try {
            if (option == ModeOptions.DISTANCE || option == ModeOptions.SPEED || option == ModeOptions.RPM || option == ModeOptions.COOLANT_TEMP || option == ModeOptions.TANK || option == ModeOptions.ENGINE_LOAD || option == ModeOptions.AMBIENT_TEMP || option == ModeOptions.THROTTLE_POS || option == ModeOptions.COOLANT_TEMP) {

                String value = getCalculatedResult();
                if (value == null || value.isEmpty()) {
                    return;
                }
                for (OnDataChangedListener currentListener : listener) {
                    currentListener.engineLoadChanged(12.00);
                }
                int percentage = getCalculatedPercentage();
                Setting setting = SettingsManager.getSetting();
                double fuel = setting.getFuel();
                double engineLoad = setting.getEngineLoad();
                double temp = setting.getEngineTemp();
                double speed = setting.getSpeed();
                if (option == ModeOptions.SPEED && Integer.parseInt(value) > speed) {
                    if (setting.isEnableSound()) {
                        mediaPlayer.start();
                    }
                }
                boolean enablesound = setting.isEnableSound();
                int intvalue = Integer.parseInt(value);
                if (option == ModeOptions.SPEED) {
                    DataManager.onSpeedChanged(Double.valueOf(value));
                }
                if (option == ModeOptions.TANK && Integer.parseInt(value) < setting.getFuel()) {
                    playSound(setting.isEnableSound());
                }
                if (option == ModeOptions.ENGINE_LOAD &&  intvalue > setting.getEngineLoad()) {
                    playSound(enablesound);
                }
                if (option == ModeOptions.COOLANT_TEMP &&  intvalue > setting.getEngineTemp()) {
                    playSound(enablesound);
                }
                if(option == ModeOptions.SPEED && intvalue > setting.getSpeed()) {
                    playSound(enablesound);
                }

                textViewUpdate.setText(value);
                if (option == ModeOptions.THROTTLE_POS || option == ModeOptions.SPEED || option == ModeOptions.RPM || option == ModeOptions.COOLANT_TEMP || option == ModeOptions.TANK || option == ModeOptions.ENGINE_LOAD) {
                    progressbarUpdate.setProgress(percentage);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playSound(boolean enable) {
        toast = Toast.makeText(activity,"Achtung ! Ein maximalweer wurde überschritten "+option.toString(),Toast.LENGTH_SHORT);
        toast.show();
        if (!enable) {
            return;
        }
        mediaPlayer.start();
    }
}
