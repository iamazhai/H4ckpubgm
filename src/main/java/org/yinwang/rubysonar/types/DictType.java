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


 