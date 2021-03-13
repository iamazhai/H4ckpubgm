
package org.yinwang.rubysonar;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.jetbrains.annotations.NotNull;
import org.yinwang.rubysonar.ast.Function;
import org.yinwang.rubysonar.ast.Node;
import org.yinwang.rubysonar.ast.Str;
import org.yinwang.rubysonar.types.FunType;
import org.yinwang.rubysonar.types.Type;
import org.yinwang.rubysonar.types.UnionType;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class JSONDump {

    private static Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static Set<String> seenDef = new HashSet<>();
    private static Set<String> seenRef = new HashSet<>();


    @NotNull
    private static String dirname(@NotNull String path) {
        File f = new File(path);
        if (f.getParent() != null) {
            return f.getParent();
        } else {
            return path;
        }
    }


    private static Analyzer newAnalyzer(String projectDir, List<String> srcpath, List<String> inclpaths) {
        Analyzer idx = new Analyzer();
        idx.addPath(projectDir);
        idx.addPaths(inclpaths);
        idx.analyze(srcpath);
        idx.finish();
        return idx;
    }


    private static String kindName(Binding.Kind kind) {
        if (kind == Binding.Kind.CLASS_METHOD) {
            return "method";
        } else {
            return kind.toString().toLowerCase();
        }
    }


    private static void writeSymJson(@NotNull Binding binding, JsonGenerator json) throws IOException {
        if (binding.start < 0) {
            return;
        }

        String name = binding.node.name;
        boolean isExported = !(
                Binding.Kind.VARIABLE == binding.kind ||
                        Binding.Kind.PARAMETER == binding.kind ||
                        Binding.Kind.SCOPE == binding.kind ||
                        Binding.Kind.ATTRIBUTE == binding.kind ||
                        (name != null && (name.length() == 0 || name.startsWith("lambda%"))));

        String path = binding.qname.replace("%20", ".");

        if (!seenDef.contains(path)) {
            seenDef.add(path);

            json.writeStartObject();
            json.writeStringField("name", name);
            json.writeStringField("path", path);
            json.writeStringField("file", binding.file);
            json.writeNumberField("identStart", binding.start);
            json.writeNumberField("identEnd", binding.end);
            json.writeNumberField("defStart", binding.bodyStart);
            json.writeNumberField("defEnd", binding.bodyEnd);
            json.writeBooleanField("exported", isExported);
            json.writeStringField("kind", kindName(binding.kind));

            if (binding.kind == Binding.Kind.METHOD || binding.kind == Binding.Kind.CLASS_METHOD) {
                // get args expression
                Type t = binding.type;

                if (t instanceof UnionType) {