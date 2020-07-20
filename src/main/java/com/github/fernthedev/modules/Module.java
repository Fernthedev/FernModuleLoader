package com.github.fernthedev.modules;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public abstract class Module {

    /**
     * Injected after registered
     */
    @Setter(value = AccessLevel.PACKAGE)
    @Getter
    private ModuleDescription description;

    public String getName() {
        return description.getName();
    }

    public void onEnable() {}

    public void onDisable() {}
}
