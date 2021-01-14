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
     */
    public boolean clearDiskCache() {
        try {
            _.deleteDirectory(new File(Analyzer.self.cacheDir));
            return true;
        } catch (Exception x) {
            LOG.log(Level.SEVERE, "Failed to clear disk cache: " + x);
            return false;
        }
    }


    public void close() {
        parser.close();
//        clearDiskCache();
    }


    /**
     * Returns the syntax tree for {@code path}.  May find and/or create a
     * cached copy in the mem cache or the disk cache.
     *
     * @param path absolute path to a source file
     * @return the AST, or {@code null} if the parse failed for any reason
     */
    @Nullable
    public Node getAST(@NotNull String path) {
        // Cache stores null value if the parse failed.
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        // Might be cached on disk but not in memory.
        Node node = getSerializedModule(path);
        if (node != null) {
            LOG.log(Level.FINE, "reusing " + path);
            cache.put(path, node);
            return node;
        }

        node = null;
        try {
            LOG.log(Level.FINE, "parsing " + path);
            node = parser.pars