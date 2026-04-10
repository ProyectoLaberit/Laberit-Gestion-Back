package com.example.demo.repository;


import com.example.demo.entity.Excel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExcelRepository extends JpaRepository<Excel, Integer> {

    Excel findTopByIdProyectoOrderByIdExcelDesc(Long idProyecto);
}