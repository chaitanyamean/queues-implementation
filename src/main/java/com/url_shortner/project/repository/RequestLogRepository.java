package com.url_shortner.project.repository;

import com.url_shortner.project.entity.RequestLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RequestLogRepository extends JpaRepository<RequestLogEntity, Long> {


}
