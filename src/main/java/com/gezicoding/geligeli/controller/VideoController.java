package com.gezicoding.geligeli.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gezicoding.geligeli.common.BaseResponse;
import com.gezicoding.geligeli.common.ResultUtils;
import com.gezicoding.geligeli.model.dto.comment.CreateCommentRequest;
import com.gezicoding.geligeli.service.CommentService;
import com.gezicoding.geligeli.model.dto.video.CancelVideoActionRequest;
import com.gezicoding.geligeli.model.dto.video.VideoActionRequest;
import com.gezicoding.geligeli.model.vo.comment.CommentResponse;
import com.gezicoding.geligeli.model.vo.video.CommentVideoResponse;
import com.gezicoding.geligeli.model.vo.video.TripleActionResponse;
import com.gezicoding.geligeli.model.vo.video.VideoListResponse;
import com.gezicoding.geligeli.model.vo.video.VideoResponse;
import com.gezicoding.geligeli.model.vo.video.VideoSubmitRequest;
import com.gezicoding.geligeli.service.CoinService;
import com.gezicoding.geligeli.service.FavoriteService;
import com.gezicoding.geligeli.service.LikeService;
import com.gezicoding.geligeli.service.VideoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/video")       
public class VideoController {


    @Autowired
    private VideoService videoService;  

    @Autowired
    private LikeService likeService;

    @Autowired
    private CoinService coinService;

    @Autowired
    private FavoriteService favoriteService;


    @Autowired
    private CommentService commentService;

    @PostMapping("/submit")
    public BaseResponse<Boolean> submit(
         @RequestParam("fileUrl") String fileUrl,
         @RequestParam("userId") Long userId,
         @RequestParam("file") MultipartFile file,
         @RequestParam("title") String title,
         @RequestParam("type") Integer type,
         @RequestParam("duration") Double duration,
         @RequestParam("categoryId") Integer categoryId,
         @RequestParam("tags") String tags,
         @RequestParam("description") String description) throws Exception {

        VideoSubmitRequest videoSubmitRequest = new VideoSubmitRequest();
        videoSubmitRequest.setFileUrl(fileUrl);
        videoSubmitRequest.setUserId(userId);
        videoSubmitRequest.setFile(file);
        videoSubmitRequest.setTitle(title);
        videoSubmitRequest.setType(type);
        videoSubmitRequest.setDuration(duration);
        videoSubmitRequest.setCategoryId(categoryId);
        videoSubmitRequest.setTags(tags);
        videoSubmitRequest.setDescription(description);
        boolean result = videoService.submit(videoSubmitRequest);
        return ResultUtils.success(result);
    }


    @GetMapping("/list")
    public BaseResponse<List<VideoListResponse>> videoList(@RequestParam Integer current, @RequestParam Integer pageSize) {
        return ResultUtils.success(videoService.getVideoList(current, pageSize));
    }

    @GetMapping("/submit/list")   
    public BaseResponse<List<VideoListResponse>> submitVideoList(@RequestParam Long userId) {
        return ResultUtils.success(videoService.getSubmitVideoList(userId));
    }


    @PostMapping("/detail")
    public BaseResponse<VideoResponse> videoDetail(@RequestBody VideoActionRequest videoActionRequest) {
        return ResultUtils.success(videoService.videoDetail(videoActionRequest));
    }


    @PostMapping("/like")
    public BaseResponse<Long> likeVideo(@Valid @RequestBody VideoActionRequest videoActionRequest) {
        return ResultUtils.success(likeService.likeVideo(videoActionRequest));
    }

    @PostMapping("/cancel/like")
    public BaseResponse<Boolean> cancelLikeVideo(@Valid @RequestBody CancelVideoActionRequest cancelVideoActionRequest) {
        return ResultUtils.success(likeService.cancelLikeVideo(cancelVideoActionRequest));
    }

    @PostMapping("/coin")
    public BaseResponse<Boolean> coinVideo(@Valid @RequestBody VideoActionRequest videoActionRequest) {
        return ResultUtils.success(coinService.coinVideo(videoActionRequest));
    }


    @PostMapping("/favorite")
    public BaseResponse<Long> favoriteVideo(@Valid @RequestBody VideoActionRequest videoActionRequest) {
        return ResultUtils.success(favoriteService.favoriteVideo(videoActionRequest));
    }

    @PostMapping("/cancel/favorite")
    public BaseResponse<Boolean> cancelFavoriteVideo(@Valid @RequestBody CancelVideoActionRequest cancelVideoActionRequest) {
        return ResultUtils.success(favoriteService.cancelFavoriteVideo(cancelVideoActionRequest));
    }

    @PostMapping("/create/comment")
    public BaseResponse<CommentResponse> createCommentVideo(@Valid @RequestBody CreateCommentRequest createCommentRequest) {
        return ResultUtils.success(commentService.createCommentVideo(createCommentRequest));
    }

    @PostMapping("/delete/comment")
    public BaseResponse<Boolean> deleteCommentVideo(@Valid @RequestBody CancelVideoActionRequest cancelVideoActionRequest) {
        return ResultUtils.success(commentService.deleteCommentVideo(cancelVideoActionRequest));
    }
    
    @GetMapping("/comment/list")
    public BaseResponse<List<CommentVideoResponse>> getCommentVideoList(@Valid @NotNull(message = "视频ID不能为空") @RequestParam Long videoId) {
        return ResultUtils.success(commentService.getCommentVideoList(videoId));
    }


    @PostMapping("/triple/action")
    public BaseResponse<TripleActionResponse> tripleAction(@Valid @RequestBody VideoActionRequest videoActionRequest) {
        return ResultUtils.success(videoService.tripleAction(videoActionRequest));
    }


    
}
