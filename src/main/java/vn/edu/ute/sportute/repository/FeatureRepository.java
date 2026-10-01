package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.Feature;

import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, String> {
    Optional<Feature> findByName(String name);
}