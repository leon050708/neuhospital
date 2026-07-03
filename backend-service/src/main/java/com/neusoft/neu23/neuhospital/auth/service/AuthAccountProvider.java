package com.neusoft.neu23.neuhospital.auth.service;

import com.neusoft.neu23.neuhospital.auth.security.AuthAccount;

/**
 * Looks up login accounts for the auth module.
 */
public interface AuthAccountProvider {

    AuthAccount findByUsername(String username);
}
