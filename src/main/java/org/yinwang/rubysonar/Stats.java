package org.yinwang.rubysonar;

import java.util.HashMap;
import java.util.Map;


public class Stats {
    Map<String, Object> contents = new HashMap<>();


    public void putInt(String key, long value) {
        contents.put(key, value);
    }


    public void inc(String key, long x) {
        Long old = getInt(key);

        if (old == null) {
            contents.put(key, 1);
        } else {
            contents.put(key, old + x);
        }
    }


    public void inc(String key) {
        inc(key, 1);
    }


    public Long getInt(String key) {
        Long ret = (Long) contents.get(key);
        if (ret == null) {
            return 0L;
        } else {
            return ret;
        }
    }


    public String print() {
        StringBuilder sb = 