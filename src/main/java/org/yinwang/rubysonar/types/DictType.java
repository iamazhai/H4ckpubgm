package org.yinwang.rubysonar.types;

import org.jetbrains.annotations.NotNull;


public class DictType extends Type {

    public Type keyType;
    public Type valueType;


    public DictType(Type key0, Type val0) {
        keyType = key0;
        valueType = val0;
    }


    public void add(@NotNull Type key, @NotNull Type val) {
        keyType = UnionType.union(keyType, key);
        valueType = UnionType.union(valueType, val);
    }


    @NotNull
    public TupleType toTupleType(int n) {
        TupleType ret = new TupleType();
        for (int i = 0; i < n; i++) {
            ret.add(keyType);
        }
        return ret;
    }


    @Override
    public boolean equals(Object other) {
        if (typeStack.contains(this, other)) {
            return true;
        } else if (other instanceof DictType) {
            typeStack.push(this, other);
            DictType co = (DictType) other;
            boolean ret = (co.keyType.equals