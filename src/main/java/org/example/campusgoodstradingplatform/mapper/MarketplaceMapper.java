package org.example.campusgoodstradingplatform.mapper;

import org.example.campusgoodstradingplatform.dto.ProductRequest;
import org.example.campusgoodstradingplatform.dto.RegisterRequest;
import org.example.campusgoodstradingplatform.entity.CartItem;
import org.example.campusgoodstradingplatform.entity.Order;
import org.example.campusgoodstradingplatform.entity.OrderStatus;
import org.example.campusgoodstradingplatform.entity.Product;
import org.example.campusgoodstradingplatform.entity.ProductStatus;
import org.example.campusgoodstradingplatform.entity.Review;
import org.example.campusgoodstradingplatform.entity.Role;
import org.example.campusgoodstradingplatform.entity.User;
import org.example.campusgoodstradingplatform.entity.UserStatus;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Repository
public class MarketplaceMapper {
    private final JdbcTemplate jdbc;

    public MarketplaceMapper(JdbcTemplate jdbc) {
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

    /*
     * 根据账号和密码查询用户。
     */
    public User findUserByCredentials(String username, String password) {
        try {
            return jdbc.queryForObject("SELECT * FROM users WHERE username=? AND password=?", userMapper, username, password);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    /*
     * 判断用户名是否已经存在。
     */
    public boolean usernameExists(String username) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username=?", Integer.class, username);
        return count != null && count > 0;
    }

    /*
     * 新增用户记录。
     * 返回新增用户ID。
     */
    public long insertUser(RegisterRequest request, Role role, UserStatus status, String licenseImage, String idCardImage,
                           int merchantLevel, BigDecimal feeRate) {
        return insert("""
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
            ps.setInt(14, merchantLevel);
            ps.setBigDecimal(15, feeRate);
        });
    }

    /*
     * 查询已发布商品。
     * 支持关键词、价格区间和排序SQL片段。
     */
    public List<Product> searchPublishedProducts(String keyword, BigDecimal minPrice, BigDecimal maxPrice, String orderBy) {
        String word = "%" + (keyword == null ? "" : keyword.toLowerCase()) + "%";
        return jdbc.query(productSql("WHERE p.status='PUBLISHED' AND LOWER(p.name) LIKE ? AND p.sale_price BETWEEN ? AND ? ORDER BY " + orderBy),
                productMapper, word, minPrice, maxPrice);
    }

    /*
     * 根据商品ID查询商品详情。
     */
    public Product product(long id) {
        try {
            return jdbc.queryForObject(productSql("WHERE p.id=?"), productMapper, id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("商品不存在");
        }
    }

    /*
     * 查询指定商家的全部商品。
     */
    public List<Product> shopProducts(long merchantId) {
        return jdbc.query(productSql("WHERE p.merchant_id=? ORDER BY p.id DESC"), productMapper, merchantId);
    }

    /*
     * 查询用户购物车条目。
     */
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

    /*
     * 新增购物车条目。
     * 已存在时累加数量。
     */
    public void upsertCartItem(long userId, long productId, int quantity) {
        jdbc.update("""
                INSERT INTO cart_items(user_id,product_id,quantity,selected)
                VALUES(?,?,?,1)
                ON DUPLICATE KEY UPDATE quantity=quantity+VALUES(quantity), selected=1
                """, userId, productId, quantity);
    }

    /*
     * 清空指定用户购物车。
     */
    public void clearCart(long userId) {
        jdbc.update("DELETE FROM cart_items WHERE user_id=?", userId);
    }

    /*
     * 插入单条购物车记录。
     */
    public void insertCartItem(long userId, CartItem item) {
        jdbc.update("INSERT INTO cart_items(user_id,product_id,quantity,selected) VALUES(?,?,?,?)",
                userId, item.productId, item.quantity, item.selected);
    }

    /*
     * 新增订单主表记录。
     * 返回订单ID。
     */
    public long insertOrder(long userId, BigDecimal payable, int usablePoints) {
        return insert("INSERT INTO orders(buyer_id,total_amount,points_used,status,paid_at) VALUES(?,?,?,?,NOW())", ps -> {
            ps.setLong(1, userId);
            ps.setBigDecimal(2, payable);
            ps.setInt(3, usablePoints);
            ps.setString(4, OrderStatus.PAID.name());
        });
    }

    /*
     * 新增订单商品明细。
     */
    public void insertOrderItem(long orderId, CartItem item, BigDecimal unitPrice) {
        jdbc.update("INSERT INTO order_items(order_id,product_id,quantity,unit_price) VALUES(?,?,?,?)",
                orderId, item.productId, item.quantity, unitPrice);
    }

    /*
     * 扣减商品库存并增加销量。
     */
    public void decreaseProductStockAndIncreaseSales(long productId, int quantity) {
        jdbc.update("UPDATE products SET stock=stock-?, sales=sales+? WHERE id=?", quantity, quantity, productId);
    }

    /*
     * 更新用户钱包余额和积分。
     */
    public void updateUserWalletAndPoints(long userId, BigDecimal payable, int usedPoints, int earnedPoints) {
        jdbc.update("UPDATE users SET wallet=wallet-?, points=points-?+? WHERE id=?", payable, usedPoints, earnedPoints, userId);
    }

    /*
     * 删除用户购物车中已结算的选中商品。
     */
    public void deleteSelectedCartItems(long userId) {
        jdbc.update("DELETE FROM cart_items WHERE user_id=? AND selected=1", userId);
    }

    /*
     * 查询用户订单列表。
     */
    public List<Order> userOrders(long userId) {
        return jdbc.query("SELECT * FROM orders WHERE buyer_id=? ORDER BY id DESC", orderMapper(), userId);
    }

    /*
     * 更新订单为已收货状态。
     */
    public void updateOrderReceived(long orderId) {
        jdbc.update("UPDATE orders SET status=?, received_at=NOW() WHERE id=?", OrderStatus.RECEIVED.name(), orderId);
    }

    /*
     * 更新订单为退货申请状态。
     */
    public void updateOrderReturnRequested(long orderId, String reason) {
        jdbc.update("UPDATE orders SET status=?, return_reason=? WHERE id=?", OrderStatus.RETURN_REQUESTED.name(), reason, orderId);
    }

    /*
     * 根据订单ID查询订单详情。
     */
    public Order order(long id) {
        try {
            return jdbc.queryForObject("SELECT * FROM orders WHERE id=?", orderMapper(), id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("订单不存在");
        }
    }

    /*
     * 新增评价记录。
     * 返回评价ID。
     */
    public long insertReview(long userId, long orderId, long merchantId, Long productId, int stars, String content) {
        return insert("INSERT INTO reviews(order_id,user_id,merchant_id,product_id,stars,content) VALUES(?,?,?,?,?,?)", ps -> {
            ps.setLong(1, orderId);
            ps.setLong(2, userId);
            ps.setLong(3, merchantId);
            if (productId == null) {
                ps.setObject(4, null);
            } else {
                ps.setLong(4, productId);
            }
            ps.setInt(5, stars);
            ps.setString(6, content);
        });
    }

    /*
     * 根据评价ID查询评价。
     */
    public Review review(long id) {
        return jdbc.queryForObject("SELECT * FROM reviews WHERE id=?", reviewMapper(), id);
    }

    /*
     * 重新计算商家好评率。
     */
    public void recalculateMerchantRate(long merchantId) {
        jdbc.update("""
                UPDATE users SET favorable_rate=COALESCE((
                    SELECT ROUND(SUM(CASE WHEN stars>=4 THEN 1 ELSE 0 END) * 100 / NULLIF(COUNT(*),0), 2)
                    FROM reviews WHERE merchant_id=?
                ),100) WHERE id=?
                """, merchantId, merchantId);
    }

    /*
     * 重新计算商品好评率。
     */
    public void recalculateProductRate(long productId) {
        jdbc.update("""
                UPDATE products SET favorable_rate=COALESCE((
                    SELECT ROUND(SUM(CASE WHEN stars>=4 THEN 1 ELSE 0 END) * 100 / NULLIF(COUNT(*),0), 2)
                    FROM reviews WHERE product_id=?
                ),100) WHERE id=?
                """, productId, productId);
    }

    /*
     * 新增商品记录。
     * 默认进入待审核状态。
     */
    public long insertProduct(long merchantId, ProductRequest request) {
        return insert("""
                INSERT INTO products(merchant_id,name,category,original_price,sale_price,size,photos,usage_guide,negotiable,stock,condition_text,status,favorable_rate)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,'AUDITING',100)
                """, ps -> fillProduct(ps, merchantId, request, 1));
    }

    /*
     * 更新商家商品信息。
     * 修改后重新进入待审核状态。
     */
    public void updateProduct(long merchantId, ProductRequest request) {
        jdbc.update("""
                UPDATE products SET name=?,category=?,original_price=?,sale_price=?,size=?,photos=?,usage_guide=?,negotiable=?,stock=?,condition_text=?,status='AUDITING'
                WHERE id=? AND merchant_id=?
                """, request.name(), request.category(), nvl(request.originalPrice(), BigDecimal.ZERO),
                nvl(request.salePrice(), request.originalPrice()), request.size(), joinPhotos(request.photos()),
                request.usageGuide(), request.negotiable(), request.stock(), request.condition(), request.id(), merchantId);
    }

    /*
     * 更新商品状态。
     */
    public void updateProductStatus(long productId, ProductStatus status) {
        jdbc.update("UPDATE products SET status=? WHERE id=?", status.name(), productId);
    }

    /*
     * 查询全部用户。
     */
    public List<User> users() {
        return jdbc.query("SELECT * FROM users ORDER BY id", userMapper);
    }

    /*
     * 查询待审核用户或商家。
     */
    public List<User> pendingMerchants() {
        return jdbc.query("SELECT * FROM users WHERE status='PENDING' ORDER BY id", userMapper);
    }

    /*
     * 按状态查询商品。
     */
    public List<Product> productsByStatus(ProductStatus status) {
        if (status == null) {
            return jdbc.query(productSql("ORDER BY p.id DESC"), productMapper);
        }
        return jdbc.query(productSql("WHERE p.status=? ORDER BY p.id DESC"), productMapper, status.name());
    }

    /*
     * 更新用户状态。
     */
    public void updateUserStatus(long userId, UserStatus status) {
        jdbc.update("UPDATE users SET status=? WHERE id=?", status.name(), userId);
    }

    /*
     * 增加用户钱包余额。
     */
    public void addWallet(long userId, BigDecimal amount) {
        jdbc.update("UPDATE users SET wallet=wallet+? WHERE id=?", amount, userId);
    }

    /*
     * 锁定指定商家的全部商品。
     */
    public void lockProductsByMerchant(long merchantId) {
        jdbc.update("UPDATE products SET status='LOCKED' WHERE merchant_id=?", merchantId);
    }

    /*
     * 更新商家等级和手续费率。
     */
    public void updateMerchantFee(long merchantId, int level, BigDecimal feeRate) {
        jdbc.update("UPDATE users SET merchant_level=?, fee_rate=? WHERE id=?", level, feeRate, merchantId);
    }

    /*
     * 根据用户ID查询用户。
     */
    public User user(long id) {
        try {
            return jdbc.queryForObject("SELECT * FROM users WHERE id=?", userMapper, id);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    /*
     * 将订单查询结果映射为订单对象。
     */
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

    /*
     * 查询订单关联评价。
     */
    private List<Review> reviewsByOrder(long orderId) {
        return jdbc.query("SELECT * FROM reviews WHERE order_id=? ORDER BY id DESC", reviewMapper(), orderId);
    }

    /*
     * 查询商家收到的评价。
     */
    private List<Review> reviewsByMerchant(long merchantId) {
        return jdbc.query("SELECT * FROM reviews WHERE merchant_id=? ORDER BY id DESC", reviewMapper(), merchantId);
    }

    /*
     * 查询商品评价。
     */
    private List<Review> reviewsByProduct(long productId) {
        return jdbc.query("SELECT * FROM reviews WHERE product_id=? ORDER BY id DESC", reviewMapper(), productId);
    }

    /*
     * 将评价查询结果映射为评价对象。
     */
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

    /*
     * 拼接商品查询基础SQL。
     */
    private String productSql(String suffix) {
        return """
                SELECT p.*, u.shop_name merchant_name
                FROM products p
                JOIN users u ON u.id=p.merchant_id
                """ + " " + suffix;
    }

    /*
     * 执行插入SQL并返回自增主键。
     */
    private long insert(String sql, StatementFiller filler) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            filler.fill(ps);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    /*
     * 填充商品插入语句参数。
     */
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

    /*
     * 将数据库中的图片字符串拆分为列表。
     */
    private List<String> splitPhotos(String photos) {
        if (photos == null || photos.isBlank()) {
            return List.of("/images/product-default.svg");
        }
        return Arrays.stream(photos.split("\\|")).filter(value -> !value.isBlank()).toList();
    }

    /*
     * 将图片列表合并为数据库存储字符串。
     */
    private String joinPhotos(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            return "/images/product-default.svg";
        }
        return String.join("|", photos);
    }

    /*
     * BigDecimal 空值兜底。
     */
    private BigDecimal nvl(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    /*
     * 将数据库时间转换为 LocalDateTime。
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    @FunctionalInterface
    private interface StatementFiller {
        void fill(PreparedStatement ps) throws java.sql.SQLException;
    }
}
