
package org.yinwang.rubysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.ast.Class;
import org.yinwang.rubysonar.ast.RbModule;
import org.yinwang.rubysonar.ast.*;
import org.yinwang.rubysonar.types.ModuleType;
import org.yinwang.rubysonar.types.Type;

import java.util.LinkedHashSet;
import java.util.Set;


public class Binding implements Comparable<Object> {

    public enum Kind {
        MODULE,       // file
        CLASS,        // class definition
        METHOD,       // instance method
        CLASS_METHOD,       // class method
        ATTRIBUTE,    // attr accessed with "." on some other object
        PARAMETER,    // function param
        SCOPE,        // top-level variable ("scope" means we assume it can have attrs)
        VARIABLE,      // local variable
        CONSTANT,
    }


    @NotNull
    public Node node;
    @NotNull
    public String qname;    // qualified name
    public Type type;       // inferred type
    public Kind kind;        // name usage context

    public Set<Node> refs;

    public int start = -1;
    public int end = -1;
    public int bodyStart = -1;
    public int bodyEnd = -1;

    @Nullable
    public String file;


    public Binding(@NotNull Node node, @NotNull Type type, @NotNull Kind kind) {
        this.qname = type.table.path;