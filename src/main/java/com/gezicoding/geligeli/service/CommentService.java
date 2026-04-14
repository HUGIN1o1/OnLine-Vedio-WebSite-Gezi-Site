package com.gezicoding.geligeli.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gezicoding.geligeli.model.entity.Comment;
import com.gezicoding.geligeli.model.vo.comment.CommentResponse;
import com.gezicoding.geligeli.model.dto.comment.CreateCommentRequest;
import com.gezicoding.geligeli.model.dto.video.CancelVideoActionRequest;
import com.gezicoding.geligeli.model.vo.video.CommentVideoResponse;

public interface CommentService extends IService<Comment> {

    CommentResponse createCommentVideo(CreateCommentRequest createCommentRequest);

    Boolean deleteCommentVideo(CancelVideoActionRequest cancelVideoActionRequest);

    List<CommentVideoResponse> getCommentVideoList(Long videoId);
}
