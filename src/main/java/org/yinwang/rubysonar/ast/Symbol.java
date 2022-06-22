package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.SymbolType;
import org.yinwang.rubysonar.types.Type;


public class Symbol extends Node {

    @NotNull
    public final String id;  // identifier


    public Symbol(@NotNull String id, String file, int start, int end) {
        super(file, start, end);
        this.id = id;
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {