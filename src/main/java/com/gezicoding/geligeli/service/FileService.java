package com.gezicoding.geligeli.service;

import java.util.List;
import java.util.Set;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.dto.file.MergeChunkRequest;
import com.gezicoding.geligeli.model.dto.file.UploadUrlRquest;
import com.gezicoding.geligeli.model.entity.File;

public interface FileService extends IService<File> {

    String checkFileExistance(String fileHash);

    List<String> getUploadUrls(UploadUrlRquest uploadUrlRquest);

    Set<Integer> getUploadProgress(String fileHash);

    String mergeChunk(MergeChunkRequest mergeChunkRequest);
}
