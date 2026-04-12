package com.gezicoding.geligeli.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gezicoding.geligeli.common.ErrorCode;
import com.gezicoding.geligeli.constants.JwtConstants;
import com.gezicoding.geligeli.constants.RegisterCodeConstants;
import com.gezicoding.geligeli.constants.SMSConstant;
import com.gezicoding.geligeli.exception.BusinessException;
import com.gezicoding.geligeli.mapper.UserMapper;
import com.gezicoding.geligeli.model.dto.user.LoginCodeRequest;
import com.gezicoding.geligeli.model.dto.user.LoginPasswordRequest;
import com.gezicoding.geligeli.model.dto.user.RegisterRequest;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.model.entity.UserStats;
import com.gezicoding.geligeli.model.vo.user.LoginResponse;
import com.gezicoding.geligeli.service.UserService;
import com.gezicoding.geligeli.service.UserStatsService;
import com.gezicoding.geligeli.utils.EmailUtils;
import com.gezicoding.geligeli.utils.JWTUtils;
import com.gezicoding.geligeli.utils.RandomCodeUtil;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    

    @Autowired
    private UserStatsService userStatsService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public LoginResponse loginCode(LoginCodeRequest loginCodeRequest, HttpServletRequest httpServletRequest) {
        String account = loginCodeRequest.getAccount();
        String code = loginCodeRequest.getCode();
        
        // 校验账号格式
        validateAccountFormat(account);

        // 获取当前用户
        User user = getCurrentUser(account);

        // 获取redis中的验证码
        String rediscode = redisTemplate.opsForValue().get(RegisterCodeConstants.REDIS_REGISTER_CODE_PREFIX + account);
        // 如果验证码不存在或不正确，抛出登录错误异常
        if (user == null || rediscode == null || !rediscode.equals(code)) {
            throw new BusinessException(ErrorCode.LOGIN_ERROR_CODE);
        }

        LoginResponse loginResponse = new LoginResponse();

        BeanUtil.copyProperties(user, loginResponse);

        UserStats userStats = userStatsService.getById(user.getUserId());
        BeanUtil.copyProperties(userStats, loginResponse);

        redisTemplate.delete(RegisterCodeConstants.REDIS_REGISTER_CODE_PREFIX + account);

        String token = JWTUtils.generateToken(user.getUserId().toString());
        redisTemplate.opsForValue().set(user.getUserId().toString(), token, JwtConstants.JWT_TIME_OUT, TimeUnit.DAYS);

        loginResponse.setToken(token);
        return loginResponse;        
    }

    @Override
    public LoginResponse loginPassword(LoginPasswordRequest loginPasswordRequest, HttpServletRequest httpServletRequest) {
        String account = loginPasswordRequest.getAccount();
        String password = loginPasswordRequest.getPassword();

        // 检查账号格式
        validateAccountFormat(account);

        // 获取当前用户
        User user = getCurrentUser(account);

        // 检查密码
        String encryptedPassword = DigestUtils.md5DigestAsHex((RegisterCodeConstants.SALT + password).getBytes());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_ERROR);
        }

        // 设置用户
        LoginResponse loginResponse = new LoginResponse();
        BeanUtil.copyProperties(user, loginResponse);

        // 设置用户统计信息
        UserStats userStats = userStatsService.getById(user.getUserId());
        BeanUtil.copyProperties(userStats, loginResponse);

        // generate and store jwt token
        String token = JWTUtils.generateToken(user.getUserId().toString());
        redisTemplate.opsForValue().set(user.getUserId().toString(), token, JwtConstants.JWT_TIME_OUT, TimeUnit.DAYS);

        loginResponse.setToken(token);
        return loginResponse;
    }


    @Override
    public void sendVertificationCodeAsync(String account) {
        if (StringUtils.isBlank(account)) {
            throw new BusinessException(ErrorCode.PHONE_EMAIL_ERROR);
        }

        String code = RandomCodeUtil.generateSixDigitRandomNumber();

        if (account.matches(RegisterCodeConstants.EMAIL_REGEX)) {
            EmailUtils.sendEmailCodeAysc(account, code);
        } else if (account.matches(RegisterCodeConstants.PHONE_REGEX)) {
            throw new BusinessException(ErrorCode.PHONE_REGISTRATION_NOT_SUPPORTED);
        } else {
            throw new BusinessException(ErrorCode.PHONE_EMAIL_ERROR);
        }

        redisTemplate.opsForValue().set(RegisterCodeConstants.REDIS_REGISTER_CODE_PREFIX + account, code, SMSConstant.SMS_EXPIRE_TIME, TimeUnit.MINUTES);           

    }


    
    @Override
    public void sendVerticatinCode(String account) throws Exception {
        String code = RandomUtil.randomString(6);
        EmailUtils.sendEmailCode(account, code);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request, HttpServletRequest httpServletRequest) {

        validateRegisterRequest(request);

        validateVerificationCode(request.getAccount(), request.getVerificationCode());

        checkUserExistence(request.getAccount());

        User user = createUser(request);

        LoginResponse loginResponse = saveUserAndGenerateToken(user, request.getAccount(), httpServletRequest);

        return loginResponse;
    }

    
    /**
     * 校验注册请求参数
     */
    private void validateRegisterRequest(RegisterRequest request) {
        String account = request.getAccount();
        if (!account.matches(RegisterCodeConstants.PHONE_REGEX) && !account.matches(RegisterCodeConstants.EMAIL_REGEX)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号必须是有效的手机号或邮箱");
        }
        if (StringUtils.isBlank(request.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (StringUtils.isBlank(request.getNickname())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称不能为空");
        }
    }


    /**
     * 验证码校验
     */
    private void validateVerificationCode(String account, String code) {
        String redisCode = redisTemplate.opsForValue().get(RegisterCodeConstants.REDIS_REGISTER_CODE_PREFIX + account);
        if (StringUtils.isBlank(redisCode) || !redisCode.equals(code)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_ERROR);
        }
    }

    /**
     * 检查用户是否已存在
     */
    private void checkUserExistence(String account) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (account.matches(RegisterCodeConstants.EMAIL_REGEX)) {
            queryWrapper.eq(User::getEmail, account);
        }
        if (this.getOne(queryWrapper) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
    }


    /**
     * 创建用户实体
     */
    private User createUser(RegisterRequest request) {
        User user = new User();
        user.setUserId(IdUtil.getSnowflake().nextId());
        user.setNickname(request.getNickname());

        // 设置账号(手机号或邮箱)
        if (request.getAccount().matches(RegisterCodeConstants.EMAIL_REGEX)) {
            user.setEmail(request.getAccount());
            user.setPhone("");
        }

        // 密码加密
        String password = request.getPassword();
        String encryptedPassword = DigestUtils.md5DigestAsHex((RegisterCodeConstants.SALT + password).getBytes());
        user.setPassword(encryptedPassword);

        return user;
    }


    /**
     * 保存用户并生成Token(并发安全处理)
     */
    private LoginResponse saveUserAndGenerateToken(User user, String account, HttpServletRequest httpServletRequest) {
        synchronized (account.intern()) {
            // 保存用户
            user.setDescription("这个人很懒什么都没写");

            boolean saveSuccess = this.baseMapper.insert(user) > 0;

            // 初始化用户统计信息
            UserStats stats = new UserStats();
            stats.setUserId(user.getUserId());
            boolean saveStatsSuccess = userStatsService.save(stats);

            if (!saveSuccess || !saveStatsSuccess) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
            }

            // 清理验证码
            redisTemplate.delete(RegisterCodeConstants.REDIS_REGISTER_CODE_PREFIX + account);

            String token = JWTUtils.generateToken(user.getUserId().toString());
            redisTemplate.opsForValue().set(user.getUserId().toString(), token, JwtConstants.JWT_TIME_OUT, TimeUnit.DAYS);

            // 返回用户信息和 Token
            LoginResponse response = this.baseMapper.getUserInfo(user.getUserId()); 
            response.setToken(token);
            return response;
        }
    }


    /**
     * 校验注册请求参数
     */
    private void validateAccountFormat(String account) {
        if (!account.matches(RegisterCodeConstants.PHONE_REGEX) && !account.matches(RegisterCodeConstants.EMAIL_REGEX)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号必须是有效的手机号或邮箱");
        }
    }

    private User getCurrentUser(String account) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if (account.matches(RegisterCodeConstants.EMAIL_REGEX)) {
            queryWrapper.eq(User::getEmail, account);
        } else if (account.matches(RegisterCodeConstants.PHONE_REGEX)) {
            queryWrapper.eq(User::getPhone, account);
        }

        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXISTS);
        }

        return user;
    }
    
}
