## spring的几个重要阶段
### spring配置阶段
```
1. web.xml
2. DispatcherServlet    --> spring web开发中的入口
3. application.xml路径  --> 配置spring启动前需要加载的bean
4. url-pattern          --> 方便匹配用户在浏览器输入的地址
```

### spring初始化阶段
```
1. init()                --> 由web容器自动调用Servlet的初始方法
2. 加载application.xml   --> 
3. 加载IOC容器           --> 就是Map<String,Object>
4. 依赖注入              --> @Autowired
5. 初始化HandlerMapping  --> 就是Map<String,Method>
6. 存储储@RequestMapping配置的url，url会和方法对应上保存到HandlerMapping中
```
### spring运行时阶段
```
1. service(Request,Response)        --> 只要用户请求,就会自动调用doservice
2. Request.getURI()                 --> 获得用户请求的URL,匹配URL和其对应的Method
3. 调用method                       --> 利用反射动态调用method,
4. 利用response将调用结果输出到浏览器
```

## 知识点
注解,反射,代理

## 测试
```
http://localhost:8080/demo/query.json?name=tone
```