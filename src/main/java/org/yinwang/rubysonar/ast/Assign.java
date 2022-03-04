package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.*;
import org.yinwang.rubysonar.types.ModuleType;
import org.yinwang.rubysonar.types.Type;


public class Assign extends Node {

    @NotNull
    public Node target;
    @NotNull
    public Node value;


    public Assign(@NotNull Node target, @NotNull Node value, String file, int start, int end) {
        super(file, start, end);
        this.target = target;
        this.value = value;
        addChildren(ta