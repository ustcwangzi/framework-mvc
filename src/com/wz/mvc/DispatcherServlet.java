package com.wz.mvc;

import com.wz.annotation.*;
import com.wz.util.MVCUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by wz on 2017-07-17.
 */
public class DispatcherServlet extends HttpServlet {
    //保存被Service或Controller注解的类
    private List<String> classNames = new ArrayList<>();
    //保存beanName和实例的映射
    private Map<String, Object> beanMapping = new HashMap<>();
    //保存URL和方法的映射
    //private Map<Pattern, HandlerModel> handlerMapping = new HashMap<>();
    private List<HandlerModel> handlerMapping = new ArrayList<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("init ...");
        //获取被注解的类
        scanPackage(config.getInitParameter("scanPackage"));
        //实例化被注解的类
        initBean();
        //给被AutoWired注解的属性注入值
        doAutoWired();
        //建立URL到方法的映射
        doHandlerMapping();
        System.out.print(beanMapping);
    }

    /**
     * 扫描被注解的类
     * @param pkgName
     */
    private void scanPackage(String pkgName){
        //将com.wz变成/com/wz
        URL url = getClass().getClassLoader().getResource("/" + pkgName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        //递归查询所有class文件
        for (File file : dir.listFiles()){
            if(file.isDirectory()){ //是目录，递归下一层
                scanPackage(pkgName + "." + file.getName());
            }else{
                if(!file.getName().endsWith(".class")){
                    continue;
                }
                String className = pkgName + "." + file.getName().replace(".class", "");
                try{ //判断是否被Controller或者Service注解了
                    Class clazz =  Class.forName(className);
                    if(clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(Service.class)){
                        classNames.add(className);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 实例化被注解的类
     */
    private void initBean(){
        if (classNames.size() == 0){
            return;
        }
        //遍历被注解的类，并实例化
        for (String className : classNames){
            try{
                Class clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller.class)){
                    beanMapping.put(lowerFirstChar(clazz.getSimpleName()), clazz.newInstance());
                }else if (clazz.isAnnotationPresent(Service.class)){
                    Service service = (Service) clazz.getAnnotation(Service.class);
                    String value = service.value(); //获取注解上的值
                    if(!"".equals(value.trim())){ //有值
                        beanMapping.put(value.trim(), clazz.newInstance());
                    }else { //没有值，使用接口名
                        Class[] inters = clazz.getInterfaces(); //获取接口
                        for (Class c: inters){
                            beanMapping.put(lowerFirstChar(c.getSimpleName()), clazz.newInstance());
                            break;
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 给被AutoWired注解的属性注入值
     */
    private void doAutoWired(){
        if (beanMapping.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : beanMapping.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            //查找所有被AutoWired注解的属性
            for (Field field : fields){
                if (!field.isAnnotationPresent(Autowired.class)){
                    continue;
                }
                String beanName;
                Autowired autowired = field.getAnnotation(Autowired.class);
                if("".equals(autowired.value().trim())){
                    beanName = lowerFirstChar(field.getType().getSimpleName());
                }else {
                    beanName = autowired.value().trim();
                }
                //将私有属性的访问权限公开
                field.setAccessible(true);
                if (beanMapping.containsKey(beanName)){
                    try{
                        field.set(entry.getValue(), beanMapping.get(beanName));
                    }catch (IllegalAccessException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 建立URL到方法的映射
     */
    private void doHandlerMapping(){
        if (beanMapping.isEmpty()){
            return;
        }
        for (Map.Entry<String, Object> entry : beanMapping.entrySet()){
            Class clazz = entry.getValue().getClass();
            //只处理被Controller注解的
            if (!clazz.isAnnotationPresent(Controller.class)){
                continue;
            }
            String url = "/";
            if (clazz.isAnnotationPresent(RequestMapping.class)){
                RequestMapping requestMapping = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
                url += requestMapping.value();
            }
            Method[] methods = clazz.getMethods();
            //处理被RequestMapping注解的方法
            for (Method method : methods){
                if (!method.isAnnotationPresent(RequestMapping.class)){
                    continue;
                }
                RequestMapping methodMapping = method.getAnnotation(RequestMapping.class);
                String regex = (url + "/" + methodMapping.value().trim()).replaceAll("/+", "/").replaceAll("\\*", ".*");
                //获取参数上的注解
                Annotation[][] annotations = method.getParameterAnnotations();
                Map<String, Integer> paranMap = new HashMap<>();
                //获取参数名
                String[] paramNames = MVCUtils.getMethodParamNames(clazz, method);
                //获取参数类型
                Class[] paramTypes = method.getParameterTypes();
                for (int i = 0; i < annotations.length; i++){
                    //获取每个参数上的注解
                    Annotation[] anns = annotations[i];
                    if (anns.length == 0){ //无注解
                        //如果是Request或者Response，就直接用类名作key；如果是普通属性，就用属性名
                        Class type = paramTypes[i];
                        if (type == HttpServletResponse.class || type == HttpServletRequest.class){
                            paranMap.put(type.getName(), i);
                        }else {
                            paranMap.put(paramNames[i], i);
                        }
                        continue;
                    }else { //有注解
                        for (Annotation ann : anns){
                            if (ann.annotationType() == RequestParam.class){
                                String value = ((RequestParam)ann).value();
                                if (!"".equals(value.trim())){
                                    paranMap.put(value, i);
                                }
                            }
                        }
                    }
                }
                handlerMapping.add(new HandlerModel(Pattern.compile(regex), method, entry.getValue(), paranMap));
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //根据请求的URL去查找对应的method
        try {
            boolean isMatcher = isMatch(req, resp);
            if (!isMatcher) {
                out(resp,"404 not found");
            }
        } catch (Exception ex) {
            ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            ex.printStackTrace(new java.io.PrintWriter(buf, true));
            String expMessage = buf.toString();
            buf.close();
            out(resp, "500 Exception" + "\n" + expMessage);
        }
    }

    /**
     * URL是否匹配
     * @param req
     * @param resp
     * @return
     * @throws Exception
     */
    private boolean isMatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        if (handlerMapping.isEmpty()) {
            return false;
        }
        //用户请求地址
        String requestUri = req.getRequestURI();
        String contextPath = req.getContextPath();
        //用户写了多个"///"，只保留一个
        requestUri = requestUri.replace(contextPath, "").replaceAll("/+", "/");
        //遍历HandlerMapping，寻找url匹配的
        for (HandlerModel handler : handlerMapping) {
            if (!handler.pattern.matcher(requestUri).matches()) {
                continue;
            }
            Map<String, Integer> paramIndexMap = handler.paramMap;
            //定义一个数组来保存应该给method的所有参数赋值的数组
            Object[] paramValues = new Object[paramIndexMap.size()];
            Class<?>[] types = handler.method.getParameterTypes();
            //遍历一个方法的所有参数[name->0,addr->1,HttpServletRequest->2]
            for (Map.Entry<String, Integer> param : paramIndexMap.entrySet()) {
                String key = param.getKey();
                if (key.equals(HttpServletRequest.class.getName())) {
                    paramValues[param.getValue()] = req;
                } else if (key.equals(HttpServletResponse.class.getName())) {
                    paramValues[param.getValue()] = resp;
                } else {
                    //如果用户传了参数，譬如 name= "wolf"，做一下参数类型转换，将用户传来的值转为方法中参数的类型
                    String parameter = req.getParameter(key);
                    if (parameter != null) {
                        paramValues[param.getValue()] = convert(parameter.trim(), types[param.getValue()]);
                    }
                }
            }
            //激活该方法
            handler.method.invoke(handler.controller, paramValues);
            return true;
        }
        return false;
    }

    /**
     * 将用户传来的参数转换为方法需要的参数类型
     * @param parameter
     * @param targetType
     * @return
     */
    private Object convert(String parameter, Class<?> targetType) {
        if (targetType == String.class) {
            return parameter;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(parameter);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(parameter);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            if (parameter.toLowerCase().equals("true") || parameter.equals("1")) {
                return true;
            } else if (parameter.toLowerCase().equals("false") || parameter.equals("0")) {
                return false;
            }
            throw new RuntimeException("不支持的参数");
        }
        else {
            //还有很多其他的类型，char、double之类的依次类推，也可以做List<>, Array, Map之类的转化
            return null;
        }
    }

    private static String lowerFirstChar(String str){
        if (str != null && str.length() > 0){
            char[] chars = str.toCharArray();
            if (chars[0] > 64 && chars[0] < 91){
                chars[0] += 32;
            }
            return String.valueOf(chars);
        }
        return str;
    }

    private void out(HttpServletResponse response, String str){
        try{
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().print(str);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private class HandlerModel{
        private Pattern pattern;
        private Method method;
        private Object controller;
        private Map<String, Integer> paramMap;

        public HandlerModel(Pattern pattern, Method method, Object controller, Map<String, Integer> paramMap) {
            this.pattern = pattern;
            this.method = method;
            this.controller = controller;
            this.paramMap = paramMap;
        }
    }
}
