package com.invokeTest;

import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author baofeng
 * @date 2022/02/20
 */
public class InvokeTestMain {
    private static Map<String,Object>instanceMap = new HashMap<>();
    private static Map<String,Method>methodMap = new HashMap<>();

    public static void main(String[] args) throws Exception{
        /*SingerServiceImpl singerServiceImpl = new SingerServiceImpl();
        singerServiceImpl.sing(1);*/
        List<Class<?>> clazzs = getAnnotationClasss("com.invokeTest", ServiceHy.class);
        registryService(clazzs);
        System.out.println("instanceMap = " + JSON.toJSONString(instanceMap));
        System.out.println("methodMap = " + JSON.toJSONString(methodMap));
        // 1.固定执行com.invokeTest.SingerService.sing(int no)方法
        String interfaceName = SingerService.class.getName();
        String methodName = "sing";
        for (int i=0;i<5;i++) {
            Object object = instanceMap.get(interfaceName);
            Method method = methodMap.get(interfaceName + "." + methodName);
            System.out.println("i=" + i + "，begin invoke，method=" + method);
            method.invoke(object, i);
        }

        // 2.SongerService
        System.out.println("=========begin song=========");
        interfaceName = SongerService.class.getName();
        for (int i=0;i<5;i++) {
            Object object = instanceMap.get(interfaceName);
            Method method = methodMap.get(interfaceName + "." + methodName);
            System.out.println("i=" + i + "，begin invoke，method=" + method);
            method.invoke(object, i);
        }
    }



    private static List<Class<?>> getAnnotationClasss(String pkgName, Class<?> annotationClass) {
        if (!annotationClass.isAnnotation()) {
            System.out.println("class {} must be annotation.");
            throw new IllegalArgumentException(annotationClass + "is not annotation");
        }
        boolean recursive = true;
        List<Class<?>> clazzs = new ArrayList<Class<?>>();
        String packageDirName = pkgName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    File[] files = new File(filePath).listFiles();
                    for (File file : files) {
                        System.out.println("fullName:" + file.getName());
                        String className = file.getName().substring(0, file.getName().length() - 6);
                        System.out.println("fileName:" + className);
                        Class<?> clazz = Thread.currentThread().getContextClassLoader()
                                .loadClass(pkgName + "." + className);
                        if (clazz.isAnnotationPresent((Class<? extends Annotation>) annotationClass)) {
                            clazzs.add(clazz);
                        }
                    }
                    System.out.println();
                } else {
                    System.out.println("not file,protocol=" + protocol);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clazzs;
    }

    private static void registryService(List<Class<?>> clazzs) {
        for (Class<?> clazz : clazzs) {
            try {
                if (clazz.isAnnotationPresent(ServiceHy.class)) {
                    autowireSingleServiceMethods(clazz);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void autowireSingleServiceMethods(Class<?> serviceClass) throws Exception{
        Class<?>[]interfaces=serviceClass.getInterfaces();
        if(interfaces.length==0){
            throw new IllegalArgumentException(serviceClass + " should have one remote interface");
        }

        for (Class<?> interfaceClass : interfaces) {
            Set<String> methodSet = new HashSet<String>();
            String instanceName = interfaceClass.getName();
            System.out.println("autowire service: " + instanceName);
            Object object = serviceClass.newInstance();
            instanceMap.put(instanceName, object);
            for(Method m:serviceClass.getDeclaredMethods()){
                //Transaction annotation add on impl class so we should use implClass
                if(!Modifier.isPublic(m.getModifiers())){
                    continue;
                }
                if(Modifier.isStatic(m.getModifiers())){
                    continue;
                }
                if (Proxy.isProxyClass(serviceClass)) {
                    if (!methodSet.contains(m.getName()+"_"+m.getParameterCount())) {
                        continue;
                    }
                }
                String methodName=instanceName+"."+m.getName();
                if(methodMap.containsKey(methodName)){
                    throw new IllegalArgumentException("method:"+methodName
                            +" already exists.");
                }
                System.out.println("autowire method: {}" + methodName);
                methodMap.put(methodName, m);
            }
        }
    }

}
