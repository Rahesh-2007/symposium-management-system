package com.raheshel.repository;

import com.raheshel.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Integer> {

    boolean existsByNameIgnoreCase(String name);

    List<Participant> findByNameIgnoreCase(String name);

    // Find all participants enrolled in a specific event
    List<Participant> findByEvents_EventId(int eventId);
}
