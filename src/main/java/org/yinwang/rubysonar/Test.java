
package org.yinwang.rubysonar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.ast.Dummy;
import org.yinwang.rubysonar.ast.Node;

import java.io.File;
import java.util.*;

public class Test {

    static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Analyzer analyzer;
    String inputDir;
    boolean exp;
    String expecteRefsFile;
    String failedRefsFile;
    String extraRefsFile;


    public Test(String inputDir, boolean exp) {
        // make a quiet analyzer
        Map<String, Object> options = new HashMap<>();
//        options.put("quiet", true);
        this.analyzer = new Analyzer(options);

        this.inputDir = inputDir;
        this.exp = exp;
        if (new File(inputDir).isDirectory()) {
            expecteRefsFile = _.makePathString(inputDir, "refs.json");
            failedRefsFile = _.makePathString(inputDir, "failed_refs.json");
            extraRefsFile = _.makePathString(inputDir, "extra_refs.json");
        } else {
            expecteRefsFile = _.makePathString(inputDir + ".refs.json");
            failedRefsFile = _.makePathString(inputDir, ".failed_refs.json");
            extraRefsFile = _.makePathString(inputDir, ".extra_refs.json");
        }
    }


    public void runAnalysis(String dir) {
        analyzer.analyze(dir);
        analyzer.finish();
    }


    public void generateRefs() {