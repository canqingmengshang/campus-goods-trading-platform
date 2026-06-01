package org.example.campusgoodstradingplatform.service;

import org.example.campusgoodstradingplatform.dto.FeeRequest;
import org.example.campusgoodstradingplatform.dto.LoginRequest;
import org.example.campusgoodstradingplatform.dto.ProductRequest;
import org.example.campusgoodstradingplatform.dto.PunishRequest;
import org.example.campusgoodstradingplatform.dto.RechargeRequest;
import org.example.campusgoodstradingplatform.dto.RegisterRequest;
import org.example.campusgoodstradingplatform.dto.ReviewRequest;
import org.example.campusgoodstradingplatform.entity.CartItem;
import org.example.campusgoodstradingplatform.entity.Order;
import org.example.campusgoodstradingplatform.entity.OrderStatus;
import org.example.campusgoodstradingplatform.entity.Product;
import org.example.campusgoodstradingplatform.entity.ProductStatus;
import org.example.campusgoodstradingplatform.entity.Review;
import org.example.campusgoodstradingplatform.entity.Role;
import org.example.campusgoodstradingplatform.entity.User;
import org.example.campusgoodstradingplatform.entity.UserStatus;
import org.example.campusgoodstradingplatform.mapper.MarketplaceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MarketplaceService {
    private final MarketplaceMapper marketplaceMapper;

    public MarketplaceService(MarketplaceMapper marketplaceMapper) {
        this.marketplaceMapper = marketplaceMapper;
    }

    public Map<String, Object> captcha(String code) {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='128' height='44'>"
                + "<rect width='128' height='44' rx='8' fill='#eef6ff'/>"
                + "<path d='M8 34 C28 4, 52 54, 76 15 S110 12,120 32' stroke='#36a3ff' fill='none' stroke-width='3'/>"
                + "<text x='22' y='30' font-size='24' font-family='Arial' font-weight='700' fill='#22324d'>"
                + code + "</text></svg>";
        String image = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return Map.of("image", image);
    }

    /*
     * 验证登录验证码。
     * 查询账号密码。
     * 判断用户状态是否允许登录。
     */
    public User login(LoginRequest request, String expectedCaptcha) {
        if (expectedCaptcha == null || request.captcha() == null
                || request.captcha().isBlank()
                || !request.captcha().trim().equalsIgnoreCase(expectedCaptcha)) {
            throw new IllegalArgumentException("图形验证码错误");
        }
        User user = marketplaceMapper.findUserByCredentials(request.username(), request.password());
        if (user == null) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.status == UserStatus.PENDING) {
            throw new IllegalArgumentException("账号正在等待管理员审核，审核通过后方可登录");
        }
        if (user.status != UserStatus.ACTIVE && user.status != UserStatus.LIMITED) {
            throw new IllegalArgumentException("账号当前状态不可登录：" + user.status);
        }
        return user;
    }

    /*
     * 校验注册信息。
     * 检查用户名是否重复。
     * 根据用户角色设置初始审核状态。
     */
    public User register(RegisterRequest request, String licenseImage, String idCardImage) {
        validateRegister(request, licenseImage, idCardImage);
        if (marketplaceMapper.usernameExists(request.username())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Role role = request.role() == null ? Role.BUYER : request.role();
        UserStatus status = role == Role.ADMIN ? UserStatus.ACTIVE : UserStatus.PENDING;
        long id = marketplaceMapper.insertUser(request, role, status, licenseImage, idCardImage, 3, feeRateOf(3));
        return user(id);
    }

    /*
     * 按关键词搜索已发布商品。
     * 按价格区间筛选。
     * 按价格、销量、好评率等规则排序。
     */
    public List<Product> searchProducts(String keyword, String sort, BigDecimal minPrice, BigDecimal maxPrice) {
        String orderBy = switch (sort == null ? "" : sort) {
            case "price" -> "p.sale_price ASC";
            case "sales" -> "p.sales DESC";
            case "rate" -> "p.favorable_rate DESC";
            default -> "p.id DESC";
        };
        String word = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        BigDecimal min = minPrice == null ? BigDecimal.ZERO : minPrice;
        BigDecimal max = maxPrice == null ? new BigDecimal("99999999") : maxPrice;
        return marketplaceMapper.searchPublishedProducts(word, min, max, orderBy);
    }

    public Product product(long id) {
        return marketplaceMapper.product(id);
    }

    public List<Product> shopProducts(long merchantId) {
        return marketplaceMapper.shopProducts(merchantId);
    }

    public List<CartItem> cart(long userId) {
        return marketplaceMapper.cart(userId);
    }

    /*
     * 校验商品是否存在。
     * 将商品加入购物车。
     * 已存在时累加购买数量。
     */
    public List<CartItem> addToCart(long userId, long productId, int quantity) {
        product(productId);
        marketplaceMapper.upsertCartItem(userId, productId, Math.max(1, quantity));
        return cart(userId);
    }

    /*
     * 清空用户原购物车记录。
     * 按前端提交内容重新保存购物车。
     * 保证购物车数量最小为1。
     */
    @Transactional
    public List<CartItem> updateCart(long userId, List<CartItem> items) {
        marketplaceMapper.clearCart(userId);
        if (items != null) {
            for (CartItem item : items) {
                item.quantity = Math.max(1, item.quantity);
                marketplaceMapper.insertCartItem(userId, item);
            }
        }
        return cart(userId);
    }

    /*
     * 生成订单并完成结算。
     * 检查库存、积分和钱包余额。
     * 扣减库存、销量、钱包余额和积分。
     */
    @Transactional
    public Order checkout(long userId, int pointsUsed) {
        User user = user(userId);
        List<CartItem> selected = cart(userId).stream().filter(item -> item.selected && item.quantity > 0).toList();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("请先选择购物车商品");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : selected) {
            Product product = product(item.productId);
            if (product.stock < item.quantity) {
                throw new IllegalArgumentException(product.name + "库存不足");
            }
            total = total.add(product.salePrice.multiply(BigDecimal.valueOf(item.quantity)));
        }
        int usablePoints = Math.min(Math.max(pointsUsed, 0), Math.min(user.points, total.multiply(BigDecimal.valueOf(100)).intValue()));
        BigDecimal payable = total.subtract(BigDecimal.valueOf(usablePoints).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        if (user.wallet.compareTo(payable) < 0) {
            throw new IllegalArgumentException("钱包余额不足");
        }
        long orderId = marketplaceMapper.insertOrder(userId, payable, usablePoints);
        for (CartItem item : selected) {
            Product product = product(item.productId);
            marketplaceMapper.insertOrderItem(orderId, item, product.salePrice);
            marketplaceMapper.decreaseProductStockAndIncreaseSales(item.productId, item.quantity);
        }
        int earnedPoints = payable.setScale(0, RoundingMode.DOWN).intValue();
        marketplaceMapper.updateUserWalletAndPoints(userId, payable, usablePoints, earnedPoints);
        marketplaceMapper.deleteSelectedCartItems(userId);
        return marketplaceMapper.order(orderId);
    }

    public List<Order> userOrders(long userId) {
        return marketplaceMapper.userOrders(userId);
    }

    /*
     * 将订单状态更新为已收货。
     * 记录收货时间。
     */
    public Order markReceived(long orderId) {
        marketplaceMapper.updateOrderReceived(orderId);
        return marketplaceMapper.order(orderId);
    }

    /*
     * 校验订单是否在收货后24小时内。
     * 提交退货申请原因。
     * 将订单状态更新为退货申请中。
     */
    public Order requestReturn(long orderId, String reason) {
        Order order = marketplaceMapper.order(orderId);
        LocalDateTime receivedAt = order.receivedAt == null ? LocalDateTime.now() : order.receivedAt;
        if (Duration.between(receivedAt, LocalDateTime.now()).toHours() > 24) {
            throw new IllegalArgumentException("退货申请已超过24小时");
        }
        marketplaceMapper.updateOrderReturnRequested(orderId, reason);
        return marketplaceMapper.order(orderId);
    }

    /*
     * 保存用户评价。
     * 关联订单、商家和商品。
     * 重新计算商家和商品好评率。
     */
    public Review review(long userId, ReviewRequest request) {
        int stars = Math.max(1, Math.min(5, request.stars()));
        long id = marketplaceMapper.insertReview(userId, request.orderId(), request.merchantId(), request.productId(), stars, request.content());
        marketplaceMapper.recalculateMerchantRate(request.merchantId());
        if (request.productId() != null) {
            marketplaceMapper.recalculateProductRate(request.productId());
        }
        return marketplaceMapper.review(id);
    }

    /*
     * 校验商家发布权限。
     * 新增或编辑商品信息。
     * 将商品提交到待审核状态。
     */
    public Product saveProduct(long merchantId, ProductRequest request) {
        User merchant = user(merchantId);
        if (merchant.status == UserStatus.LIMITED || merchant.status == UserStatus.BLACKLISTED) {
            throw new IllegalArgumentException("当前商家被限制发布");
        }
        if (request.id() == null) {
            long id = marketplaceMapper.insertProduct(merchantId, request);
            return product(id);
        }
        marketplaceMapper.updateProduct(merchantId, request);
        return product(request.id());
    }

    /*
     * 调整商品状态。
     * 支持审核上架、下架、锁定等状态流转。
     */
    public Product setProductStatus(long productId, ProductStatus status) {
        marketplaceMapper.updateProductStatus(productId, status);
        return product(productId);
    }

    public List<User> users() {
        return marketplaceMapper.users();
    }

    public List<User> pendingMerchants() {
        return marketplaceMapper.pendingMerchants();
    }

    public List<Product> productsByStatus(ProductStatus status) {
        return marketplaceMapper.productsByStatus(status);
    }

    /*
     * 管理员审核用户。
     * 将待审核用户状态改为正常可用。
     */
    public User approveUser(long userId) {
        marketplaceMapper.updateUserStatus(userId, UserStatus.ACTIVE);
        return user(userId);
    }

    /*
     * 管理员为用户充值。
     * 增加用户钱包余额。
     */
    public User recharge(RechargeRequest request) {
        marketplaceMapper.addWallet(request.userId(), nvl(request.amount(), BigDecimal.ZERO));
        return user(request.userId());
    }

    /*
     * 调整用户处罚状态。
     * 限制或拉黑商家时同步锁定其商品。
     */
    public User punish(PunishRequest request) {
        marketplaceMapper.updateUserStatus(request.userId(), request.status());
        if (request.status() == UserStatus.LIMITED || request.status() == UserStatus.BLACKLISTED) {
            marketplaceMapper.lockProductsByMerchant(request.userId());
        }
        return user(request.userId());
    }

    /*
     * 设置商家等级。
     * 根据等级更新商家手续费率。
     */
    public User setFee(FeeRequest request) {
        int level = Math.max(1, Math.min(5, request.level()));
        marketplaceMapper.updateMerchantFee(request.merchantId(), level, feeRateOf(level));
        return user(request.merchantId());
    }

    public User user(long id) {
        return marketplaceMapper.user(id);
    }

    private void validateRegister(RegisterRequest request, String licenseImage, String idCardImage) {
        Role role = request.role() == null ? Role.BUYER : request.role();
        require(request.username(), "登录账号不能为空");
        require(request.password(), "密码不能为空");
        require(request.realName(), "姓名不能为空");
        require(request.phone(), "手机号不能为空");
        if (!request.phone().matches("1\\d{10}")) {
            throw new IllegalArgumentException("手机号必须为11位数字且以1开头");
        }
        require(request.gender(), "性别不能为空");
        require(request.bankAccount(), "银行账号不能为空");
        if (!request.bankAccount().matches("\\d{16}")) {
            throw new IllegalArgumentException("银行账号必须为16位数字");
        }
        if (role == Role.BUYER) {
            require(request.email(), "邮箱不能为空");
            require(request.city(), "城市不能为空");
        }
        if (role == Role.MERCHANT) {
            require(request.shopName(), "店铺名不能为空");
            require(licenseImage, "商家注册必须上传营业执照");
            require(idCardImage, "商家注册必须上传身份证图片");
        }
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private BigDecimal nvl(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal feeRateOf(int level) {
        return switch (level) {
            case 1 -> new BigDecimal("0.10");
            case 2 -> new BigDecimal("0.30");
            case 3 -> new BigDecimal("0.50");
            case 4 -> new BigDecimal("0.80");
            default -> new BigDecimal("1.00");
        };
    }
}
