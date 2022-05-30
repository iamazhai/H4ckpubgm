package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.Analyzer;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar._;
import org.yinwang.rubysonar.types.Type;
import org.yinwang.rubysonar.types.UnionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public abstract class Node implements java.io.Serializable {

    public String file;
    public int start = -1;
    public int end = -1;
    public String name;
    public String path;
    public Node parent = null;


    public Node() {
    }


    public Node(String file, int start, int end) {
        this.fil