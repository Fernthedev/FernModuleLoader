package com.github.fernthedev.modules.test;

import com.github.fernthedev.modules.ModuleHandler;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestingModuleHandler implements ModuleHandler {

    private static final Logger logger = LoggerFactory.getLogger(TestingModuleHandler.class);
    private final Injector injector = Guice.createInjector();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public Injector getInjector() {
        return injector;
    }

    @Override
    public boolean isDebug() {
        return true;
    }
}
