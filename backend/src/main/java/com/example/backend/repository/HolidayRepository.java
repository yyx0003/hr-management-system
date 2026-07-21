package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Holiday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HolidayRepository extends BaseMapper<Holiday> {

    @Select("SELECT * FROM holiday WHERE holiday_date BETWEEN #{start} AND #{end}")
    List<Holiday> findByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
