package com.citybooking.server.admin;

import com.citybooking.server.common.ApiResponse;
import com.citybooking.server.common.PageResult;
import com.citybooking.server.dto.MerchantDto.MerchantView;
import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.merchant.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> audit(@PathVariable Long id, @RequestParam boolean approve) {
        adminService.auditMerchant(id, approve);
        return ApiResponse.ok();
    }

    @GetMapping("/merchants")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<MerchantView>> merchants(@RequestParam(required = false) String status) {
        return ApiResponse.ok(adminService.listMerchants(status));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Long> createCategory(@RequestParam String name,
                                            @RequestParam(required = false) Long parentId,
                                            @RequestParam(required = false) Integer sort) {
        return ApiResponse.ok(adminService.createCategory(name, parentId, sort));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(adminService.listCategories());
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<OrderView>> orders(@RequestParam(required = false) String status) {
        return ApiResponse.ok(adminService.listOrders(status));
    }

    @PostMapping("/refunds/{orderId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> refund(@PathVariable Long orderId) {
        adminService.refundApprove(orderId);
        return ApiResponse.ok();
    }
}
