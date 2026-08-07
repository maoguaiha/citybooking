package com.citybooking.server.admin;

import com.citybooking.server.common.ApiResponse;
import com.citybooking.server.common.PageResult;
import com.citybooking.server.dto.MerchantDto.MerchantView;
import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.merchant.Category;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/merchants/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> audit(@PathVariable Long id, @RequestParam boolean approve,
                                   @RequestParam(required = false) String reason) {
        adminService.auditMerchant(id, approve, approve ? null : reason);
        return ApiResponse.ok();
    }

    @GetMapping("/merchants")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<MerchantView>> merchants(@RequestParam(required = false) String status) {
        return ApiResponse.ok(adminService.listMerchants(status));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Long> createCategory(@RequestParam String name,
                                            @RequestParam(required = false) Long parentId,
                                            @RequestParam(required = false) Integer sort) {
        return ApiResponse.ok(adminService.createCategory(name, parentId, sort));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(adminService.listCategories());
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<PageResult<OrderView>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(adminService.listOrders(page, size, keyword, status));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<OrderView> orderDetail(@PathVariable Long id) {
        return ApiResponse.ok(adminService.orderDetail(id));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<AdminService.DashboardView> dashboard() {
        return ApiResponse.ok(adminService.dashboard());
    }

    // ===== 用户 / 消费者管理 =====

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<PageResult<AdminService.UserView>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(adminService.listUsers(page, size, keyword, status));
    }

    @PostMapping("/users/{id}/ban")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> banUser(@PathVariable Long id) {
        adminService.banUser(id);
        return ApiResponse.ok();
    }

    @PostMapping("/users/{id}/unban")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> unbanUser(@PathVariable Long id) {
        adminService.unbanUser(id);
        return ApiResponse.ok();
    }

    @GetMapping("/users/{id}/orders")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<PageResult<OrderView>> listUserOrders(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(adminService.listUserOrders(id, page, size));
    }

    // ===== 技师管理 =====

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<PageResult<AdminService.TechnicianView>> listTechnicians(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(adminService.listTechnicians(page, size, keyword, status));
    }

    @PostMapping("/technicians/{id}/enable")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> enableTechnician(@PathVariable Long id) {
        adminService.enableTechnician(id);
        return ApiResponse.ok();
    }

    @PostMapping("/technicians/{id}/disable")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> disableTechnician(@PathVariable Long id) {
        adminService.disableTechnician(id);
        return ApiResponse.ok();
    }

    // ===== 服务内容治理 =====

    @GetMapping("/services")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<PageResult<AdminService.ServiceView>> listServices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(adminService.listServices(page, size, keyword, status));
    }

    @PostMapping("/services/{id}/offline")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> offlineService(@PathVariable Long id) {
        adminService.offlineService(id);
        return ApiResponse.ok();
    }

    @PostMapping("/services/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> restoreService(@PathVariable Long id) {
        adminService.restoreService(id);
        return ApiResponse.ok();
    }

    @PostMapping("/refunds/{orderId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> refund(@PathVariable Long orderId) {
        adminService.refundApprove(orderId);
        return ApiResponse.ok();
    }

    @PostMapping("/refunds/{orderId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<Void> refundReject(@PathVariable Long orderId) {
        adminService.refundReject(orderId);
        return ApiResponse.ok();
    }

    // ===== 管理员账号管理（仅 SUPER_ADMIN） =====

    @PostMapping("/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<AdminService.AdminView> createAdmin(@RequestBody @Valid AdminService.CreateAdminReq req) {
        return ApiResponse.ok(adminService.createAdmin(req));
    }

    @GetMapping("/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<List<AdminService.AdminView>> listAdmins() {
        return ApiResponse.ok(adminService.listAdmins());
    }
}
