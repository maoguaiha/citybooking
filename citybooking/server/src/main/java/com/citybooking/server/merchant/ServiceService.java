package com.citybooking.server.merchant;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.common.PageResult;
import com.citybooking.server.dto.MerchantDto.ServiceView;
import com.citybooking.server.geo.GeoHit;
import com.citybooking.server.geo.GeoService;
import com.citybooking.server.merchant.Category;
import com.citybooking.server.merchant.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

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

        // Build WHERE clause manually — avoids MyBatis-Plus PaginationInnerInterceptor
        // which crashes on H2 embedded (SQLFeatureNotSupportedException on TIMESTAMP columns
        // during prepare(), even with H2 dialect, MP 3.5.7 + Spring Boot 3.3.4).
        StringBuilder where = new StringBuilder(" status='ON' AND deleted=0");
        List<Object> params = new java.util.ArrayList<>();
        if (merchantIds != null) {
            if (merchantIds.isEmpty()) return PageResult.of(0, page, size, List.of());
            where.append(" AND merchant_id IN (");
            for (int i = 0; i < merchantIds.size(); i++) {
                if (i > 0) where.append(",");
                where.append("?");
                params.add(merchantIds.get(i));
            }
            where.append(")");
        }
        if (categoryId != null) {
            where.append(" AND category_id=?");
            params.add(categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND title LIKE ?");
            params.add("%" + keyword + "%");
        }

        // Manual pagination via LIMIT/OFFSET
        String countSql = "SELECT COUNT(*) FROM service_item WHERE" + where;
        Integer total = jdbcTemplate.queryForObject(countSql, params.toArray(), Integer.class);
        if (total == null || total == 0) return PageResult.of(0, page, size, List.of());

        long t = total;
        int fromIdx = (page - 1) * size;
        String dataSql = "SELECT * FROM service_item WHERE" + where + " LIMIT ? OFFSET ?";
        params.add(size);
        params.add(fromIdx);
        List<ServiceItem> paged = jdbcTemplate.query(dataSql, params.toArray(),
                new BeanPropertyRowMapper<>(ServiceItem.class));

        Map<Long, Merchant> merchantMap = merchantMapper.selectBatchIds(
                        paged.stream().map(ServiceItem::getMerchantId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Merchant::getId, Function.identity()));
        List<ServiceView> list = paged.stream().map(s -> {
            Merchant m = merchantMap.get(s.getMerchantId());
            String name = m == null ? "" : m.getName();
            Double rating = m == null ? 0.0 : m.getRating();
            Double dist = distanceMap.getOrDefault(s.getMerchantId(), null);
            return toView(s, name, rating, dist);
        }).toList();
        return PageResult.of(t, page, size, list);
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
