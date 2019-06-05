package com.dpp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.dpp.domain.Config;


@Repository
public interface ConfigRepository extends MongoRepository<Config,String>{

	@Query(value="{ 'channel': ?0 }")
	Config findCfgByChannel(@Param("channel") String channel);
}
