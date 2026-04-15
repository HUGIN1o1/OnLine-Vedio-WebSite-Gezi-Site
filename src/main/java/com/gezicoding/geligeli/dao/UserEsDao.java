package com.gezicoding.geligeli.dao;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

import com.gezicoding.geligeli.model.es.UserEs;

/**
 * User ES dao
 */
@Component
public interface UserEsDao extends ElasticsearchRepository<UserEs, Long> {
}
