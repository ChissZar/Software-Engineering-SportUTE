package vn.edu.ute.sportute.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.ute.sportute.entity.Employee;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Optional<Employee> findByPhone(String phone);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByAccount_Id(String accountId);
    List<Employee> findByStatus(String status);
}