package vn.edu.ute.sportute.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.ute.sportute.dto.response.RoleSummaryDTO;
import vn.edu.ute.sportute.dto.response.UserAccountDTO;
import vn.edu.ute.sportute.entity.Feature;
import vn.edu.ute.sportute.entity.Role;
import vn.edu.ute.sportute.service.PermissionService;

import java.util.*;

@Controller
@RequestMapping("/admin/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public String index(Model model) {
        List<Role> roles = permissionService.getAllRoles();
        List<Feature> features = permissionService.getAllFeatures();
        List<UserAccountDTO> accounts = permissionService.getAllUserAccountDTOs();
        List<RoleSummaryDTO> roleSummaries = permissionService.getRoleSummaries();
        Map<String, Set<String>> matrix = permissionService.getRoleFeatureMatrix();
        Map<String, Object> stats = permissionService.getPermissionStatistics();

        model.addAttribute("roles", roles);
        model.addAttribute("features", features);
        model.addAttribute("accounts", accounts);
        model.addAttribute("roleSummaries", roleSummaries);
        model.addAttribute("matrix", matrix);
        model.addAttribute("stats", stats);

        return "admin/permissions";
    }

    @PostMapping("/matrix/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveMatrix(@RequestBody Map<String, List<String>> payload) {
        permissionService.saveFullMatrix(payload);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã lưu và đồng bộ ma trận phân quyền VAITROCHUCNANG thành công!");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/matrix/reset")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resetMatrix() {
        permissionService.resetMatrixToDefault();
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã khôi phục ma trận phân quyền về cấu hình mặc định ban đầu");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/account/roles")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> updateAccountRoles(@RequestBody Map<String, Object> payload) {
        String accountId = (String) payload.get("accountId");
        List<String> roleIds = (List<String>) payload.get("roleIds");

        if (accountId == null || accountId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu mã tài khoản"));
        }

        permissionService.updateAccountRoles(accountId, roleIds != null ? roleIds : Collections.emptyList());

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã cập nhật vai trò phân quyền cho tài khoản " + accountId + " thành công!");
        return ResponseEntity.ok(res);
    }

    @PostMapping("/account/toggle/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable("id") String accountId) {
        String newStatus = permissionService.toggleAccountStatus(accountId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("status", newStatus);
        res.put("message", "Trạng thái tài khoản " + accountId + " hiện tại là: " + newStatus);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/role/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createRole(@RequestBody Map<String, String> payload) {
        String id = payload.get("id");
        String name = payload.get("name");
        String desc = payload.get("description");

        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mã và tên vai trò không được để trống!"));
        }

        permissionService.createRole(id.trim(), name.trim(), desc != null ? desc.trim() : "");

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã thêm vai trò " + name + " vào hệ thống thành công!");
        return ResponseEntity.ok(res);
    }
}
