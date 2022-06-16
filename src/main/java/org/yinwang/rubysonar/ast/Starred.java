package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.Type;


public class Starred extends Node {

    public Node value;


    public Starred(Node n, String file, int start, int end) {
        super(file, start