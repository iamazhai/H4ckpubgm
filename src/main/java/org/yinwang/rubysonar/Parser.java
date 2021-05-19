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
        Double startDouble = (Double) map.get("start");
        Double endDouble = (Double) map.get("end");

        int start = startDouble == null ? 0 : startDouble.intValue();
        int end = endDouble == null ? 1 : endDouble.intValue();


        if (type.equals("program")) {
            return convert(map.get("body"));
        }

        if (type.equals("module")) {
            Node name = convert(map.get("name"));
            Block body = (Block) convert(map.get("body"));

            if (name instanceof Name) {
                String id = ((Name) name).id;
                if (id.startsWith("InstanceMethods")) {
                    return body;
                }
            }
            Str docstring = (Str) convert(map.get("doc"));
            return new RbModule(name, body, docstring, file, start, end);
        }

        if (type.equals("block")) {
            List<Node> stmts = convertList(map.get("stmts"));
            return new Block(stmts, file, start, end);
        }

        if (type.equals("def") || type.equals("lambda")) {
            Node binder = convert(map.get("name"));
            Node body = convert(map.get("body"));
            Map<String, Object> argsMap = (Map<String, Object>) map.get("params");
            List<Node> positional = convertList(argsMap.get("positional"));
            List<Node> defaults = convertList(argsMap.get("defaults"));
            Name var = (Name) convert(argsMap.get("rest"));
            Name vararg = var == null ? null : var;
            Name kw = (Name) convert(argsMap.get("rest_kw"));
            Name kwarg = kw == null ? null : kw;
            List<Node> afterRest = convertList(argsMap.get("after_rest"));
            Name blockarg = (Name) convert(argsMap.get("blockarg"));
            Str docstring = (Str) convert(map.get("doc"));
            return new Function(binder, positional, body, defaults, vararg, kwarg, afterRest, blockarg,
                    docstring, file, start, end);
        }

        if (type.equals("call")) {
            Node func = convert(map.get("func"));
            Map<String, Object> args = (Map<String, Object>) map.get("args");
            Node blockarg = null;
            Node stararg = null;

            if (args != null) {
                List<Node> posKey = convertList(args.get("positional"));
                List<Node> pos = new ArrayList<>();
                List<Keyword> kws = new ArrayList<>();
                if (posKey != null) {
                    for (Node node : posKey) {
                        if (node instanceof Assign && ((Assign) node).target instanceof Name) {
                            kws.add(new Keyword(((Name) ((Assign) node).target).id,
                                    ((Assign) node).value,
                                    file,
                                    node.start,
                                    node.end));
                        } else {
                            pos.add(node);
                        }
                    }
                }
                stararg = convert(args.get("star"));
                blockarg = convert(args.get("blockarg"));
                return new Call(func, pos, kws, null, stararg, blockarg, file, start, end);
            } else {
                // call with no arguments
                return new Call(func, null, null, null, stararg, blockarg, file, start, end);
            }
        }

        if (type.equals("attribute")) {
            Node value = convert(map.get("value"));
            Name attr = (Name) convert(map.get("attr"));
            return new Attribute(value, attr, file, start, end);
        }

        if (type.equals("binary")) {
            Node left = convert(map.get("left"));
            Node right = convert(map.get("right"));
            Op op = convertOp(map.get("op"));

            // desugar complex operators
            if (op == Op.NotEqual) {
                Node eq = new BinOp(Op.Equal, left, right, file, start, end);
                return new UnaryOp(Op.Not, eq, file, start, end);
            }

            if (op == Op.NotMatch) {
                Node eq = new BinOp(Op.Match, left, right, file, start, end);
                return new UnaryOp(Op.Not, eq, file, start, end);
            }

            if (op == Op.LtE) {
                Node lt = new BinOp(Op.Lt, left, right, file, start, end);
                Node eq = new BinOp(Op.Eq, left, right, file, start, end);
                return new BinOp(Op.Or, lt, eq, file, start, end);
            }

            if (op == Op.GtE) {
                Node gt = new BinOp(Op.Gt, left, right, file, start, end);
                Node eq = new BinOp(Op.Eq, left, right, file, start, end);
                return new BinOp(Op.Or, gt, eq, file, start, end);
            }

            if (op == Op.NotIn) {
                Node in = new BinOp(Op.In, left, right, file, start, end);
                return new UnaryOp(Op.Not, in, file, start, end);
            }

            if (op == Op.NotEq) {
                Node in = new BinOp(Op.Eq, left, right, file, start, end);
                return new UnaryOp(Op.Not, in, file, start, end);
            }

            return new BinOp(op, left, right, file, start, end);

        }

        if (type.equals("void")) {
            return new Void(file, start, end);
        }


        if (type.equals("break")) {
            return new Control("break", file, start, end);
        }

        if (type.equals("retry")) {
            return new Control("retry", file, start, end);
        }

        if (type.equals("redo")) {
            return new Control("redo", file, start, end);
        }

        if (type.equals("continue")) {
            return new Control("continue", file, start, end);
        }

        if (type.equals("class")) {
            Node locator = convert(map.get("name"));
            Node base = convert(map.get("super"));
            Node body = convert(map.get("body"));
            Str docstring = (Str) convert(map.get("doc"));
 