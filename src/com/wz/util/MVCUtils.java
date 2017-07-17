package com.wz.util;

import org.objectweb.asm.*;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by wz on 2017-07-17.
 */
public class MVCUtils {
    /**
     * 获取类指定方法的参数列表
     * @param clazz 类
     * @param method 方法
     * @return
     */
    public static String[] getMethodParamNames(final Class clazz, final Method method){
        final String methodName = method.getName();
        final Class[] methodParamTypes = method.getParameterTypes();
        final int methodParamCount = methodParamTypes.length;
        String className = method.getDeclaringClass().getName();
        final boolean isStatic  = Modifier.isStatic(method.getModifiers());
        final String[] methodParamNames = new String[methodParamCount];
        int lastDotIndex = className.lastIndexOf(".");
        className = className.substring(lastDotIndex + 1) + ".class";
        InputStream inputStream = clazz.getResourceAsStream(className);
        try {
            ClassReader cr = new ClassReader(inputStream);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cr.accept(new ClassAdapter(cw) {
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                    final Type[] argTypes = Type.getArgumentTypes(desc);
                    //参数类型不一致
                    if (!methodName.equals(name) || !matchTypes(argTypes, methodParamTypes)) {
                        return mv;
                    }
                    return new MethodAdapter(mv) {
                        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                            //如果是静态方法，第一个参数就是方法参数，非静态方法，则第一个参数是 this ,然后才是方法的参数
                            int methodParameterIndex = isStatic ? index : index - 1;
                            if (0 <= methodParameterIndex && methodParameterIndex < methodParamCount) {
                                methodParamNames[methodParameterIndex] = name;
                            }
                            super.visitLocalVariable(name, desc, signature, start, end, index);
                        }
                    };
                }
            }, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return methodParamNames;
    }

    private static boolean matchTypes(Type[] types, Class<?>[] parameterTypes) {
        if (types.length != parameterTypes.length) {
            return false;
        }
        for (int i = 0; i < types.length; i++) {
            if (!Type.getType(parameterTypes[i]).equals(types[i])) {
                return false;
            }
        }
        return true;
    }
}
