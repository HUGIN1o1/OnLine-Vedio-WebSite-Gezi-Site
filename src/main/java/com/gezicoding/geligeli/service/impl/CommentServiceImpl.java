package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.mapper.CommentMapper;
import com.gezicoding.geligeli.model.entity.Comment;
import com.gezicoding.geligeli.service.CommentService;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
}
