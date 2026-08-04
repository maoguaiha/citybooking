package com.citybooking.server.admin;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.citybooking.server.admin.AdminRoles;
import com.citybooking.server.auth.User;
import com.citybooking.server.auth.UserMapper;
import com.citybooking.server.common.BizException;
import com.citybooking.server.common.PageResult;
import com.citybooking.server.common.ResultCode;
import com.citybooking.server.merchant.Category;
import com.citybooking.server.merchant.CategoryMapper;
import com.citybooking.server.merchant.Merchant;
import com.citybooking.server.merchant.MerchantMapper;
import com.citybooking.server.merchant.MerchantService;
import com.citybooking.server.merchant.ServiceItem;
import com.citybooking.server.merchant.ServiceItemMapper;
import com.citybooking.server.merchant.Technician;
import com.citybooking.server.merchant.TechnicianMapper;
import com.citybooking.server.dto.MerchantDto.MerchantView;
import com.citybooking.server.notice.NoticeService;
import com.citybooking.server.order.Order;
import com.citybooking.server.order.OrderMapper;
import com.citybooking.server.order.OrderService;
import com.citybooking.server.order.OrderStatus;
import com.citybooking.server.dto.OrderDto.OrderView;
import com.citybooking.server.payment.PaymentService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final MerchantService merchantService;
    private final CategoryMapper categoryMapper;
    private final MerchantMapper merchantMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final NoticeService noticeService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TechnicianMapper technicianMapper;
    private final ServiceItemMapper serviceItemMapper;

    private static final String PHONE_RE = "^1[3-9]\\d{9}$";

    public void auditMerchant(Long merchantId, boolean approve) {
        merchantService.audit(merchantId, approve);
    }

    public List<MerchantView> listMerchants(String status) {
        var q = Wrappers.<Merchant>lambdaQuery();
        if (status != null) {
            q.eq(Merchant::getStatus, status);
        }
        q.orderByDesc(Merchant::getId);
        return merchantMapper.selectList(q).stream()
                .map(m -> new MerchantView(m.getId(), m.getName(), m.getAddress(), m.getLng(),
                        m.getLat(), m.getRadius(), m.getStatus(), m.getRating()))
                .toList();
    }

    public Long createCategory(String name, Long parentId, Integer sort) {
        Category c = new Category();
        c.setName(name);
        c.setParentId(parentId == null ? 0L : parentId);
        c.setSort(sort == null ? 0 : sort);
        categoryMapper.insert(c);
        return c.getId();
    }

    public List<Category> listCategories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSort));
    }

    public List<OrderView> listOrders(String status) {
        var q = Wrappers.<Order>lambdaQuery();
        if (status != null) {
            q.eq(Order::getStatus, status);
        }
        q.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectList(q).stream().map(orderService::toView).toList();
    }

    public void refundApprove(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (OrderStatus.COMPLETED.name().equals(order.getStatus())
                || OrderStatus.SERVICING.name().equals(order.getStatus())
                || OrderStatus.ACCEPTED.name().equals(order.getStatus())) {
            paymentService.refund(orderId, order.getAmount());
            order.setRefundStatus("FULL");
            order.setPayStatus("REFUNDED");
            order.setStatus(OrderStatus.REFUNDED.name());
            orderMapper.updateById(order);
            noticeService.send(order.getConsumerId(), "REFUND_APPROVED",
                    Map.of("orderId", orderId, "amount", order.getAmount()));
            if (order.getMerchantId() != null) {
                noticeService.send(order.getMerchantId(), "REFUND_NOTICE", Map.of("orderId", orderId));
            }
        } else {
            throw new BizException(ResultCode.BAD_REQUEST, "该订单状态不支持仲裁退款");
        }
    }

    // ===== 管理员账号管理（仅 SUPER_ADMIN 可调用，见 AdminController） =====

    public record AdminView(Long id, String phone, String nickname, String role, Integer status) {
    }

    public record CreateAdminReq(@NotBlank String phone, @NotBlank String password, String nickname) {
    }

    /**
     * 超管创建运营管理员（ADMIN）。手机号唯一，密码至少 6 位。
     */
    public AdminView createAdmin(CreateAdminReq req) {
        if (req.phone() == null || !req.phone().matches(PHONE_RE)) {
            throw new BizException(ResultCode.BAD_REQUEST, "手机号格式不正确");
        }
        if (req.password() == null || req.password().length() < 6) {
            throw new BizException(ResultCode.BAD_REQUEST, "密码至少 6 位");
        }
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getPhone, req.phone())) > 0) {
            throw new BizException(ResultCode.CONFLICT, "手机号已注册");
        }
        User u = new User();
        u.setPhone(req.phone());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setNickname(req.nickname() == null ? req.phone() : req.nickname());
        u.setRole(AdminRoles.ADMIN);
        u.setStatus(1);
        userMapper.insert(u);
        return new AdminView(u.getId(), u.getPhone(), u.getNickname(), u.getRole(), u.getStatus());
    }

    /**
     * 列出全部管理员账号（ADMIN + SUPER_ADMIN），脱敏不含密码。
     */
    public List<AdminView> listAdmins() {
        return userMapper.selectList(Wrappers.<User>lambdaQuery()
                        .in(User::getRole, AdminRoles.ALL)
                        .orderByDesc(User::getId))
                .stream()
                .map(u -> new AdminView(u.getId(), u.getPhone(), u.getNickname(), u.getRole(), u.getStatus()))
                .toList();
    }

    // ===== 平台数据看板 =====

    public record DashboardView(
            long todayOrderCount,
            BigDecimal todayGmv,
            long totalUsers,
            long totalMerchants,
            long pendingMerchants,
            long totalTechnicians,
            long pendingRefunds,
            long totalServices) {
    }

    /**
     * 平台运营总览统计。所有聚合均带过滤条件，不对全表做内存分页（红线⑨⑫）。
     */
    public DashboardView dashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayOrderCount = orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .ge(Order::getCreatedAt, todayStart).eq(Order::getDeleted, 0));
        List<Order> todayOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .ge(Order::getCreatedAt, todayStart)
                .ne(Order::getStatus, "CANCELLED").eq(Order::getDeleted, 0));
        BigDecimal todayGmv = todayOrders.stream().map(Order::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalUsers = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getRole, "CONSUMER").eq(User::getDeleted, 0));
        long totalMerchants = merchantMapper.selectCount(Wrappers.<Merchant>lambdaQuery().eq(Merchant::getDeleted, 0));
        long pendingMerchants = merchantMapper.selectCount(Wrappers.<Merchant>lambdaQuery()
                .eq(Merchant::getStatus, "PENDING").eq(Merchant::getDeleted, 0));
        long totalTechnicians = technicianMapper.selectCount(Wrappers.<Technician>lambdaQuery().eq(Technician::getDeleted, 0));
        long pendingRefunds = orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .ne(Order::getRefundStatus, "NONE").ne(Order::getStatus, "REFUNDED").eq(Order::getDeleted, 0));
        long totalServices = serviceItemMapper.selectCount(Wrappers.<ServiceItem>lambdaQuery().eq(ServiceItem::getDeleted, 0));
        return new DashboardView(todayOrderCount, todayGmv, totalUsers, totalMerchants,
                pendingMerchants, totalTechnicians, pendingRefunds, totalServices);
    }

    // ===== 用户 / 消费者管理 =====

    public record UserView(Long id, String phone, String nickname, Integer status, LocalDateTime createdAt) {
    }

    public PageResult<UserView> listUsers(int page, int size, String keyword, Integer status) {
        var q = Wrappers.<User>lambdaQuery()
                .eq(User::getRole, "CONSUMER")
                .eq(User::getDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(User::getPhone, keyword).or().like(User::getNickname, keyword));
        }
        if (status != null) {
            q.eq(User::getStatus, status);
        }
        long total = userMapper.selectCount(q);
        q.orderByDesc(User::getId);
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectList(q.last("LIMIT " + size + " OFFSET " + offset));
        return new PageResult<>(total, page, size,
                users.stream()
                        .map(u -> new UserView(u.getId(), u.getPhone(), u.getNickname(), u.getStatus(), u.getCreatedAt()))
                        .toList());
    }

    @Transactional
    public void banUser(Long id) {
        setUserStatus(id, 0);
    }

    @Transactional
    public void unbanUser(Long id) {
        setUserStatus(id, 1);
    }

    private void setUserStatus(Long id, int status) {
        User u = userMapper.selectById(id);
        if (u == null || !"CONSUMER".equals(u.getRole())) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在或不可操作");
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, id).set(User::getStatus, status));
    }

    public PageResult<OrderView> listUserOrders(Long userId, int page, int size) {
        var q = Wrappers.<Order>lambdaQuery()
                .eq(Order::getConsumerId, userId).eq(Order::getDeleted, 0);
        long total = orderMapper.selectCount(q);
        q.orderByDesc(Order::getId);
        int offset = (page - 1) * size;
        List<Order> orders = orderMapper.selectList(q.last("LIMIT " + size + " OFFSET " + offset));
        return new PageResult<>(total, page, size,
                orders.stream().map(orderService::toView).toList());
    }

    // ===== 技师管理 =====

    public record TechnicianView(Long id, String name, String skill, String status,
                                 Double rating, Long merchantId) {
    }

    public PageResult<TechnicianView> listTechnicians(int page, int size, String keyword, String status) {
        var q = Wrappers.<Technician>lambdaQuery().eq(Technician::getDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(Technician::getName, keyword).or().like(Technician::getSkill, keyword));
        }
        if (status != null && !status.isBlank()) {
            q.eq(Technician::getStatus, status);
        }
        long total = technicianMapper.selectCount(q);
        q.orderByDesc(Technician::getId);
        int offset = (page - 1) * size;
        List<Technician> techs = technicianMapper.selectList(q.last("LIMIT " + size + " OFFSET " + offset));
        return new PageResult<>(total, page, size,
                techs.stream().map(t -> new TechnicianView(
                        t.getId(), t.getName(), t.getSkill(), t.getStatus(), t.getRating(), t.getMerchantId()))
                        .toList());
    }

    @Transactional
    public void enableTechnician(Long id) {
        setTechnicianStatus(id, "APPROVED");
    }

    @Transactional
    public void disableTechnician(Long id) {
        setTechnicianStatus(id, "REJECTED");
    }

    private void setTechnicianStatus(Long id, String status) {
        if (technicianMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "技师不存在");
        }
        technicianMapper.update(null, Wrappers.<Technician>lambdaUpdate()
                .eq(Technician::getId, id).set(Technician::getStatus, status));
    }
}
