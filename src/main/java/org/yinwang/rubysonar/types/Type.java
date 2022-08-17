package org.yinwang.rubysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.Analyzer;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.TypeStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class Type {

    @NotNull
    public State t