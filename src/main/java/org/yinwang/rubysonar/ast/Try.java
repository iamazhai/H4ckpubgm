package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.Type;
import org.yinwang.rubysonar.types.UnionType;


public class Try extends Node {

    public Node rescue;
    public Node body;
    public Node orelse;
    public Node finalbody;


    public Try(Node rescue, Node body, Node orelse, Node finalbody,
               String file, int start, int end)
    {
        super(file, start, end);
        this.rescue = rescue;
        this.body = body;
        this.orelse = orelse;
        this.finalbody = finalbody;
        addChildren(rescue);
        addChildren(body, orelse);
    }

