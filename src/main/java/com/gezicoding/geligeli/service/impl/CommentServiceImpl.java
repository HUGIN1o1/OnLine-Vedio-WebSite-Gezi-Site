package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.mapper.CommentMapper;
import com.gezicoding.geligeli.model.entity.Comment;
import com.gezicoding.geligeli.service.CommentService;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.gezicoding.geligeli.model.dto.comment.CreateCommentRequest;
import com.gezicoding.geligeli.model.dto.video.CancelVideoActionRequest;
import com.gezicoding.geligeli.model.vo.comment.CommentResponse;
import com.gezicoding.geligeli.model.vo.video.CommentVideoResponse;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.model.entity.VideoStats;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.constants.SnowFlakeConstants;
import com.gezicoding.geligeli.exception.BusinessException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.gezicoding.geligeli.service.UserService;
import com.gezicoding.geligeli.service.VideoStatsService;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {


    @Autowired
    private UserService userService;

    @Autowired
    private VideoStatsService videoStatsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentResponse createCommentVideo(CreateCommentRequest createCommentRequest) {

        CommentResponse commentResponse = new CommentResponse();
        // 创建评论
        Comment comment = new Comment();
        comment.setContent(createCommentRequest.getContent());
        comment.setVideoId(createCommentRequest.getVideoId());
        comment.setUserId(createCommentRequest.getUserId());
        Snowflake snowflake = IdUtil.getSnowflake(SnowFlakeConstants.MACHINE_ID, SnowFlakeConstants.DATA_CENTER_ID);
        comment.setCommentId(snowflake.nextId());

        // 如果有父评论，则设置父评论id
        if (createCommentRequest.getParentCommentId() != null) {
            if (!this.lambdaQuery().eq(Comment::getCommentId, createCommentRequest.getParentCommentId()).exists()) {
                throw new BusinessException(ErrorCode.PARENT_COMMENT_NOT_EXISTS);
            }
            comment.setParentCommentId(createCommentRequest.getParentCommentId());
            Comment parentComment = this.getById(createCommentRequest.getParentCommentId());
            User parentUser = userService.lambdaQuery().eq(User::getUserId, parentComment.getUserId()).one();
            commentResponse.setToUserId(parentUser.getUserId());
            commentResponse.setToNickname(parentUser.getNickname());
        }

        // 保存评论
        boolean save = this.save(comment);
        if (!save) {
            throw new BusinessException(ErrorCode.CREATE_COMMENT_ERROR);
        }

        // 更新视频评论数
        boolean updatedVideComment = videoStatsService.lambdaUpdate().setSql("comment_count = comment_count + 1")
                .eq(VideoStats::getVideoId, createCommentRequest.getVideoId()).update();
        if (!updatedVideComment) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
 

        // 获取评论的用户信息
        BeanUtil.copyProperties(comment, commentResponse);

        // 获取评论
        Comment commentCreate = this.getById(comment.getCommentId());
        commentResponse.setCreateTime(commentCreate.getCreateTime());
        // 获取评论的用户信息
        User user = userService.lambdaQuery().eq(User::getUserId, comment.getUserId()).one();
        commentResponse.setNickname(user.getNickname());
        commentResponse.setAvatar(user.getAvatar());
        return commentResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCommentVideo(CancelVideoActionRequest cancelVideoActionRequest) {
        // 删除评论
        int result = this.baseMapper.deleteById(cancelVideoActionRequest.getId());
        if (result == 0) {
            throw new BusinessException(ErrorCode.DELETE_COMMENT_ERROR);
        }
    
        // 读取时评评论数
        Long countComments = this.baseMapper.selectCount(new QueryWrapper<Comment>().eq("video_id", cancelVideoActionRequest.getVideoId()));
    
        // 更新视频评论数
        boolean updatedVideoComment = videoStatsService.lambdaUpdate()
                                    .set(VideoStats::getCommentCount, countComments)
                                    .eq(VideoStats::getVideoId, cancelVideoActionRequest.getVideoId()).update();
    
        if (!updatedVideoComment) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新用户评论数失败");
        }

        return true;
    }



    @Override
    public List<CommentVideoResponse> getCommentVideoList(Long videoId) {

        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("video_id", videoId);
        queryWrapper.orderByAsc("create_time");

        List<Comment> comments = this.list(queryWrapper);

        if (comments.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userService.lambdaQuery().in(User::getUserId, userIds).list().stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        // 获取顶级评论和子评论
        Map<Long, CommentVideoResponse> videoResponseMap = new HashMap<>(); 
        Map<Long, CommentResponse> commentResponseMap = new HashMap<>();   
        List<CommentVideoResponse> rootComments = new ArrayList<>();  

        // 初始化所有评论对象
        for (Comment comment : comments) {
            Long parentId = comment.getParentCommentId();
            if (parentId == null) {
                // 顶级评论放到videoResponseMap中
                CommentVideoResponse response = new CommentVideoResponse();
                BeanUtil.copyProperties(comment, response);
                response.setNickname(userMap.get(comment.getUserId()).getNickname());
                response.setAvatar(userMap.get(comment.getUserId()).getAvatar());
                response.setChildren(new ArrayList<>());
                videoResponseMap.put(comment.getCommentId(), response);
                rootComments.add(response);
            } else {
                // 子评论放到commentResponseMap中
                CommentResponse response = new CommentResponse();
                BeanUtil.copyProperties(comment, response);
                response.setNickname(userMap.get(comment.getUserId()).getNickname());
                response.setAvatar(userMap.get(comment.getUserId()).getAvatar());
                commentResponseMap.put(comment.getCommentId(), response);
            }
        }

        // 构建评论树
        for (Comment comment : comments) {
            Long parentId = comment.getParentCommentId();
            if (parentId != null) {
                // 子评论需要挂到对应的顶级评论下
                Long rootParentId = findRootParentId(comments, parentId);
                if (rootParentId != null && videoResponseMap.containsKey(rootParentId)) {
                    CommentResponse current = commentResponseMap.get(comment.getCommentId());
                    // 设置 toUserId 和 toNickname（指向直接父评论）
                    CommentResponse directParent = commentResponseMap.get(parentId);
                    if (directParent != null) {
                        current.setToUserId(directParent.getUserId());
                        current.setToNickname(directParent.getNickname());
                    } else {
                        // 如果父评论是顶级评论
                        CommentVideoResponse videoParent = videoResponseMap.get(parentId);
                        if (videoParent != null) {
                            current.setToUserId(videoParent.getUserId());
                            current.setToNickname(videoParent.getNickname());
                        }
                    }
                    // 添加到顶级评论的 children
                    videoResponseMap.get(rootParentId).getChildren().add(current);
                }
            }
        }

        // 按时间排序
        rootComments.sort(Comparator.comparing(CommentVideoResponse::getCreateTime).reversed());

        for (CommentVideoResponse root : rootComments) {
            // 子评论按时间排序
            root.getChildren().sort(Comparator.comparing(CommentResponse::getCreateTime).reversed());
        }

        return rootComments;

    }


    private Long findRootParentId(List<Comment> comments, Long commentId) {
        // 遍历整个comments列表，找到commentId的顶级评论
        for (Comment comment : comments) {
            if (comment.getCommentId().equals(commentId)) {
                if (comment.getParentCommentId() == null) {
                    return commentId; // 找到顶级评论
                } else {
                    return findRootParentId(comments, comment.getParentCommentId()); // 递归向上查找
                }
            }
        }
        return null; // 未找到
    }
}
