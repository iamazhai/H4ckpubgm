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
        this.file = file;
        this.start = start;
        this.end = end;
    }


    public void setParent(Node parent) {

        this.parent = parent;
    }


    @NotNull
    public Node getAstRoot() {
        if (parent == null) {
            return this;
        }
        return parent.getAstRoot();
    }


    public void addChildren(@Nullable Node... nodes) {
        if (nodes != null) {
            for (Node n : nodes) {
                if (n != null) {
                    n.setParent(this);
                }
            }
        }
    }


    public void addChildren(@Nullable Collection<?