/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.application.authentication.framework.util;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.identity.application.authentication.framework.exception.FrameworkException;
import org.wso2.carbon.identity.core.util.IdentityKeyStoreResolverUtil;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Tests for signing user assertions with software keys and with keys held in an HSM.
 */
public class UserAssertionUtilsTest {

    private MockedStatic<IdentityTenantUtil> identityTenantUtil;
    private MockedStatic<KeyStoreManager> keyStoreManagerStatic;
    private MockedStatic<IdentityKeyStoreResolverUtil> resolverUtil;
    private KeyStoreManager keyStoreManager;

    @BeforeMethod
    public void setUp() {

        identityTenantUtil = mockStatic(IdentityTenantUtil.class);
        keyStoreManagerStatic = mockStatic(KeyStoreManager.class);
        resolverUtil = mockStatic(IdentityKeyStoreResolverUtil.class);
        keyStoreManager = mock(KeyStoreManager.class);
        keyStoreManagerStatic.when(() -> KeyStoreManager.getInstance(anyInt())).thenReturn(keyStoreManager);
    }

    @AfterMethod
    public void tearDown() {

        resolverUtil.close();
        keyStoreManagerStatic.close();
        identityTenantUtil.close();
    }

    @Test
    public void testSignWithSoftwareKey() throws Exception {

        // Tenant IDs differ per test because UserAssertionUtils caches keys per tenant.
        String tenantDomain = "software.com";
        mockTenant(tenantDomain, 101);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        when(keyStoreManager.getPrivateKey("software-com.p12", tenantDomain)).thenReturn(keyPair.getPrivate());

        String assertion = UserAssertionUtils.generateSignedUserAssertion(claims(), tenantDomain);

        assertTrue(SignedJWT.parse(assertion).verify(new RSASSAVerifier((RSAPublicKey) keyPair.getPublic())));
        // Software keys keep the default provider selection; no key store lookup.
        verify(keyStoreManager, never()).getKeyStore("software-com.p12");
    }

    @Test
    public void testSignWithOpaqueKey() throws Exception {

        String tenantDomain = "hsm.com";
        mockTenant(tenantDomain, 102);
        OpaqueRSAKeyTestProvider hsmProvider = new OpaqueRSAKeyTestProvider(2048);
        mockHsmTenantKeyStore(tenantDomain, "hsm-com.p11", hsmProvider);

        String assertion = UserAssertionUtils.generateSignedUserAssertion(claims(), tenantDomain);

        SignedJWT signedJWT = SignedJWT.parse(assertion);
        assertEquals(signedJWT.getHeader().getType().toString(), UserAssertionUtils.JWT_USER_ASSERTION_TYPE);
        assertTrue(signedJWT.verify(new RSASSAVerifier((RSAPublicKey) hsmProvider.getPublicKey())));
    }

    @Test
    public void testWeakOpaqueKeyIsRejected() throws Exception {

        String tenantDomain = "weak.com";
        mockTenant(tenantDomain, 103);
        mockHsmTenantKeyStore(tenantDomain, "weak-com.p11", new OpaqueRSAKeyTestProvider(1024));

        expectThrows(FrameworkException.class,
                () -> UserAssertionUtils.generateSignedUserAssertion(claims(), tenantDomain));
    }

    private void mockTenant(String tenantDomain, int tenantId) {

        identityTenantUtil.when(() -> IdentityTenantUtil.getTenantId(tenantDomain)).thenReturn(tenantId);
        String keyStoreName = tenantDomain.replace(".", "-") +
                (tenantDomain.startsWith("software") ? ".p12" : ".p11");
        resolverUtil.when(() -> IdentityKeyStoreResolverUtil.buildTenantKeyStoreName(tenantDomain))
                .thenReturn(keyStoreName);
    }

    private void mockHsmTenantKeyStore(String tenantDomain, String keyStoreName, OpaqueRSAKeyTestProvider provider)
            throws Exception {

        when(keyStoreManager.getPrivateKey(keyStoreName, tenantDomain)).thenReturn(provider.getPrivateKey());
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getPublicKey()).thenReturn(provider.getPublicKey());
        KeyStore keyStore = mock(KeyStore.class);
        when(keyStore.getProvider()).thenReturn(provider);
        when(keyStore.getCertificate(tenantDomain)).thenReturn(certificate);
        when(keyStoreManager.getKeyStore(keyStoreName)).thenReturn(keyStore);
    }

    private static JWTClaimsSet claims() {

        return new JWTClaimsSet.Builder().subject("alice").build();
    }
}
