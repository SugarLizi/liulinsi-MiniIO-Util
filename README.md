# liulinsi-MiniIO-Util

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

一个简化MinIO文件操作的Java工具包，提供文件上传、删除、信息查询等功能，支持本地文件路径上传和流式上传。

## 目录
- [安装依赖](#安装依赖)
- [快速开始](#快速开始)
- [核心功能](#核心功能)
  - [文件上传](#文件上传)
  - [文件删除](#文件删除)
  - [文件信息查询](#文件信息查询)
  - [获取文件URL](#获取文件url)
- [高级功能](#高级功能)
  - [前缀目录支持](#前缀目录支持)
  - [修改MinIO服务器地址](#修改minio服务器地址)
  - [桶策略检测](#桶策略检测)
- [注意事项](#注意事项)
- [更新日志](#更新日志)

## 安装依赖

在项目中引入以下依赖（暂不可用）：

```xml
<dependency>
    <groupId>com.sugarli.utils</groupId>
    <artifactId>liulinsi-MiniIO-Util</artifactId>
    <version>5.0</version>
</dependency>
```

或直接下载JAR文件：
[liulinsi-MiniIO-Util-5.0.jar](http://oss.sugarli.top:9000/aaa/liulinsi-MiniIO-Util-5.0.jar)

## 快速开始

```java
import com.sugarli.utils.MinIOFileUtil;

public class Demo {
    public static void main(String[] args) throws Exception {
        // 初始化工具类
        MinIOFileUtil minIO = new MinIOFileUtil(
            "YOUR_ACCESS_KEY_ID", 
            "YOUR_ACCESS_KEY_SECRET", 
            "BUCKET_NAME"
        );
        
        // 上传文件
        String url = minIO.uploadFileByPath("logo.png", "/path/to/local/logo.png");
        System.out.println("文件URL: " + url);
        
        // 获取文件信息
        String info = minIO.info(url);
        System.out.println("文件信息: " + info);
    }
}
```

## 核心功能

### 文件上传

**通过文件路径上传：**
```java
String url = minIO.uploadFileByPath("filename.ext", "/local/path/file.ext");
```

**流式上传（适合Spring Boot后端）：**
```java
@PostMapping("/upload")
public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
    try {
        String url = minIO.uploadFileByStream(
            "filename.ext", 
            file.getInputStream(), 
            file.getSize()
        );
        return ResponseEntity.ok(Collections.singletonMap("url", url));
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body("上传失败: " + e.getMessage());
    }
}
```

### 文件删除
```java
minIO.deleteObject("http://your_ip:9000/bucket/file.ext");
```

### 文件信息查询

**查询桶内所有文件：**
```java
String bucketInfo = minIO.info();
System.out.println(bucketInfo);
```

**查询单个文件信息：**
```java
String fileInfo = minIO.info("http://your_ip:9000/bucket/file.ext");
System.out.println(fileInfo);
```

### 获取文件URL
```java
String url = minIO.getURL("filename.ext");
System.out.println("文件URL: " + url);
```

## 高级功能

### 前缀目录支持
```java
// 上传到aaa目录
String url1 = minIO.uploadFileByPath("aaa", "file.txt", "/local/file.txt");

// 上传到aaa/bbb目录
String url2 = minIO.uploadFileByPath("aaa/bbb", "file.txt", "/local/file.txt");
```

### 修改MinIO服务器地址
```java
// 获取当前服务器地址
String currentEndpoint = minIO.getEndpoint();

// 修改服务器地址
minIO.setEndpoint("http://new.server:9000");
```

### 桶策略检测
```java
String policy = minIO.getBucketStatue();
System.out.println("桶策略: " + policy); // 输出: Public 或 Private
```

## 注意事项

1. **URL有效期**：
   - 当桶策略为`Private`时，返回的URL有效期为7天
   - 当桶策略为`Public`时，返回永久有效的URL
   - 桶策略需在MinIO管理后台修改

2. **路径安全**：
   - 路径中禁止使用`..`防止目录遍历攻击
   - 空文件名会被拒绝

3. **文件管理**：
   - 请妥善保存文件URL，丢失后可通过`getURL()`方法找回
   - 删除操作不可逆，请谨慎使用

4. **自动创建桶**：
   - 如果指定的桶不存在，工具会自动创建

5. **性能优化**：
   - 文件上传使用20MB分块，优化大文件传输

## 更新日志

### v5.0
- 添加`getEndpoint()`/`setEndpoint()`方法
- 添加桶策略自动检测功能
- 根据桶策略返回不同类型URL

### v4.5
- 增强路径安全性
- 添加前缀目录支持
- 修复潜在安全漏洞

### v4.0
- 添加流式上传支持
- 改进URL解析方法
- 优化错误处理

### v3.0
- 添加`getURL()`方法
- 改进文件存在性检查

### v2.0
- 添加文件信息查询功能
- 优化API设计

### v1.0
- 初始版本发布
- 支持文件上传和删除功能
