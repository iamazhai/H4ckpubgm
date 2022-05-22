
package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.Analyzer;
import org.yinwang.rubysonar.Binding;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar._;
import org.yinwang.rubysonar.types.ClassType;
import org.yinwang.rubysonar.types.FunType;
import org.yinwang.rubysonar.types.ModuleType;
import org.yinwang.rubysonar.types.Type;

import java.util.ArrayList;
import java.util.List;


public class Function extends Node {

    public Node locator;
    public Name name;
    public List<Node> args;
    public List<Node> defaults;
    public Name vararg;  // *args
    public Name kwarg;   // **kwarg
    public Name blockarg = null;   // block arg of Ruby
    public List<Node> afterRest = null;   // after rest arg of Ruby
    public Node body;
    public boolean called = false;
    public boolean isLamba = false;
    public Str docstring;


    public Function(Node locator, List<Node> args, Node body, List<Node> defaults,
                    Name vararg, Name kwarg, List<Node> afterRest, Name blockarg,
                    Str docstring, String file, int start, int end)
    {
        super(file, start, end);
        if (locator != null) {
            this.locator = locator;
        } else {
            isLamba = true;
            String fn = genLambdaName();
            this.locator = new Name(fn, file, -1, -1);
            addChildren(this.locator);
        }

        if (this.locator instanceof Attribute) {
            this.name = ((Attribute) this.locator).attr;
        } else if (this.locator instanceof Name) {
            this.name = (Name) this.locator;
        }

        this.args = args;
        this.body = body;
        this.defaults = defaults;
        this.vararg = vararg;
        this.kwarg = kwarg;
        this.afterRest = afterRest;
        this.blockarg = blockarg;
        this.docstring = docstring;
        addChildren(args);
        addChildren(defaults);
        addChildren(afterRest);
        addChildren(locator, body, vararg, kwarg, blockarg);
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        Type locType = null;
        if (locator instanceof Attribute) {
            locType = transformExpr(((Attribute) locator).target, s);
            if (!locType.isUnknownType()) {
                s = locType.table;
            }
        }

        FunType fun = new FunType(this, s);
        fun.table.setParent(s);
        fun.setDefaultTypes(resolveList(defaults, s));

        if (!isLamba) {
            Type outType = s.type;
            if (outType instanceof ClassType) {
                fun.setCls((ClassType) outType);
            }
        }

        if (locType instanceof ClassType || locType instanceof ModuleType || Analyzer.self.staticContext) {
            fun.setClassMethod(true);
            s.insertTagged(name.id, "class", name, fun, Binding.Kind.CLASS_METHOD);
            fun.table.setPath(s.extendPath(name.id, "."));
        } else {
            s.insert(name.id, name, fun, Binding.Kind.METHOD);
            fun.table.setPath(s.extendPath(name.id, "#"));
        }
        Analyzer.self.addUncalled(fun);
        return Type.CONT;
    }

