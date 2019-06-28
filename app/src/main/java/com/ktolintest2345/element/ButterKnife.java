package com.ktolintest2345.element;

import com.ktolintest2345.apt_api.ViewBinding;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @ClassName: ButterKnife
 * @Author: Sun Peng
 * @Date: 2019/6/28 12:17
 * @Description:
 */

public class ButterKnife {

  static final Map<Class<?>, Constructor<? extends ViewBinding>> BINDINGS = new LinkedHashMap<>();

  public static void inject(Object object) {
    if (object == null) {
      return;
    }
    try {
      Class<?> cls = object.getClass();
      Constructor<? extends ViewBinding> constructor = findBindingConstructorForClass(cls);
      ViewBinding viewBinding = constructor.newInstance();
      viewBinding.bindView(object);
    } catch (Exception e) {

    }
  }

  private static Constructor<? extends ViewBinding> findBindingConstructorForClass(Class<?> cls) throws Exception {
    Constructor<? extends ViewBinding> constructor = BINDINGS.get(cls);
    if (constructor == null) {
      String className = cls.getName();
      Class<?> bindingClass = cls.getClassLoader().loadClass(className + "_ViewBinding");
      constructor = (Constructor<? extends ViewBinding>) bindingClass.getConstructor();
      BINDINGS.put(cls, constructor);
    }
    return constructor;
  }
}
