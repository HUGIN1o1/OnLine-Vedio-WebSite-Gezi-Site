package com.gezicoding.geligeli.controller;
import java.util.List;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gezicoding.geligeli.common.BaseResponse;
import com.gezicoding.geligeli.common.ResultUtils;
import com.gezicoding.geligeli.model.vo.video.CategoryListResponse;
import com.gezicoding.geligeli.model.vo.video.VideoListResponse;
import com.gezicoding.geligeli.service.CategoryService;
import com.gezicoding.geligeli.service.VideoService;

@RestController
public class CategoryController {

    @Resource
    private VideoService videoService;

    @Resource
    private CategoryService categoryService;

    @GetMapping("/category/list")
    public BaseResponse<List<VideoListResponse>> categoryList(@RequestParam Integer categoryId) {
        return ResultUtils.success(videoService.getCategoryVideoList(categoryId));
    }

    @GetMapping("/category")
    public BaseResponse<List<CategoryListResponse>> category() {
        return ResultUtils.success(categoryService.categoryList());
    }
}
    