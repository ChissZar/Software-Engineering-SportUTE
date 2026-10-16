package vn.edu.ute.sportute.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ute.sportute.dto.request.AdminBookingDTO;
import vn.edu.ute.sportute.service.AdminBookingService;
import vn.edu.ute.sportute.service.BookingService;

import java.util.List;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private final AdminBookingService adminBookingService;

    public BookingServiceImpl(AdminBookingService adminBookingService) {
        this.adminBookingService = adminBookingService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminBookingDTO> getAllBookings() {
        return adminBookingService.getBookings(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBookingDTO getBookingById(String id) {
        return adminBookingService.getBookingById(id);
    }
}
