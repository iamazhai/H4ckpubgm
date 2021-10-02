
package org.yinwang.rubysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.ast.Attribute;
import org.yinwang.rubysonar.ast.Name;
import org.yinwang.rubysonar.ast.Node;
import org.yinwang.rubysonar.types.FunType;
import org.yinwang.rubysonar.types.ModuleType;
import org.yinwang.rubysonar.types.Type;
import org.yinwang.rubysonar.types.UnionType;

import java.util.*;


public class State {
    public enum StateType {
        CLASS,
        INSTANCE,
        FUNCTION,
        MODULE,
        GLOBAL,
        SCOPE
    }


    @NotNull
    public Map<String, List<Binding>> table = new HashMap<>();
    @Nullable
    public State parent;      // all are non-null except global table
    @Nullable
    public State supers;
    public StateType stateType;
    public Type type;
    @NotNull
    public String path = "";


    public State(@Nullable State parent, StateType type) {
        this.parent = parent;
        this.stateType = type;
    }


    public State(@NotNull State s) {
        this.table = new HashMap<>();
        this.table.putAll(s.table);
        this.parent = s.parent;
        this.stateType = s.stateType;
        this.supers = s.supers;
        this.type = s.type;
        this.path = s.path;
    }


    // erase and overwrite this to s's contents
    public void overwrite(@NotNull State s) {
        this.table = s.table;
        this.parent = s.parent;
        this.stateType = s.stateType;
        this.supers = s.supers;
        this.type = s.type;
        this.path = s.path;
    }


    @NotNull
    public State copy() {
        return new State(this);
    }


    public void merge(State other) {
        for (Map.Entry<String, List<Binding>> e1 : this.table.entrySet()) {
            List<Binding> b1 = e1.getValue();
            List<Binding> b2 = other.table.get(e1.getKey());

            // both branch have the same name, need merge
            if (b2 != null && b1 != b2) {
                b1.addAll(b2);
            }
        }

        for (Map.Entry<String, List<Binding>> e2 : other.table.entrySet()) {
            List<Binding> b1 = this.table.get(e2.getKey());
            List<Binding> b2 = e2.getValue();

            // both branch have the same name, need merge
            if (b1 == null && b1 != b2) {
                this.update(e2.getKey(), b2);
            }
        }
    }


    public static State merge(State state1, State state2) {
        State ret = state1.copy();
        ret.merge(state2);
        return ret;
    }


    public void setParent(@Nullable State parent) {
        this.parent = parent;
    }


    public void setSuper(State sup) {
        supers = sup;
    }


    public void setStateType(StateType type) {
        this.stateType = type;