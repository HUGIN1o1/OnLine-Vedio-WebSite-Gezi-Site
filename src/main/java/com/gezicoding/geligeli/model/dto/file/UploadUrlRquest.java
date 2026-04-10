package com.gezicoding.geligeli.model.dto.file;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UploadUrlRquest {

    @NotBlank(message = "fileHash 不能为空")
    String fileHash;

    @Min(value = 1, message = "chunkCount 不能小于 1")
    Integer chunkCount;

}
