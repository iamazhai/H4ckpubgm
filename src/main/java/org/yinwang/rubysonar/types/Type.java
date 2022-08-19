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
    public State table = new State(Analyzer.self.globaltable, State.StateType.INSTANCE);
    public String file = null;
    public boolean mutated = false;


    @NotNull
    protected static TypeStack typeStack = new TypeStack();


    public Type() {
    }


    public void setTable(@NotNull State table) {
        this.table = table;
    }


    public void setFile(String file) {
        this.file = file;
    }


    public boolean isMutated() {
        return mutated;
    }


    public void setMutated(boolean mutated) {
        this.mutated = mutated;
    }


    public boolean isUndecidedBool() {
        return this instanceof BoolType && ((BoolType) this).value == BoolType.Value.Undecided &&
                ((BoolType) this).s1 != null && ((BoolType) this).s2 != null;
    }


    public boolean isNumType() {
        return 