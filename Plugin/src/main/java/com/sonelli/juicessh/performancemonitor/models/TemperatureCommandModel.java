package com.sonelli.juicessh.performancemonitor.models;

import com.sonelli.juicessh.performancemonitor.helpers.TextFormatter;

import java.util.regex.Pattern;

public class TemperatureCommandModel {
    private String command;
    private Pattern pattern;
    private TextFormatter textFormatter;

    public TemperatureCommandModel(String command, Pattern pattern, TextFormatter textFormatter) {
        this.command = command;
        this.pattern = pattern;
        this.textFormatter = textFormatter;
    }

    public String getCommand() {
        return command;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public TextFormatter getTextFormatter() {
        return textFormatter;
    }

}
