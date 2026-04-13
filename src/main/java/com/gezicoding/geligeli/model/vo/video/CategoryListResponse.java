package com.gezicoding.geligeli.model.vo.video;

import java.io.Serializable;

import lombok.Data;

@Data
public class CategoryListResponse implements Serializable {
    private int categoryId;

    private String categoryName;
}