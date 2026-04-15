package com.gezicoding.geligeli.dao;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;
    
import com.gezicoding.geligeli.model.es.VideoEs;


/**
 * Video ES dao
 */
@Component
public interface VideoEsDao extends ElasticsearchRepository<VideoEs, Long> {
}
