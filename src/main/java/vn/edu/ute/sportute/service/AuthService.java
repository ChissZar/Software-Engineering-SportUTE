package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.RegisterRequest;
import vn.edu.ute.sportute.entity.Account;

public interface AuthService {
    Account registerCustomer(RegisterRequest request);
}