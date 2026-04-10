package com.gezicoding.geligeli.model.dto.file;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MergeChunkRequest {
    /**
     * 文件哈希
     */
    @NotBlank(message = "文件哈希不能为空")
    private String fileHash;

    /**
     * 合并分片数量
     */
    @Min(value = 1, message = "分片数量不能小于1")
    private int chunkCount;

    /**
     * 文件类型
     */
    @NotBlank(message = "文件类型不能为空")
    private String fileType;
}
