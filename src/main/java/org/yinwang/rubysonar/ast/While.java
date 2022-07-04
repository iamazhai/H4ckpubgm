package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.Type;
import org.yinwang.rubysonar.types.UnionType;


public class While extends Node {

    public Node test;
    public Node body;
    public Node orelse;


    public While(Node test, Node