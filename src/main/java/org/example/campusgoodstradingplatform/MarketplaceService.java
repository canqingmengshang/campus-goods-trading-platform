package org.example.campusgoodstradingplatform;

import org.example.campusgoodstradingplatform.CampusStoreData.CartItem;
import org.example.campusgoodstradingplatform.CampusStoreData.FeeRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.LoginRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.Order;
import org.example.campusgoodstradingplatform.CampusStoreData.OrderStatus;
import org.example.campusgoodstradingplatform.CampusStoreData.Product;
import org.example.campusgoodstradingplatform.CampusStoreData.ProductRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.ProductStatus;
import org.example.campusgoodstradingplatform.CampusStoreData.PunishRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.RechargeRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.RegisterRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.Review;
import org.example.campusgoodstradingplatform.CampusStoreData.ReviewRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.Role;
import org.example.campusgoodstradingplatform.CampusStoreData.User;
import org.example.campusgoodstradingplatform.CampusStoreData.UserStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class MarketplaceService {
    private final JdbcTemplate jdbc;

    public MarketplaceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<User> userMapper = (rs, rowNum) -> {
        User user = new User();
        user.id = rs.getLong("id");
        user.username = rs.getString("username");
        user.password = rs.getString("password");
        user.realName = rs.getString("real_name");
        user.phone = rs.getString("phone");
        user.email = rs.getString("email");
        user.city = rs.getString("city");
        user.gender = rs.getString("gender");
        user.bankAccount = rs.getString("bank_account");
        user.role = Role.valueOf(rs.getString("role"));
        user.status = UserStatus.valueOf(rs.getString("status"));
        user.wallet = rs.getBigDecimal("wallet");
        user.points = rs.getInt("points");
        user.shopName = rs.getString("shop_name");
        user.licenseImage = rs.getString("license_image");
        user.idCardImage = rs.getString("id_card_image");
        user.merchantLevel = rs.getInt("merchant_level");
        user.feeRate = rs.getBigDecimal("fee_rate").doubleValue();
        user.favorableRate = rs.getBigDecimal("favorable_rate").doubleValue();
        user.reviews = reviewsByMerchant(user.id);
        return user;
    };

    private final RowMapper<Product> productMapper = (rs, rowNum) -> {
        Product product = new Product();
        product.id = rs.getLong("id");
        product.merchantId = rs.getLong("merchant_id");
        product.merchantName = rs.getString("merchant_name");
        product.name = rs.getString("name");
        product.category = rs.getString("category");
        product.originalPrice = rs.getBigDecimal("original_price");
        product.salePrice = rs.getBigDecimal("sale_price");
        product.size = rs.getString("size");
        product.photos = splitPhotos(rs.getString("photos"));
        product.usageGuide = rs.getString("usage_guide");
        product.negotiable = rs.getBoolean("negotiable");
        product.stock = rs.getInt("stock");
        product.sales = rs.getInt("sales");
        product.condition = rs.getString("condition_text");
        product.status = ProductStatus.valueOf(rs.getString("status"));
        product.favorableRate = rs.getBigDecimal("favorable_rate").doubleValue();
        product.reviews = reviewsByProduct(product.id);
        return product;
    };

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

    public User login(LoginRequest request, String expectedCaptcha) {
        if (expectedCaptcha == null || request.captcha() == null
                || request.captcha().isBlank()
                || !request.captcha().trim().equalsIgnoreCase(expectedCaptcha)) {
            throw new IllegalArgumentException("图形验证码错误");
        }
        try {
            User user = jdbc.queryForObject("SELECT * FROM users WHERE username=? AND password=?", userMapper, request.username(), request.password());
            if (user.status == UserStatus.PENDING) {
                throw new IllegalArgumentException("账号正在等待管理员审核，审核通过后方可登录");
            }
            if (user.status != UserStatus.ACTIVE) {
                throw new IllegalArgumentException("账号当前状态不可登录：" + user.status);
            }
            return user;
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
    }

    public User register(RegisterRequest request, String licenseImage, String idCardImage) {
        validateRegister(request, licenseImage, idCardImage);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username=?", Integer.class, request.username());
        if (count != null && count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Role role = request.role() == null ? Role.BUYER : request.role();
        UserStatus status = role == Role.ADMIN ? UserStatus.ACTIVE : UserStatus.PENDING;
        long id = insert("""
                INSERT INTO users(username,password,real_name,phone,email,city,gender,bank_account,role,status,wallet,points,shop_name,license_image,id_card_image,merchant_level,fee_rate,favorable_rate)
                VALUES(?,?,?,?,?,?,?,?,?,?,500,1200,?,?,?,?,?,100)
                """, ps -> {
            ps.setString(1, request.username());
            ps.setString(2, request.password());
            ps.setString(3, request.realName());
            ps.setString(4, request.phone());
            ps.setString(5, request.email());
            ps.setString(6, request.city());
            ps.setString(7, request.gender());
            ps.setString(8, request.bankAccount());
            ps.setString(9, role.name());
            ps.setString(10, status.name());
            ps.setString(11, request.shopName());
            ps.setString(12, licenseImage);
            ps.setString(13, idCardImage);
            ps.setInt(14, 3);
            ps.setBigDecimal(15, feeRateOf(3));
        });
        return user(id);
    }

    public List<Product> searchProducts(String keyword, String sort, BigDecimal minPrice, BigDecimal maxPrice) {
        String orderBy = switch (sort == null ? "" : sort) {
            case "price" -> "p.sale_price ASC";
            case "sales" -> "p.sales DESC";
            case "rate" -> "p.favorable_rate DESC";
            default -> "p.id DESC";
        };
        String word = "%" + (keyword == null ? "" : keyword.toLowerCase(Locale.ROOT)) + "%";
        BigDecimal min = minPrice == null ? BigDecimal.ZERO : minPrice;
        BigDecimal max = maxPrice == null ? new BigDecimal("99999999") : maxPrice;
        return jdbc.query(productSql("WHERE p.status='PUBLISHED' AND LOWER(p.name) LIKE ? AND p.sale_price BETWEEN ? AND ? ORDER BY " + orderBy),
                productMapper, word, min, max);
    }

    public Product product(long id) {
        try {
            return jdbc.queryForObject(productSql("WHERE p.id=?"), productMapper, id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("商品不存在");
        }
    }

    public List<Product> shopProducts(long merchantId) {
        return jdbc.query(productSql("WHERE p.merchant_id=? ORDER BY p.id DESC"), productMapper, merchantId);
    }

    public List<CartItem> cart(long userId) {
        return jdbc.query("SELECT * FROM cart_items WHERE user_id=?",
                (rs, rowNum) -> {
                    CartItem item = new CartItem();
                    item.productId = rs.getLong("product_id");
                    item.quantity = rs.getInt("quantity");
                    item.selected = rs.getBoolean("selected");
                    return item;
                }, userId);
    }

    public List<CartItem> addToCart(long userId, long productId, int quantity) {
        product(productId);
        jdbc.update("""
                INSERT INTO cart_items(user_id,product_id,quantity,selected)
                VALUES(?,?,?,1)
                ON DUPLICATE KEY UPDATE quantity=quantity+VALUES(quantity), selected=1
                """, userId, productId, Math.max(1, quantity));
        return cart(userId);
    }

    @Transactional
    public List<CartItem> updateCart(long userId, List<CartItem> items) {
        jdbc.update("DELETE FROM cart_items WHERE user_id=?", userId);
        if (items != null) {
            for (CartItem item : items) {
                jdbc.update("INSERT INTO cart_items(user_id,product_id,quantity,selected) VALUES(?,?,?,?)",
                        userId, item.productId, Math.max(1, item.quantity), item.selected);
            }
        }
        return cart(userId);
    }

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
        long orderId = insert("INSERT INTO orders(buyer_id,total_amount,points_used,status,paid_at) VALUES(?,?,?,?,NOW())", ps -> {
            ps.setLong(1, userId);
            ps.setBigDecimal(2, payable);
            ps.setInt(3, usablePoints);
            ps.setString(4, OrderStatus.PAID.name());
        });
        for (CartItem item : selected) {
            Product product = product(item.productId);
            jdbc.update("INSERT INTO order_items(order_id,product_id,quantity,unit_price) VALUES(?,?,?,?)",
                    orderId, item.productId, item.quantity, product.salePrice);
            jdbc.update("UPDATE products SET stock=stock-?, sales=sales+? WHERE id=?", item.quantity, item.quantity, item.productId);
        }
        jdbc.update("UPDATE users SET wallet=wallet-?, points=points-? WHERE id=?", payable, usablePoints, userId);
        jdbc.update("DELETE FROM cart_items WHERE user_id=? AND selected=1", userId);
        return order(orderId);
    }

    public List<Order> userOrders(long userId) {
        return jdbc.query("SELECT * FROM orders WHERE buyer_id=? ORDER BY id DESC", orderMapper(), userId);
    }

    public Order markReceived(long orderId) {
        jdbc.update("UPDATE orders SET status=?, received_at=NOW() WHERE id=?", OrderStatus.RECEIVED.name(), orderId);
        return order(orderId);
    }

    public Order requestReturn(long orderId, String reason) {
        Order order = order(orderId);
        LocalDateTime receivedAt = order.receivedAt == null ? LocalDateTime.now() : order.receivedAt;
        if (Duration.between(receivedAt, LocalDateTime.now()).toHours() > 24) {
            throw new IllegalArgumentException("退货申请已超过24小时");
        }
        jdbc.update("UPDATE orders SET status=?, return_reason=? WHERE id=?", OrderStatus.RETURN_REQUESTED.name(), reason, orderId);
        return order(orderId);
    }

    public Review review(long userId, ReviewRequest request) {
        long id = insert("INSERT INTO reviews(order_id,user_id,merchant_id,product_id,stars,content) VALUES(?,?,?,?,?,?)", ps -> {
            ps.setLong(1, request.orderId());
            ps.setLong(2, userId);
            ps.setLong(3, request.merchantId());
            if (request.productId() == null) ps.setObject(4, null); else ps.setLong(4, request.productId());
            ps.setInt(5, Math.max(1, Math.min(5, request.stars())));
            ps.setString(6, request.content());
        });
        recalculateRates(request.merchantId(), request.productId());
        return review(id);
    }

    public Product saveProduct(long merchantId, ProductRequest request) {
        User merchant = user(merchantId);
        if (merchant.status == UserStatus.LIMITED || merchant.status == UserStatus.BLACKLISTED) {
            throw new IllegalArgumentException("当前商家被限制发布");
        }
        if (request.id() == null) {
            long id = insert("""
                    INSERT INTO products(merchant_id,name,category,original_price,sale_price,size,photos,usage_guide,negotiable,stock,condition_text,status,favorable_rate)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,'AUDITING',100)
                    """, ps -> fillProduct(ps, merchantId, request, 1));
            return product(id);
        }
        jdbc.update("""
                UPDATE products SET name=?,category=?,original_price=?,sale_price=?,size=?,photos=?,usage_guide=?,negotiable=?,stock=?,condition_text=?,status='AUDITING'
                WHERE id=? AND merchant_id=?
                """, request.name(), request.category(), nvl(request.originalPrice(), BigDecimal.ZERO),
                nvl(request.salePrice(), request.originalPrice()), request.size(), joinPhotos(request.photos()),
                request.usageGuide(), request.negotiable(), request.stock(), request.condition(), request.id(), merchantId);
        return product(request.id());
    }

    public Product setProductStatus(long productId, ProductStatus status) {
        jdbc.update("UPDATE products SET status=? WHERE id=?", status.name(), productId);
        return product(productId);
    }

    public List<User> users() {
        return jdbc.query("SELECT * FROM users ORDER BY id", userMapper);
    }

    public List<User> pendingMerchants() {
        return jdbc.query("SELECT * FROM users WHERE status='PENDING' ORDER BY id", userMapper);
    }

    public List<Product> productsByStatus(ProductStatus status) {
        if (status == null) {
            return jdbc.query(productSql("ORDER BY p.id DESC"), productMapper);
        }
        return jdbc.query(productSql("WHERE p.status=? ORDER BY p.id DESC"), productMapper, status.name());
    }

    public User approveUser(long userId) {
        jdbc.update("UPDATE users SET status='ACTIVE' WHERE id=?", userId);
        return user(userId);
    }

    public User recharge(RechargeRequest request) {
        jdbc.update("UPDATE users SET wallet=wallet+? WHERE id=?", nvl(request.amount(), BigDecimal.ZERO), request.userId());
        return user(request.userId());
    }

    public User punish(PunishRequest request) {
        jdbc.update("UPDATE users SET status=? WHERE id=?", request.status().name(), request.userId());
        if (request.status() == UserStatus.LIMITED || request.status() == UserStatus.BLACKLISTED) {
            jdbc.update("UPDATE products SET status='LOCKED' WHERE merchant_id=?", request.userId());
        }
        return user(request.userId());
    }

    public User setFee(FeeRequest request) {
        int level = Math.max(1, Math.min(5, request.level()));
        jdbc.update("UPDATE users SET merchant_level=?, fee_rate=? WHERE id=?", level, feeRateOf(level), request.merchantId());
        return user(request.merchantId());
    }

    public User user(long id) {
        try {
            return jdbc.queryForObject("SELECT * FROM users WHERE id=?", userMapper, id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    private Order order(long id) {
        try {
            return jdbc.queryForObject("SELECT * FROM orders WHERE id=?", orderMapper(), id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("订单不存在");
        }
    }

    private RowMapper<Order> orderMapper() {
        return (rs, rowNum) -> {
            Order order = new Order();
            order.id = rs.getLong("id");
            order.buyerId = rs.getLong("buyer_id");
            order.totalAmount = rs.getBigDecimal("total_amount");
            order.pointsUsed = rs.getInt("points_used");
            order.status = OrderStatus.valueOf(rs.getString("status"));
            order.paidAt = toLocalDateTime(rs.getTimestamp("paid_at"));
            order.receivedAt = toLocalDateTime(rs.getTimestamp("received_at"));
            order.returnReason = rs.getString("return_reason");
            order.items = jdbc.query("SELECT product_id,quantity,1 selected FROM order_items WHERE order_id=?",
                    (itemRs, itemRow) -> {
                        CartItem item = new CartItem();
                        item.productId = itemRs.getLong("product_id");
                        item.quantity = itemRs.getInt("quantity");
                        item.selected = true;
                        return item;
                    }, order.id);
            order.reviews = reviewsByOrder(order.id);
            return order;
        };
    }

    private Review review(long id) {
        return jdbc.queryForObject("SELECT * FROM reviews WHERE id=?", reviewMapper(), id);
    }

    private List<Review> reviewsByOrder(long orderId) {
        return jdbc.query("SELECT * FROM reviews WHERE order_id=? ORDER BY id DESC", reviewMapper(), orderId);
    }

    private List<Review> reviewsByMerchant(long merchantId) {
        return jdbc.query("SELECT * FROM reviews WHERE merchant_id=? ORDER BY id DESC", reviewMapper(), merchantId);
    }

    private List<Review> reviewsByProduct(long productId) {
        return jdbc.query("SELECT * FROM reviews WHERE product_id=? ORDER BY id DESC", reviewMapper(), productId);
    }

    private RowMapper<Review> reviewMapper() {
        return (rs, rowNum) -> {
            Review review = new Review();
            review.id = rs.getLong("id");
            review.userId = rs.getLong("user_id");
            review.merchantId = rs.getLong("merchant_id");
            long productId = rs.getLong("product_id");
            review.productId = rs.wasNull() ? null : productId;
            review.stars = rs.getInt("stars");
            review.content = rs.getString("content");
            review.createdAt = toLocalDateTime(rs.getTimestamp("created_at"));
            return review;
        };
    }

    private String productSql(String suffix) {
        return """
                SELECT p.*, u.shop_name merchant_name
                FROM products p
                JOIN users u ON u.id=p.merchant_id
                """ + " " + suffix;
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

    private void recalculateRates(long merchantId, Long productId) {
        jdbc.update("""
                UPDATE users SET favorable_rate=COALESCE((
                    SELECT ROUND(SUM(CASE WHEN stars>=4 THEN 1 ELSE 0 END) * 100 / NULLIF(COUNT(*),0), 2)
                    FROM reviews WHERE merchant_id=?
                ),100) WHERE id=?
                """, merchantId, merchantId);
        if (productId != null) {
            jdbc.update("""
                    UPDATE products SET favorable_rate=COALESCE((
                        SELECT ROUND(SUM(CASE WHEN stars>=4 THEN 1 ELSE 0 END) * 100 / NULLIF(COUNT(*),0), 2)
                        FROM reviews WHERE product_id=?
                    ),100) WHERE id=?
                    """, productId, productId);
        }
    }

    private long insert(String sql, StatementFiller filler) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            filler.fill(ps);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    private void fillProduct(PreparedStatement ps, long merchantId, ProductRequest request, int offset) throws java.sql.SQLException {
        ps.setLong(offset, merchantId);
        ps.setString(offset + 1, request.name());
        ps.setString(offset + 2, request.category());
        ps.setBigDecimal(offset + 3, nvl(request.originalPrice(), BigDecimal.ZERO));
        ps.setBigDecimal(offset + 4, nvl(request.salePrice(), request.originalPrice()));
        ps.setString(offset + 5, request.size());
        ps.setString(offset + 6, joinPhotos(request.photos()));
        ps.setString(offset + 7, request.usageGuide());
        ps.setBoolean(offset + 8, request.negotiable());
        ps.setInt(offset + 9, request.stock());
        ps.setString(offset + 10, request.condition());
    }

    private List<String> splitPhotos(String photos) {
        if (photos == null || photos.isBlank()) {
            return List.of("/images/product-default.svg");
        }
        return Arrays.stream(photos.split("\\|")).filter(value -> !value.isBlank()).toList();
    }

    private String joinPhotos(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            return "/images/product-default.svg";
        }
        return String.join("|", photos);
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    @FunctionalInterface
    private interface StatementFiller {
        void fill(PreparedStatement ps) throws java.sql.SQLException;
    }
}
