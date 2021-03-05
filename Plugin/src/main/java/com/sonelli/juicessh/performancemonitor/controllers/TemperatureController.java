package com.sonelli.juicessh.performancemonitor.controllers;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.sonelli.juicessh.performancemonitor.R;
import com.sonelli.juicessh.performancemonitor.helpers.TextFormatter;
import com.sonelli.juicessh.performancemonitor.models.TemperatureCommandModel;
import com.sonelli.juicessh.pluginlibrary.exceptions.ServiceNotConnectedException;
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemperatureController extends BaseController {

    public static final String TAG = "TemperatureController";

    final List<TemperatureCommandModel> temperatureCommandModels = Arrays.asList(
            new TemperatureCommandModel("/opt/vc/bin/vcgencmd measure_temp",
                    Pattern.compile("temp=(\\S+)"),
                    new TextFormatter() {
                        @Override
                        public String format(Pattern p, String line) {
                            return p.matcher(line).group(1);
                        }
                    }),
            new TemperatureCommandModel("cat /sys/class/thermal/thermal_zone0/temp",
                    Pattern.compile("(\\d{5})"),
                    new TextFormatter() {
                        @Override
                        public String format(Pattern p, String line) {
                            try {
                                int intLine = Integer.parseInt(line);
                                double doubleTemp = intLine / 1000;
                                return doubleTemp + "°C";
                            } catch (Exception e) { return null; }
                        }
                    })
    );

    List<TemperatureCommandModel> validTemperatureCommandModels = new ArrayList<>();

    public TemperatureController(Context context) {
        super(context);
    }

    @Override
    public BaseController start() {
        super.start();

        // Work out the temperature of the device

        // First, let do some service discovery
        // Check which of the commands in temperatureCommandModels is valid

        new Handler().post(new Runnable() {
            @Override
            public void run() {

                for (final TemperatureCommandModel t : temperatureCommandModels) {
                    try {
                        getPluginClient().executeCommandOnSession(getSessionId(), getSessionKey(), t.getCommand(), new OnSessionExecuteListener() {
                            @Override
                            public void onCompleted(int exitCode) {
                                switch(exitCode){
                                    case 127:
                                        Log.d(TAG, t.getCommand() + " was not found on the server");
                                }
                            }

                            @Override
                            public void onOutputLine(String s) {
                                Matcher isValidChecker = t.getPattern().matcher(s);
                                if (isValidChecker.find()) {
                                    // This is a valid command - add to our list of valid commands
                                    validTemperatureCommandModels.add(t);
                                }
                            }

                            @Override
                            public void onError(int i, String reason) {
                                toast(reason);
                            }
                        });
                    } catch (ServiceNotConnectedException e) {
                        Log.e(TAG, "Tried to execute a command but could not connect to JuiceSSH plugin service");
                    }
                }
            }

        });


        // Read in from the available commands, and use the first available command to do the temperature check

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (isRunning() && validTemperatureCommandModels.isEmpty()){
                    handler.postDelayed(this, INTERVAL_SECONDS * 1000L);
                    return;
                }

                try {
                    final TemperatureCommandModel temperatureCommandModel = validTemperatureCommandModels.get(0);
                    getPluginClient().executeCommandOnSession(getSessionId(), getSessionKey(), temperatureCommandModel.getCommand(), new OnSessionExecuteListener() {
                        @Override
                        public void onCompleted(int exitCode) {
                            switch(exitCode){
                                case 127:
                                    setText(getString(R.string.error));
                                    Log.d(TAG, "Tried to run a command but the command was not found on the server");
                                    break;
                            }
                        }
                        @Override
                        public void onOutputLine(String line) {
                            setText(temperatureCommandModel.getTextFormatter().format(temperatureCommandModel.getPattern(), line));
                        }

                        @Override
                        public void onError(int error, String reason) {
                            toast(reason);
                        }
                    });
                } catch (ServiceNotConnectedException e){
                    Log.d(TAG, "Tried to execute a command but could not connect to JuiceSSH plugin service");
                }

                if(isRunning()){
                    handler.postDelayed(this, INTERVAL_SECONDS * 1000L);
                }
            }


        });

        return this;
    }
}