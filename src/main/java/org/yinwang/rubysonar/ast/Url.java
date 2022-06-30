
package org.yinwang.rubysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.State;
import org.yinwang.rubysonar.types.Type;


/**
 * virtual-AST node used to represent virtual source locations for builtins
 * as external urls.
 */
public class Url extends Node {

    private String url;

