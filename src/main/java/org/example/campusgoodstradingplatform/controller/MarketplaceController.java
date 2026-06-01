package org.example.campusgoodstradingplatform.controller;

import jakarta.servlet.http.HttpSession;
import org.example.campusgoodstradingplatform.dto.FeeRequest;
import org.example.campusgoodstradingplatform.dto.LoginRequest;
import org.example.campusgoodstradingplatform.dto.ProductRequest;
import org.example.campusgoodstradingplatform.dto.PunishRequest;
import org.example.campusgoodstradingplatform.dto.RechargeRequest;
import org.example.campusgoodstradingplatform.dto.RegisterRequest;
import org.example.campusgoodstradingplatform.dto.ReviewRequest;
import org.example.campusgoodstradingplatform.entity.CartItem;
import org.example.campusgoodstradingplatform.entity.ProductStatus;
import org.example.campusgoodstradingplatform.entity.Role;
import org.example.campusgoodstradingplatform.entity.User;
import org.example.campusgoodstradingplatform.entity.UserStatus;
import org.example.campusgoodstradingplatform.service.FileStorageService;
import org.example.campusgoodstradingplatform.service.MarketplaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class MarketplaceController {
    private final MarketplaceService service;
    private final FileStorageService fileStorage;

    public MarketplaceController(MarketplaceService service, FileStorageService fileStorage) {
        this.service = service;
        this.fileStorage = fileStorage;
    }

    @GetMapping({"/", "/login", "/home", "/product/{id}", "/cart", "/orders", "/user", "/shop/{id}", "/merchant/products",
            "/merchant/publish", "/admin/audit", "/admin/users"})
    /*
     * 转发前端单页应用路由。
     */
    public String index() {
        return "forward:/index.html";
    }

    @ResponseBody
    @GetMapping("/api/captcha")
    /*
     * 生成登录验证码并保存到会话。
     */
    public Map<String, Object> captcha(HttpSession session) {
        String code = randomCaptcha();
        session.setAttribute("LOGIN_CAPTCHA", code);
        return service.captcha(code);
    }

    @ResponseBody
    @PostMapping("/api/login")
    /*
     * 处理用户登录。
     * 登录成功后记录用户会话。
     */
    public Object login(@RequestBody LoginRequest request, HttpSession session) {
        String expectedCaptcha = (String) session.getAttribute("LOGIN_CAPTCHA");
        User user = service.login(request, expectedCaptcha);
        session.setAttribute("LOGIN_USER_ID", user.id);
        session.removeAttribute("LOGIN_CAPTCHA");
        return user;
    }

    @ResponseBody
    @GetMapping("/api/session")
    /*
     * 获取当前登录用户信息。
     */
    public Object session(HttpSession session) {
        return requireUser(session);
    }

    @ResponseBody
    @PostMapping("/api/logout")
    /*
     * 清空当前登录会话。
     */
    public Object logout(HttpSession session) {
        session.invalidate();
        return Map.of("ok", true);
    }

    @ResponseBody
    @PostMapping("/api/register")
    /*
     * 处理买家或商家注册。
     * 商家注册同时接收证件图片。
     */
    public Object register(RegisterRequest request,
                           @RequestParam(required = false) MultipartFile license,
                           @RequestParam(required = false) MultipartFile idCard) {
        return service.register(request, fileStorage.save(license), fileStorage.save(idCard));
    }

    @ResponseBody
    @PostMapping("/api/uploads")
    /*
     * 上传商品或证件图片。
     * 仅商家可用。
     */
    public Object upload(@RequestParam("files") List<MultipartFile> files, HttpSession session) {
        requireRole(session, Role.MERCHANT);
        return fileStorage.saveAll(files);
    }

    @ResponseBody
    @GetMapping("/api/products")
    /*
     * 查询商品列表。
     * 支持关键词、价格区间和排序条件。
     */
    public Object products(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String sort,
                           @RequestParam(required = false) BigDecimal minPrice,
                           @RequestParam(required = false) BigDecimal maxPrice,
                           HttpSession session) {
        requireUser(session);
        return service.searchProducts(keyword, sort, minPrice, maxPrice);
    }

    @ResponseBody
    @GetMapping("/api/products/{id}")
    /*
     * 查询商品详情。
     */
    public Object product(@PathVariable long id, HttpSession session) {
        requireUser(session);
        return service.product(id);
    }

    @ResponseBody
    @GetMapping("/api/shops/{merchantId}/products")
    /*
     * 查询指定商家的商品列表。
     */
    public Object shopProducts(@PathVariable long merchantId, HttpSession session) {
        requireUser(session);
        return service.shopProducts(merchantId);
    }

    @ResponseBody
    @GetMapping("/api/users/{userId}")
    /*
     * 查询用户信息。
     * 仅本人或管理员可访问。
     */
    public Object user(@PathVariable long userId, HttpSession session) {
        requireSelfOrAdmin(session, userId);
        return service.user(userId);
    }

    @ResponseBody
    @GetMapping("/api/users/{userId}/orders")
    /*
     * 查询用户订单列表。
     */
    public Object userOrders(@PathVariable long userId, HttpSession session) {
        requireSelfOrAdmin(session, userId);
        return service.userOrders(userId);
    }

    @ResponseBody
    @GetMapping("/api/users/{userId}/cart")
    /*
     * 查询用户购物车。
     */
    public Object cart(@PathVariable long userId, HttpSession session) {
        requireSelfOrAdmin(session, userId);
        return service.cart(userId);
    }

    @ResponseBody
    @PostMapping("/api/users/{userId}/cart/{productId}")
    /*
     * 添加商品到购物车。
     */
    public Object addToCart(@PathVariable long userId, @PathVariable long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpSession session) {
        requireSelfOrAdmin(session, userId);
        return service.addToCart(userId, productId, quantity);
    }

    @ResponseBody
    @PutMapping("/api/users/{userId}/cart")
    /*
     * 更新购物车商品数量和选中状态。
     */
    public Object updateCart(@PathVariable long userId, @RequestBody List<CartItem> items, HttpSession session) {
        requireSelfOrAdmin(session, userId);
        return service.updateCart(userId, items);
    }

    @ResponseBody
    @PostMapping("/api/users/{userId}/checkout")
    /*
     * 提交购物车结算。
     * 支持积分抵扣。
     */
    public Object checkout(@PathVariable long userId, @RequestParam(defaultValue = "0") int pointsUsed, HttpSession session) {
        requireSelfOrAdmin(session, userId);
        return service.checkout(userId, pointsUsed);
    }

    @ResponseBody
    @PostMapping("/api/orders/{orderId}/received")
    /*
     * 确认订单收货。
     */
    public Object received(@PathVariable long orderId, HttpSession session) {
        requireUser(session);
        return service.markReceived(orderId);
    }

    @ResponseBody
    @PostMapping("/api/orders/{orderId}/return")
    /*
     * 提交退货申请。
     */
    public Object requestReturn(@PathVariable long orderId,
                                @RequestParam(defaultValue = "24小时内退货申请") String reason,
                                HttpSession session) {
        requireUser(session);
        return service.requestReturn(orderId, reason);
    }

    @ResponseBody
    @PostMapping("/api/users/{userId}/reviews")
    /*
     * 提交订单评价。
     */
    public Object review(@PathVariable long userId, @RequestBody ReviewRequest request, HttpSession session) {
        requireSelfOrAdmin(session, userId);
        return service.review(userId, request);
    }

    @ResponseBody
    @PostMapping("/api/merchants/{merchantId}/products")
    /*
     * 商家发布或编辑商品。
     */
    public Object saveProduct(@PathVariable long merchantId, @RequestBody ProductRequest request, HttpSession session) {
        requireSelfOrAdmin(session, merchantId);
        requireRole(session, Role.MERCHANT);
        return service.saveProduct(merchantId, request);
    }

    @ResponseBody
    @PostMapping("/api/products/{productId}/status")
    /*
     * 调整商品状态。
     * 管理员和商家可操作。
     */
    public Object productStatus(@PathVariable long productId, @RequestParam ProductStatus status, HttpSession session) {
        User user = requireUser(session);
        if (user.role != Role.ADMIN && user.role != Role.MERCHANT) {
            throw new SecurityException("无权调整商品状态");
        }
        return service.setProductStatus(productId, status);
    }

    @ResponseBody
    @GetMapping("/api/admin/users")
    /*
     * 管理员查询全部用户。
     */
    public Object adminUsers(HttpSession session) {
        requireRole(session, Role.ADMIN);
        return service.users();
    }

    @ResponseBody
    @GetMapping("/api/admin/pending-merchants")
    /*
     * 管理员查询待审核商家。
     */
    public Object pendingMerchants(HttpSession session) {
        requireRole(session, Role.ADMIN);
        return service.pendingMerchants();
    }

    @ResponseBody
    @GetMapping("/api/admin/products")
    /*
     * 管理员按状态查询商品。
     */
    public Object adminProducts(@RequestParam(required = false) ProductStatus status, HttpSession session) {
        requireRole(session, Role.ADMIN);
        return service.productsByStatus(status);
    }

    @ResponseBody
    @PostMapping("/api/admin/users/{userId}/approve")
    /*
     * 管理员批准用户审核。
     */
    public Object approveUser(@PathVariable long userId, HttpSession session) {
        requireRole(session, Role.ADMIN);
        return service.approveUser(userId);
    }

    @ResponseBody
    @PostMapping("/api/admin/recharge")
    /*
     * 管理员为用户充值。
     */
    public Object recharge(@RequestBody RechargeRequest request, HttpSession session) {
        requireRole(session, Role.ADMIN);
        return service.recharge(request);
    }

    @ResponseBody
    @PostMapping("/api/admin/punish")
    /*
     * 管理员处罚或恢复用户状态。
     */
    public Object punish(@RequestBody PunishRequest request, HttpSession session) {
        requireRole(session, Role.ADMIN);
        return service.punish(request);
    }

    @ResponseBody
    @PostMapping("/api/admin/fee")
    /*
     * 管理员设置商家等级和手续费率。
     */
    public Object fee(@RequestBody FeeRequest request, HttpSession session) {
        requireRole(session, Role.ADMIN);
        return service.setFee(request);
    }

    @ResponseBody
    @ExceptionHandler(SecurityException.class)
    /*
     * 处理登录和权限异常。
     */
    public ResponseEntity<Map<String, String>> unauthorized(SecurityException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", exception.getMessage()));
    }

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    /*
     * 处理业务参数异常。
     */
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    /*
     * 校验用户是否已登录。
     * 限制发布用户登录后不能继续操作。
     */
    private User requireUser(HttpSession session) {
        Object userId = session.getAttribute("LOGIN_USER_ID");
        if (!(userId instanceof Long id)) {
            throw new SecurityException("请先登录");
        }
        User user = service.user(id);
        if (user.status == UserStatus.LIMITED) {
            throw new SecurityException("账号已被限制，无法进行操作");
        }
        return user;
    }

    /*
     * 校验是否为本人或管理员。
     */
    private User requireSelfOrAdmin(HttpSession session, long userId) {
        User user = requireUser(session);
        if (user.id != userId && user.role != Role.ADMIN) {
            throw new SecurityException("无权访问该用户数据");
        }
        return user;
    }

    /*
     * 校验当前用户是否具备指定角色。
     */
    private User requireRole(HttpSession session, Role role) {
        User user = requireUser(session);
        if (user.role != role) {
            throw new SecurityException("无权访问该功能");
        }
        return user;
    }

    /*
     * 生成四位随机验证码。
     */
    private String randomCaptcha() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder(4);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}
