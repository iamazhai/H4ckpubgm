package org.yinwang.rubysonar.types;

import org.yinwang.rubysonar.Analyzer;
import org.yinwang.rubysonar.State;


public class BoolType extends Type {

    public enum Value {
        True,
        False,
        Undecided
    }


    public Value value;
    public State s1;
    public State