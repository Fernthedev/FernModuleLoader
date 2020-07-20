package com.github.fernthedev.modules;


import com.github.fernthedev.modules.exceptions.ModuleException;
import lombok.Getter;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class ModuleClassLoader extends URLClassLoader {

    @Getter
    private final List<Module> moduleList = new ArrayList<>();

    /**
     * Constructs a new URLClassLoader for the given URLs. The URLs will be
     * searched in the order specified for classes and resources after first
     * searching in the specified parent class loader.  Any {@code jar:}
     * scheme URL is assumed to refer to a JAR file.  Any {@code file:} scheme
     * URL that ends with a '/' is assumed to refer to a directory.  Otherwise,
     * the URL is assumed to refer to a JAR file which will be downloaded and
     * opened as needed.
     *
     * <p>If there is a security manager, this method first
     * calls the security manager's {@code checkCreateClassLoader} method
     * to ensure creation of a class loader is allowed.
     *
     * @param folder   the URLs from which to load classes and resources
     * @param parent the parent class loader for delegation
     * @throws SecurityException    if a security manager exists and its
     *                              {@code checkCreateClassLoader} method doesn't allow
     *                              creation of a class loader.
     * @throws NullPointerException if {@code urls} or any of its
     *                              elements is {@code null}.
     * @see SecurityManager#checkCreateClassLoader
     */
    public ModuleClassLoader(File folder, ModuleInfoJSON description, ClassLoader parent) throws MalformedURLException {
        super(new URL[] {folder.toURI().toURL()}, parent);

        try {

            for (String clazz : description.getClassList()) {
                Class<?> jarClass;

                try {
                    jarClass = Class.forName(clazz, true, this);
                } catch (ClassNotFoundException ex) {
                    throw new ModuleException("Cannot find main class `" + clazz + "'", ex);
                }


                Class<? extends Module> pluginClass;
                try {
                    pluginClass = jarClass.asSubclass(Module.class);
                } catch (ClassCastException ex) {
                    throw new ModuleException("main class `" + clazz + "' does not extend " + Module.class, ex);
                }

                moduleList.add(pluginClass.getDeclaredConstructor().newInstance());

            }
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            throw new ModuleException("No public constructor", ex);
        } catch (InstantiationException ex) {
            throw new ModuleException("Abnormal plugin type", ex);
        }
    }
}
