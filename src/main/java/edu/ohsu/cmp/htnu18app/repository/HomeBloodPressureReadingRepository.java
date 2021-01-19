package edu.ohsu.cmp.htnu18app.repository;

import edu.ohsu.cmp.htnu18app.entity.HomeBloodPressureReading;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HomeBloodPressureReadingRepository extends CrudRepository<HomeBloodPressureReading, Long> {
    @Query("select bpr from HomeBloodPressureReading bpr where bpr.patId=:patId order by bpr.readingDate desc")
    public List<HomeBloodPressureReading> findAllByPatId(@Param("patId") int patId);
}
