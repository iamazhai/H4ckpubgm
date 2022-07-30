package org.yinwang.rubysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.State;


public class ClassType extends Type {

    public String name;
    public InstanceType canon;
    public Type superclass;


    public ClassType(@NotNull String name, @Nullable State parent) {
        this.name = name;
        this.setTable(new State(parent, State.StateType.CLASS));
        table.setType(this);
        if (parent != null) {
            table.setPath(parent.extendPath(name, "::"));
        } else {
            table.setPath(name);
        }
    }


    public ClassType(@NotNull St