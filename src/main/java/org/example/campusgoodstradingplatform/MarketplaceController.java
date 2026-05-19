package org.example.campusgoodstradingplatform;

import jakarta.servlet.http.HttpSession;
import org.example.campusgoodstradingplatform.CampusStoreData.CartItem;
import org.example.campusgoodstradingplatform.CampusStoreData.FeeRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.LoginRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.ProductRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.ProductStatus;
import org.example.campusgoodstradingplatform.CampusStoreData.PunishRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.RechargeRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.RegisterRequest;
import org.example.campusgoodstradingplatform.CampusStoreData.ReviewRequest;
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

    @GetMapping({"/", "/login", "/home", "/product/{id}", "/cart", "/user", "/shop/{id}", "/merchant/products",
            "/merchant/publish", "/admin/audit", "/admin/users"})
    public String index() {
        return "forward:/index.html";
    }

    @ResponseBody
    @GetMapping("/api/captcha")
    public Map<String, Object> captcha(HttpSession session) {
        String code = randomCaptcha();
        session.setAttribute("LOGIN_CAPTCHA", code);
        return service.captcha(code);
    }

    @ResponseBody
    @PostMapping("/api/login")
    public Object login(@RequestBody LoginRequest request, HttpSession session) {
        String expectedCaptcha = (String) session.getAttribute("LOGIN_CAPTCHA");
        Object user = service.login(request, expectedCaptcha);
        session.removeAttribute("LOGIN_CAPTCHA");
        return user;
    }

    @ResponseBody
    @PostMapping("/api/register")
    public Object register(RegisterRequest request,
                           @RequestParam(required = false) MultipartFile license,
                           @RequestParam(required = false) MultipartFile idCard) {
        return service.register(request, fileStorage.save(license), fileStorage.save(idCard));
    }

    @ResponseBody
    @PostMapping("/api/uploads")
    public Object upload(@RequestParam("files") List<MultipartFile> files) {
        return fileStorage.saveAll(files);
    }

    @ResponseBody
    @GetMapping("/api/products")
    public Object products(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String sort,
                           @RequestParam(required = false) BigDecimal minPrice,
                           @RequestParam(required = false) BigDecimal maxPrice) {
        return service.searchProducts(keyword, sort, minPrice, maxPrice);
    }

    @ResponseBody
    @GetMapping("/api/products/{id}")
    public Object product(@PathVariable long id) {
        return service.product(id);
    }

    @ResponseBody
    @GetMapping("/api/shops/{merchantId}/products")
    public Object shopProducts(@PathVariable long merchantId) {
        return service.shopProducts(merchantId);
    }

    @ResponseBody
    @GetMapping("/api/users/{userId}")
    public Object user(@PathVariable long userId) {
        return service.user(userId);
    }

    @ResponseBody
    @GetMapping("/api/users/{userId}/orders")
    public Object userOrders(@PathVariable long userId) {
        return service.userOrders(userId);
    }

    @ResponseBody
    @GetMapping("/api/users/{userId}/cart")
    public Object cart(@PathVariable long userId) {
        return service.cart(userId);
    }

    @ResponseBody
    @PostMapping("/api/users/{userId}/cart/{productId}")
    public Object addToCart(@PathVariable long userId, @PathVariable long productId,
                            @RequestParam(defaultValue = "1") int quantity) {
        return service.addToCart(userId, productId, quantity);
    }

    @ResponseBody
    @PutMapping("/api/users/{userId}/cart")
    public Object updateCart(@PathVariable long userId, @RequestBody List<CartItem> items) {
        return service.updateCart(userId, items);
    }

    @ResponseBody
    @PostMapping("/api/users/{userId}/checkout")
    public Object checkout(@PathVariable long userId, @RequestParam(defaultValue = "0") int pointsUsed) {
        return service.checkout(userId, pointsUsed);
    }

    @ResponseBody
    @PostMapping("/api/orders/{orderId}/received")
    public Object received(@PathVariable long orderId) {
        return service.markReceived(orderId);
    }

    @ResponseBody
    @PostMapping("/api/orders/{orderId}/return")
    public Object requestReturn(@PathVariable long orderId,
                                @RequestParam(defaultValue = "24小时内退货申请") String reason) {
        return service.requestReturn(orderId, reason);
    }

    @ResponseBody
    @PostMapping("/api/users/{userId}/reviews")
    public Object review(@PathVariable long userId, @RequestBody ReviewRequest request) {
        return service.review(userId, request);
    }

    @ResponseBody
    @PostMapping("/api/merchants/{merchantId}/products")
    public Object saveProduct(@PathVariable long merchantId, @RequestBody ProductRequest request) {
        return service.saveProduct(merchantId, request);
    }

    @ResponseBody
    @PostMapping("/api/products/{productId}/status")
    public Object productStatus(@PathVariable long productId, @RequestParam ProductStatus status) {
        return service.setProductStatus(productId, status);
    }

    @ResponseBody
    @GetMapping("/api/admin/users")
    public Object adminUsers() {
        return service.users();
    }

    @ResponseBody
    @GetMapping("/api/admin/pending-merchants")
    public Object pendingMerchants() {
        return service.pendingMerchants();
    }

    @ResponseBody
    @GetMapping("/api/admin/products")
    public Object adminProducts(@RequestParam(required = false) ProductStatus status) {
        return service.productsByStatus(status);
    }

    @ResponseBody
    @PostMapping("/api/admin/users/{userId}/approve")
    public Object approveUser(@PathVariable long userId) {
        return service.approveUser(userId);
    }

    @ResponseBody
    @PostMapping("/api/admin/recharge")
    public Object recharge(@RequestBody RechargeRequest request) {
        return service.recharge(request);
    }

    @ResponseBody
    @PostMapping("/api/admin/punish")
    public Object punish(@RequestBody PunishRequest request) {
        return service.punish(request);
    }

    @ResponseBody
    @PostMapping("/api/admin/fee")
    public Object fee(@RequestBody FeeRequest request) {
        return service.setFee(request);
    }

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

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
