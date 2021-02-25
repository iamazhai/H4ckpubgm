
package org.yinwang.rubysonar;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.ast.*;
import org.yinwang.rubysonar.types.*;

import java.util.List;


/**
 * Handles binding names to scopes, including destructuring assignment.
 */
public class Binder {

    public static void bind(@NotNull State s, Node target, @NotNull Type rvalue, Binding.Kind kind) {
        if (target instanceof Name) {
            bind(s, (Name) target, rvalue, kind);
        } else if (target instanceof Array) {
            bind(s, ((Array) target).elts, rvalue, kind);
        } else if (target instanceof Attribute) {
            ((Attribute) target).setAttr(s, rvalue);
        } else if (target instanceof Subscript) {
            Subscript sub = (Subscript) target;
            Type valueType = Node.transformExpr(sub.value, s);
            if (sub.slice != null) {
                Node.transformExpr(sub.slice, s);
            }
            if (valueType instanceof ListType) {
                ListType t = (ListType) valueType;
                t.setElementType(UnionType.union(t.eltType, rvalue));
            }
        } else if (target != null) {
            Analyzer.self.putProblem(target, "invalid location for assignment");
        }
    }


    /**
     * Without specifying a kind, bind determines the kind according to the type
     * of the scope.
     */
    public static void bind(@NotNull State s, Node target, @NotNull Type rvalue) {
        Binding.Kind kind;
        if (s.getStateType() == State.StateType.FUNCTION) {
            kind = Binding.Kind.VARIABLE;
        } else if (s.stateType == State.StateType.CLASS ||
                s.stateType == State.StateType.INSTANCE) {
          kind = Binding.Kind.ATTRIBUTE;
        } else {
            kind = Binding.Kind.SCOPE;
        }
        bind(s, target, rvalue, kind);
    }


    public static void bind(@NotNull State s, @NotNull List<Node> xs, @NotNull Type rvalue, Binding.Kind kind) {
        if (rvalue instanceof TupleType) {
            List<Type> vs = ((TupleType) rvalue).eltTypes;
            if (xs.size() != vs.size()) {
                reportUnpackMismatch(xs, vs.size());
            } else {
                for (int i = 0; i < xs.size(); i++) {
                    bind(s, xs.get(i), vs.get(i), kind);
                }
            }
        } else {
            if (rvalue instanceof ListType) {
                bind(s, xs, ((ListType) rvalue).toTupleType(xs.size()), kind);
            } else if (rvalue instanceof DictType) {
                bind(s, xs, ((DictType) rvalue).toTupleType(xs.size()), kind);
            } else if (rvalue.isUnknownType()) {
                for (Node x : xs) {
                    bind(s, x, Type.UNKNOWN, kind);
                }
            } else if (xs.size() > 0) {
                Analyzer.self.putProblem(xs.get(0).file,
                        xs.get(0).start,
                        xs.get(xs.size() - 1).end,
                        "unpacking non-iterable: " + rvalue);
            }
        }