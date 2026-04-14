package com.gezicoding.geligeli.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.entity.Follow;
import com.gezicoding.geligeli.model.dto.user.FollowRequest;
import com.gezicoding.geligeli.model.vo.user.UserListResponse;

import java.util.List;

public interface FollowService extends IService<Follow> {

    Boolean follow(FollowRequest followRequest);

    List<UserListResponse> followList(Long userId);

    List<UserListResponse> followerList(Long userId);
}
