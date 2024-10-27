package com.olufemithompson.springbeanregistry;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

//@SupportedAnnotationTypes("com.olufemithompson.springbeanregistry.ServiceIdentifier")
//@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ServiceInterfaceProcessor extends AbstractProcessor {
    private final Map<String, String> serviceMethods = new HashMap<>();

    private static final String REGISTRY_CLASS_NAME="ServiceRegistry";
    private static final String SERVICE_MAP="serviceMap";

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.singleton("org.gradle.annotation.processing.incremental");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Create a JavaPoet type for your generated methods
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(REGISTRY_CLASS_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Component.class)
                .addField(ServiceMapComponent.class, SERVICE_MAP, Modifier.PRIVATE)
                .addMethod(MethodSpec.constructorBuilder()
                        .addAnnotation(Autowired.class)
                        .addParameter(ServiceMapComponent.class, SERVICE_MAP)
                        .addStatement("this."+SERVICE_MAP+" = "+ SERVICE_MAP)
                        .build());

        // Generate methods based on the annotations processed
        for (Element element : roundEnv.getElementsAnnotatedWith(ServiceIdentifier.class)) {
            ServiceIdentifier annotation = element.getAnnotation(ServiceIdentifier.class);

            TypeElement typeElement = (TypeElement) element;
            if(typeElement.getSuperclass() != null){
                registerServiceMethod(classBuilder,typeElement.getSuperclass(),annotation);
            }
            if(typeElement.getInterfaces() != null){
                for(var i = 0; i < typeElement.getInterfaces().size(); i++){
                    registerServiceMethod(classBuilder,typeElement.getInterfaces().get(i),annotation);
                }
            }
        }


        JavaFile javaFile = JavaFile.builder("com.olufemithompson.springbeanregistry", classBuilder.build())
                .build();
        // Write the Java file to the generated sources
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void registerServiceMethod(
            TypeSpec.Builder classBuilder,
            TypeMirror superClassOrInterface,
            ServiceIdentifier annotation
            ){

        String serviceName = annotation.value();

        // Determine the class or interface type
        TypeElement superElement = (TypeElement) processingEnv.getTypeUtils().asElement(superClassOrInterface);
        String superClassName = superElement.getSimpleName().toString();
        if(superClassName.equals("Object")){
            return;
        }
        String superPackageName = processingEnv.getElementUtils().getPackageOf(superElement).getQualifiedName().toString();

        ClassName serviceClassType = ClassName.get(superPackageName, superClassName);

        // Check if we have already generated a method for this abstract class or interface
        if (!serviceMethods.containsKey(superClassName)) {
            // Generate the method to get the service
            classBuilder.addMethod(MethodSpec.methodBuilder("get" + superClassName)
                    .returns(serviceClassType)  // Use the determined service class type
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String.class, "serviceName")
                    .addStatement("return "+SERVICE_MAP+".getService($T.class, serviceName)", serviceClassType)
                    .build());

            // Record that we have generated a method for this abstract class or interface
            serviceMethods.put(superClassName, serviceClassType.toString());
        }

        // Generate the method to get the service
        classBuilder.addMethod(MethodSpec.methodBuilder("get" + serviceName + superClassName)
                .returns(serviceClassType)  // Use the determined service class type
                .addModifiers(Modifier.PUBLIC)
                .addStatement("String serviceName = $S", serviceName)
                .addStatement("return "+SERVICE_MAP+".getService($T.class, serviceName)", serviceClassType)
                .build());
    }
}
