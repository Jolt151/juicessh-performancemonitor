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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemperatureController extends BaseController {

    public static final String TAG = "TemperatureController";

    public TemperatureController(Context context) {
        super(context);
    }

    @Override
    public BaseController start() {
        super.start();

        // Work out the temperature of the device

        final List<TemperatureCommandModel> temperatureCommandModels = new ArrayList<>();

        // First, let do some service discovery

        new Handler().post(new Runnable() {
            @Override
            public void run() {

                try {
                    // Check first command
                    final Pattern vcgPattern = Pattern.compile("temp=(\\S+)");
                    final String vcgCommand = "/opt/vc/bin/vcgencmd measure_temp";

                    getPluginClient().executeCommandOnSession(getSessionId(), getSessionKey(), vcgCommand, new OnSessionExecuteListener() {
                        @Override
                        public void onCompleted(int exitCode) {
                            switch(exitCode){
                                case 127:
                                    Log.d(TAG, vcgCommand + " was not found on the server");
                            }
                        }
                        @Override
                        public void onOutputLine(String line) {
                            Matcher vcgMatcher = vcgPattern.matcher(line);
                            if(vcgMatcher.find()){
                                temperatureCommandModels.add(new TemperatureCommandModel(vcgCommand, vcgPattern, new TextFormatter() {
                                    @Override
                                    public String format(Pattern pattern, String line) {
                                        return pattern.matcher(line).group(1);
                                    }
                                }));
                            }
                        }

                        @Override
                        public void onError(int error, String reason) {
                            toast(reason);
                        }
                    });


                    // Check second command
                    final Pattern sysPattern = Pattern.compile("(\\d{5})");
                    final String sysCommand = "cat /sys/class/thermal/thermal_zone0/temp";

                    getPluginClient().executeCommandOnSession(getSessionId(), getSessionKey(), sysCommand, new OnSessionExecuteListener() {
                        @Override
                        public void onCompleted(int exitCode) {
                            switch(exitCode){
                                case 127:
                                    Log.d(TAG, "Tried to run a command but the command was not found on the server");
                            }
                        }
                        @Override
                        public void onOutputLine(String line) {
                            Matcher sysMatcher = sysPattern.matcher(line);
                            if (sysMatcher.find()) {
                                temperatureCommandModels.add(new TemperatureCommandModel(sysCommand, sysPattern, new TextFormatter() {
                                    @Override
                                    public String format(Pattern pattern, String line) {
                                        int intLine = Integer.parseInt(line);
                                        double doubleTemp = intLine / 1000;
                                        return doubleTemp + "°C";
                                    }
                                }));
                            }
                        }

                        @Override
                        public void onError(int error, String reason) {
                            toast(reason);
                        }
                    });

                } catch (ServiceNotConnectedException e) {
                    Log.d(TAG, "Tried to execute a command but could not connect to JuiceSSH plugin service");
                }

            }
        });


        // Read in from the available commands, and use the first available command to do the temperature check

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (isRunning() && temperatureCommandModels.isEmpty()){
                    handler.postDelayed(this, INTERVAL_SECONDS * 1000L);
                    return;
                }

                try {
                    final TemperatureCommandModel temperatureCommandModel = temperatureCommandModels.get(0);
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