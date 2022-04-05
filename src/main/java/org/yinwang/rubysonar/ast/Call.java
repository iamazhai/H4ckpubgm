package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.*;
import org.yinwang.rubysonar.types.*;

import java.util.*;

import static org.yinwang.rubysonar.Binding.Kind.SCOPE;


public class Call extends Node {

    public Node func;
    public List<Node> args;
    @Nullable
    public List<Keyword> keywords;
    public Node kwargs;
    public Node starargs;
    public Node blockarg = null;


    public Call(Node func, List<Node> args, @Nullable List<Keyword> keywords,
                Node kwargs, Node starargs, Node blockarg, String file, int start, int end)
    {
        super(file, start, end);
        this.func = func;
        this.args = args;
        this.keywords = keywords;
        this.kwargs = kwargs;
        this.starargs = starargs;
        this.blockarg = blockarg;
        addChildren(func, kwargs, starargs, blockarg);
        addChildren(args);
        addChildren(keywords);
    }


    /**
     * Most of the work here is done by the static method invoke, which is also
     * used by Analyzer.applyUncalled. By using a static method we avoid building
     * a NCall node for those dummy calls.
     */
    @NotNull
    @Override
    public Type transform(State s) {
        if (func instanceof Name) {
            Name fn = (Name) func;

            // handle 'require' and 'load'
            if (fn.id.equals("require") || fn.id.equals("load")) {
                if (args != null && args.size() > 0) {
                    Node arg1 = args.get(0);
                    if (arg1 instanceof Str) {
                        Analyzer.self.requireFile(((Str) arg1).value);
                        return Type.TRUE;
                    }
                }
                Analyzer.self.putProblem(this, "failed to require file");
                return Type.FALSE;
            }

            // handle 'include'
            if (fn.id.equals("include") || fn.id.equals("extend")) {
                if (args != null && args.size() > 0) {
                    Node arg1 = args.get(0);
                    Type mod = transformExpr(arg1, s);
                    s.putAll(mod.table);
                    return Type.TRUE;
                }
                Analyzer.self.putProblem(this, "failed to include module");
            }

            if (fn.id.equals("module_function")) {
                Analyzer.self.setStaticContext(true);
                return Type.CONT;
            }

            if (fn.id.equals("attr_accessor")) {
                return Type.CONT;
            }
        }

        // Class.new
        Name newName = null;
        if (func instanceof Attribute) {
            Attribute afun = (Attribute) func;
            if (afun.attr.id.equals("new")) {
                func = afun.target;
                newName = afun.attr;
            } else if (afun.attr.id.equals("class")) {
                if (afun.target != null) {
                    Type inst = afun.target.transform(s);
                    if (inst instanceof InstanceType) {
                        return ((InstanceType) inst).classType;
                    } else {
                        return Type.UNKNOWN;
                    }
                } else {
                    return Type.UNKNOWN;
                }
            }
        }

        Type fun = transformExpr(func, s);
        List<Type> pos = resolveList(args, s);
        Map<String, Type> hash = new HashMap<>();

        if (keywords != null) {
            for (Keyword kw : keywords) {
                hash.put(kw.getArg(), transformExpr(kw.getValue(), s));
            }
        }

        Type kw = kwargs == null ? null : transformExpr(kwargs, s);
        Type star = starargs == null ? null : transformExpr(starargs, s);
        Type block = blockarg == null ? null : transformExpr(blockarg, s);

        if (fun instanceof UnionType) {
            Set<Type> types = ((UnionType) fun).types;
            Type retType = Type.UNKNOWN;
            for (Type ft : types) {
                Type t = resolveCall(ft, newName, pos, hash, kw, star, block, s);
           