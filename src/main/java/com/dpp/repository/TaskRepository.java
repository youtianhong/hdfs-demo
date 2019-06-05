package com.dpp.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.dpp.domain.Task;


@Repository
public interface TaskRepository extends MongoRepository<Task,String>{
	
	@Query(value="{ 'name': ?0 ,'channel': ?1}")
	Task findByNameAndChannel(@Param("name") String name, @Param("channel") String channel);

}
