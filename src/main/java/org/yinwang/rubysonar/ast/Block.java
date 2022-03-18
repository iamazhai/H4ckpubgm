package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.Analyzer;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.Type;
import org.yinwang.rubysonar.types.UnionType;

import java.util.List;


public class Block extends Node {

    @NotNull
    public List<Node> seq;


    public Block(@NotNull List<Node> seq, String file, int start, int end) {
        super(file, start, end);
        this.seq = seq;
        addChildren(seq);
    }


    @NotNull
    @Override
    public Type transform(@NotNull State stat