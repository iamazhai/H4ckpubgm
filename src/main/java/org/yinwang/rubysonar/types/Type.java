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
        return this instanceof IntType || this instanceof FloatType;
    }


    public boolean isStrType() {
        return this == STR;
    }


    public boolean isUnknownType() {
        return this == Type.UNKNOWN;
    }


    /**
     * Internal class to support printing in the presence of type-graph cycles.
     */
    protected class CyclicTypeRecorder {
        int count = 0;
        @NotNull
        private Map<Type, Integer> elements = new HashMap<>();
        @NotNull
        private Set<Type> used = new HashSet<>();


        public Integer push(Type t) {
            count += 1;
            elements.put(t, count);
            return count;
        }


        public void pop(Type t) {
            elements.remove(t);
           