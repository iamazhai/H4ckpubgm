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
    public State s2;


    public BoolType(Value value) {
        this.value = value;
    }


    public BoolType(State s1, State s2) {
        this.value = Value.Undecided;
        this.s1 = s1;
        this.s2 = s2;
    }


    public void setValue(Value value) {
        this.value = value;
    }


    public void setS1(State s1) {
        this.s1 = s1;
    }


    public void setS2(State s2) {
        this.s2 = s2;
    }


    public BoolType 