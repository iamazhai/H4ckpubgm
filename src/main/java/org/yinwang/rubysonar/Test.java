
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

        List<Map<String, Object>> refs = new ArrayList<>();
        for (Map.Entry<Node, List<Binding>> e : analyzer.references.entrySet()) {

            String file = e.getKey().file;

            // only record those in the inputDir
            if (file != null && file.startsWith(Analyzer.self.projectDir)) {
                file = _.projRelPath(file);
                Map<String, Object> writeout = new LinkedHashMap<>();

                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("name", e.getKey().name);
                ref.put("file", file);
                ref.put("start", e.getKey().start);
                ref.put("end", e.getKey().end);

                List<Map<String, Object>> dests = new ArrayList<>();
                for (Binding b : e.getValue()) {
                    String destFile = b.file;
                    if (destFile != null && destFile.startsWith(Analyzer.self.projectDir)) {
                        destFile = _.projRelPath(destFile);
                        Map<String, Object> dest = new LinkedHashMap<>();
                        dest.put("name", b.node.name);
                        dest.put("file", destFile);
                        dest.put("start", b.start);
                        dest.put("end", b.end);
                        dests.add(dest);
                    }
                }
                if (!dests.isEmpty()) {
                    writeout.put("ref", ref);
                    writeout.put("dests", dests);
                    refs.add(writeout);
                }
            }
        }

        String json = gson.toJson(refs);
        _.writeFile(expecteRefsFile, json);
    }


    public boolean checkRefs() {
        List<Map<String, Object>> failedRefs = new ArrayList<>();
        List<Map<String, Object>> extraRefs = new ArrayList<>();
        String json = _.readFile(expecteRefsFile);
        if (json == null) {
            _.msg("Expected refs not found in: " + expecteRefsFile +
                    "Please run Test with -exp to generate");
            return false;
        }
        List<Map<String, Object>> expectedRefs = gson.fromJson(json, List.class);
        for (Map<String, Object> r : expectedRefs) {
            Map<String, Object> refMap = (Map<String, Object>) r.get("ref");
            Dummy dummy = makeDummy(refMap);

            List<Map<String, Object>> dests = (List<Map<String, Object>>) r.get("dests");
            List<Binding> actualDests = analyzer.references.get(dummy);
            List<Map<String, Object>> failedDests = new ArrayList<>();
            List<Map<String, Object>> extraDests = new ArrayList<>();

            for (Map<String, Object> d : dests) {
                // names are ignored, they are only for human readers
                String file = _.projAbsPath((String) d.get("file"));
                int start = (int) Math.floor((double) d.get("start"));
                int end = (int) Math.floor((double) d.get("end"));

                if (actualDests == null ||
                        !checkBindingExist(actualDests, file, start, end))
                {
                    failedDests.add(d);
                }
            }

            if (actualDests != null && !actualDests.isEmpty()) {
                for (Binding b : actualDests) {
                    String destFile = b.file;
                    if (destFile != null && destFile.startsWith(Analyzer.self.projectDir)) {
                        destFile = _.projRelPath(destFile);
                        Map<String, Object> d1 = new LinkedHashMap<>();
                        d1.put("file", destFile);
                        d1.put("start", b.start);
                        d1.put("end", b.end);
                        extraDests.add(d1);
                    }
                }
            }

            // record the ref & failed dests if any
            if (!failedDests.isEmpty()) {
                Map<String, Object> failedRef = new LinkedHashMap<>();
                failedRef.put("ref", refMap);
                failedRef.put("dests", failedDests);
                failedRefs.add(failedRef);
            }

            if (!extraDests.isEmpty()) {
                Map<String, Object> extraRef = new LinkedHashMap<>();
                extraRef.put("ref", refMap);