package org.yinwang.rubysonar;

import org.jetbrains.annotations.NotNull;


public class Diagnostic {
    public enum Category {
        INFO, WARNING, ERROR
    }


    public String file;
    public Catego