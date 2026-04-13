package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.mapper.CategoryMapper;
import com.gezicoding.geligeli.model.entity.Category;
import com.gezicoding.geligeli.model.vo.video.CategoryListResponse;
import com.gezicoding.geligeli.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Override
    public List<CategoryListResponse> categoryList() {
        List<Category> categoryList = this.list();

        return categoryList.stream().map(category -> {
            CategoryListResponse categoryListResponse = new CategoryListResponse();
            categoryListResponse.setCategoryId(category.getCategoryId());
            categoryListResponse.setCategoryName(category.getCategoryName());
            return categoryListResponse;
        }).collect(Collectors.toList());
    }
}
