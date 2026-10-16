package vn.edu.ute.sportute.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.ute.sportute.dto.request.EmployeeDTO;
import vn.edu.ute.sportute.service.EmployeeService;

/**
 * Quản trị nhân sự và tài khoản nhân viên (UC05 - UC08).
 */
@Controller
@RequestMapping("/admin/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public String index(@RequestParam(value = "keyword", required = false) String keyword,
                        @RequestParam(value = "position", required = false, defaultValue = "Tất cả") String position,
                        @RequestParam(value = "status", required = false, defaultValue = "Tất cả") String status,
                        @RequestParam(value = "editId", required = false) String editId,
                        Model model) {
        model.addAttribute("employees", employeeService.getAllEmployees(keyword, position, status));
        model.addAttribute("stats", employeeService.getEmployeeStatistics());
        model.addAttribute("currentKeyword", keyword != null ? keyword : "");
        model.addAttribute("selectedPosition", position);
        model.addAttribute("selectedStatus", status);

        if (editId != null && !editId.trim().isEmpty()) {
            model.addAttribute("employeeDTO", employeeService.getEmployeeById(editId));
        } else {
            model.addAttribute("employeeDTO", new EmployeeDTO());
        }

        return "employees/index";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("employeeDTO") EmployeeDTO dto,
                       BindingResult result,
                       RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thông tin nhân viên không hợp lệ!");
            return "redirect:/admin/employees";
        }
        try {
            employeeService.saveEmployee(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu thông tin nhân viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @PostMapping("/toggle/{id}")
    public String toggleStatus(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        try {
            employeeService.toggleEmployeeStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái nhân viên #" + id + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/employees";
    }
}
