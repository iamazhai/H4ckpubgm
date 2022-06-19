package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.StrType;
import org.yinwang.rubysonar.types.Type;


public class Str extends Node {

    public String value;


    public Str(@NotNull Object value, String file, int start, int end) {
        super(file, start, end);
        this.value = value.toString();
    }


    @NotNull
    @Override
    public Type transform(State s) {
        retu