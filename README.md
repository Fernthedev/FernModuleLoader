# What is this?
A light-weight library for loading modules. This does not include any API for the modules except depdency injection (which you can customize using Guice) and a Logger. This means that it is the implementation's job and responsibility to create an API that will complement the loader like PiMP3 does. Modules created using this API of course are not guaranteed to work everywhere because they use a project's specific API (unless they manage to create a module that does something completely standalone, like a standalone GUI or web server as an example)

# Features
- Parallel module loading 
    - Modules are loaded in parallel of another. If a module declares a dependency or soft dependency, it will wait if the module system has the module. If it does, it will wait for it to load. 
    - If a module depends on a module 
- Simple

# Implementors
To implement, simply create a class implementing `ModuleHandler` e.g `YourModuleHandler`
and use this code
```java
YourModuleHandler moduleHandler = new YourModuleHandler();
ModuleLoadingHandler moduleLoadingHandler = new ModuleLoadingHandler(moduleHandler);

moduleLoadingHandler.scanDirectory(new File("./modules", getClass().getClassLoader()));
// or
moduleLoadingHandler.loadModule(new File("./modules", getClass().getClassLoader()));
```

Look at the [test files as an example](src/test/com/github/fernthedev/modules/test)

Modules will be loaded asynchronously from one another, so make sure your APIs are Thread-Safe or have a ThreadQueue if needed. 

# Module creators

To create your own Module, simply create a class that extends `Module` and for it to be recognized by the module loader, add the `@ModuleInfo` annotation with its values.
e.g
```java
@ModuleInfo(authors = "Fernthedev", name = "TestModule")
public class TestModuleClass extends Module {


}
```