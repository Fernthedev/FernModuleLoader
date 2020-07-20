package com.github.fernthedev.modules;

import java.util.ArrayList;
import java.util.List;

public class ModuleInfoJSON {

    public static final String FILE_NAME = "modules_info.json";

    private List<String> classList = new ArrayList<>();

    public ModuleInfoJSON(List<String> classList) {
        this();
        this.classList = classList;
    }

    public ModuleInfoJSON() {

    }

    public List<String> getClassList() {
        return this.classList;
    }
}
