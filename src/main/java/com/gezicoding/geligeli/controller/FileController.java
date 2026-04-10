package com.gezicoding.geligeli.controller;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gezicoding.geligeli.common.BaseResponse;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.common.ResultUtils;
import com.gezicoding.geligeli.model.dto.file.MergeChunkRequest;
import com.gezicoding.geligeli.model.dto.file.UploadUrlRquest;
import com.gezicoding.geligeli.service.FileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/check")
    public BaseResponse<String> checkFile(@RequestParam("fileHash") String fileHash) {
        String fileUrl = fileService.checkFileExistance(fileHash);

        if (fileUrl == null) {
            return ResultUtils.error(ErrorCode.VIDEO_NOT_FOUND_ERROR);
        }
        return ResultUtils.success(fileUrl);
    }

    @PostMapping("/get/upload/urls")
    public BaseResponse<List<String>> getUploadUrls(@Valid @RequestBody UploadUrlRquest uploadUrlRquest) {
        List<String> uploadUrls = fileService.getUploadUrls(uploadUrlRquest);
        return ResultUtils.success(uploadUrls);
    }

    @GetMapping("/get/upload/progress")
    public BaseResponse<Set<Integer>> getUploadProgress(@RequestParam("fileHash") String fileHash) {
        Set<Integer> uploadProgress = fileService.getUploadProgress(fileHash);
        return ResultUtils.success(uploadProgress);
    }

    @PostMapping("/merge/chunk")
    public BaseResponse<String> mergeChunk(@Valid @RequestBody MergeChunkRequest mergeChunkRequest) {
        String fileUrl = fileService.mergeChunk(mergeChunkRequest);
        return ResultUtils.success(fileUrl);
    }


}
