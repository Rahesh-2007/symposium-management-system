package com.raheshel.repository;

import com.raheshel.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer> {

    List<Event> findByDepartment_DepartmentId(int departmentId);

    List<Event> findByDepartment_DepartmentIdAndCategoryIgnoreCase(int departmentId, String category);

    boolean existsByEventNameIgnoreCase(String eventName);
    Optional<Event> findByEventNameIgnoreCase(String eventName);
}
