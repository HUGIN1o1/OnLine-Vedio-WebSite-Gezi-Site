package com.gezicoding.geligeli.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.entity.Category;
import com.gezicoding.geligeli.model.vo.video.CategoryListResponse;

import java.util.List;

public interface CategoryService extends IService<Category> {
    List<CategoryListResponse> categoryList();
}
