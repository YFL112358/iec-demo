# iec-demo

## 背景

- java服务读取机器的配置参数数据，并且把数据展示出来或者存储起来，所以使用iec

## 参考

- https://www.beanit.com/iec-61850/ 

## demo实现

1、下载iec编译好的包：https://www.beanit.com/iec-61850/distributions/iec61850bean-1.9.0.tgz 
      
2、解压包 

3、进入iec61850bean/bin目录，启动iec的模拟机器的服务器端，执行如下脚本  

```
iec61850bean-console-server.bat -m sample-model.icd
```

4、启动客户端
- 可以执行以下脚本来模拟java应用客户端，连接模拟机器的服务器端
```
iec61850bean-gui-client.bat
```
-  也可以启动springboot应用来连接模拟机器的服务器端，启动springboot成功后，调用以下地址，可以查看到模拟机器的参数数据
```
http://localhost:8080/getIceData
```


