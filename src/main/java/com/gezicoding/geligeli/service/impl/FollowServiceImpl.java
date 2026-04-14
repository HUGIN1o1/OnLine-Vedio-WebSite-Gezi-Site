package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.constants.SnowFlakeConstants;
import com.gezicoding.geligeli.exception.BusinessException;
import com.gezicoding.geligeli.mapper.FollowMapper;
import com.gezicoding.geligeli.model.dto.user.FollowRequest;
import com.gezicoding.geligeli.model.entity.Follow;
import com.gezicoding.geligeli.model.entity.UserStats;
import com.gezicoding.geligeli.model.vo.user.UserListResponse;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.service.FollowService;
import com.gezicoding.geligeli.service.UserService;
import com.gezicoding.geligeli.service.UserStatsService;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements FollowService {

    @Autowired
    private UserService userService;

    @Autowired
    private UserStatsService userStatsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean follow(FollowRequest followRequest) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", followRequest.getUserId(), followRequest.getCreatorId());
        List<User> users = userService.list(queryWrapper);

        if (users.size() != 2) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        Follow follow = new Follow();
        follow.setUserId(followRequest.getUserId());
        follow.setCreatorId(followRequest.getCreatorId());
        Snowflake snowflake = IdUtil.createSnowflake(SnowFlakeConstants.MACHINE_ID, SnowFlakeConstants.DATA_CENTER_ID);
        follow.setFollowId(snowflake.nextId());
        boolean saved = this.save(follow);
        if (!saved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "关注失败");
        }


        boolean updatedFollowers = userStatsService.lambdaUpdate().setSql("followers = followers + 1").eq(UserStats::getUserId, followRequest.getCreatorId()).update();
        
        if (!updatedFollowers) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新关注粉丝失败");
        }

        // 更新关注数
        boolean updatedFollowing = userStatsService.lambdaUpdate().setSql("following = following + 1").eq(UserStats::getUserId, followRequest.getUserId()).update();
        if (!updatedFollowing) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新关注数失败");
        }


        return true;
    }


    @Override
    public List<UserListResponse> followList(Long userId) {

        QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);

        List<Follow> follows = this.list(queryWrapper);
        if (follows.isEmpty()) {
            return new ArrayList<>();
        }

        List<User> users = userService.listByIds(follows.stream().map(Follow::getCreatorId).collect(Collectors.toSet()));

        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getUserId, user -> user));

        List<UserListResponse> userListResponses = new ArrayList<>();

        // 将关注列表转换为用户列表
        for (Follow follow : follows) {
            UserListResponse userListResponse = new UserListResponse();
            userListResponse.setUserId(follow.getCreatorId());
            userListResponse.setAvatar(userMap.get(follow.getCreatorId()).getAvatar());
            userListResponse.setNickname(userMap.get(follow.getCreatorId()).getNickname());
            userListResponse.setDescription(userMap.get(follow.getCreatorId()).getDescription());
            userListResponses.add(userListResponse);
        }

        return userListResponses;
    }


    @Override
    public List<UserListResponse> followerList(Long userId) {
        QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("creator_id", userId);

        List<Follow> follows = this.list(queryWrapper);
        if (follows.isEmpty()) {
            return new ArrayList<>();
        }

        List<User> users = userService.listByIds(follows.stream().map(Follow::getUserId).collect(Collectors.toSet()));

        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getUserId, user -> user));

        List<UserListResponse> userListResponses = new ArrayList<>();

        // 将粉丝列表转换为用户列表
        for (Follow follow : follows) {
            UserListResponse userListResponse = new UserListResponse();
            userListResponse.setUserId(follow.getUserId());
            userListResponse.setAvatar(userMap.get(follow.getUserId()).getAvatar());
            userListResponse.setNickname(userMap.get(follow.getUserId()).getNickname());
            userListResponse.setDescription(userMap.get(follow.getUserId()).getDescription());
            userListResponses.add(userListResponse);
        }

        return userListResponses;
    }
}
