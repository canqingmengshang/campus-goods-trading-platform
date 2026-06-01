package org.example.campusgoodstradingplatform.mapper;

import org.example.campusgoodstradingplatform.dto.ProductRequest;
import org.example.campusgoodstradingplatform.dto.RegisterRequest;
import org.example.campusgoodstradingplatform.entity.CartItem;
import org.example.campusgoodstradingplatform.entity.Order;
import org.example.campusgoodstradingplatform.entity.Product;
import org.example.campusgoodstradingplatform.entity.ProductStatus;
import org.example.campusgoodstradingplatform.entity.Review;
import org.example.campusgoodstradingplatform.entity.Role;
import org.example.campusgoodstradingplatform.entity.User;
import org.example.campusgoodstradingplatform.entity.UserStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class MarketplaceMapper {
    private final MarketplaceSqlMapper sql;

    public MarketplaceMapper(MarketplaceSqlMapper sql) {
        this.sql = sql;
    }

    public User findUserByCredentials(String username, String password) {
        return hydrate(sql.findUserByCredentials(username, password));
    }

    public boolean usernameExists(String username) {
        return sql.countUsername(username) > 0;
    }

    public long insertUser(RegisterRequest request, Role role, UserStatus status, String licenseImage, String idCardImage,
                           int merchantLevel, BigDecimal feeRate) {
        UserInsertCommand command = new UserInsertCommand();
        command.username = request.username();
        command.password = request.password();
        command.realName = request.realName();
        command.phone = request.phone();
        command.email = request.email();
        command.city = request.city();
        command.gender = request.gender();
        command.bankAccount = request.bankAccount();
        command.role = role.name();
        command.status = status.name();
        command.shopName = request.shopName();
        command.licenseImage = licenseImage;
        command.idCardImage = idCardImage;
        command.merchantLevel = merchantLevel;
        command.feeRate = feeRate;
        sql.insertUser(command);
        return command.id;
    }

    public List<Product> searchPublishedProducts(String keyword, BigDecimal minPrice, BigDecimal maxPrice, String orderBy) {
        String word = "%" + (keyword == null ? "" : keyword.toLowerCase()) + "%";
        return hydrateProducts(sql.searchPublishedProducts(word, minPrice, maxPrice, orderBy));
    }

    public Product product(long id) {
        Product product = hydrate(sql.product(id));
        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        return product;
    }

    public List<Product> shopProducts(long merchantId) {
        return hydrateProducts(sql.shopProducts(merchantId));
    }

    public List<CartItem> cart(long userId) {
        return sql.cart(userId);
    }

    public void upsertCartItem(long userId, long productId, int quantity) {
        sql.upsertCartItem(userId, productId, quantity);
    }

    public void clearCart(long userId) {
        sql.clearCart(userId);
    }

    public void insertCartItem(long userId, CartItem item) {
        sql.insertCartItem(userId, item);
    }

    public long insertOrder(long userId, BigDecimal payable, int usablePoints) {
        OrderInsertCommand command = new OrderInsertCommand();
        command.userId = userId;
        command.payable = payable;
        command.usablePoints = usablePoints;
        sql.insertOrder(command);
        return command.id;
    }

    public void insertOrderItem(long orderId, CartItem item, BigDecimal unitPrice) {
        sql.insertOrderItem(orderId, item, unitPrice);
    }

    public void decreaseProductStockAndIncreaseSales(long productId, int quantity) {
        sql.decreaseProductStockAndIncreaseSales(productId, quantity);
    }

    public void updateUserWalletAndPoints(long userId, BigDecimal payable, int usedPoints, int earnedPoints) {
        sql.updateUserWalletAndPoints(userId, payable, usedPoints, earnedPoints);
    }

    public void deleteSelectedCartItems(long userId) {
        sql.deleteSelectedCartItems(userId);
    }

    public List<Order> userOrders(long userId) {
        return hydrateOrders(sql.userOrders(userId));
    }

    public void updateOrderReceived(long orderId) {
        sql.updateOrderReceived(orderId);
    }

    public void updateOrderReturnRequested(long orderId, String reason) {
        sql.updateOrderReturnRequested(orderId, reason);
    }

    public Order order(long id) {
        Order order = hydrate(sql.order(id));
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    public long insertReview(long userId, long orderId, long merchantId, Long productId, int stars, String content) {
        ReviewInsertCommand command = new ReviewInsertCommand();
        command.userId = userId;
        command.orderId = orderId;
        command.merchantId = merchantId;
        command.productId = productId;
        command.stars = stars;
        command.content = content;
        sql.insertReview(command);
        return command.id;
    }

    public Review review(long id) {
        return sql.review(id);
    }

    public void recalculateMerchantRate(long merchantId) {
        sql.recalculateMerchantRate(merchantId);
    }

    public void recalculateProductRate(long productId) {
        sql.recalculateProductRate(productId);
    }

    public long insertProduct(long merchantId, ProductRequest request) {
        ProductInsertCommand command = new ProductInsertCommand();
        fillProduct(command, merchantId, request);
        sql.insertProduct(command);
        return command.id;
    }

    public void updateProduct(long merchantId, ProductRequest request) {
        ProductUpdateCommand command = new ProductUpdateCommand();
        command.id = request.id();
        fillProduct(command, merchantId, request);
        sql.updateProduct(command);
    }

    public void updateProductStatus(long productId, ProductStatus status) {
        sql.updateProductStatus(productId, status);
    }

    public List<User> users() {
        return hydrateUsers(sql.users());
    }

    public List<User> pendingMerchants() {
        return hydrateUsers(sql.pendingMerchants());
    }

    public List<Product> productsByStatus(ProductStatus status) {
        if (status == null) {
            return hydrateProducts(sql.products());
        }
        return hydrateProducts(sql.productsByStatus(status));
    }

    public void updateUserStatus(long userId, UserStatus status) {
        sql.updateUserStatus(userId, status);
    }

    public void addWallet(long userId, BigDecimal amount) {
        sql.addWallet(userId, amount);
    }

    public void lockProductsByMerchant(long merchantId) {
        sql.lockProductsByMerchant(merchantId);
    }

    public void updateMerchantFee(long merchantId, int level, BigDecimal feeRate) {
        sql.updateMerchantFee(merchantId, level, feeRate);
    }

    public User user(long id) {
        User user = hydrate(sql.user(id));
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private List<User> hydrateUsers(List<User> users) {
        users.forEach(this::hydrate);
        return users;
    }

    private User hydrate(User user) {
        if (user != null) {
            user.reviews = sql.reviewsByMerchant(user.id);
        }
        return user;
    }

    private List<Product> hydrateProducts(List<Product> products) {
        products.forEach(this::hydrate);
        return products;
    }

    private Product hydrate(Product product) {
        if (product != null) {
            product.reviews = sql.reviewsByProduct(product.id);
        }
        return product;
    }

    private List<Order> hydrateOrders(List<Order> orders) {
        orders.forEach(this::hydrate);
        return orders;
    }

    private Order hydrate(Order order) {
        if (order != null) {
            order.items = sql.orderItems(order.id);
            order.reviews = sql.reviewsByOrder(order.id);
        }
        return order;
    }

    private void fillProduct(ProductInsertCommand command, long merchantId, ProductRequest request) {
        command.merchantId = merchantId;
        command.name = request.name();
        command.category = request.category();
        command.originalPrice = nvl(request.originalPrice(), BigDecimal.ZERO);
        command.salePrice = nvl(request.salePrice(), request.originalPrice());
        command.size = request.size();
        command.photos = joinPhotos(request.photos());
        command.usageGuide = request.usageGuide();
        command.negotiable = request.negotiable();
        command.stock = request.stock();
        command.condition = request.condition();
    }

    private void fillProduct(ProductUpdateCommand command, long merchantId, ProductRequest request) {
        command.merchantId = merchantId;
        command.name = request.name();
        command.category = request.category();
        command.originalPrice = nvl(request.originalPrice(), BigDecimal.ZERO);
        command.salePrice = nvl(request.salePrice(), request.originalPrice());
        command.size = request.size();
        command.photos = joinPhotos(request.photos());
        command.usageGuide = request.usageGuide();
        command.negotiable = request.negotiable();
        command.stock = request.stock();
        command.condition = request.condition();
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
}
