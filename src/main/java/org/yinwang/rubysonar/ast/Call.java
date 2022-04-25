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
                retType = UnionType.union(retType, t);
            }
            return retType;
        } else {
            return resolveCall(fun, newName, pos, hash, kw, star, block, s);
        }
    }


    @NotNull
    private Type resolveCall(@NotNull Type fun,
                             Name newName,
                             List<Type> pos,
                             Map<String, Type> hash,
                             Type kw,
                             Type star,
                             Type block,
                             State s)
    {
        if (fun instanceof FunType) {
            FunType ft = (FunType) fun;
            return apply(ft, pos, hash, kw, star, block, this);
        } else if (fun instanceof ClassType) {
            // constructor
            InstanceType inst = new InstanceType(fun, newName, this, pos);
            ((ClassType) fun).setCanon(inst);

            if (!isSuperCall()) {
                return inst;
            } else {
                Type selfType = s.lookupType(Constants.INSTNAME);
                if (selfType != null) {
                    selfType.table.putAll(inst.table);
                }
                return Type.CONT;
            }
        } else {
            addWarning("calling non-function and non-class: " + fun);
            return Type.UNKNOWN;
        }
    }


    @NotNull
    public static Type apply(@NotNull FunType func,
                             @Nullable List<Type> pos,
                             Map<String, Type> hash,
                             Type kw,
                             Type star,
                             Type block,
                             @Nullable Node call)
    {
        Analyzer.self.removeUncalled(func);

        if (func.func != null && !func.func.called) {
            Analyzer.self.nCalled++;
            func.func.called = true;
        }

        if (func.func == null) {
            // func without definition (possibly builtins)
            return func.getReturnType();
        } else if (call != null && Analyzer.self.inStack(call)) {
            func.setSelfType(null);
            return Type.UNKNOWN;
        }

        if (call != null) {
            Analyzer.self.pushStack(call);
        }

        List<Type> pTypes = new ArrayList<>();

        if (pos != null) {
            pTypes.addAll(pos);
        }

        State funcTable = new State(func.env, State.StateType.FUNCTION);

        if (!func.table.path.isEmpty()) {
            funcTable.path = func.table.path;
        } else if (func.table.parent != null) {
            funcTable.setPath(func.table.parent.extendPath(func.func.name.id, "#"));
        } else {
            funcTable.setPath(func.func.name.id);
        }

        // bind a special this name to the table
        if (func.selfType != null) {
            Binder.bind(funcTable, new Name(Constants.INSTNAME), func.selfType, SCOPE);
        } else if (func.cls != null) {
            Binder.bind(funcTable, new Name(Constants.INSTNAME), func.cls.getCanon(), SCOPE);
        }

        Type fromType = bindParams(call, func.func, funcTable, func.func.args,
                func.func.vararg, func.func.kwarg,
                pTypes, func.defaultTypes, hash, kw, star, block);

        Type cachedTo = func.getMapping(fromType);
        if (cachedTo != null && !(call != null && (call instanceof Call) && ((Call) call).isSuperCall())) {
            func.setSelfType(null);
            return cachedTo;
        } else {
            Type toType;
            if (func.isClassMethod) {
                boolean wasStatic = Analyzer.self.staticContext;
                Analyzer.self.setStaticContext(true);
                toType = transformExpr(func.func.body, funcTable);
                Analyzer.self.setStaticContext(wasStatic);
            } else {
                toType = transformExpr(func.func.body, funcTable);
            }

            if (missingReturn(toType)) {
                Analyzer.self.putProblem(func.func.locator, "Function not always return a value");

                if (call != null) {
                    Analyzer.self.putProblem(call, "Call not always return a value");
                }
            }

            func.addMapping(fromType, toType);
            func.setSelfType(null);
            return toType;
        }
    }


    @NotNull
    static private Type bindParams(@Nullable Node call,
                                   @NotNull Function func,
                                   @NotNull State funcTable,
                                   @Nullable List<Node> args,
                                   @Nullable Name rest,
                                   @Nullable Name restKw,
                                   @Nullable List<Type> pTypes,
                                   @Nullable List<Type> dTypes,
                                   @Nullable Map<String, Type> hash,
                                   @Nullable Type kw,
                                   @Nullable Type star,
                                   @Nullable Type block)
    {
        TupleType fromType = new TupleType();
        int pSize = args == null ? 0 : args.size();
        int aSize = pTypes == null ? 0 : pTypes.size();
        int dSize = dTypes == null ? 0 : dTypes.size();
        int nPos = pSize - dSi