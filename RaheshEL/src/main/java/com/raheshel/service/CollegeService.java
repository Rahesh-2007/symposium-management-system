package com.raheshel.service;

import com.raheshel.model.College;
import com.raheshel.repository.CollegeRepository;
import org.springframework.stereotype.Service;

@Service
public class CollegeService {

    private final CollegeRepository collegeRepository;

    public CollegeService(CollegeRepository collegeRepository) {
        this.collegeRepository = collegeRepository;
    }

    public College getCollege() {
        return ensureDefaultCollege();
    }

    public boolean collegeExists() {
        return true;
    }

    public College ensureDefaultCollege() {
        return collegeRepository.findAll().stream().findFirst().orElseGet(() ->
                collegeRepository.save(new College(
                        "National Engineering College",
                        1001,
                        "Nalatinputhur, Kovilpatti"
                ))
        );
    }
}
