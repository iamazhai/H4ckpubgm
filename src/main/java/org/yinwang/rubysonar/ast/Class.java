
package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.*;
import org.yinwang.rubysonar.types.ClassType;
import org.yinwang.rubysonar.types.Type;


public class Class extends Node {
    private static int classCounter = 0;

    @Nullable
    public Node locator;