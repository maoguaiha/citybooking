package com.citybooking.server.merchant;

import com.citybooking.server.common.ApiResponse;
import com.citybooking.server.common.SecurityUtil;
import com.citybooking.server.dto.MerchantDto.MerchantView;
import com.citybooking.server.dto.MerchantDto.OnboardReq;
import com.citybooking.server.dto.MerchantDto.ServiceReq;
import com.citybooking.server.dto.MerchantDto.ServiceView;
import com.citybooking.server.dto.MerchantDto.TechnicianReq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping("/onboard")
    @PreAuthorize("hasAnyRole('MERCHANT','TECHNICIAN')")
    public ApiResponse<Long> onboard(@RequestBody @Valid OnboardReq req) {
        Long uid = SecurityUtil.currentUserId();
        return ApiResponse.ok(merchantService.onboard(uid, SecurityUtil.currentRole(), req));
    }

    @PostMapping("/technicians")
    @PreAuthorize("hasRole('MERCHANT')")
    public ApiResponse<Long> addTechnician(@RequestBody @Valid TechnicianReq req) {
        Long uid = SecurityUtil.currentUserId();
        Merchant merchant = merchantService.merchantOf(uid);
        TechnicianReq fixed = new TechnicianReq(req.name(), req.skill(), req.lng(), req.lat(), merchant.getId());
        return ApiResponse.ok(merchantService.addTechnician(uid, fixed));
    }

    @PostMapping("/services")
    @PreAuthorize("hasAnyRole('MERCHANT','TECHNICIAN')")
    public ApiResponse<Long> createService(@RequestBody @Valid ServiceReq req) {
        return ApiResponse.ok(merchantService.createService(SecurityUtil.currentUserId(), req));
    }

    @GetMapping("/services")
    @PreAuthorize("hasAnyRole('MERCHANT','TECHNICIAN')")
    public ApiResponse<List<ServiceView>> myServices() {
        return ApiResponse.ok(merchantService.listOwnServices(SecurityUtil.currentUserId()));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('MERCHANT','TECHNICIAN')")
    public ApiResponse<MerchantView> profile() {
        Merchant merchant = merchantService.merchantOf(SecurityUtil.currentUserId());
        return ApiResponse.ok(merchantService.view(merchant));
    }
}
