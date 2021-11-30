package org.yinwang.rubysonar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.net.www.protocol.file.FileURLConnection;

import java.io.*;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * unsorted utility class
 */
public class _ {

    public static final Charset UTF_8 = Charset.forName("UTF-8");


    public static String baseFileName(String filename) {
        return new File(filename).getName();
    }


    public static boolean same(@Nullable Object o1, @Nullable Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else {
            return o1.equals(o2);
        }
    }


    public static String getSystemTempDir() {
        String tmp = System.getProperty("java.io.tmpdir");
        String sep = System.getProperty("file.separator");
        if (tmp.endsWith(sep)) {
            return tmp;
        }
        return tmp + sep;
    }


    /**
     * Returns the parent qname of {@code qname} -- everything up to the
     * last dot (exclusive), or if there are no dots, the empty string.
     */
    public static String getQnameParent(@Nullable String qname) {
        if (qname == null || qname.isEmpty()) {
            return "";
        }
        int index = qname.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return qname.substring(0, index);
    }


    public static String mainName(@NotNull String taggedName) {
        String[] segs = taggedName.split(Constants.IDSEP_REGEX);
        if (segs.length == 0) {
            // shouldn't happen, but just in case
            return taggedName;
        } else {
            return segs[0];
        }
    }


    @NotNull
    public static String arrayToString(@NotNull Collection<String> strings) {
        StringBuffer sb = new StringBuffer();
        for (String s : strings) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }


    public static void writeFile(String path, String contents) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(path)));
            out.print(contents);
            out.flush();
        } catch (Exception e) {
            _.die("Failed to write: " + path);
        } finally {
            if (out != null) {
                out.close();
            }
        }