package com.gezicoding.geligeli.utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.exception.BusinessException;

import cn.hutool.core.util.StrUtil;

import org.springframework.beans.factory.annotation.Value;
import io.minio.BucketExistsArgs;
import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MinIOUtil {


    /**
     * 上传的设置是：文件的chunk变成 filehash/countIndex 的文件 存在minio中
     *  合并通过合并这些sources 来控制顺序
     *  
     */

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url}")
    private String url;


    /**
     * @description 确保桶存在，不存在则创建
     * @param bucketName 桶名   
     * @return
     */
    public boolean ensureBucketExists() {
        try{
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if(!exists){
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("桶操作失败", e);
        }
    }

    /**
     * 获取上传 url
     * @param objectName
     * @return
     */
    public String getDownloadUrl(String objectName) {
        return url + StrUtil.SLASH + bucketName + StrUtil.SLASH + objectName;
    }


    public String mergeChunks(String fileHash, int chunkCount, String fileType) {
        ensureBucketExists();
        int chunkUploaded = getChunkProgress(fileHash).size();
        if (chunkUploaded != chunkCount) {
            throw new BusinessException(ErrorCode.MERGE_FILE_ERROR);
        }
        
        List<ComposeSource> sources = new ArrayList<>();
        List<String> objectNames = new ArrayList<>();
        for(int i = 0; i < chunkCount; i++) {
            String objectName = String.format("%s/%s", fileHash, i);
            objectNames.add(objectName);
            sources.add(ComposeSource.builder().bucket(bucketName).object(objectName).build());
        }
        if (objectNames.size() != chunkCount || sources.size() != chunkCount) {
            throw new BusinessException(ErrorCode.MERGE_FILE_ERROR);
        }
        String finalFileName = fileHash + "." + fileType;

        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", fileType);
        headers.put("Content-Disposition", "inline; filename=" + finalFileName);

        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
            .bucket(bucketName)
            .object(finalFileName)
            .sources(sources)
            .headers(headers)
            .build();

        try{
            minioClient.composeObject(composeObjectArgs);
            for (String objectName : objectNames) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            }
        } catch (Exception e) {
            log.error("合并分片 {} 失败", fileHash);
            throw new RuntimeException("合并分片失败", e);
        }
        return getDownloadUrl(finalFileName);
    }


    /**
     * 获取上传分片 url
     * @param fileHash 文件 hash
     * @param countIndex 分片索引
     * @param expires 过期时间
     * @param timeUnit 时间单位
     * @return 上传分片 url
     */
    public String uploadChunkUrl(String fileHash, int countIndex, Integer expires, TimeUnit timeUnit) {
        String objectName = String.format("%s/%s", fileHash, countIndex);
        ensureBucketExists();

        try{
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .method(Method.PUT)
                .expiry(expires, timeUnit)
                .build()
            );
        } catch (Exception e) {
            log.error("生成临时链接 {} 失败", objectName);
            throw new RuntimeException("生成临时链接失败", e);
        }
    }

    /**
     * 获取分片进度
     * @param fileHash 文件 hash
     * @return 分片进度
     */
    public Set<Integer> getChunkProgress(String fileHash) {
        Set<Integer> chunkProgress = new HashSet<Integer>();
        ensureBucketExists();
        String prefix = fileHash + "/";

        try {
            Iterable<Result<Item>> objects = minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).build());

            for(Result<Item> object : objects) {
                Item item = object.get();
                if (!item.isDir()) {
                    String objectName = item.objectName();
                    String chountIndex = objectName.substring(objectName.lastIndexOf("/") + 1);
                    chunkProgress.add(Integer.parseInt(chountIndex));
                }
            }
        } catch (Exception e) {
            log.error("获取分片进度 {} 失败", fileHash);
            throw new RuntimeException("获取分片进度失败", e);
        }
        

        return chunkProgress;
    }

    

    /**
     * 更新封面
     * @param file 封面文件
     * @return 封面 url
     * @throws Exception
     */
    public String updateCover(MultipartFile file) throws Exception{
        ensureBucketExists();
        String fileSuffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);
        String id = UUID.randomUUID().toString();
        String fileName = id + "." + fileSuffix;
        InputStream inputStream = file.getInputStream();
        String contentType = ContentType.getType(fileSuffix);
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(inputStream, file.getSize(), -1).contentType(contentType).build());
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
        return getDownloadUrl(fileName);
    }

}
