package org.yinwang.rubysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.ast.RbModule;
import org.yinwang.rubysonar.ast.Node;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Provides a factory for ruby source ASTs.  Maintains configurable on-disk and
 * in-memory caches to avoid re-parsing files during analysis.
 */
public class AstCache {

    private static final Logger LOG = Logger.getLogger(AstCache.class.getCanonicalName());

    private static AstCache INSTANCE;

    @NotNull
    private Map<String, Node> cache = new HashMap<>();
    @NotNull
    private static Parser parser;


    private AstCache() {
    }


    public static AstCache get() {
        if (INSTANCE == null) {
            INSTANCE = new AstCache();
        }
        parser = new Parser();
        return INSTANCE;
    }


    /**
     * Clears the memory cache.
     */
    public void clear() {
        cache.clear();
    }


    /**
     * Removes all serialized ASTs from the on-disk cache.
     *
     * @return {@code true} if all cached AST files were removed