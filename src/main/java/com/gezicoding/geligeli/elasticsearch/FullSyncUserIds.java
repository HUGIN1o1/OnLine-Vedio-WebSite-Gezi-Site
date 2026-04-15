package com.gezicoding.geligeli.elasticsearch;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.gezicoding.geligeli.dao.UserEsDao;
import com.gezicoding.geligeli.model.entity.User;
import com.gezicoding.geligeli.model.es.UserEs;
import com.gezicoding.geligeli.service.UserService;

import lombok.extern.slf4j.Slf4j;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;

@Component
@Slf4j
public class FullSyncUserIds implements CommandLineRunner {

    @Resource
    private UserService userService;

    @Resource
    private UserEsDao userEsDao;

    @Override
    public void run(String... args) {
        List<User> userList = userService.list();
        if (CollUtil.isEmpty(userList)) {
            return;
        }


        List<UserEs> userEsList = userList.stream().map(user -> {
            UserEs userEs = new UserEs();
            userEs.setId(user.getUserId());
            userEs.setNickname(user.getNickname());
            userEs.setDescription(user.getDescription());
            return userEs;
        }).collect(Collectors.toList());

        System.out.println(userEsList);


        // 分页批量插入到 ES
        final int pageSize = 500;
        int total = userList.size();
        log.info("FullSyncUserToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            log.info("Sync from {} to {}", i, end);
            userEsDao.saveAll(userEsList.subList(i, end));
        }
        log.info("FullSyncUserToEs end, total {}", total);
    }
}
