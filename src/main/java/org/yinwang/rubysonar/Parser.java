package org.yinwang.rubysonar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.ast.Class;
import org.yinwang.rubysonar.ast.RbModule;
import org.yinwang.rubysonar.ast.Void;
import org.yinwang.rubysonar.ast.*;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class Parser {

    private static final String RUBY_EXE = "irb";
    private static final int TIMEOUT = 30000;

    @Nullable
    Process rubyProcess;
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String dumpRubyResource = "org/yinwang/rubysonar/ruby/dump_ruby.rb";
    private String exchangeFile;
    private String endMark;
    private String jsonizer;
    private String parserLog;
    private String file;


    public Parser() {
        exchangeFile = _.locateTmp("json");
        endMark = _.locateTmp("end");
        jsonizer = _.locateTmp("dump_ruby");
        parserLog = _.locateTmp("parser_log");

        startRubyProcesses();
        if (rubyProcess != null) {
            _.msg("started: " + RUBY_EXE);
        }
    }


    // start or restart ruby process
    private void startRubyProcesses() {
        if (rubyProcess != null) {
            rubyProcess.destroy();
        }

        rubyProcess = startInterpreter(RUBY_EXE);

        if (rubyProcess == null) {
            _.die("You don't seem to have ruby on PATH");
        }
    }


    public void close() {
        if (!Analyzer.self.hasOption("debug")) {
            new File(jsonizer).delete();
            new File(exchangeFile).delete();
            new File(endMark).delete();
            new File(parserLog).delete();
        }
    }


    @Nullable
    public Node convert(Object o) {
        if (!(o instanceof Map) || ((Map) o).isEmpty()) {
            return null;
        }

        Map<String, Object> map = (Map<String, Object>) o;

        String type = (String) map.get("type");
        Dou