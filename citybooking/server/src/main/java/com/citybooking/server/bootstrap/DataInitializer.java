package com.citybooking.server.bootstrap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.admin.AdminRoles;
import com.citybooking.server.auth.User;
import com.citybooking.server.auth.UserMapper;
import com.citybooking.server.geo.GeoService;
import com.citybooking.server.merchant.Category;
import com.citybooking.server.merchant.CategoryMapper;
import com.citybooking.server.merchant.Merchant;
import com.citybooking.server.merchant.MerchantMapper;
import com.citybooking.server.merchant.ServiceItem;
import com.citybooking.server.merchant.ServiceItemMapper;
import com.citybooking.server.merchant.Technician;
import com.citybooking.server.merchant.TechnicianMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final TechnicianMapper technicianMapper;
    private final GeoService geoService;
    private final PasswordEncoder passwordEncoder;

    private final String adminPhone;
    private final String adminPassword;

    public DataInitializer(CategoryMapper categoryMapper,
                            UserMapper userMapper,
                            MerchantMapper merchantMapper,
                            ServiceItemMapper serviceItemMapper,
                            TechnicianMapper technicianMapper,
                            GeoService geoService,
                            PasswordEncoder passwordEncoder,
                            @Value("${citybooking.admin.phone:10000000000}") String adminPhone,
                            @Value("${citybooking.admin.password:Admin@123456}") String adminPassword) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.technicianMapper = technicianMapper;
        this.geoService = geoService;
        this.passwordEncoder = passwordEncoder;
        this.adminPhone = adminPhone;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        seedCategoriesIfAbsent();
        seedAdminIfAbsent();
        seedDemoData();
    }

    private void seedCategoriesIfAbsent() {
        if (categoryMapper.selectCount(Wrappers.<Category>lambdaQuery().eq(Category::getParentId, 0L)) > 0) {
            return;
        }
        List<Category> roots = List.of(
                category("家政保洁"), category("家电维修"), category("搬家服务"),
                category("上门按摩"), category("宠物服务"), category("美容美甲"));
        roots.forEach(categoryMapper::insert);
    }

    private void seedAdminIfAbsent() {
        long existing = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().in(User::getRole, AdminRoles.ALL));
        if (existing > 0) {
            return;
        }
        User admin = new User();
        admin.setPhone(adminPhone);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNickname("平台超管");
        admin.setRole(AdminRoles.SUPER_ADMIN);
        admin.setStatus(1);
        userMapper.insert(admin);
    }

    // Demo data so /api/services returns real records in dev (H2 in-memory).
    // Merchants sit within ~2km of Beijing center so default geo radius (5km) covers them.
    // Each merchant is also registered into GeoService so coordinate-based search works.
    private void seedDemoData() {
        if (merchantMapper.selectCount(Wrappers.<Merchant>lambdaQuery()) > 0) {
            return;
        }

        record Svc(String title, String desc, double price, int dur) {}
        record Seed(String name, String address, double lng, double lat, double rating,
                    String cat, Svc... svcs) {}

        Seed[] seeds = new Seed[]{
                new Seed("闪洁家政", "朝阳区建国路88号", 116.401, 39.918, 4.8, "家政保洁",
                        new Svc("日常保洁3小时", "专业保洁阿姨上门，含地面/厨房/卫生间", 99.0, 180),
                        new Svc("深度保洁(含擦窗)", "全屋深度清洁 + 玻璃擦拭", 199.0, 240)),
                new Seed("快修哥家电维修", "海淀区中关村大街19号", 116.408, 39.912, 4.6, "家电维修",
                        new Svc("空调清洗加氟", "挂机/柜机深度清洗 + 冷媒补充", 129.0, 60),
                        new Svc("洗衣机维修", "波轮/滚筒故障检测与维修", 89.0, 90)),
                new Seed("轻松搬家", "丰台区方庄南路12号", 116.399, 39.920, 4.7, "搬家服务",
                        new Svc("小件搬家(含2人搬运)", "含2名师傅搬运 + 基础打包", 299.0, 120),
                        new Svc("贵重物品打包", "易碎/贵重物品专业打包", 159.0, 90)),
                new Seed("舒心上门按摩", "东城区东直门外大街5号", 116.410, 39.908, 4.9, "上门按摩",
                        new Svc("中式推拿60分钟", "经络推拿，缓解疲劳", 219.0, 60),
                        new Svc("肩颈放松30分钟", "针对久坐肩颈不适", 119.0, 30)),
                new Seed("萌宠之家", "西城区西单北大街31号", 116.398, 39.922, 4.5, "宠物服务",
                        new Svc("宠物上门喂养", "喂食/换水/清理，含拍照反馈", 59.0, 60),
                        new Svc("宠物洗澡美容", "洗澡 + 修剪 + 造型", 139.0, 120)),
                new Seed("美丽定制美甲", "朝阳区三里屯路19号", 116.412, 39.906, 4.8, "美容美甲",
                        new Svc("基础美甲", "修甲 + 单色/法式", 99.0, 90),
                        new Svc("新娘跟妆", "全天跟妆 + 造型", 599.0, 180)),
        };

        for (Seed s : seeds) {
            Long catId = catId(s.cat());
            Merchant m = new Merchant();
            m.setName(s.name());
            m.setAddress(s.address());
            m.setLng(s.lng());
            m.setLat(s.lat());
            m.setRating(s.rating());
            m.setStatus("APPROVED");
            m.setRadius(5000);
            merchantMapper.insert(m);
            geoService.addMerchant(m.getId(), m.getLng(), m.getLat());

            Technician t = new Technician();
            t.setMerchantId(m.getId());
            t.setName(s.name() + "·王师傅");
            t.setSkill(s.cat());
            t.setLng(m.getLng() + 0.001);
            t.setLat(m.getLat() - 0.001);
            t.setStatus("ONLINE");
            t.setRating(4.7);
            technicianMapper.insert(t);
            geoService.addTechnician(t.getId(), t.getLng(), t.getLat());

            for (Svc sv : s.svcs()) {
                ServiceItem si = new ServiceItem();
                si.setMerchantId(m.getId());
                si.setTechnicianId(t.getId());
                si.setCategoryId(catId);
                si.setTitle(sv.title());
                si.setDescription(sv.desc());
                si.setPrice(java.math.BigDecimal.valueOf(sv.price()));
                si.setDurationMin(sv.dur());
                si.setStatus("ON");
                si.setAvailableStart(LocalDateTime.now());
                si.setAvailableEnd(LocalDateTime.now().plusHours(8));
                serviceItemMapper.insert(si);
            }
        }
    }

    private Long catId(String name) {
        return categoryMapper.selectOne(
                Wrappers.<Category>lambdaQuery().eq(Category::getName, name)).getId();
    }

    private Category category(String name) {
        Category c = new Category();
        c.setName(name);
        c.setParentId(0L);
        c.setSort(0);
        return c;
    }
}
