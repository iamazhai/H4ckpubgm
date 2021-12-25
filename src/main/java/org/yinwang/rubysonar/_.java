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
    }


    @Nullable
    public static String readFile(@NotNull String path) {
        // Don't use line-oriented file read -- need to retain CRLF if present
        // so the style-run and link offsets are correct.
        byte[] content = getBytesFromFile(path);
        if (content == null) {
            return null;
        } else {
            return new String(content, UTF_8);
        }
    }


    @Nullable
    public static byte[] getBytesFromFile(@NotNull String filename) {
        try {
            return FileUtils.readFileToByteArray(new File(filename));
        } catch (Exception e) {
            return null;
        }
    }


    static boolean isReadableFile(String path) {
        File f = new File(path);
        return f.canRead() && f.isFile();
    }


    @NotNull
    public static String readWhole(@NotNull InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = new byte[8192];

        int nRead;
        while ((nRead = is.read(bytes, 0, 8192)) > 0) {
            sb.append(new String(bytes, 0, nRead));
        }
        return sb.toString();
    }


    public static void copyResourcesRecursively(URL originUrl, File destination) throws Exception {
        URLConnection urlConnection = originUrl.openConnection();
        if (urlConnection instanceof JarURLConnection) {
            copyJarResourcesRecursively(destination, (JarURLConnection) urlConnection);
        } else if (urlConnection instanceof FileURLConnection) {
            FileUtils.copyDirectory(new File(originUrl.getPath()), destination);
        } else {
            die("Unsupported URL type: " + urlConnection);
        }
    }


    public static void copyJarResourcesRecursively(File destination, JarURLConnection jarConnection) {
        JarFile jarFile;
        try {
            jarFile = jarConnection.getJarFile();
        } catch (Exception e) {
            _.die("Failed to get jar file)");
            return;
        }

        Enumeration<JarEntry> em = jarFile.entries();
        while (em.hasMoreElements()) {
            JarEntry entry = em.nextElement();
            if (entry.getName().startsWith(jarConnection.getEntryName())) {
                String fileName = StringUtils.removeStart(entry.getName(), jarConnection.getEntryName());
                if (!fileName.equals("/")) {  // exclude the directory
                    InputStream entryInputStream = null;
                    try {
                        entryInputStream = jarFile.getInputStream(entry);
                        FileUtils.copyInputStreamToFile(entryInputStream, new File(destination, fileName));
                    } catch (Exception e) {
                        die("Failed to copy resource: " + fileName);
                    } finally {
                        if (entryInputStream != null) {
                            try {
                                entryInputStream.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        }
    }


    public static String readResource(String resource) {
        InputStream s = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        return readWholeStream(s);
    }


    @NotNull
    public static String getSHA(@NotNull String path) {
        byte[] bytes = getBytesFromFile(path);
        return getSHA(bytes);
    }


    @NotNull
    public static String getSHA(byte[] fileContents) {
        MessageDigest algorithm;

        try {
            algorithm = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            _.die("Failed to get SHA, shouldn't happen");
            return "";
        }

        algorithm.reset();
        algorithm.update(fileContents);
        byte messageDigest[] = algorithm.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aMessageDigest : messageDigest) {
            sb.append(String.format("%02x", 0xFF & aMessageDigest));
        }
        return sb.toString();
    }


    static public String escapeQname(@NotNull String s) {
        return s.replaceAll("[.&@%-]", "_");
    }


    public static String escapeWindowsPath(String path) {
        return path.replace("\\", "\\\\");
    }


    @NotNull
    public static Collection<String> toStringCollection(@NotNull Collection<Integer> collection) {
        List<String> ret = new ArrayList<>();
        for (Integer x : collection) {
            ret.add(x.toString());
        }
        return ret;
    }


    @NotNull
    static public String joinWithSep(@NotNull Collection<String> ls, String sep, @Nullable String start,
                                     @Nullable String end)
    {
        StringBuilder sb = new StringBuilder();
    