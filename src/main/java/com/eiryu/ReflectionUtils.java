package com.eiryu;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ReflectionUtils {

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     * <p>
     * support for default package.
     * <a href="https://blog.cles.jp/item/4717">link</a>
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    @SuppressWarnings("rawtypes")
    public static List<Class> getClassesInSpecificPackage(String packageName)
            throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String fileName = resource.getFile();
            String fileNameDecoded = URLDecoder.decode(fileName, "UTF-8");
            dirs.add(new File(fileNameDecoded));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }

    /**
     * Recursive method used to find all classes in a given directory and
     * subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("rawtypes")
    private static List<Class> findClasses(File directory, String packageName)
            throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            return classes;
        }
        String prefix = packageName + '.';
        // default package
        if ("".equals(packageName)) {
            prefix = "";
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                assert !fileName.contains(".");
                classes.addAll(findClasses(file, prefix + fileName));
            } else if (fileName.endsWith(".class") && !fileName.contains("$")) {
                Class _class;
                try {
                    String className = prefix
                            + fileName.substring(0, fileName.length() - 6);
                    _class = Class.forName(className);
                } catch (ExceptionInInitializerError e) {
                    // happen, for example, in classes, which depend on
                    // Spring to inject some beans, and which fail,
                    // if dependency is not fulfilled
                    _class = Class.forName(
                            prefix
                                    + fileName.substring(0,
                                    fileName.length() - 6), false,
                            Thread.currentThread().getContextClassLoader());
                }
                classes.add(_class);
            }
        }
        return classes;
    }
}

