package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.Type;


public class RbFloat extends Node {

    public double value;


    public RbFloat(String s, String file, int start, int end) {
        super(file, start, e