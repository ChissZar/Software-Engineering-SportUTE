package vn.edu.ute.sportute.service;

import vn.edu.ute.sportute.dto.request.AddServiceOrderRequestDTO;
import vn.edu.ute.sportute.dto.request.ServiceItemDTO;
import vn.edu.ute.sportute.entity.BookingServiceDetail;
import vn.edu.ute.sportute.entity.ServiceItem;

import java.math.BigDecimal;
import java.util.List;

public interface ServiceItemService {
    List<ServiceItem> getAllServices();
    List<ServiceItem> getActiveServices();
    List<ServiceItem> getServicesByCategory(String category);
    ServiceItem getServiceById(String id);
    ServiceItem saveService(ServiceItemDTO dto);
    void toggleServiceStatus(String id);
    void deleteService(String id);

    BookingServiceDetail addServiceToBooking(AddServiceOrderRequestDTO request);
    List<BookingServiceDetail> getServicesByBookingId(String bookingId);
    BigDecimal getTotalServiceAmountByBookingId(String bookingId);
    void removeServiceFromBooking(String detailId);
}
