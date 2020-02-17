# 秒杀示例

1. 启动后打开地址： http://localhost:27000/seckill/list
2. 点击秒杀按钮进入秒杀页面
3. 在弹出输入框内随意输入11位数值
4. 点击“开始秒杀”按钮进行秒杀


原示例地址：https://github.com/bootsrc/jseckill

# 开发日志

#### 【2020-02-17】

* 完成初步修改

* 运行示例需要MySQL、RabbitMQ服务、Zookeeper服务器

* 可能出现的错误：

##### 【错误一】

```
02/17-23:35:08 [http-nio-27000-exec-75] ERROR org.xwl.demo.seckill.service.impl.SeckillServiceImpl- null
org.xwl.demo.seckill.exception.SeckillException: null
	at org.xwl.demo.seckill.service.impl.SeckillServiceImpl.handleSeckillAsync(SeckillServiceImpl.java:178)
	at org.xwl.demo.seckill.service.impl.SeckillServiceImpl.executeSeckill(SeckillServiceImpl.java:127)
	at org.xwl.demo.seckill.service.impl.SeckillServiceImpl$$FastClassBySpringCGLIB$$3c6ab3c.invoke(<generated>)
	at org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)
	at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:684)
	at org.xwl.demo.seckill.service.impl.SeckillServiceImpl$$EnhancerBySpringCGLIB$$90d560f.executeSeckill(<generated>)
	at org.xwl.demo.seckill.controller.SeckillController.execute(SeckillController.java:98)	
```
对应代码位置： throw new SeckillException(SeckillStateEnum.REPEAT_KILL);	
应该跟动态代理有关系，暂未确定具体原因

##### 【错误二】

```
02/17-23:36:17 [http-nio-27000-exec-27] ERROR org.xwl.demo.seckill.controller.SeckillController- Cannot be started more than once
java.lang.IllegalStateException: Cannot be started more than once
	at org.apache.curator.framework.imps.CuratorFrameworkImpl.start(CuratorFrameworkImpl.java:311)
	at org.xwl.demo.seckill.service.impl.SeckillServiceImpl.handleSeckillAsync(SeckillServiceImpl.java:162)
```
	
CuratorFrameworkImpl重复启动的问题，未找到具体原因，目前看不影响应用正常运行。


##### 【可考虑的其他尝试】

1. 前后分离、动静分离
2. 增加用户登录模块
3. 使用其他验证代替输入电话号码
4. 使用其他分布式锁
5. 不使用分布式锁

