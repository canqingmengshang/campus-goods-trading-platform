package org.example.campusgoodstradingplatform;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public DatabaseInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(64) NOT NULL UNIQUE,
                    password VARCHAR(128) NOT NULL,
                    real_name VARCHAR(64),
                    phone VARCHAR(32),
                    email VARCHAR(120),
                    city VARCHAR(64),
                    gender VARCHAR(20),
                    bank_account CHAR(16),
                    role VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    wallet DECIMAL(12,2) NOT NULL DEFAULT 0,
                    points INT NOT NULL DEFAULT 0,
                    shop_name VARCHAR(100),
                    license_image VARCHAR(255),
                    id_card_image VARCHAR(255),
                    merchant_level INT NOT NULL DEFAULT 3,
                    fee_rate DECIMAL(5,2) NOT NULL DEFAULT 0.50,
                    favorable_rate DECIMAL(5,2) NOT NULL DEFAULT 100.00,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        addColumnIfMissing("users", "real_name", "VARCHAR(64)");
        addColumnIfMissing("users", "email", "VARCHAR(120)");
        addColumnIfMissing("users", "city", "VARCHAR(64)");
        addColumnIfMissing("users", "gender", "VARCHAR(20)");
        addColumnIfMissing("users", "bank_account", "CHAR(16)");

        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    merchant_id BIGINT NOT NULL,
                    name VARCHAR(120) NOT NULL,
                    category VARCHAR(60),
                    original_price DECIMAL(12,2) NOT NULL,
                    sale_price DECIMAL(12,2) NOT NULL,
                    size VARCHAR(80),
                    photos TEXT,
                    usage_guide TEXT,
                    negotiable TINYINT(1) NOT NULL DEFAULT 0,
                    stock INT NOT NULL DEFAULT 0,
                    sales INT NOT NULL DEFAULT 0,
                    condition_text VARCHAR(40),
                    status VARCHAR(30) NOT NULL,
                    favorable_rate DECIMAL(5,2) NOT NULL DEFAULT 100.00,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_products_merchant FOREIGN KEY (merchant_id) REFERENCES users(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS cart_items (
                    user_id BIGINT NOT NULL,
                    product_id BIGINT NOT NULL,
                    quantity INT NOT NULL,
                    selected TINYINT(1) NOT NULL DEFAULT 1,
                    PRIMARY KEY (user_id, product_id),
                    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    buyer_id BIGINT NOT NULL,
                    total_amount DECIMAL(12,2) NOT NULL,
                    points_used INT NOT NULL DEFAULT 0,
                    status VARCHAR(30) NOT NULL,
                    paid_at DATETIME NOT NULL,
                    received_at DATETIME,
                    return_reason VARCHAR(255),
                    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    product_id BIGINT NOT NULL,
                    quantity INT NOT NULL,
                    unit_price DECIMAL(12,2) NOT NULL,
                    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS reviews (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    merchant_id BIGINT NOT NULL,
                    product_id BIGINT,
                    stars INT NOT NULL,
                    content VARCHAR(500),
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id),
                    CONSTRAINT fk_reviews_merchant FOREIGN KEY (merchant_id) REFERENCES users(id),
                    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (count != null && count == 0) {
            seed();
        }
    }

    private void seed() {
        jdbc.update("""
                INSERT INTO users(id, username, password, real_name, phone, email, city, gender, bank_account, role, status, wallet, points, shop_name, merchant_level, fee_rate, favorable_rate)
                VALUES
                (1,'admin','123456','平台管理员','13800000001','admin@campus.test','杭州','男','1111222233334444','ADMIN','ACTIVE',1000,2000,'平台管理',5,1.00,100),
                (2,'buyer','123456','学生买家','13800000002','buyer@campus.test','杭州','女','2222333344445555','BUYER','ACTIVE',800,1600,'学生买家',3,0.50,100),
                (3,'merchant','123456','商家负责人','13800000003','merchant@campus.test','杭州','男','3333444455556666','MERCHANT','ACTIVE',800,1600,'梧桐二手铺',3,0.50,97.50),
                (4,'newshop','123456','新店负责人','13800000004','newshop@campus.test','杭州','女','4444555566667777','MERCHANT','PENDING',800,1600,'新芽数码店',3,0.50,100)
                """);
        jdbc.update("""
                INSERT INTO products(merchant_id, name, category, original_price, sale_price, size, photos, usage_guide, negotiable, stock, sales, condition_text, status, favorable_rate)
                VALUES
                (3,'九成新山地车','出行',899,650,'26寸','/images/product-bike.jpg','线下校园面交，支持验货后确认收货。',1,3,28,'九成新','PUBLISHED',98.40),
                (3,'考研英语真题套装','图书',128,56,'12册','/images/product-book.jpg','资料完整无缺页，可在教学楼自取。',0,12,61,'七成新','PUBLISHED',96.80),
                (3,'宿舍小冰箱','电器',699,420,'48L','/images/product-fridge.jpg','通电正常，支持现场验货。',1,2,17,'九成新','PUBLISHED',94.20),
                (3,'机械键盘青轴','数码',299,169,'87键','/images/product-keyboard.jpg','按键正常，附数据线。',0,8,43,'九成新','PUBLISHED',97.60),
                (4,'摄影补光灯套装','数码',260,118,'双灯','/images/product-light.jpg','适合社团拍摄和直播。',1,5,0,'九成新','PUBLISHED',100.00)
                """);
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count != null && count == 0) {
            jdbc.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }
}
