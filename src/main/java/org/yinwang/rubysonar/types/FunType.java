
package org.yinwang.rubysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.Analyzer;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.TypeStack;
import org.yinwang.rubysonar.ast.Function;

import java.util.*;


public class FunType extends Type {

    @NotNull
    public Map<Type, Type> arrows = new HashMap<>();
    public Function func;
    @Nullable
    public ClassType cls = null;
    public State env;
    @Nullable
    public Type selfType;                 // self's type for calls
    public List<Type> defaultTypes;       // types for default parameters (evaluated at def time)
    public boolean isClassMethod = false;


    public FunType() {
    }


    public FunType(Function func, State env) {
        this.func = func;
        this.env = env;
    }


    public FunType(Type from, Type to) {
        addMapping(from, to);
    }


    public void addMapping(Type from, Type to) {
        if (arrows.size() < 5) {
            arrows.put(from, to);
            Map<Type, Type> oldArrows = arrows;
            arrows = compressArrows(arrows);

            if (toString().length() > 900) {
                arrows = oldArrows;
            }
        }
    }


    @Nullable
    public Type getMapping(@NotNull Type from) {
        return arrows.get(from);
    }


    public Type getReturnType() {
        if (!arrows.isEmpty()) {
            return arrows.values().iterator().next();
        } else {
            return Type.UNKNOWN;
        }
    }


    public void setCls(ClassType cls) {
        this.cls = cls;
    }


    public void setSelfType(Type selfType) {
        this.selfType = selfType;
    }


    public void setDefaultTypes(List<Type> defaultTypes) {
        this.defaultTypes = defaultTypes;
    }


    public void setClassMethod(boolean isClassMethod) {
        this.isClassMethod = isClassMethod;
    }


    @Override
    public boolean equals(Object other) {
        if (other instanceof FunType) {
            return func.equals(((FunType) other).func);
        } else {