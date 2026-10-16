package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.CustomerDTO;
import vn.edu.ute.sportute.dto.response.CustomerRentHistoryDTO;
import vn.edu.ute.sportute.service.CustomerService;

import java.util.List;

/**
 * Quản trị hồ sơ khách hàng (UC30 - UC34).
 */
@Controller
@RequestMapping("/admin/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public String index(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "tier", required = false, defaultValue = "Tất cả") String tier,
                        @RequestParam(value = "status", required = false, defaultValue = "Tất cả") String status,
                        @RequestParam(value = "editId", required = false) String editId,
                        Model model) {
        model.addAttribute("customers", customerService.getAllCustomers(keyword, tier, status));
        model.addAttribute("stats", customerService.getCustomerStatistics());
        model.addAttribute("currentKeyword", keyword != null ? keyword : "");
        model.addAttribute("selectedTier", tier);
        model.addAttribute("selectedStatus", status);

        if (editId != null && !editId.trim().isEmpty()) {
            model.addAttribute("customerDTO", customerService.getCustomerById(editId));
        } else {
            model.addAttribute("customerDTO", new CustomerDTO());
        }

        return "customers/index";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("customerDTO") CustomerDTO dto,
                       BindingResult result,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin khách hàng không hợp lệ, vui lòng kiểm tra lại!");
            return "redirect:/admin/customers";
        }
        try {
            customerService.saveCustomer(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin khách hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/toggle/{id}")
    public String toggleStatus(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            customerService.toggleCustomerStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái khách hàng #" + id + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @GetMapping("/api/history/{id}")
    @ResponseBody
    public List<CustomerRentHistoryDTO> getHistory(@PathVariable("id") String id) {
        return customerService.getCustomerRentHistory(id);
    }

    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public CustomerDTO getDetail(@PathVariable("id") String id) {
        return customerService.getCustomerById(id);
    }
}
