package com.neusoft.neu23.neuhospital.auth.service;

import com.neusoft.neu23.neuhospital.auth.security.AuthAccount;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary demo account provider. Replace with DB-backed sys_user lookup later.
 */
@Component
@Profile("demo-auth")
public class DemoAuthAccountProvider implements AuthAccountProvider {

    private final Map<String, AuthAccount> accounts;

    public DemoAuthAccountProvider(PasswordEncoder passwordEncoder) {
        this.accounts = new ConcurrentHashMap<>();
        accounts.put("patient001", new AuthAccount(
                201L, "patient001", passwordEncoder.encode("password123"),
                "PATIENT", "PATIENT", 20001L, "ENABLED"
        ));
        accounts.put("doctor001", new AuthAccount(
                101L, "doctor001", passwordEncoder.encode("password123"),
                "DOCTOR", "DOCTOR", 30001L, "ENABLED"
        ));
        accounts.put("frontdesk001", new AuthAccount(
                301L, "frontdesk001", passwordEncoder.encode("password123"),
                "MANAGEMENT", "REGISTRATION_CLERK", null, "ENABLED"
        ));
    }

    @Override
    public AuthAccount findByUsername(String username) {
        return accounts.get(username);
    }
}
