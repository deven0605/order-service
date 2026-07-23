package com.thalicloud.order.config;

import com.thalicloud.order.repository.CustomerRepository;
import com.thalicloud.order.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final VendorRepository vendorRepository;
    private final CustomerRepository customerRepository;

    /**
     * This service authenticates two JWT subject shapes issued by auth-service:
     * a vendor's email (contains "@") or a customer's phone number (everything
     * else). Vendor and customer tokens never collide on a username, so this
     * dispatch is unambiguous.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            if (username.contains("@")) {
                return vendorRepository.findByEmail(username)
                        .map(UserDetails.class::cast)
                        .orElseThrow(() -> new UsernameNotFoundException("Vendor not found: " + username));
            }
            return customerRepository.findByPhone(username)
                    .map(UserDetails.class::cast)
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found: " + username));
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Used by DeliveryDispatchClient to notify delivery-service when a vendor
    // accepts an order. Short timeouts so a slow/unavailable delivery-service
    // degrades the accept flow instead of hanging it.
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }
}
