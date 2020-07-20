package com.github.fernthedev.modules.test;

import com.github.fernthedev.modules.ModuleLoadingHandler;
import com.github.fernthedev.modules.exceptions.ModuleAlreadyRegisteredException;
import com.github.fernthedev.modules.exceptions.ModuleException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestModules {

    private static final TestingModuleHandler moduleHandler = new TestingModuleHandler();
    private static ModuleLoadingHandler moduleLoadingHandler = new ModuleLoadingHandler(moduleHandler);

    @DisplayName("Same class module registration exception")
    @Test
    public void testDependencySameClassFail() {
        Assertions.assertThrows(ModuleAlreadyRegisteredException.class, () -> {
            moduleLoadingHandler.registerModule(new TestModuleClass());
            moduleLoadingHandler.registerModule(new TestModuleClass());
        });

    }

    @DisplayName("Same name module registration exception")
    @Test
    public void testDependencySameNameFail() {
        Assertions.assertThrows(ModuleAlreadyRegisteredException.class, () -> {

            moduleLoadingHandler.registerModule(new TestModuleClass());
            moduleLoadingHandler.registerModule(new TestModuleSameNameClass());

        });
    }

    @DisplayName("Same self dependency module registration exception")
    @Test
    public void testDependencySelfDependFail() {

        Assertions.assertThrows(ModuleException.class, () -> {
            moduleLoadingHandler.registerModule(new TestModuleSelfClass());
            moduleLoadingHandler.initializeModules();
        });
    }


}
