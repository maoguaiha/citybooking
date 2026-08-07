package com.citybooking.server.merchant;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.common.BizException;
import com.citybooking.server.common.ResultCode;
import com.citybooking.server.common.SecurityUtil;
import com.citybooking.server.dto.MerchantDto.MerchantView;
import com.citybooking.server.dto.MerchantDto.OnboardReq;
import com.citybooking.server.dto.MerchantDto.ServiceReq;
import com.citybooking.server.dto.MerchantDto.ServiceView;
import com.citybooking.server.dto.MerchantDto.TechnicianReq;
import com.citybooking.server.geo.GeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantMapper merchantMapper;
    private final TechnicianMapper technicianMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final GeoService geoService;

    public Long onboard(Long userId, String role, OnboardReq req) {
        Merchant merchant = new Merchant();
        merchant.setUserId(userId);
        merchant.setName(req.name());
        merchant.setAddress(req.address());
        merchant.setLng(req.lng());
        merchant.setLat(req.lat());
        merchant.setRadius(req.radius() == null ? 5000 : req.radius());
        merchant.setStatus("PENDING");
        merchant.setRating(0.0);
        merchantMapper.insert(merchant);

        if ("TECHNICIAN".equals(role)) {
            Technician tech = new Technician();
            tech.setUserId(userId);
            tech.setMerchantId(merchant.getId());
            tech.setName(req.name());
            tech.setSkill(req.name());
            tech.setLng(req.lng());
            tech.setLat(req.lat());
            tech.setStatus("PENDING");
            tech.setRating(0.0);
            technicianMapper.insert(tech);
        }
        return merchant.getId();
    }

    public Long addTechnician(Long userId, TechnicianReq req) {
        Technician tech = new Technician();
        tech.setUserId(userId);
        tech.setMerchantId(req.merchantId());
        tech.setName(req.name());
        tech.setSkill(req.skill());
        tech.setLng(req.lng());
        tech.setLat(req.lat());
        tech.setStatus("PENDING");
        tech.setRating(0.0);
        technicianMapper.insert(tech);
        return tech.getId();
    }

    public Long createService(Long userId, ServiceReq req) {
        Merchant merchant = merchantOf(userId);
        if (!"APPROVED".equals(merchant.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "商家未通过审核，无法发布服务");
        }
        ServiceItem item = new ServiceItem();
        item.setMerchantId(merchant.getId());
        item.setCategoryId(req.categoryId());
        item.setTitle(req.title());
        item.setDescription(req.description());
        item.setPrice(req.price());
        item.setDurationMin(req.durationMin());
        item.setAvailableStart(req.availableStart());
        item.setAvailableEnd(req.availableEnd());
        item.setStatus("ON");
        serviceItemMapper.insert(item);
        return item.getId();
    }

    public List<ServiceView> listOwnServices(Long userId) {
        Merchant merchant = merchantOf(userId);
        return serviceItemMapper.selectList(Wrappers.<ServiceItem>lambdaQuery()
                        .eq(ServiceItem::getMerchantId, merchant.getId()))
                .stream().map(s -> toView(s, merchant.getName(), merchant.getRating(), null)).toList();
    }

    public Merchant merchantOf(Long userId) {
        Merchant merchant = merchantMapper.selectOne(Wrappers.<Merchant>lambdaQuery().eq(Merchant::getUserId, userId));
        if (merchant == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商家资料不存在，请先入驻");
        }
        return merchant;
    }

    public MerchantView view(Merchant merchant) {
        return new MerchantView(merchant.getId(), merchant.getName(), merchant.getAddress(),
                merchant.getLng(), merchant.getLat(), merchant.getRadius(), merchant.getStatus(),
                merchant.getRating(), merchant.getRejectReason());
    }

    public void audit(Long merchantId, boolean approve, String reason) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商家不存在");
        }
        merchant.setStatus(approve ? "APPROVED" : "REJECTED");
        if (!approve) {
            merchant.setRejectReason(reason);
        }
        merchantMapper.updateById(merchant);
        technicianMapper.selectList(Wrappers.<Technician>lambdaQuery().eq(Technician::getMerchantId, merchantId))
                .forEach(t -> {
                    t.setStatus(merchant.getStatus());
                    technicianMapper.updateById(t);
                });
        if (approve) {
            geoService.addMerchant(merchantId, merchant.getLng(), merchant.getLat());
            technicianMapper.selectList(Wrappers.<Technician>lambdaQuery().eq(Technician::getMerchantId, merchantId))
                    .forEach(t -> geoService.addTechnician(t.getId(), t.getLng(), t.getLat()));
        }
    }

    public ServiceView toView(ServiceItem s, String merchantName, Double merchantRating, Double distanceM) {
        return new ServiceView(s.getId(), s.getMerchantId(), s.getTechnicianId(), s.getCategoryId(),
                s.getTitle(), s.getDescription(), s.getPrice(), s.getDurationMin(),
                s.getAvailableStart(), s.getAvailableEnd(), s.getStatus(),
                merchantName, merchantRating, distanceM);
    }

    public ServiceItem requireService(Long serviceId) {
        ServiceItem s = serviceItemMapper.selectById(serviceId);
        if (s == null) {
            throw new BizException(ResultCode.NOT_FOUND, "服务不存在");
        }
        return s;
    }

    public Merchant requireMerchant(Long merchantId) {
        Merchant m = merchantMapper.selectById(merchantId);
        if (m == null) {
            throw new BizException(ResultCode.NOT_FOUND, "商家不存在");
        }
        return m;
    }
}
