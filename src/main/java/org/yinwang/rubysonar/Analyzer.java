
package org.yinwang.rubysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.rubysonar.ast.Call;
import org.yinwang.rubysonar.ast.Name;
import org.yinwang.rubysonar.ast.Node;
import org.yinwang.rubysonar.ast.Url;
import org.yinwang.rubysonar.types.ClassType;
import org.yinwang.rubysonar.types.FunType;
import org.yinwang.rubysonar.types.ModuleType;
import org.yinwang.rubysonar.types.Type;

import java.io.File;
import java.net.URL;
import java.util.*;


public class Analyzer {

    public static String MODEL_LOCATION = "org/yinwang/rubysonar/models";

    // global static instance of the analyzer itself
    public static Analyzer self;

    public String sid = _.newSessionId();
    public String cwd = null;
    public int nCalled = 0;

    public State globaltable = new State(null, State.StateType.GLOBAL);

    public Set<String> loadedFiles = new HashSet<>();
    public List<Binding> allBindings = new ArrayList<>();
    public Map<Node, List<Binding>> references = new LinkedHashMap<>();
    public Set<Name> resolved = new HashSet<>();
    public Set<Name> unresolved = new HashSet<>();

    public Map<String, List<Diagnostic>> semanticErrors = new HashMap<>();
    public Set<String> failedToParse = new HashSet<>();


    public List<String> path = new ArrayList<>();
    private Set<FunType> uncalled = new LinkedHashSet<>();
    private Set<Object> callStack = new HashSet<>();
    private Set<Object> importStack = new HashSet<>();

    private AstCache astCache;
    public Stats stats = new Stats();
    private Progress loadingProgress = null;

    public String projectDir;
    public String cacheDir;
    public String modelDir;

    public boolean multilineFunType = false;
    public String suffix;

    public boolean staticContext = false;

    public Map<String, Object> options;


    public Analyzer() {
        this(null);
    }


    public Analyzer(Map<String, Object> options) {
        self = this;
        if (options != null) {
            this.options = options;
        } else {
            this.options = new HashMap<>();
        }
        stats.putInt("startTime", System.currentTimeMillis());
        this.suffix = ".rb";
        addEnvPath();
        copyModels();
        createCacheDir();
        getAstCache();
    }


    public boolean hasOption(String option) {
        Object op = options.get(option);
        if (op != null && op.equals(true)) {
            return true;
        } else {
            return false;
        }
    }


    private void copyModels() {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(MODEL_LOCATION);
        String dest = _.locateTmp("models");
        this.modelDir = dest;

        try {
            _.copyResourcesRecursively(resource, new File(dest));
            _.msg("copied models to: " + modelDir);
        } catch (Exception e) {
            _.die("Failed to copy models. Please check permissions of writing to: " + dest);
        }
        addPath(dest);
    }


    // main entry to the analyzer
    public void analyze(String path) {
        String upath = _.unifyPath(path);
        File f = new File(upath);
        projectDir = f.isDirectory() ? f.getPath() : f.getParent();
        loadFileRecursive(upath);
    }


    // main entry to the analyzer (for JSONDump only)
    public void analyze(List<String> paths) {
        for (String path : paths) {
            loadFileRecursive(path);
        }
    }


    public void setCWD(String cd) {
        if (cd != null) {
            cwd = _.unifyPath(cd);
        }
    }


    public void addPaths(@NotNull List<String> p) {
        for (String s : p) {
            addPath(s);
        }
    }


    public void addPath(String p) {
        path.add(_.unifyPath(p));
    }


    public void setPath(@NotNull List<String> path) {
        this.path = new ArrayList<>(path.size());
        addPaths(path);
    }


    private void addEnvPath() {
        String path = System.getenv("RUBYLIB");
        if (path != null) {
            String[] segments = path.split(":");
            for (String p : segments) {
                addPath(p);
            }
        }
    }


    @NotNull
    public List<String> getLoadPath() {
        List<String> loadPath = new ArrayList<>();
        loadPath.addAll(path);
        loadPath.add("/Users/yinwang/.rvm/src/ruby-2.0.0-p247/lib");

        if (cwd != null) {
            loadPath.add(cwd);
        }
        if (projectDir != null && (new File(projectDir).isDirectory())) {
            loadPath.add(projectDir);
        }

        return loadPath;
    }


    public boolean inStack(Object f) {
        return callStack.contains(f);
    }


    public void pushStack(Object f) {
        callStack.add(f);
    }


    public void popStack(Object f) {
        callStack.remove(f);
    }


