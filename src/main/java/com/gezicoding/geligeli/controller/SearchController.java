package com.gezicoding.geligeli.controller;

import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gezicoding.geligeli.common.BaseResponse;
import com.gezicoding.geligeli.common.ResultUtils;
import com.gezicoding.geligeli.common.VideoContants;
import com.gezicoding.geligeli.constants.UserConstant;
import com.gezicoding.geligeli.dao.UserEsDao;
import com.gezicoding.geligeli.dao.VideoEsDao;
import com.gezicoding.geligeli.model.es.UserEs;
import com.gezicoding.geligeli.model.es.VideoEs;
import com.gezicoding.geligeli.model.vo.user.SearchUserListResponse;
import com.gezicoding.geligeli.model.vo.video.SearchVideoListResponse;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@RestController
@RequestMapping("/api/search")
public class SearchController {


    @Resource
    private ElasticsearchTemplate elasticsearchTemplate;

    @Resource
    private UserEsDao userEsDao;

    @Resource
    private VideoEsDao videoEsDao;

    @GetMapping("/user")
    public BaseResponse<List<SearchUserListResponse>> searchUser(@Valid @NotEmpty(message = "关键字不能为空") @RequestParam String keyword) {

        Criteria criteria = new Criteria("nickname").matches(keyword);
        Query searchQuery = new CriteriaQuery(criteria);

        SearchHits<UserEs> searchHits = elasticsearchTemplate.search(searchQuery, UserEs.class, IndexCoordinates.of(UserConstant.USER_ES_INDEX));

        List<SearchUserListResponse> userListResponses = searchHits.stream().map(SearchHit::getContent).map(this::convertUserToResponse).collect(Collectors.toList());

        return ResultUtils.success(userListResponses);

    }

    private SearchUserListResponse convertUserToResponse(UserEs userEs) {
        SearchUserListResponse response = new SearchUserListResponse();
        response.setAvatar(userEs.getAvatar());
        response.setDescription(userEs.getDescription());
        response.setFollowers(userEs.getFollowers());
        response.setNickname(userEs.getNickname());
        response.setUserId(userEs.getId());
        response.setVideoCount(userEs.getVideoCount());
        return response;
    }


    @GetMapping("/video")
    public BaseResponse<List<SearchVideoListResponse>> searchVideo(@Valid @NotEmpty(message = "关键字不能为空") @RequestParam String keyword) {

        Criteria criteria = new Criteria("title").matches(keyword);
        Query searchQuery = new CriteriaQuery(criteria);

        SearchHits<VideoEs> searchHits = elasticsearchTemplate
        .search(searchQuery, VideoEs.class, 
            IndexCoordinates.of(
                VideoContants.VIDEO_ES_INDEX
            )
        );

        List<SearchVideoListResponse> videoListResponses = searchHits.stream().map(SearchHit::getContent).map(this::convertVideoToResponse).collect(Collectors.toList());
        
        return ResultUtils.success(videoListResponses);
    }

    private SearchVideoListResponse convertVideoToResponse(VideoEs videoEs) {
        SearchVideoListResponse response = new SearchVideoListResponse();
        response.setBulletCount(videoEs.getBulletCount());
        response.setCoverUrl(videoEs.getCoverUrl());
        response.setCreateTime(videoEs.getCreateTime());
        response.setDuration(videoEs.getDuration());
        response.setFileUrl(videoEs.getFileUrl());
        response.setNickName(videoEs.getNickName());
        response.setTitle(videoEs.getTitle());
        response.setUserId(videoEs.getUserId());
        response.setVideoId(videoEs.getId());
        response.setViewCount(videoEs.getViewCount());
        return response;
    }





}
