package com.github.fernthedev.modules;

import com.google.inject.Injector;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;

public interface ModuleHandler {

    /**
     *
     * @return logger
     */
    Logger getLogger();

    /**
     * The executor service for loading modules in parallel
     *
     * @see java.util.concurrent.Executors for getting executor services. Preferrably a multithreaded service
     *
     * @return service
     */
    ExecutorService getExecutorService();

    /**
     * The dependency injector
     *
     * @see com.google.inject.Guice#createInjector(com.google.inject.Module...)
     *
     * @return injector
     */
    Injector getInjector();

    /**
     * Whether if the application is in debug mode
     * @return
     */
    boolean isDebug();

    /**
     * Callback event for when a module is loaded.
     * You may use an event system to fire an event when this is called
     *
     * @param module loaded module
     */
    default void loadedModule(Module module) {}
}