    public boolean inImportStack(Object f) {
        return importStack.contains(f);
    }


    public void pushImportStack(Object f) {
        importStack.add(f);
    }


    public void popImportStack(Object f) {
        importStack.remove(f);
    }


    @NotNull
    public List<Binding> getAllBindings() {
        return allBindings;
    }


    public List<Diagnostic> getDiagnosticsForFile(String file) {
        List<Diagnostic> errs = semanticErrors.get(file);
        if (errs != null) {
            return errs;
        }
        return new ArrayList<>();
    }


    public void putRef(@NotNull Node node, @NotNull List<Binding> bs) {
        if (!(node instanceof Url)) {
            List<Binding> bindings = references.get(node);
            if (bindings == null) {
                bindings = new ArrayList<>(1);
                references.put(node, bindings);
            }
            for (Binding b : bs) {
                if (!bindings.contains(b)) {
                    bindings.add(b);
                }
                b.addRef(node);
            }
        }
    }


    public void putRef(@NotNull Node node, @NotNull Binding b) {
        List<Binding> bs = new ArrayList<>();
        bs.add(b);
        putRef(node, bs);
    }


    @NotNull
    public Map<Node, List<Binding>> getReferences() {
        return references;
    }


    public void putProblem(@NotNull Node loc, String msg) {
        String file = loc.file;
        if (file != null) {
            addFileErr(file, loc.start, loc.end, msg);
        }
    }


    // for situations without a Node
    public void putProblem(@Nullable String file, int begin, int end, String msg) {
        if (file != null) {
            addFileErr(file, begin, end, msg);
        }
    }


    void addFileErr(String file, int begin, int end, String msg) {
        Diagnostic d = new Diagnostic(file, Diagnostic.Category.ERROR, begin, end, msg);
        getFileErrs(file, semanticErrors).add(d);
    }


    List<Diagnostic> getFileErrs(String file, @NotNull Map<String, List<Diagnostic>> map) {
        List<Diagnostic> msgs = map.get(file);
        if (msgs == null) {
            msgs = new ArrayList<>();
            map.put(file, msgs);
        }
        return msgs;
    }


    @Nullable
    public Type loadFile(String path) {
        path = _.unifyPath(path);
        File f = new File(path);

        if (!f.canRead()) {
            return null;
        }

        // detect circular import
        if (Analyzer.self.inImportStack(path)) {
            return null;
        }

        // set new CWD and save the old one on stack
        String oldcwd = cwd;
        setCWD(f.getParent());

        Analyzer.self.pushImportStack(path);
        Type type = parseAndResolve(path);

        // restore old CWD
        setCWD(oldcwd);
        Analyzer.self.popImportStack(path);

        return type;
    }


    @Nullable
    private Type parseAndResolve(String file) {
        try {
            Node ast = getAstForFile(file);

            if (ast == null) {
                failedToParse.add(file);
                return null;
            } else {
                Type type = Node.transformExpr(ast, globaltable);
                if (!loadedFiles.contains(file)) {
                    loadedFiles.add(file);
                    loadingProgress.tick();
                }
                return type;
            }
        } catch (OutOfMemoryError | StackOverflowError e) {
            if (astCache != null) {
                astCache.clear();
            }
            System.gc();
            if(e instanceof OutOfMemoryError) {
                _.msg("Skiping for memory size limit: " + file);
            }
            if(e instanceof StackOverflowError) {
                _.msg("Skiping for stack size limit: " + file);
            }
            return null;
        }
    }


    private void createCacheDir() {
        cacheDir = _.makePathString(_.getSystemTempDir(), "rubysonar", "ast_cache");
        File f = new File(cacheDir);
        _.msg("AST cache is at: " + cacheDir);

        if (!f.exists()) {
            if (!f.mkdirs()) {
                _.die("Failed to create tmp directory: " + cacheDir +
                        ".Please check permissions");
            }
        }
    }


    private AstCache getAstCache() {
        if (astCache == null) {
            astCache = AstCache.get();
        }
        return astCache;
    }


    @Nullable
    public Node getAstForFile(String file) {
        return getAstCache().getAST(file);
    }


    public Type requireFile(String headName) {
        List<String> loadPath = getLoadPath();

        for (String p : loadPath) {
            String trial = _.makePathString(p, headName + suffix);
            if (new File(trial).exists()) {
                return loadFile(trial);
            }
        }

        return null;
    }


    public void loadFileRecursive(String fullname) {
        int count = countFileRecursive(fullname);
        if (loadingProgress == null) {
            loadingProgress = new Progress(count, 50);