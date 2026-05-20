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
import org.example.campusgoodstradingplatform.entity.Product;
import org.example.campusgoodstradingplatform.entity.ProductStatus;
import org.example.campusgoodstradingplatform.entity.Review;
import org.example.campusgoodstradingplatform.entity.User;
import org.example.campusgoodstradingplatform.mapper.MarketplaceMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class MarketplaceService {
    private final MarketplaceMapper marketplaceMapper;

    public MarketplaceService(MarketplaceMapper marketplaceMapper) {
        this.marketplaceMapper = marketplaceMapper;
    }

    public Map<String, Object> captcha(String code) {
        return marketplaceMapper.captcha(code);
    }

    public User login(LoginRequest request, String expectedCaptcha) {
        return marketplaceMapper.login(request, expectedCaptcha);
    }

    public User register(RegisterRequest request, String licenseImage, String idCardImage) {
        return marketplaceMapper.register(request, licenseImage, idCardImage);
    }

    public List<Product> searchProducts(String keyword, String sort, BigDecimal minPrice, BigDecimal maxPrice) {
        return marketplaceMapper.searchProducts(keyword, sort, minPrice, maxPrice);
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

    public List<CartItem> addToCart(long userId, long productId, int quantity) {
        return marketplaceMapper.addToCart(userId, productId, quantity);
    }

    public List<CartItem> updateCart(long userId, List<CartItem> items) {
        return marketplaceMapper.updateCart(userId, items);
    }

    public Order checkout(long userId, int pointsUsed) {
        return marketplaceMapper.checkout(userId, pointsUsed);
    }

    public List<Order> userOrders(long userId) {
        return marketplaceMapper.userOrders(userId);
    }

    public Order markReceived(long orderId) {
        return marketplaceMapper.markReceived(orderId);
    }

    public Order requestReturn(long orderId, String reason) {
        return marketplaceMapper.requestReturn(orderId, reason);
    }

    public Review review(long userId, ReviewRequest request) {
        return marketplaceMapper.review(userId, request);
    }

    public Product saveProduct(long merchantId, ProductRequest request) {
        return marketplaceMapper.saveProduct(merchantId, request);
    }

    public Product setProductStatus(long productId, ProductStatus status) {
        return marketplaceMapper.setProductStatus(productId, status);
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

    public User approveUser(long userId) {
        return marketplaceMapper.approveUser(userId);
    }

    public User recharge(RechargeRequest request) {
        return marketplaceMapper.recharge(request);
    }

    public User punish(PunishRequest request) {
        return marketplaceMapper.punish(request);
    }

    public User setFee(FeeRequest request) {
        return marketplaceMapper.setFee(request);
    }

    public User user(long id) {
        return marketplaceMapper.user(id);
    }
}
