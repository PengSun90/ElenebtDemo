package com.ktolintest2345.apt_processor;

import static com.google.auto.common.MoreElements.getPackage;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.auto.service.AutoService;
import com.ktolintest2345.apt_api.BindView;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;


@AutoService(Processor.class)
public class ButterKnifeProcessor extends AbstractProcessor {

  /**
   * 元素相关的工具类
   */
  private Elements elementUtils;
  /**
   * 文件相关的工具类
   */
  private Filer filer;
  /**
   * 日志相关的工具类
   */
  private Messager messager;
  /**
   * 类型相关工具类
   */
  private Types typeUtils;

  private Map<TypeElement, List<Element>> elementPackage = new HashMap<>();
  private static final String VIEW_TYPE = "android.view.View";
  private static final String VIEW_BINDER = "com.ktolintest2345.apt_api.ViewBinding";

  @Override
  public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
    if (set == null || set.isEmpty()) {
      return false;
    }
    elementPackage.clear();
    Set<? extends Element> bindViewElement = roundEnvironment.getElementsAnnotatedWith(BindView.class);
    //收集数据放入elementPackage中
    collectData(bindViewElement);
    //根据elementPackage中的数据生成.java代码
    generateCode();
    return true;
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    Set<String> set = new HashSet<>();
    set.add(BindView.class.getCanonicalName());
    return set;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.RELEASE_7;
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnvironment) {
    super.init(processingEnvironment);
    elementUtils = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    messager = processingEnv.getMessager();
    typeUtils = processingEnv.getTypeUtils();
  }

  private void collectData(Set<? extends Element> elements) {
    Iterator<? extends Element> iterable = elements.iterator();
    while (iterable.hasNext()) {
      Element element = iterable.next();
      TypeMirror elementTypeMirror = element.asType();
      //判断元素的类型是否是View或者是View的子类型。
      TypeMirror viewTypeMirror = elementUtils.getTypeElement(VIEW_TYPE).asType();
      if (typeUtils.isSubtype(elementTypeMirror, viewTypeMirror) || typeUtils.isSameType(elementTypeMirror, viewTypeMirror)) {
        //找到父元素，这里认为是@BindView标记字段所在的类。
        TypeElement parent = (TypeElement) element.getEnclosingElement();
        //根据parent不同存储的List中
        List<Element> parentElements = elementPackage.get(parent);
        if (parentElements == null) {
          parentElements = new ArrayList<>();
          elementPackage.put(parent, parentElements);
        }
        parentElements.add(element);
      } else {
        throw new RuntimeException("错误处理，BindView应该标注在类型是View的字段上");
      }
    }
  }


  private void generateCode() {
    Set<Map.Entry<TypeElement, List<Element>>> entries = elementPackage.entrySet();
    Iterator<Map.Entry<TypeElement, List<Element>>> iterator = entries.iterator();
    while (iterator.hasNext()) {
      Map.Entry<TypeElement, List<Element>> entry = iterator.next();
      //类元素
      TypeElement parent = entry.getKey();
      //当前类元素下，注解了BindView的元素
      List<Element> elements = entry.getValue();
      //通过JavaPoet生成bindView的MethodSpec
      MethodSpec methodSpec = generateBindViewMethod(parent, elements);

      String packageName = getPackage(parent).getQualifiedName().toString();
      ClassName viewBinderInterface = ClassName.get(elementUtils.getTypeElement(VIEW_BINDER));
      String className = parent.getQualifiedName().toString().substring(
          packageName.length() + 1).replace('.', '$');
      ClassName bindingClassName = ClassName.get(packageName, className + "_ViewBinding");

      try {
        //生成 className_ViewBinding.java文件
        JavaFile.builder(packageName, TypeSpec.classBuilder(bindingClassName)
            .addModifiers(PUBLIC)
            .addSuperinterface(viewBinderInterface)
            .addMethod(methodSpec)
            .build()
        ).build().writeTo(filer);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }


  private MethodSpec generateBindViewMethod(TypeElement parent, List<Element> elementList) {
    ParameterSpec.Builder parameter = ParameterSpec.builder(TypeName.OBJECT, "target");
    MethodSpec.Builder bindViewMethod = MethodSpec.methodBuilder("bindView");
    bindViewMethod.addParameter(parameter.build());
    bindViewMethod.addModifiers(Modifier.PUBLIC);
    bindViewMethod.addStatement("$T temp = ($T)target", parent, parent);
    for (Element element :
        elementList) {
      int id = element.getAnnotation(BindView.class).value();
      bindViewMethod.addStatement("temp.$N = temp.findViewById($L)", element.getSimpleName().toString(), id);
    }

    return bindViewMethod.build();
  }

}
