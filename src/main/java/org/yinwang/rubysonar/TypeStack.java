package org.yinwang.rubysonar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class TypeStack {

    class Pair {
        public Object first;
        public Object second;


        public Pair(Object first, Object second) {
            this.first = first;
            this.second = second;
        }
    }


    @NotNull
    private List<Pair> stack = new ArrayList<>();


    public void push(Object first, Object second) {
        stack.add(new Pair(first, second));
    }


    public void pop(Object first, Object second) {
    