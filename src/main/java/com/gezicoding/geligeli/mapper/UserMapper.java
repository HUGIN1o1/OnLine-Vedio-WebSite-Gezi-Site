package com.gezicoding.geligeli.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.model.vo.user.LoginResponse;

import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<User> {

    LoginResponse getUserInfo(@Param("userId") Long userId);
}
