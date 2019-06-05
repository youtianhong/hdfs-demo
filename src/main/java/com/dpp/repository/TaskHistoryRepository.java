package com.dpp.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dpp.domain.Task;
import com.dpp.domain.TaskHistory;


@Repository
public interface TaskHistoryRepository extends MongoRepository<TaskHistory,String>{
	

	@Query(value="{ 'name': ?0 ,'channel': ?1}")
	Task findByNameAndChannel(@Param("name") String name, @Param("channel") String channel);
	
	@Query(value="{'startTime' : {$gte: ?0}}")
	public List<TaskHistory> findAfterDate(@Param("startTime") Date startTime);
}
