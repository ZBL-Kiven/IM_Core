<p align="center" >
   <img src = "https://github.com/ZBL-Kiven/iM-Core/raw/master/demo/title.png"/>
   <br>
   <a href = "http://cityfruit.io/">
   <img src = "https://img.shields.io/static/v1?label=By&message=CityFruit.io&color=2af"/>
   </a>
   <a href = "https://github.com/ZBL-Kiven/loadingView">
      <img src = "https://img.shields.io/static/v1?label=platform&message=Android&color=6bf"/>
   </a>
   <a href = "https://github.com/ZBL-Kiven">
      <img src = "https://img.shields.io/static/v1?label=author&message=ZJJ&color=9cf"/>
  </a>

  <a href = "https://www.android-doc.com/guide/components/android7.0.html">
      <img src = "https://img.shields.io/static/v1?label=minVersion&message=Nougat&color=cce"/>
  </a>
</p>
 
## Introduction：

###### IMCore 是 IM 框架的底层实现，内部以 MultiPipe + PriQueue + Looper + Auto-Efficiency 实现，可保证消息时序、跟踪式发送、回调式发送、条件阻断、全链路拦截等高要求性需求。经长期多个大型项目运行实测，性能卓越，表现稳定优异，不依赖任何第三方库，兼容 Android 目前全版本。


## Features：

> 配置：

* 支持 配置服务端连接 （http、grpc、socket、webSocket 等）
* 支持 配置数据 加/解密 算法

> 启动：随时一键 启动/停止 核心服务。

* 兼容重复启动
* 兼容重复终止
* 支持：链路自检自修复
* 支持：自动重连及自动队列阻塞功能[1]
* 支持：网络状态监听
* 支持：网络信号强度监听

>日志

* 支持：运行状态 log 文件缓存
* 支持：收发详情 log 文件缓存
* 支持：log 文件路径自定义
* 支持：log 文件动态分段
* 支持：数据包大小记录
* 支持：数据动态监听

> 队列
 
* 支持：队列阻塞
* 支持：线程调度
* 支持：负载均衡
* 支持：优先级分发
* 支持：运行效率动态自适应

> 消息

* 支持：消息发送
* 支持：消息重发
* 支持：消息组合
* 支持：消息拦截 & 再次编辑
* 支持：自定义消息 ID
* 支持：消息跟踪
* 支持：消息发送状态监听
* 支持：消息发送进度回调
* 支持：消息发送线程挂载
* 支持：消息发送时序
* 支持：消息超时

> 心跳：配置 / 关闭 客户端接口 - 心跳。

* 支持：动态（向量）心跳
* 支持：定时心跳
* 支持：取消心跳
* 支持：心跳参数 静/动 态配置
* 支持：心跳模组 静/动 态解析
* 支持：心跳超时
* 支持：心跳循环条件配置
 
> 认证：配置 / 关闭 客户端接口 - 认证。

 * 支持：跳过认证
 * 支持：阻塞 / 非阻塞 认证
 * 支持：认证参数 静/动 态配置
 * 支持：认证模组 静/动 态解析
 * 支持：认证超时
 
> 解析：

 * 支持：自定义配置客户端数据解析器
 * 支持：泛参合并
 * 支持：丢弃解析数据

> 支持：数据库自定义

 * 支持：泛参合并
 * 支持：使用自定义数据库
 * 支持：单个事件数据库逻辑处理
 * 支持：单个事件处理生命周期回调

> 订阅
 
 * 支持：消息订阅
 * 支持：本地消息按条件返回
 * 支持：订阅条件过滤
 * 支持：取消订阅
 
> Query

 * 支持：常见 SQL 协议键入
 * 支持：自定义逻辑转 SQL
 
> server

  * 支持：自定义 Server
  * 支持：server 路径动态获取
  * 支持：server 断线通知
  * 支持：service 自定义
  

> 单元测试

- 暂无

## demo：

本模块暂无测试 demo 可供把玩。

## Installation :


im-core 已发布至私有仓库，你可以使用如下方式安装它：

> by dependencies:

```kotlin
repo{
     maven (url = "https://nexus.i-mocca.com/repository/cf_core")
}

implementation 'com.cf.core:im-core:+'
```

> by [aar](https://nexus.i-mocca.com/repository/cf_core/com/cf/core/im-core/1.0.0/im-core-1.0.0.aar) import:

```
copy the aar file in your app libs to use
```

> by [module](https://github.com/ZBL-Kiven/iM-Core/archive/master.zip) copy:
 
```
copy the module 'im-core' into your app

implementation project(":im-core")

```

## Usage:

> 定义 SDK 的客户端

``` kotlin
class MyClient:MessageHubClient{...}

```

> 定义 SDK 的服务端

``` kotlin
class MyServer:MessageHubServer{...}

```
> 定义 SDK 的接口

```kotlin
class yourInterface:IMInterface{...}
```
> 开始 SDK
 
```kotlin
yourInterface.initChat(BaseOption)
```
> 创建一个 options

```kotlin
BaseOption.create(Application) //开始配置，以下为可选。

.setNotify(notification: Notification?)//用于保活的前台服务通知实现，nullable。

.setSessionId(id:Int) //用于服务的 sessionid，默认可忽略，当需要使用多个 notify 或 动态 notify 时使用。

.setLevel(efficiency: RuntimeEfficiency)//枚举类型，用于配置 SDK 默认功耗等级，默认为 medium。

.logsCollectionAble(()->boolean) //lambda 返回是否允许收集日志（比如动态控制日志收集上传）。

.logsFileName(String) //日志默认保存在 Context.externalCacheDir/${logsFileName} 下。

.logsMaxRetain(Long) //日志默认保存时间，超时自动清理。

.build(OnBuildOption)//完成配置并启动 SDK。
```

> OnBuildOption

```kotlin
//实现抽象方法

@abstract getClient() // return yourClient

@abstract getServer() // return yourServer 

@abstract onError() // 处理可能抛出的异常信息 如 Token 失效等

可复写扩展

prepare（）//当 IM-SDK 每次初始化完成，都会调用此方法。

shutdown（） //当 IM—SDK 执行完成 shutdown 后回调。

onLayerChanged(inBackground: Boolean)//应用层级改变时调用，比如从后台切换至前台

checkNetWorkIsWorking() 定义一个自定义的网络链接情况。比如自己的服务器是否可以成功 ping，消息发送队列会根据此返回值的 false 状态延迟出栈并等待其返回 true 值。不过通常情况下你并不需要复写此方法。


``` 


### Contributing

Contributions are very welcome 🎉

### Licence :  

Copyright (c) 2019 CityFruit zjj0888@gmail.com<br>
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.<br>
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
