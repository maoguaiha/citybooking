package com.citybooking.server.merchant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.citybooking.server.common.PageResult;
import com.citybooking.server.dto.MerchantDto.ServiceView;
import com.citybooking.server.geo.GeoHit;
import com.citybooking.server.geo.GeoService;
import com.citybooking.server.merchant.Category;
import com.citybooking.server.merchant.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceService {

    private final ServiceItemMapper serviceItemMapper;
    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;
    private final GeoService geoService;

    @Value("${app.dispatch.default-radius-m:5000}")
    private double defaultRadius;

    public PageResult<ServiceView> search(Double lng, Double lat, Double radius,
                                          Long categoryId, String keyword, int page, int size) {
        List<Long> merchantIds = null;
        Map<Long, Double> distanceMap;
        if (lng != null && lat != null) {
            double r = radius != null ? radius : defaultRadius;
            List<GeoHit> hits = geoService.nearbyMerchants(lng, lat, r);
            merchantIds = hits.stream().map(GeoHit::id).toList();
            distanceMap = hits.stream().collect(Collectors.toMap(GeoHit::id, GeoHit::distanceM));
            if (merchantIds.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
        } else {
            distanceMap = Map.of();
        }

        var q = Wrappers.<ServiceItem>lambdaQuery()
                .eq(ServiceItem::getStatus, "ON");
        if (merchantIds != null) {
            q.in(ServiceItem::getMerchantId, merchantIds);
        }
        if (categoryId != null) {
            q.eq(ServiceItem::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            q.like(ServiceItem::getTitle, keyword);
        }
        IPage<ServiceItem> pg = serviceItemMapper.selectPage(new Page<>(page, size), q);
        Map<Long, Merchant> merchantMap = merchantMapper.selectBatchIds(
                        pg.getRecords().stream().map(ServiceItem::getMerchantId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Merchant::getId, Function.identity()));
        List<ServiceView> list = pg.getRecords().stream().map(s -> {
            Merchant m = merchantMap.get(s.getMerchantId());
            String name = m == null ? "" : m.getName();
            Double rating = m == null ? 0.0 : m.getRating();
            Double dist = distanceMap.getOrDefault(s.getMerchantId(), null);
            return toView(s, name, rating, dist);
        }).toList();
        return PageResult.of(pg.getTotal(), page, size, list);
    }

    private ServiceView toView(ServiceItem s, String name, Double rating, Double dist) {
        return new ServiceView(s.getId(), s.getMerchantId(), s.getTechnicianId(), s.getCategoryId(),
                s.getTitle(), s.getDescription(), s.getPrice(), s.getDurationMin(),
                s.getAvailableStart(), s.getAvailableEnd(), s.getStatus(), name, rating, dist);
    }

    public ServiceView detail(Long id) {
        ServiceItem s = serviceItemMapper.selectById(id);
        if (s == null) {
            throw new com.citybooking.server.common.BizException(
                    com.citybooking.server.common.ResultCode.NOT_FOUND, "服务不存在");
        }
        Merchant m = merchantMapper.selectById(s.getMerchantId());
        return toView(s, m == null ? "" : m.getName(), m == null ? 0.0 : m.getRating(), null);
    }

    public List<Category> listCategories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSort));
    }
}
