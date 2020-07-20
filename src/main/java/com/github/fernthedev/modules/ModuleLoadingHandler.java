package com.github.fernthedev.modules;

import com.github.fernthedev.fernutils.thread.ThreadUtils;
import com.github.fernthedev.fernutils.thread.multiple.TaskInfoList;
import com.github.fernthedev.modules.exceptions.*;
import com.google.gson.Gson;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.Validate;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ModuleLoadingHandler {

    private final ModuleHandler moduleHandler;

    private final Map<String, ClassLoader> loaders = new HashMap<>();

    private Map<Class<?>, Module> moduleMap = new HashMap<>();
    private Map<String, Module> moduleNameMap = new HashMap<>();
    private List<Pattern> jarPatterns = List.of(
            Pattern.compile(".*\\.jar")
    );

//    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();


    public Set<Module> getModuleList() {
        return new HashSet<>(moduleNameMap.values());
    }

    /**
     * Initializes the modules asynchronously in a new thread, making this non-blocking.
     *
     * If you want to block, call {@link TaskInfoList#join(int)}
     *
     * @return a task that handles the status of completion. You can await it
     */
    public TaskInfoList initializeModules() {
        Map<String, Module> loadedModules = new HashMap<>();

        // Handles futures
        Map<String, CompletableFuture<?>> futureMap = new HashMap<>();

        moduleNameMap.forEach((s, module) -> futureMap.put(s, new CompletableFuture<>()));

        TaskInfoList task = ThreadUtils.runForLoopAsync(moduleNameMap.keySet(), s -> {
            moduleHandler.getLogger().info(ColorCode.YELLOW + "Loading module {}", s);

            Module module = moduleNameMap.get(s);
            ModuleInfo moduleInfo = module.getClass().getAnnotation(ModuleInfo.class);

            List<String> missingDependencies = new ArrayList<>();

            for (String dep : moduleInfo.depend()) {
                if (!moduleNameMap.containsKey(dep)) {
                    missingDependencies.add(dep);
                }
            }

            if (!missingDependencies.isEmpty()) {
                throw new MissingDependenciesException("Missing dependencies for module " + module.getName() + " are: " + missingDependencies);
            }

            List<String> awaitDependencies = new ArrayList<>();

            awaitDependencies.addAll(Arrays.asList(moduleInfo.depend()));
            awaitDependencies.addAll(Arrays.asList(moduleInfo.softDepend()));

            moduleHandler.getLogger().debug("{} Waiting for dependencies: {}", module.getName(), awaitDependencies);


            for (String awaitDep : awaitDependencies) {
                if (awaitDep.isEmpty() || awaitDep.trim().isEmpty()) continue;

                moduleHandler.getLogger().debug("Checking for module {} await dependency {}", module.getName(), awaitDep);

                // Wait for module to load
                futureMap.get(awaitDep).join();

//                while (!loadedModules.containsKey(awaitDep)) {
//                    try {
//                        // Wait for module to load
//                        Thread.sleep(1);
//                    } catch (InterruptedException e) {
//                        Thread.currentThread().interrupt();
//                        e.printStackTrace();
//                    }
//                }
            }

            moduleHandler.getLogger().info(ColorCode.GREEN + "Loaded module {} successfully", module.getName());
            moduleHandler.getLogger().info(ColorCode.UNDERLINE + "Starting module {}", module.getName());
            module.onEnable();
            loadedModules.put(module.getName(), module);
            futureMap.get(module.getName()).complete(null);
            moduleHandler.getLogger().info(ColorCode.GREEN + "Started module {}", module.getName());
        });


        task.runThreads(moduleHandler.getExecutorService());

        moduleHandler.getExecutorService().submit(() -> {
            task.awaitFinish(1);
            moduleHandler.getLogger().info(ColorCode.GREEN + "Finished loading modules");
        });


        return task;
    }

    public ModuleInfoJSON getModuleDescription(File file) throws ModuleInvalidDescriptionException {
        Validate.notNull(file, "File cannot be null");

        JarFile jar = null;
        InputStream stream = null;

        try {
            jar = new JarFile(file);
            JarEntry entry = jar.getJarEntry(ModuleInfoJSON.FILE_NAME);

            if (entry == null) {
                throw new ModuleInvalidDescriptionException(new FileNotFoundException("Jar does not contain " + ModuleInfoJSON.FILE_NAME));
            }

            stream = jar.getInputStream(entry);

            return new Gson().fromJson(new InputStreamReader(stream), ModuleInfoJSON.class);

        } catch (IOException ex) {
            throw new ModuleInvalidDescriptionException(ex);
        } /* catch (YAMLException ex) {
            throw new InvalidDescriptionException(ex);
        }*/ finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException ignored) {}
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public ModuleDescription parseDescription(ModuleInfo moduleInfo, Module module) {
        String version = moduleInfo.version();

        if (version.isEmpty()) version = module.getClass().getPackage().getImplementationVersion();

        if (version == null) version = "null";

        return new ModuleDescription(moduleInfo.authors(),
                version,
                moduleInfo.name(),
                moduleInfo.depend(),
                moduleInfo.softDepend()
        );
    }

    public void registerModule(Module module) {
        if (moduleMap.containsKey(module.getClass())) {
            throw new ModuleAlreadyRegisteredException("Module " + module.getClass().getName() + " is already registered");
        }

        if (module.getClass().isAnnotationPresent(ModuleInfo.class)) {
            ModuleInfo moduleInfo = module.getClass().getAnnotation(ModuleInfo.class);
            module.setDescription(parseDescription(moduleInfo, module));

            if (moduleNameMap.containsKey(module.getName())) {
                throw new ModuleAlreadyRegisteredException("Module " + module.getClass().getName() + "'s  name " + module.getName() + " already taken by " + module.getClass().getName());
            }

            List<String> awaitDependencies = new ArrayList<>();

            awaitDependencies.addAll(Arrays.asList(moduleInfo.depend()));
            awaitDependencies.addAll(Arrays.asList(moduleInfo.softDepend()));

            if (awaitDependencies.parallelStream().anyMatch(s -> s.equals(module.getName())))
                throw new ModuleException("You cannot depend on your own module " + module.getName());

        } else {
            throw new ModuleRequirementException("Class " + module.getClass().getName() + " must have a @ModuleInfo annotation.");
        }


        moduleHandler.getInjector().injectMembers(module);

        moduleMap.put(module.getClass(), module);
        moduleNameMap.put(module.getName(), module);

        moduleHandler.loadedModule(module);
    }

    public void unregisterModule(@NonNull Module module) {
        module.onDisable();

        moduleMap.remove(module.getClass());
        moduleNameMap.remove(module.getName());

        ClassLoader classLoader = loaders.get(module.getName());

        if (classLoader != null) {
            if (classLoader instanceof ModuleClassLoader) {
                ModuleClassLoader loader = (ModuleClassLoader) classLoader;
                loaders.values().removeAll(Collections.singleton(loader)); // Remove only removes one element, so use removeAll

//                Set<String> names = loader.getClasses();
//
//                for (String name : names) {
//                    removeClass(name);
//                }
            }
        }
    }


    public List<Module> loadModule(final File file, ClassLoader classLoader) throws ModuleException {
        Validate.notNull(file, "File cannot be null");

        if (!file.exists()) {
            throw new ModuleException(new FileNotFoundException(file.getPath() + " does not exist"));
        }

        final ModuleInfoJSON description;
        try {
            description = getModuleDescription(file);
        } catch (ModuleInvalidDescriptionException ex) {
            throw new ModuleException(ex);
        }

        if (description.getClassList().isEmpty()) throw new NotAModuleException(file.getName() + " is not a module");


        final ModuleClassLoader loader;
        try {
            loader = new ModuleClassLoader(file, description, classLoader);
        } catch (ModuleException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new ModuleException(ex);
        }

        loader.getModuleList().forEach(module -> {
            loaders.put(module.getName(), loader);
            registerModule(module);
        });

        return loader.getModuleList();
    }


    /**
     * Example usage
     *
     * "moduleHandler.scanDirectory(moduleFolder, getClass().getClassLoader());"
     *
     * Loads all jars in a directory
     *
     * @param folder
     * @param classLoader
     */
    public void scanDirectory(File folder, ClassLoader classLoader) {
        if (!folder.isDirectory()) throw new IllegalArgumentException("File " + folder.toPath().toString() + " is not a folder");

        for (File file : Objects.requireNonNull(
                folder.listFiles(
                        (dir, name) -> jarPatterns.parallelStream().anyMatch(pattern -> pattern.matcher(name).matches())
                )
        )) {
            try {
                loadModule(file, classLoader);
            } catch (NotAModuleException e) {
                if (!moduleHandler.isDebug())
                    e.printStackTrace();
            }
        }
    }


}
