package com.sonelli.juicessh.performancemonitor.helpers;

import java.util.regex.Pattern;

public interface TextFormatter {
    public String format(Pattern p, String line);
}
