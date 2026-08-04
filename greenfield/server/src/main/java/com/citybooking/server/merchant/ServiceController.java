package com.citybooking.server.merchant;

import com.citybooking.server.common.ApiResponse;
import com.citybooking.server.common.PageResult;
import com.citybooking.server.dto.MerchantDto.ServiceView;
import com.citybooking.server.merchant.Category;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping
    public ApiResponse<PageResult<ServiceView>> search(
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(serviceService.search(lng, lat, radius, categoryId, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ServiceView> detail(@PathVariable Long id) {
        return ApiResponse.ok(serviceService.detail(id));
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> categories() {
        return ApiResponse.ok(serviceService.listCategories());
    }
}
