package org.yinwang.rubysonar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class TypeStack {

    class Pair {
        public Object first;
        public Object second;


        public Pair(Object first, Object second) {
            this.first =