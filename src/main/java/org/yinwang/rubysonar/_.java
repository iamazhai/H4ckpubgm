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


    pu