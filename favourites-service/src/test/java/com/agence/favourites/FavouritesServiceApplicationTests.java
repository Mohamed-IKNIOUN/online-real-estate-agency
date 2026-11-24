package com.agence.favourites;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class FavouritesServiceApplicationTests {

    @DynamicPropertySource
    static void configureVerificationKey(DynamicPropertyRegistry properties) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        properties.add("favourites.security.jwt-secret", () -> Base64.getEncoder().encodeToString(key));
    }

	@Test
	void contextLoads() {
	}

}
