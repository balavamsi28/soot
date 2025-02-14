package soot.util.cfgcmd;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.G;
import soot.Singletons;

public class AltClassLoader extends ClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(AltClassLoader.class);
    private static final boolean DEBUG = false;

    private String[] locations;
    private final Map<String, Class<?>> alreadyFound = new HashMap<>();
    private final Map<String, String> nameToMangledName = new HashMap<>();
    private final Map<String, String> mangledNameToName = new HashMap<>();

    public AltClassLoader(Singletons.Global g) {}

    public static AltClassLoader v() {
        return G.v().soot_util_cfgcmd_AltClassLoader();
    }

    public void setAltClassPath(String altClassPath) {
        locations = altClassPath.split(File.pathSeparator);
    }

    public void setAltClasses(String[] classNames) {
        nameToMangledName.clear();
        for (String origName : classNames) {
            String mangledName = mangleName(origName);
            nameToMangledName.put(origName, mangledName);
            mangledNameToName.put(mangledName, origName);
        }
    }

    private static String mangleName(String origName) {
        if (!origName.contains(".")) {
            throw new IllegalArgumentException("Class name must have at least two dots: " + origName);
        }
        return origName.replace('.', '_');
    }

    @Override
    protected Class<?> findClass(String maybeMangledName) throws ClassNotFoundException {
        if (DEBUG) logger.debug("Finding class: " + maybeMangledName);
        
        return alreadyFound.computeIfAbsent(maybeMangledName, name -> {
            String originalName = mangledNameToName.getOrDefault(name, name);
            String classFilePath = "/" + originalName.replace('.', File.separatorChar) + ".class";
            
            for (String location : locations) {
                File classFile = new File(location + classFilePath);
                if (classFile.exists()) {
                    try (FileInputStream stream = new FileInputStream(classFile)) {
                        byte[] classBytes = stream.readAllBytes();
                        replaceAltClassNames(classBytes);
                        return defineClass(name, classBytes, 0, classBytes.length);
                    } catch (IOException | ClassFormatError e) {
                        if (DEBUG) logger.error("Error loading class: " + name, e);
                    }
                }
            }
            throw new RuntimeException("Class not found: " + originalName);
        });
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (DEBUG) logger.debug("Loading class: " + name);
        return super.loadClass(nameToMangledName.getOrDefault(name, name), false);
    }

    private void replaceAltClassNames(byte[] classBytes) {
        nameToMangledName.forEach((origName, mangledName) -> {
            findAndReplace(classBytes, stringToUtf8Pattern(origName), stringToUtf8Pattern(mangledName));
            findAndReplace(classBytes, stringToTypeStringPattern(origName), stringToTypeStringPattern(mangledName));
        });
    }

    private static byte[] stringToUtf8Pattern(String s) {
        byte[] bytes = s.getBytes();
        return new byte[]{1, (byte) (bytes.length >> 8), (byte) bytes.length, ...bytes};
    }

    private static byte[] stringToTypeStringPattern(String s) {
        byte[] bytes = s.getBytes();
        return new byte[]{'L', ...bytes, ';'};
    }

    private static void findAndReplace(byte[] text, byte[] pattern, byte[] replacement) {
        if (pattern.length != replacement.length) {
            throw new IllegalArgumentException("Pattern and replacement must have the same length.");
        }
        for (int i = 0; i <= text.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (text[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                System.arraycopy(replacement, 0, text, i, replacement.length);
                i += pattern.length - 1;
            }
        }
    }
}
