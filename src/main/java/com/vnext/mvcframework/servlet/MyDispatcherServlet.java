package com.vnext.mvcframework.servlet;

import com.sun.org.apache.bcel.internal.generic.IFNONNULL;
import com.sun.org.apache.bcel.internal.generic.IFNULL;
import com.vnext.mvcframework.annotation.*;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyDispatcherServlet extends HttpServlet {

    // 所有的配置都存入了properties中
    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<String>();

    private Map<String, Object> ioc = new HashMap<String, Object>();

    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    private List<Handler> handlerMappingList = new ArrayList<Handler>();

    // 初始化阶段调用的方法
    @Override
    public void init(ServletConfig config) throws ServletException {
        //super.init(config);

        // 1.加载配置文件
        doLoadConfig(config.getInitParameter("contextconfigLocation"));

        // 2.根据配置文件扫描所有的相关类
        doScanner(properties.getProperty("scanPackage"));

        // 3.初始化所有的相关类的实例,并且将其放入到IOC容器之中,也就是Map中
        doInstance();

        // 4.DI实现自动依赖注入
        doAutowired();

        // 5.初始化HandlerMapping
        initHandlerMapping();


        System.out.println("--------My MVC is init--------");
    }

    private void doLoadConfig(String contextconfigLocation) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextconfigLocation);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void initHandlerMapping() {
        if (ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            // 把Controller层以外的过滤掉
            if (!clazz.isAnnotationPresent(MyController.class)) continue;
            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            // --------------
//            Method[] methods = clazz.getMethods();
//            for (Method method : methods) {
//                if (!method.isAnnotationPresent(MyRequestMapping.class)) continue;
//                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
//                String url = (baseUrl + requestMapping.value()).replaceAll("/+", "/");
//                handlerMapping.put(url, method);
//                System.out.println("Mapping:" + url + "," + method);
//            }

            //===========
            // 获取method的url配置
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                // 没有加requestMapping注释的直接忽略
                if (!method.isAnnotationPresent(MyRequestMapping.class)) continue;
                // 映射URL
                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                String regex = ("/" + baseUrl + requestMapping.value()).replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                handlerMappingList.add(new Handler(entry.getValue(),method,pattern));
                System.out.println("Mapping:" + regex + "," + method);
            }

        }
    }

    private void doAutowired() {
        if (ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 1.要获取到所有的字段,Field

            // 不管是private还是protected还是default都要强制
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) continue;
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    // 如果beanName是默认值的话
                    beanName = field.getType().getName();
                }
                // 想要访问到私有的，或者受保护的，我们强制授权访问
                field.setAccessible(true);
                try {
                    //field.set(entry.getValue(), ioc.get(beanName));
                    field.set(entry.getValue(), ioc.get("demoService"));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        // 如果不为空,利用发射机制将刚刚扫描进来的所有的className初始化
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                // 接下来进入Bean实例化阶段，初始化IOC容器了
                // 不是所有的class都进行初始化,有注解的才初始化

                // IOC容器的规则
                //1.key默认用类型首字母小写
                //2.如果用户自定义名字，那么要优先使用自定义名字
                //3.如果是接口的话，我们可以巧用接口类作为key

                if (clazz.isAnnotationPresent(MyController.class)) {
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());

                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    //2.如果用户自定义名字，那么要优先使用自定义名字
                    MyService service = clazz.getAnnotation(MyService.class);
                    String beanName = service.value();
                    if ("".equals(beanName.trim())) {
                        // 如果等于空，用默认的数据
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);

                    //3.如果是接口的话，我们可以巧用接口类作为key
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        // 将接口的类型作为思想key
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doScanner(String packageName) {
        // 进行递归扫描,扫描到所有的class
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File classDir = new File(url.getFile());
        for (File file : classDir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(packageName + "." + file.getName());
            } else {
                String className = packageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }


    }

    // 将字符串首字母小写
    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    // 6.等待请求,进入运行阶段
    // 运行时阶段要执行的方法
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //super.doPost(req, resp);
//        String url = req.getRequestURI();
//        String contextPath = req.getContextPath();
//        url = url.replace(contextPath, "").replaceAll("/+", "/");
//        if (!handlerMapping.containsKey(url)) {
//            resp.getWriter().write("404 NOT Found!");
//        }
//        Method method = handlerMapping.get(url);
//        System.out.println("method=" + method);

        // 反射的方法：
        // 需要两个参数，第一个拿到这个method的instance；第二个参数，要拿到实参，从request中取值
        // method.invoke()

        //-----------------------------------------
        try {
            // 开始匹配到对于的方法
            doDispatch(req,resp);
        }catch (Exception e){
            // 如果匹配过程出现异常，将异常信息打印出去
            resp.getWriter().write("500 Exception,Details:\r\n" + Arrays.toString(e.getStackTrace())
                    .replaceAll("\\[|\\]","").replaceAll(",\\s","\r\n"));
        }

    }


    private void doDispatch(HttpServletRequest request,HttpServletResponse response) throws Exception{

        try {
            Handler handler = getHandler(request);
            if (handler == null) {
                // 如果没有匹配上，返回404
                response.getWriter().write("404 NOT Found");
                return;
            }
            // 获取方法的参数列表
            Class<?>[] paramTypes = handler.method.getParameterTypes();

            // 保存所有的需要自动赋值的参数值
            Object[] paramValues = new Object[paramTypes.length];

            // 这是属于J2EE中的内容
            Map<String, String[]> params = request.getParameterMap();
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");

                // 如果找到匹配的对象，则开始填充参数值
                if (!handler.paramIndexMapping.containsKey(param.getKey())) continue;
                int index = handler.paramIndexMapping.get(param.getKey());
                //paramValues[index] = convert(paramTypes[index], value);
                paramValues[index] = castStringValue(value,paramTypes[index]);

            }

            // 设置方法中的request和response对象
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = request;
            int responseIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[responseIndex] = response;

            handler.method.invoke(handler.controller, paramValues);
        } catch (Exception e) {
            throw e;
        }

    }

    // 类型转换
    private Object castStringValue(String value, Class<?> clazz) {
        if (clazz == String.class){
            return value;
        }else if(clazz == Integer.class){
            return Integer.valueOf(value);
        }else if(clazz == int.class){
            return Integer.valueOf(value).intValue();
        } else {
            return null;
        }
    }

    private Handler getHandler(HttpServletRequest request) throws Exception{
        if(handlerMappingList.isEmpty()) return null;
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");
        for (Handler handler : handlerMappingList) {
            try {
                Matcher matcher = handler.pattern.matcher(url);
                // 如果没有匹配上继续下一个匹配
                if (!matcher.matches()) continue;
                return handler;
            }catch (Exception e){
                throw e;
            }
        }
        return null;
    }


    /**
     * Handler 记录Controller中的RequestMapping和Method的对应关系
     */
    class Handler {
        protected Object controller;  // 保存方法对应的实例
        protected Method method;      // 保存映射的方法
        protected Pattern pattern;
        protected Map<String, Integer> paramIndexMapping; // 参数顺序

        public Handler(Object controller, Method method, Pattern pattern) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            paramIndexMapping = new HashMap<String, Integer>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method) {
            // 提取方法中加了注解的参数
            Annotation[][] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length; i++) {
                for (Annotation annotation : pa[i]) {
                    if (annotation instanceof MyRequestParam) {
                        String paramName = ((MyRequestParam) annotation).value();
                        if (!"".equals(paramName.trim())) {
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }

                // 提取方法中的request和response参数
                Class<?>[] parameterTypes = method.getParameterTypes();
                for (int j = 0; j < parameterTypes.length; j++) {
                    Class<?> parameterType = parameterTypes[j];
                    if (parameterType == HttpServletRequest.class || parameterType == HttpServletResponse.class){
                        paramIndexMapping.put(parameterType.getName(),i);
                    }
                }

            }
        }
    }


}
