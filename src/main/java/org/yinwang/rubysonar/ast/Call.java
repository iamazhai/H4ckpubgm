package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.*;
import org.yinwang.rubysonar.types.*;

import java.util.*;

import static org.yinwang.rubysonar.Binding.Kind.SCOPE;


public class Call extends Node {

    public Node func;
    public List<Node> args;
    @Nullable
    public List<Keyword> keywords;
    public Node kwargs;
    publi