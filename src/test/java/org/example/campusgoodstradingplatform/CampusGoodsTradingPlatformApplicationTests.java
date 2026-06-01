package org.example.campusgoodstradingplatform;

import org.example.campusgoodstradingplatform.entity.Product;
import org.example.campusgoodstradingplatform.entity.User;
import org.example.campusgoodstradingplatform.service.MarketplaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class CampusGoodsTradingPlatformApplicationTests {
    @Autowired
    private MarketplaceService marketplaceService;

    @Test
    void contextLoads() {
    }

    @Test
    void mybatisQueriesMarketplaceData() {
        User admin = marketplaceService.user(1);
        List<Product> products = marketplaceService.searchProducts(null, null, null, null);

        assertNotNull(admin);
        assertFalse(products.isEmpty());
    }

}
