package com.raheshel.service;

import com.raheshel.exception.BusinessException;
import com.raheshel.exception.DuplicateResourceException;
import com.raheshel.exception.ResourceNotFoundException;
import com.raheshel.model.Department;
import com.raheshel.repository.DepartmentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EventService eventService;

    public DepartmentService(DepartmentRepository departmentRepository,
                             @Lazy EventService eventService) {
        this.departmentRepository = departmentRepository;
        this.eventService = eventService;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    public Department getDepartmentById(int id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
    }

    public Department getDepartmentByName(String name) {
        return departmentRepository.findByDepartmentNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + name));
    }

    public void deleteDepartment(int id) {
        Department dept = getDepartmentById(id);
        boolean hasEvents = !eventService.getEventsByDepartment(id).isEmpty();
        if (hasEvents) {
            throw new BusinessException("Cannot delete department with existing events. Remove all events first.");
        }
        departmentRepository.delete(dept);
    }

    public Department addDepartment(String name, String headName, String headPhone, String headEmail) {
        if (departmentRepository.existsByDepartmentNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Duplicate department name not allowed: " + name);
        }
        Department dept = new Department(name, headName, headPhone, headEmail);
        return departmentRepository.save(dept);
    }
}
