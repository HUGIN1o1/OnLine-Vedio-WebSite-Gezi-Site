package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.constants.MinIOConstant;
import com.gezicoding.geligeli.constants.SnowFlakeConstants;
import com.gezicoding.geligeli.constants.ThreadPoolConstant;
import com.gezicoding.geligeli.exception.BusinessException;
import com.gezicoding.geligeli.mapper.FileMapper;
import com.gezicoding.geligeli.model.dto.file.MergeChunkRequest;
import com.gezicoding.geligeli.model.dto.file.UploadUrlRquest;
import com.gezicoding.geligeli.model.entity.File;
import com.gezicoding.geligeli.service.FileService;
import com.gezicoding.geligeli.utils.MinIOUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    @Resource
    private MinIOUtil minIOUtil;

    @Resource
    private ThreadPoolExecutor fileThreadPool;

    @Override
    public String checkFileExistance(String fileHash) {
        if (StrUtil.isBlank(fileHash)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<File> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(File::getFileHash, fileHash);
        File file = this.getOne(queryWrapper);
        return file == null ? null : file.getFileUrl();
    }


    @Override
    public List<String> getUploadUrls(UploadUrlRquest uploadUrlRquest) {
        if (uploadUrlRquest == null) {
            throw new IllegalArgumentException("上传请求不能为空");
        }
        String fileHash = uploadUrlRquest.getFileHash();
        int chunkCount = uploadUrlRquest.getChunkCount();       
        if (StrUtil.isBlank(fileHash)) {
            throw new IllegalArgumentException("文件 hash 不能为空");
        }
        if (chunkCount <= 0) {
            throw new IllegalArgumentException("分片数量不能小于等于0, 当前分片数量为: " + chunkCount);
        }

        // List<String> uploadUrls = new ArrayList<>();
        List<CompletableFuture<String>> futures = new ArrayList<>();
        // 使用线程池并发提交，异步获取上传 url
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            final int index = chunkIndex;
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> 
                    minIOUtil.uploadChunkUrl(
                            fileHash, 
                            index, 
                            MinIOConstant.VIDEO_EXPIRE_TIME,
                            TimeUnit.MINUTES
                        ), 
                    fileThreadPool).exceptionally(e -> {
                        // 异常处理：记录具体分片错误，后续统一抛出
                        log.error("生成分片[{}]上传URL失败", index, e);
                        throw new CompletionException("分片[" + index + "]生成URL失败", e);
                    });
            futures.add(future);
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            // 等待超时：使用线程池配置的超时时间
            allOf.get(ThreadPoolConstant.AWAIT_TERMINATION, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("生成分片URL超时（{}秒），已完成{}个分片",
                    ThreadPoolConstant.AWAIT_TERMINATION,
                    futures.stream().filter(CompletableFuture::isDone).count());
            throw new RuntimeException("生成分片URL超时，请重试", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("生成URL操作被中断", e);
        } catch (ExecutionException e) {
            // 捕获子任务的异常（由exceptionally抛出的CompletionException）
            throw new RuntimeException("生成分片URL失败", e.getCause());
        }
        List<String> uploadUrls = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        return uploadUrls;
    }


    @Override
    public Set<Integer> getUploadProgress(String fileHash) {
        if (StrUtil.isBlank(fileHash)) {
            throw new IllegalArgumentException("文件 hash 不能为空");
        }
        Set<Integer> chunkProgress = minIOUtil.getChunkProgress(fileHash);
        return chunkProgress;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String mergeChunk(MergeChunkRequest mergeChunkRequest) {
        String fileHash = mergeChunkRequest.getFileHash();
        int chunkCount = mergeChunkRequest.getChunkCount();
        String fileType = mergeChunkRequest.getFileType();
        if (StrUtil.isBlank(fileHash)) {
            throw new IllegalArgumentException("文件 hash 不能为空");
        }
        if (chunkCount <= 0) {
            throw new IllegalArgumentException("分片数量不能小于等于0, 当前分片数量为: " + chunkCount);
        }
        if (StrUtil.isBlank(fileType)) {
            throw new IllegalArgumentException("文件类型不能为空");
        }

        String url = minIOUtil.mergeChunks(fileHash, chunkCount, fileType);

        File file = new File();
        Snowflake snowflake = IdUtil.createSnowflake(
            SnowFlakeConstants.DATA_CENTER_ID, 
            SnowFlakeConstants.MACHINE_ID
        );

        file.setFileId(snowflake.nextId());
        file.setFileHash(fileHash);
        file.setFileUrl(url);        
        file.setCreateTime(new Date());
        file.setUpdateTime(new Date());
        this.save(file);
        return file.getFileUrl();
    }
}
