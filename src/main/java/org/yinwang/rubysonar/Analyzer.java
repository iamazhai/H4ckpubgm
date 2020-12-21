
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