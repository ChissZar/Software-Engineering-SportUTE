package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.CustomerBookingRequestDTO;
import vn.edu.ute.sportute.dto.request.CustomerCourtFilterDTO;
import vn.edu.ute.sportute.dto.response.CustomerCourtCardDTO;

import java.util.List;

public interface CustomerBookingService {
    List<CustomerCourtCardDTO> searchAvailableCourts(CustomerCourtFilterDTO filter);
    void bookCourtOnline(CustomerBookingRequestDTO request, String accountUsernameOrEmail);
}