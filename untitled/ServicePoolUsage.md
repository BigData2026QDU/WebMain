# ServicePool 使用说明

## 实现了什么

- 通过 Apache Commons Pool2 将“昂贵的 Service 实例”提前初始化到池中（默认 32 个），避免每次业务调用都重新 new。
- 统一由 `ServicePoolManager` 进行借用与归还，支持多种 Service 类型并发使用。
- 全局单例，应用内只需要注册一次即可重复使用。

## 适用场景

- Service 初始化成本高（加载模型、预热连接、构建缓存等）。
- 多线程并发访问同一类 Service，需要复用实例但又不想共享单例对象状态。
- 希望统一管理生命周期和回收动作。

## 使用方式

### 1. 注册 Service（应用启动时做一次）

```java
import org.example.Tool.ServicePoolManager;

ServicePoolManager poolManager = ServicePoolManager.getInstance();
poolManager.

registerService(ExpensiveService .class, ExpensiveService::new, 32);
```

### 2. 借用与归还

```java
ExpensiveService service = poolManager.borrowService(ExpensiveService.class);
try {
    service.doWork();
} finally {
    poolManager.returnService(ExpensiveService.class, service);
}
```

### 3. 销毁异常实例（可选）

```java
try {
    service.doWork();
} catch (Exception ex) {
    poolManager.invalidateService(ExpensiveService.class, service);
    service = null;
    throw ex;
} finally {
    if (service != null) {
        poolManager.returnService(ExpensiveService.class, service);
    }
}
```

### 4. 自定义销毁逻辑（可选）

```java
poolManager.registerService(
    ExpensiveService.class,
    ExpensiveService::new,
    32,
    ExpensiveService::shutdown
);
```

> 如果 Service 实现了 `AutoCloseable`，默认会在池销毁对象时调用 `close()`。

## 设计要点

- 每种 Service 类型对应一个独立的对象池（`GenericObjectPool`）。
- 默认预热 32 个实例（`minIdle/maxIdle/maxTotal` 都设置为 32）。
- 线程安全，可并发借用与归还。

## 注意事项

- Service 实例最好是“可复用”或“可重置”的，避免脏状态污染下一次借用。
- 如果借用失败会抛出运行时异常，请在业务侧兜底处理。
- 应用关闭时可调用 `ServicePoolManager.getInstance().shutdown()` 释放资源。
