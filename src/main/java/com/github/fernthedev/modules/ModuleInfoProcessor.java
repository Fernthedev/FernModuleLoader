package com.github.fernthedev.modules;

import com.github.fernthedev.config.common.Config;
import com.github.fernthedev.config.gson.GsonConfig;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableMap;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.FileObject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.*;

import static javax.tools.Diagnostic.Kind.*;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

@SupportedAnnotationTypes(
        "com.github.fernthedev.modules.ModuleInfo"
)
@AutoService(Processor.class)
public class ModuleInfoProcessor extends AbstractProcessor {

    private List<String> classes = new ArrayList<>();
    private Map<String, TypeElement> moduleIds = new HashMap<>();

    private Messager getMessager() {
        return this.processingEnv.getMessager();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * {@inheritDoc}
     *
     * @param annotations
     * @param roundEnv
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (roundEnv.processingOver()) {
            if (!roundEnv.errorRaised()) {
                finish();
            }

            return true;
        }

        if (!contains(annotations)) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(ModuleInfo.class)) {
            if (element.getKind() != ElementKind.CLASS) {

                getMessager().printMessage(ERROR, "Invalid element of type " + element.getKind() + " annotated with @ModuleInfo", element);
                continue;
            }

            TypeElement typeElement = (TypeElement) element;


            System.out.println("Processing annotation " + typeElement.getQualifiedName().toString());

//                if (!Module.class.isAssignableFrom(clazz)) throw new IllegalArgumentException("Class with annotation @" + ModuleInfo.class.getName() + " (" + clazz.getName() + ") must extend " + Module.class.getName());

            ModuleInfo moduleInfo = typeElement.getAnnotation(ModuleInfo.class);

            if (!moduleInfo.includeInModuleInfoFile()) return false;

            AnnotationMirror mirrorS = null;

            for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                if (((QualifiedNameable) mirror.getAnnotationType().asElement()).getQualifiedName().contentEquals(ModuleInfo.class.getName())) {
                    mirrorS = mirror;
                    break;
                }
            }

            ImmutableMap.Builder<String, AnnotationValue> builder = ImmutableMap.builder();
            mirrorS.getElementValues().forEach((field, value) -> builder.put(field.getSimpleName().toString(), value));

            ImmutableMap<String, AnnotationValue> values = builder.build();

            if (moduleIds.containsKey(moduleInfo.name()) && moduleIds.get(moduleInfo.name()) != typeElement) {
                reportDuplicateModule(moduleInfo.name(), typeElement, mirrorS, values.get("name"));
            }

            classes.add(typeElement.getQualifiedName().toString());

        }


        return true;
    }

    private void finish() {
        ModuleInfoJSON moduleInfoJSON = new ModuleInfoJSON(classes);

        try {
            if (!classes.isEmpty()) {
                Filer filer = processingEnv.getFiler();

                FileObject fileObject = filer.getResource(CLASS_OUTPUT, "", ModuleInfoJSON.FILE_NAME);

                boolean exists = false;
                try {
                    if (fileObject.getCharContent(false) != null) {
                        exists = true;
                    }
                } catch (NoSuchFileException ignored) {
                    exists = false;
                } catch (Exception e) {
                    e.printStackTrace();
                    exists = true;
                }

                if (exists) {
                    getMessager().printMessage(WARNING, "Resource file " + ModuleInfoJSON.FILE_NAME + " already exists");
                    return;
                }


//                URL url = Resources.getResource("");
//                File file = new File(new File(url.toURI()), ModuleInfoJSON.FILE_NAME);
                fileObject = filer.createResource(CLASS_OUTPUT, "", ModuleInfoJSON.FILE_NAME);
                File file = new File(fileObject.toUri());


                getMessager().printMessage(NOTE, "Writing plugin metadata to " + fileObject.toUri());
                BufferedWriter bufferedWriter = new BufferedWriter(fileObject.openWriter());

                try (bufferedWriter) {
                    Config<ModuleInfoJSON> yamlConfig = new GsonConfig<>(moduleInfoJSON, file);

                    bufferedWriter.write(yamlConfig.configToFileString());
                    bufferedWriter.flush();
                }
//                try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file.toPath())) {
//
//                    //bufferedWriter...
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reportDuplicateModule(String id, Element element, AnnotationMirror module1, AnnotationValue annotationValue) {
        getMessager().printMessage(ERROR, "Duplicate plugin ID: " + id, element, module1, annotationValue);
    }

    private static boolean contains(Collection<? extends TypeElement> elements) {
        if (elements.isEmpty()) {
            return false;
        }

        final String name = ModuleInfo.class.getName();
        for (TypeElement element : elements) {
            if (element.getQualifiedName().contentEquals(name)) {
                return true;
            }
        }

        return false;
    }
}
