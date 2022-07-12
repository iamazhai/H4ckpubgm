
package org.yinwang.rubysonar.demos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.*;
import org.yinwang.rubysonar.ast.Node;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;


/**
 * Collects per-file hyperlinks, as well as styles that require the
 * symbol table to resolve properly.
 */
class Linker {

    private static final Pattern CONSTANT = Pattern.compile("[A-Z_][A-Z0-9_]*");

    // Map of file-path to semantic styles & links for that path.
    @NotNull
    private Map<String, List<Style>> fileStyles = new HashMap<>();

    private File outDir;  // where we're generating the output html
    private String rootPath;

    // prevent duplication in def and ref links
    Set<Object> seenDef = new HashSet<>();
    Set<Object> seenRef = new HashSet<>();


    /**
     * Constructor.
     *
     * @param root   the root of the directory tree being indexed
     * @param outdir the html output directory
     */
    public Linker(String root, File outdir) {
        rootPath = root;
        outDir = outdir;
    }


    public void findLinks(@NotNull Analyzer analyzer) {
        _.msg("Adding xref links");
        Progress progress = new Progress(analyzer.getAllBindings().size(), 50);

        int nMethods = 0;
        int nFunc = 0;
        int nClass = 0;
        for (Binding b : analyzer.getAllBindings()) {
            if (b.kind == Binding.Kind.METHOD) {
                nMethods++;
            }
            if (b.kind == Binding.Kind.CLASS) {
                nClass++;
            }

            if (Analyzer.self.hasOption("debug")) {
                processDefDebug(b);
            } else {
                processDef(b);
            }
            progress.tick();
        }

        _.msg("found: " + nMethods + " methods, " + nFunc + " funcs, " + nClass + " classes");

        // highlight definitions
        _.msg("\nAdding ref links");
        progress = new Progress(analyzer.getReferences().size(), 50);

        for (Entry<Node, List<Binding>> e : analyzer.getReferences().entrySet()) {
            if (Analyzer.self.hasOption("debug")) {
                processRefDebug(e.getKey(), e.getValue());
            } else {
                processRef(e.getKey(), e.getValue());
            }
            progress.tick();
        }

        if (Analyzer.self.hasOption("semantic-errors")) {
            for (List<Diagnostic> ld : analyzer.semanticErrors.values()) {
                for (Diagnostic d : ld) {
                    processDiagnostic(d);
                }
            }
        }
    }


    private void processDef(@NotNull Binding binding) {
        String qname = binding.qname;
        int hash = binding.hashCode();

        if (binding.start < 0 || seenDef.contains(hash)) {
            return;
        }

        seenDef.add(hash);
        Style style = new Style(Style.Type.ANCHOR, binding.start, binding.end);
        style.message = binding.type.toString();
        style.url = binding.qname;
        style.id = qname;
        addFileStyle(binding.file, style);
    }


    private void processDefDebug(@NotNull Binding binding) {
        int hash = binding.hashCode();

        if (binding.start < 0 || seenDef.contains(hash)) {
            return;