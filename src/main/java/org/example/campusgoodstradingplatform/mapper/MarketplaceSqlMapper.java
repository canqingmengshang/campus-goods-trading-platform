package org.example.campusgoodstradingplatform.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.example.campusgoodstradingplatform.entity.CartItem;
import org.example.campusgoodstradingplatform.entity.Order;
import org.example.campusgoodstradingplatform.entity.Product;
import org.example.campusgoodstradingplatform.entity.ProductStatus;
import org.example.campusgoodstradingplatform.entity.Review;
import org.example.campusgoodstradingplatform.entity.User;
import org.example.campusgoodstradingplatform.entity.UserStatus;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface MarketplaceSqlMapper {
    String PRODUCT_SELECT = """
            SELECT p.*, u.shop_name merchant_name
            FROM products p
            JOIN users u ON u.id=p.merchant_id
            """;

    @Select("SELECT * FROM users WHERE username=#{username} AND password=#{password}")
    @Results(id = "userResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "real_name", property = "realName"),
            @Result(column = "bank_account", property = "bankAccount"),
            @Result(column = "shop_name", property = "shopName"),
            @Result(column = "license_image", property = "licenseImage"),
            @Result(column = "id_card_image", property = "idCardImage"),
            @Result(column = "merchant_level", property = "merchantLevel"),
            @Result(column = "fee_rate", property = "feeRate"),
            @Result(column = "favorable_rate", property = "favorableRate")
    })
    User findUserByCredentials(@Param("username") String username, @Param("password") String password);

    @Select("SELECT COUNT(*) FROM users WHERE username=#{username}")
    int countUsername(@Param("username") String username);

    @Insert("""
            INSERT INTO users(username,password,real_name,phone,email,city,gender,bank_account,role,status,wallet,points,shop_name,license_image,id_card_image,merchant_level,fee_rate,favorable_rate)
            VALUES(#{username},#{password},#{realName},#{phone},#{email},#{city},#{gender},#{bankAccount},#{role},#{status},500,1200,#{shopName},#{licenseImage},#{idCardImage},#{merchantLevel},#{feeRate},100)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(UserInsertCommand command);

    @Select(PRODUCT_SELECT + """
            WHERE p.status='PUBLISHED'
              AND LOWER(p.name) LIKE #{keyword}
              AND p.sale_price BETWEEN #{minPrice} AND #{maxPrice}
            ORDER BY ${orderBy}
            """)
    @Results(id = "productResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "merchant_id", property = "merchantId"),
            @Result(column = "merchant_name", property = "merchantName"),
            @Result(column = "original_price", property = "originalPrice"),
            @Result(column = "sale_price", property = "salePrice"),
            @Result(column = "photos", property = "photos", typeHandler = PhotoListTypeHandler.class),
            @Result(column = "usage_guide", property = "usageGuide"),
            @Result(column = "condition_text", property = "condition"),
            @Result(column = "favorable_rate", property = "favorableRate")
    })
    List<Product> searchPublishedProducts(@Param("keyword") String keyword,
                                          @Param("minPrice") BigDecimal minPrice,
                                          @Param("maxPrice") BigDecimal maxPrice,
                                          @Param("orderBy") String orderBy);

    @Select(PRODUCT_SELECT + " WHERE p.id=#{id}")
    @Results(id = "singleProductResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "merchant_id", property = "merchantId"),
            @Result(column = "merchant_name", property = "merchantName"),
            @Result(column = "original_price", property = "originalPrice"),
            @Result(column = "sale_price", property = "salePrice"),
            @Result(column = "photos", property = "photos", typeHandler = PhotoListTypeHandler.class),
            @Result(column = "usage_guide", property = "usageGuide"),
            @Result(column = "condition_text", property = "condition"),
            @Result(column = "favorable_rate", property = "favorableRate")
    })
    Product product(@Param("id") long id);

    @Select(PRODUCT_SELECT + " WHERE p.merchant_id=#{merchantId} ORDER BY p.id DESC")
    @Results(id = "shopProductResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "merchant_id", property = "merchantId"),
            @Result(column = "merchant_name", property = "merchantName"),
            @Result(column = "original_price", property = "originalPrice"),
            @Result(column = "sale_price", property = "salePrice"),
            @Result(column = "photos", property = "photos", typeHandler = PhotoListTypeHandler.class),
            @Result(column = "usage_guide", property = "usageGuide"),
            @Result(column = "condition_text", property = "condition"),
            @Result(column = "favorable_rate", property = "favorableRate")
    })
    List<Product> shopProducts(@Param("merchantId") long merchantId);

    @Select("SELECT * FROM cart_items WHERE user_id=#{userId}")
    @Results(id = "cartItemResult", value = {
            @Result(column = "product_id", property = "productId")
    })
    List<CartItem> cart(@Param("userId") long userId);

    @Insert("""
            INSERT INTO cart_items(user_id,product_id,quantity,selected)
            VALUES(#{userId},#{productId},#{quantity},1)
            ON DUPLICATE KEY UPDATE quantity=quantity+VALUES(quantity), selected=1
            """)
    int upsertCartItem(@Param("userId") long userId, @Param("productId") long productId, @Param("quantity") int quantity);

    @Delete("DELETE FROM cart_items WHERE user_id=#{userId}")
    int clearCart(@Param("userId") long userId);

    @Insert("INSERT INTO cart_items(user_id,product_id,quantity,selected) VALUES(#{userId},#{item.productId},#{item.quantity},#{item.selected})")
    int insertCartItem(@Param("userId") long userId, @Param("item") CartItem item);

    @Insert("INSERT INTO orders(buyer_id,total_amount,points_used,status,paid_at) VALUES(#{userId},#{payable},#{usablePoints},'PAID',NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(OrderInsertCommand command);

    @Insert("INSERT INTO order_items(order_id,product_id,quantity,unit_price) VALUES(#{orderId},#{item.productId},#{item.quantity},#{unitPrice})")
    int insertOrderItem(@Param("orderId") long orderId, @Param("item") CartItem item, @Param("unitPrice") BigDecimal unitPrice);

    @Update("UPDATE products SET stock=stock-#{quantity}, sales=sales+#{quantity} WHERE id=#{productId}")
    int decreaseProductStockAndIncreaseSales(@Param("productId") long productId, @Param("quantity") int quantity);

    @Update("UPDATE users SET wallet=wallet-#{payable}, points=points-#{usedPoints}+#{earnedPoints} WHERE id=#{userId}")
    int updateUserWalletAndPoints(@Param("userId") long userId,
                                  @Param("payable") BigDecimal payable,
                                  @Param("usedPoints") int usedPoints,
                                  @Param("earnedPoints") int earnedPoints);

    @Delete("DELETE FROM cart_items WHERE user_id=#{userId} AND selected=1")
    int deleteSelectedCartItems(@Param("userId") long userId);

    @Select("SELECT * FROM orders WHERE buyer_id=#{userId} ORDER BY id DESC")
    @Results(id = "orderResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "buyer_id", property = "buyerId"),
            @Result(column = "total_amount", property = "totalAmount"),
            @Result(column = "points_used", property = "pointsUsed"),
            @Result(column = "paid_at", property = "paidAt"),
            @Result(column = "received_at", property = "receivedAt"),
            @Result(column = "return_reason", property = "returnReason")
    })
    List<Order> userOrders(@Param("userId") long userId);

    @Update("UPDATE orders SET status='RECEIVED', received_at=NOW() WHERE id=#{orderId}")
    int updateOrderReceived(@Param("orderId") long orderId);

    @Update("UPDATE orders SET status='RETURN_REQUESTED', return_reason=#{reason} WHERE id=#{orderId}")
    int updateOrderReturnRequested(@Param("orderId") long orderId, @Param("reason") String reason);

    @Select("SELECT * FROM orders WHERE id=#{id}")
    @Results(id = "singleOrderResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "buyer_id", property = "buyerId"),
            @Result(column = "total_amount", property = "totalAmount"),
            @Result(column = "points_used", property = "pointsUsed"),
            @Result(column = "paid_at", property = "paidAt"),
            @Result(column = "received_at", property = "receivedAt"),
            @Result(column = "return_reason", property = "returnReason")
    })
    Order order(@Param("id") long id);

    @Select("SELECT product_id,quantity,1 selected FROM order_items WHERE order_id=#{orderId}")
    @Results(id = "orderItemResult", value = {
            @Result(column = "product_id", property = "productId")
    })
    List<CartItem> orderItems(@Param("orderId") long orderId);

    @Insert("INSERT INTO reviews(order_id,user_id,merchant_id,product_id,stars,content) VALUES(#{orderId},#{userId},#{merchantId},#{productId},#{stars},#{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReview(ReviewInsertCommand command);

    @Select("SELECT * FROM reviews WHERE id=#{id}")
    @Results(id = "reviewResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "merchant_id", property = "merchantId"),
            @Result(column = "product_id", property = "productId"),
            @Result(column = "created_at", property = "createdAt")
    })
    Review review(@Param("id") long id);

    @Update("""
            UPDATE users SET favorable_rate=COALESCE((
                SELECT ROUND(SUM(CASE WHEN stars>=4 THEN 1 ELSE 0 END) * 100 / NULLIF(COUNT(*),0), 2)
                FROM reviews WHERE merchant_id=#{merchantId}
            ),100) WHERE id=#{merchantId}
            """)
    int recalculateMerchantRate(@Param("merchantId") long merchantId);

    @Update("""
            UPDATE products SET favorable_rate=COALESCE((
                SELECT ROUND(SUM(CASE WHEN stars>=4 THEN 1 ELSE 0 END) * 100 / NULLIF(COUNT(*),0), 2)
                FROM reviews WHERE product_id=#{productId}
            ),100) WHERE id=#{productId}
            """)
    int recalculateProductRate(@Param("productId") long productId);

    @Insert("""
            INSERT INTO products(merchant_id,name,category,original_price,sale_price,size,photos,usage_guide,negotiable,stock,condition_text,status,favorable_rate)
            VALUES(#{merchantId},#{name},#{category},#{originalPrice},#{salePrice},#{size},#{photos},#{usageGuide},#{negotiable},#{stock},#{condition},'AUDITING',100)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertProduct(ProductInsertCommand command);

    @Update("""
            UPDATE products
            SET name=#{name},category=#{category},original_price=#{originalPrice},sale_price=#{salePrice},size=#{size},
                photos=#{photos},usage_guide=#{usageGuide},negotiable=#{negotiable},stock=#{stock},
                condition_text=#{condition},status='AUDITING'
            WHERE id=#{id} AND merchant_id=#{merchantId}
            """)
    int updateProduct(ProductUpdateCommand command);

    @Update("UPDATE products SET status=#{status} WHERE id=#{productId}")
    int updateProductStatus(@Param("productId") long productId, @Param("status") ProductStatus status);

    @Select("SELECT * FROM users ORDER BY id")
    @Results(id = "allUserResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "real_name", property = "realName"),
            @Result(column = "bank_account", property = "bankAccount"),
            @Result(column = "shop_name", property = "shopName"),
            @Result(column = "license_image", property = "licenseImage"),
            @Result(column = "id_card_image", property = "idCardImage"),
            @Result(column = "merchant_level", property = "merchantLevel"),
            @Result(column = "fee_rate", property = "feeRate"),
            @Result(column = "favorable_rate", property = "favorableRate")
    })
    List<User> users();

    @Select("SELECT * FROM users WHERE status='PENDING' ORDER BY id")
    @Results(id = "pendingUserResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "real_name", property = "realName"),
            @Result(column = "bank_account", property = "bankAccount"),
            @Result(column = "shop_name", property = "shopName"),
            @Result(column = "license_image", property = "licenseImage"),
            @Result(column = "id_card_image", property = "idCardImage"),
            @Result(column = "merchant_level", property = "merchantLevel"),
            @Result(column = "fee_rate", property = "feeRate"),
            @Result(column = "favorable_rate", property = "favorableRate")
    })
    List<User> pendingMerchants();

    @Select(PRODUCT_SELECT + " ORDER BY p.id DESC")
    @Results(id = "allProductResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "merchant_id", property = "merchantId"),
            @Result(column = "merchant_name", property = "merchantName"),
            @Result(column = "original_price", property = "originalPrice"),
            @Result(column = "sale_price", property = "salePrice"),
            @Result(column = "photos", property = "photos", typeHandler = PhotoListTypeHandler.class),
            @Result(column = "usage_guide", property = "usageGuide"),
            @Result(column = "condition_text", property = "condition"),
            @Result(column = "favorable_rate", property = "favorableRate")
    })
    List<Product> products();

    @Select(PRODUCT_SELECT + " WHERE p.status=#{status} ORDER BY p.id DESC")
    @Results(id = "statusProductResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "merchant_id", property = "merchantId"),
            @Result(column = "merchant_name", property = "merchantName"),
            @Result(column = "original_price", property = "originalPrice"),
            @Result(column = "sale_price", property = "salePrice"),
            @Result(column = "photos", property = "photos", typeHandler = PhotoListTypeHandler.class),
            @Result(column = "usage_guide", property = "usageGuide"),
            @Result(column = "condition_text", property = "condition"),
            @Result(column = "favorable_rate", property = "favorableRate")
    })
    List<Product> productsByStatus(@Param("status") ProductStatus status);

    @Update("UPDATE users SET status=#{status} WHERE id=#{userId}")
    int updateUserStatus(@Param("userId") long userId, @Param("status") UserStatus status);

    @Update("UPDATE users SET wallet=wallet+#{amount} WHERE id=#{userId}")
    int addWallet(@Param("userId") long userId, @Param("amount") BigDecimal amount);

    @Update("UPDATE products SET status='LOCKED' WHERE merchant_id=#{merchantId}")
    int lockProductsByMerchant(@Param("merchantId") long merchantId);

    @Update("UPDATE users SET merchant_level=#{level}, fee_rate=#{feeRate} WHERE id=#{merchantId}")
    int updateMerchantFee(@Param("merchantId") long merchantId, @Param("level") int level, @Param("feeRate") BigDecimal feeRate);

    @Select("SELECT * FROM users WHERE id=#{id}")
    @Results(id = "singleUserResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "real_name", property = "realName"),
            @Result(column = "bank_account", property = "bankAccount"),
            @Result(column = "shop_name", property = "shopName"),
            @Result(column = "license_image", property = "licenseImage"),
            @Result(column = "id_card_image", property = "idCardImage"),
            @Result(column = "merchant_level", property = "merchantLevel"),
            @Result(column = "fee_rate", property = "feeRate"),
            @Result(column = "favorable_rate", property = "favorableRate")
    })
    User user(@Param("id") long id);

    @Select("SELECT * FROM reviews WHERE order_id=#{orderId} ORDER BY id DESC")
    @Results(id = "orderReviewResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "merchant_id", property = "merchantId"),
            @Result(column = "product_id", property = "productId"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<Review> reviewsByOrder(@Param("orderId") long orderId);

    @Select("SELECT * FROM reviews WHERE merchant_id=#{merchantId} ORDER BY id DESC")
    @Results(id = "merchantReviewResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "merchant_id", property = "merchantId"),
            @Result(column = "product_id", property = "productId"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<Review> reviewsByMerchant(@Param("merchantId") long merchantId);

    @Select("SELECT * FROM reviews WHERE product_id=#{productId} ORDER BY id DESC")
    @Results(id = "productReviewResult", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "merchant_id", property = "merchantId"),
            @Result(column = "product_id", property = "productId"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<Review> reviewsByProduct(@Param("productId") long productId);
}
