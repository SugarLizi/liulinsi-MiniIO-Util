package com.sugarli.utils;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;

public class MinIOFileUtil {
    private String ENDPOINT = "http://your_ip:9000";
    private String ACCESS_KEY_ID;
    private String ACCESS_KEY_SECRET;
    private String BUCKET_NAME;

    public String getEndpoint() {
        return ENDPOINT;
    }

    public void setEndpoint(String endpoint) {
        ENDPOINT = endpoint;
    }

    public MinIOFileUtil(String accessKeyId, String accessKeySecret, String bucketName) {
        ACCESS_KEY_ID = accessKeyId;
        ACCESS_KEY_SECRET = accessKeySecret;
        BUCKET_NAME = bucketName;
    }

    /**
     *
     * @param objectName 上传后的文件名
     * @param filePath 源文件路径
     * @return 文件上传后的路径
     */
    public String uploadFileByPath(String objectName, String filePath) throws Exception {
        System.out.println("文件上传中，请耐心等待...");
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return "文件上传完毕：\n"+uploadFile("", objectName, inputStream, file.length());
        }
    }

    /**
     *
     * @param startFile 前置目录(例:"aaa", "aaa/bbb")
     * @param objectName 上传后的文件名
     * @param filePath 源文件路径
     * @return 文件上传后的路径
     */
    public String uploadFileByPath(String startFile, String objectName, String filePath) throws Exception {
        System.out.println("文件上传中，请耐心等待...");
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }

        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return "文件上传完毕：\n"+uploadFile(startFile, objectName, inputStream, file.length());
        }
    }

    /**
     * 带用户ID目录的文件上传（自动创建目录）
     *
     * @param objectName 上传后的文件名（不含路径）
     * @param inputStream 文件输入流
     * @param size 文件字节大小
     * @return 文件上传后的完整路径（含用户目录）
     */
    public String uploadFileByStream(String objectName, InputStream inputStream, long size) throws Exception {
        return uploadFile("", objectName, inputStream, size);
    }

    /**
     * 带用户ID目录的文件上传（自动创建目录）
     *
     * @param startFile 前置目录(例:"aaa", "aaa/bbb")
     * @param objectName 上传后的文件名（不含路径）
     * @param inputStream 文件输入流
     * @param size 文件字节大小
     * @return 文件上传后的完整路径（含用户目录）
     */
    public String uploadFileByStream(String startFile, String objectName, InputStream inputStream, long size) throws Exception {
        return uploadFile(startFile, objectName, inputStream, size);
    }

    /**
     *
     * @param startFile 前置目录(例:"aaa", "aaa/bbb")
     * @param objectName 上传后的文件名
     * @param inputStream 文件输入流
     * @param size 文件字节大小
     * @return 文件上传后的路径
     */
    public String uploadFile(String startFile, String objectName, InputStream inputStream, long size) throws Exception {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(ENDPOINT)
                .credentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET)
                .build();

        // 处理startFile路径格式（统一使用正斜杠，去除首尾斜杠）
        String normalizedStartPath = startFile.replace("\\", "/");  // 替换反斜杠
        normalizedStartPath = normalizedStartPath.replaceAll("/+", "/");  // 合并连续斜杠
        normalizedStartPath = normalizedStartPath.replaceAll("^/+|/+$", "");  // 去除首尾斜杠

        // 构建完整对象路径
        String fullObjectName;
        if (normalizedStartPath.isEmpty()) {
            fullObjectName = objectName;
        } else {
            fullObjectName = normalizedStartPath + "/" + objectName;
        }

        // 检查并创建bucket
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
        } catch (Exception e) {
            // 忽略，桶已存在
        }

        // 检查文件名是否为空
        if (objectName == null || objectName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 防止路径遍历攻击
        if (fullObjectName.contains("..")) {
            throw new SecurityException("非法路径");
        }

        // 执行上传
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(BUCKET_NAME)
                .object(fullObjectName)
                .stream(inputStream, size, 20 * 1024 * 1024) // 20MB分块
                .build());

        // 判断桶的策略是否为公开，为公开返回预签名URL，公开则返回路径URL
        String temp_URL = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(BUCKET_NAME)
                .object(fullObjectName)
                .method(Method.GET)
                .build());
        if (getBucketStatue().equals("Private")) {
            return temp_URL;
        } else {
            return temp_URL.split("\\?X-")[0];
        }
    }

    private MinioClient getClient() {
        return MinioClient.builder()
                .endpoint(ENDPOINT)
                .credentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET)
                .build();
    }

    /**
     * 删除指定对象
     * @param objectUrl 要删除的对象路径
     */
    public void deleteObject(String objectUrl) throws Exception {
        try (MinioClient client = getClient()) {
            String s = AnalyzeURL(objectUrl);
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(s)
                            .build()
            );
            System.out.println("文件“"+s+"”删除成功");
        }
    }

    /**
     *
     * @param urlStr 文件的url
     * @return 由URL分析出来的桶名
     */
    public static String AnalyzeBucket(String urlStr) {
        try {
            URI uri = new URI(urlStr);
            String path = uri.getPath().replaceFirst("^/", "");
            String[] segments = path.split("/");
            return segments.length > 0 ? segments[0] : null;
        } catch (URISyntaxException e) {
            System.err.println("无效的URL格式: " + e.getMessage());
            return null;
        }
    }

    /**
     *
     * @param urlStr 文件的url
     * @return 由URL分析出来的文件名
     */
    public String AnalyzeURL(String urlStr) {
        try {
            URI uri = new URI(urlStr);
            String path = uri.getPath().replaceFirst("^/", ""); // 移除开头的斜杠
            String[] segments = path.split("/");

            // 至少需要包含桶名+对象名两部分（如：bucket/file.txt）
            if (segments.length < 2) return null;

            // 返回对象路径（含目录结构）
            return String.join("/", Arrays.copyOfRange(segments, 1, segments.length));
        } catch (URISyntaxException e) {
            System.err.println("URL格式错误: " + e.getMessage());
            return null;
        }
    }

    /**
     *
     * @return 桶列表的详细信息
     */
    public String info() throws Exception {
        MinioClient client = getClient();
        JSONArray fileList = new JSONArray();

        if (!client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build())) {
            return new JSONObject().put("error", "Bucket不存在").toString();
        }

        Iterable<Result<Item>> results = client.listObjects(
                ListObjectsArgs.builder()
                        .bucket(BUCKET_NAME)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            JSONObject fileInfo = new JSONObject();
            fileInfo.put("objectName", item.objectName());
            fileInfo.put("size", item.size());
            fileInfo.put("lastModified", item.lastModified() != null ? item.lastModified().toString() : null);
            fileInfo.put("isDir", item.isDir());
            fileList.put(fileInfo);
        }

        return new JSONObject()
                .put("bucket", BUCKET_NAME)
                .put("fileCount", fileList.length())
                .put("files", fileList)
                .toString(2);  // 使用缩进美化输出
    }

    /**
     *
     * @param fileUrl 文件的URL
     * @return 文件的详细信息
     */
    public String info(String fileUrl) throws Exception {
        String fileName = AnalyzeURL(fileUrl);
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("无法解析URL中的文件名");
        }
        String bucket = AnalyzeBucket(fileUrl);
        if (bucket == null || bucket.isEmpty()) {
            throw new IllegalArgumentException("无法解析URL中的桶名");
        }

        MinioClient client = getClient();
        StatObjectResponse stat = client.statObject(
                StatObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build()
        );

        // 检查 Bucket 是否存在
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            throw new NullPointerException("Bucket“" + bucket + "”不存在");
        }

        // 检查文件是否存在
        if (!checkFileExists(fileName, client)) {
            throw new NullPointerException("文件“" + fileName + "”不存在");
        }

        return new JSONObject()
                .put("objectName", stat.object())
                .put("contentType", stat.contentType())
                .put("size", stat.size())
                .put("lastModified", stat.lastModified().toString())
                .put("etag", stat.etag())
                .put("metadata", new JSONObject(stat.userMetadata()))
                .toString(2);
    }

    /**
     *
     * @param objectName 上传时的文件名
     * @return 文件上传后的路径
     */
    public String getURL(String objectName) throws Exception {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(ENDPOINT)
                .credentials(ACCESS_KEY_ID, ACCESS_KEY_SECRET)
                .build();

        // 检查 Bucket 是否存在
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build())) {
            throw new NullPointerException("Bucket“" + BUCKET_NAME + "”不存在");
        }

        // 检查文件是否存在
        if (!checkFileExists(objectName, minioClient)) {
            throw new NullPointerException("文件“" + objectName + "”不存在");
        }
        // 判断桶的策略是否为公开，为公开返回预签名URL，公开则返回路径URL
        String temp_URL = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(objectName)
                        .method(Method.GET)
                        .build()
        );
        if (getBucketStatue().equals("Private")) {
            return temp_URL;
        } else {
          return temp_URL.split("\\?X-")[0];
        }
    }

    // 辅助方法：检查文件是否存在
    private boolean checkFileExists(String objectName, MinioClient minioClient) throws Exception {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 在类中添加新方法
    /**
     * 获取存储桶的访问策略（Policy）
     *
     * @param bucketName 存储桶名称（默认使用类初始化时指定的桶）
     * @return 策略JSON字符串，若未设置策略则返回null
     * @throws Exception 当发生权限错误或桶不存在时抛出异常
     */
    public String getBucketStatue(String bucketName) throws Exception {
        MinioClient client = getClient();
        JSONObject result;

        try {
            // 尝试获取存储桶策略
            result = new JSONObject(client.getBucketPolicy(
                    GetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .build()
            ));
        } catch (ErrorResponseException e) {
            // 处理特定错误码
            switch (e.errorResponse().code()) {
                case "NoSuchBucketPolicy":
                    return null; // 桶未设置显式策略
                case "AccessDenied":
                    throw new SecurityException("权限不足，无法获取桶策略");
                case "NoSuchBucket":
                    throw new IllegalArgumentException("存储桶不存在: " + bucketName);
                default:
                    throw e; // 其他未知错误
            }
        }
        return result.getJSONArray("Statement").isEmpty()? "Private":"Public";
    }

    // 可选：添加重载方法使用默认桶名
    public String getBucketStatue() throws Exception {
        return getBucketStatue(BUCKET_NAME);
    }
}