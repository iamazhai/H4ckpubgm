
package org.yinwang.rubysonar.demos;

import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.Analyzer;
import org.yinwang.rubysonar.Options;
import org.yinwang.rubysonar.Progress;
import org.yinwang.rubysonar._;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class Demo {

    private static File OUTPUT_DIR;

    private static final String CSS = _.readResource("org/yinwang/rubysonar/css/demo.css");
    private static final String JS = _.readResource("org/yinwang/rubysonar/javascript/highlight.js");
    private static final String JS_DEBUG = _.readResource("org/yinwang/rubysonar/javascript/highlight-debug.js");

    private Analyzer analyzer;
    private String rootPath;
    private Linker linker;


    private void makeOutputDir() {
        if (!OUTPUT_DIR.exists()) {
            OUTPUT_DIR.mkdirs();
            _.msg("Created directory: " + OUTPUT_DIR.getAbsolutePath());
        }
    }


    private void start(@NotNull String fileOrDir, Map<String, Object> options) throws Exception {
        File f = new File(fileOrDir);
        File rootDir = f.isFile() ? f.getParentFile() : f;
        try {
            rootPath = _.unifyPath(rootDir);
        } catch (Exception e) {
            _.die("File not found: " + f);
        }

        analyzer = new Analyzer(options);
        _.msg("Loading and analyzing files");
        analyzer.analyze(f.getPath());
        analyzer.finish();

        generateHtml();
        analyzer.close();
    }


    private void generateHtml() {
        _.msg("\nGenerating HTML");
        makeOutputDir();

        linker = new Linker(rootPath, OUTPUT_DIR);
        linker.findLinks(analyzer);

        int rootLength = rootPath.length();

        int total = 0;
        for (String path : analyzer.getLoadedFiles()) {
            if (path.startsWith(rootPath)) {
                total++;
            }
        }

        _.msg("\nWriting HTML");
        Progress progress = new Progress(total, 50);

        for (String path : analyzer.getLoadedFiles()) {
            if (path.startsWith(rootPath)) {
                progress.tick();
                File destFile = _.joinPath(OUTPUT_DIR, path.substring(rootLength));
                destFile.getParentFile().mkdirs();
                String destPath = destFile.getAbsolutePath() + ".html";
                String html = markup(path);
                try {
                    _.writeFile(destPath, html);
                } catch (Exception e) {
                    _.msg("Failed to write: " + destPath);
                }
            }
        }

        _.msg("\nWrote " + analyzer.getLoadedFiles().size() + " files to " + OUTPUT_DIR);
    }


    @NotNull
    private String markup(String path) {
        String source;

        try {
            source = _.readFile(path);
        } catch (Exception e) {
            _.die("Failed to read file: " + path);
            return "";
        }

        List<Style> styles = new ArrayList<>();
        styles.addAll(linker.getStyles(path));