package org.yinwang.rubysonar;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Options {

    private Map<String, Object> optionsMap = new LinkedHashMap<>();


    private List<String> args = new ArrayList<>();


    public Options(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (key.startsWith("--")) {
                if (i + 1 >= args.length) {
                    _.die("option needs a value: " + key);
                } else {
                    key = key.substring(2);
                    String value = args[i + 1];
                    if (!value.start