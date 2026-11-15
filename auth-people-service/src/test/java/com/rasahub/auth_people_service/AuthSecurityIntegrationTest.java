package com.rasahub.auth_people_service;

import com.rasahub.auth_people_service.entity.AuthAccount;
import com.rasahub.auth_people_service.entity.Branch;
import com.rasahub.auth_people_service.entity.Customer;
import com.rasahub.auth_people_service.entity.Employee;
import com.rasahub.auth_people_service.enums.AccountStatus;
import com.rasahub.auth_people_service.enums.BranchType;
import com.rasahub.auth_people_service.enums.EmployeePosition;
import com.rasahub.auth_people_service.enums.Role;
import com.rasahub.auth_people_service.repository.AuthAccountRepository;
import com.rasahub.auth_people_service.repository.BranchRepository;
import com.rasahub.auth_people_service.repository.CustomerRepository;
import com.rasahub.auth_people_service.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthSecurityIntegrationTest {

    private static final String STAFF_ID = "TEST1001";
    private static final String RAW_PASSWORD = "test-only-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthAccountRepository accountRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private AuthAccount account;
    private Employee employee;

    @BeforeEach
    void setUp() {
        Branch branch = branchRepository.saveAndFlush(
                new Branch("BR001", "Test Branch One", BranchType.RESTAURANT_BRANCH)
        );
        account = accountRepository.saveAndFlush(
                new AuthAccount(
                        passwordEncoder.encode(RAW_PASSWORD),
                        Role.BRANCH_MANAGER
                )
        );
        employee = new Employee(
                STAFF_ID,
                "Test",
                "Manager One",
                "TEST-NIC-1001",
                EmployeePosition.BRANCH_MANAGER,
                branch
        );
        employee.setAuthAccount(account);
        employee = employeeRepository.saveAndFlush(employee);
    }

    @Test
    void validStaffLoginReturnsSignedTokenWithConfiguredClaimsAndLifetime()
            throws Exception {
        assertNotEquals(RAW_PASSWORD, account.getPasswordHash());
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, account.getPasswordHash()));
        assertFalse(passwordEncoder.matches("wrong-password", account.getPasswordHash()));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":" test1001 ","password":"test-only-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.role").value("BRANCH_MANAGER"))
                .andReturn();

        String token = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.token"
        );
        Claims claims = Jwts.parser()
                .verifyWith(signingKey(jwtSecret))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(account.getId().toString(), claims.getSubject());
        assertEquals("BRANCH_MANAGER", claims.get("role", String.class));
        assertEquals(
                employee.getBranch().getId().longValue(),
                claims.get("branchId", Number.class).longValue()
        );
        assertTrue(claims.getIssuedAt() != null);
        assertTrue(claims.getExpiration() != null);
        assertEquals(
                3_600_000L,
                claims.getExpiration().getTime() - claims.getIssuedAt().getTime()
        );
    }

    @Test
    void customerLoginAndProfileUseTheAuthenticatedAccountSubject()
            throws Exception {
        AuthAccount customerAccount = accountRepository.saveAndFlush(
                new AuthAccount(
                        passwordEncoder.encode("customer-test-password"),
                        Role.CUSTOMER
                )
        );
        Customer customer = new Customer(
                "Test Customer",
                "One",
                "0712345678",
                "customer.one@example.test"
        );
        customer.setAuthAccount(customerAccount);
        customerRepository.saveAndFlush(customer);

        AuthAccount otherAccount = accountRepository.saveAndFlush(
                new AuthAccount(
                        passwordEncoder.encode("other-customer-password"),
                        Role.CUSTOMER
                )
        );
        Customer otherCustomer = new Customer(
                "Other Customer",
                "Two",
                "0712345679",
                "customer.two@example.test"
        );
        otherCustomer.setAuthAccount(otherAccount);
        customerRepository.saveAndFlush(otherCustomer);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/customer/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"CUSTOMER.ONE@EXAMPLE.TEST","password":"customer-test-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(customerAccount.getId()))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn();

        String customerToken = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.token"
        );
        mockMvc.perform(get("/api/v1/customers/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test Customer"));

        String tokenWithSpoofedClaim = signedToken(
                customerAccount.getId().toString(),
                Map.of(
                        "role", "CUSTOMER",
                        "accountId", otherAccount.getId()
                )
        );
        mockMvc.perform(get("/api/v1/customers/me")
                        .header("Authorization", "Bearer " + tokenWithSpoofedClaim))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test Customer"));
    }

    @Test
    void invalidPasswordAndUnknownStaffHaveSameUnauthorizedResponse()
            throws Exception {
        String wrongPassword = """
                {"identifier":"TEST1001","password":"incorrect-test-password"}
                """;
        String unknownAccount = """
                {"identifier":"UNKNOWN1001","password":"test-only-password"}
                """;

        mockMvc.perform(post("/api/v1/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPassword))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unknownAccount))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void missingLoginFieldsAreRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":" ","password":"test-only-password"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.identifier").exists());

        mockMvc.perform(post("/api/v1/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"TEST1001","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void inactiveAccountCannotLoginOrReuseAnExistingToken()
            throws Exception {
        String token = signedToken(
                account.getId().toString(),
                Map.of("role", "BRANCH_MANAGER")
        );

        account.setStatus(AccountStatus.INACTIVE);
        accountRepository.saveAndFlush(account);

        mockMvc.perform(post("/api/v1/auth/staff/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"TEST1001","password":"test-only-password"}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsMissingExpiredMalformedAndInvalidTokens()
            throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized());

        String missingSubject = Jwts.builder()
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .claim("role", "OWNER")
                .signWith(signingKey(jwtSecret))
                .compact();
        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + missingSubject))
                .andExpect(status().isUnauthorized());

        String missingExpiration = Jwts.builder()
                .subject(account.getId().toString())
                .issuedAt(new Date())
                .claim("role", "OWNER")
                .signWith(signingKey(jwtSecret))
                .compact();
        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + missingExpiration))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        String expired = Jwts.builder()
                .subject(account.getId().toString())
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .claim("role", "BRANCH_MANAGER")
                .signWith(signingKey(jwtSecret))
                .compact();
        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());

        String invalidSignature = Jwts.builder()
                .subject(account.getId().toString())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .claim("role", "BRANCH_MANAGER")
                .signWith(signingKey(
                        "another-test-signing-key-that-is-at-least-32-bytes"
                ))
                .compact();
        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + invalidSignature))
                .andExpect(status().isUnauthorized());

        String valid = signedToken(
                account.getId().toString(),
                Map.of("role", "BRANCH_MANAGER")
        );
        String[] parts = valid.split("\\.");
        parts[1] = (parts[1].charAt(0) == 'A' ? "B" : "A")
                + parts[1].substring(1);
        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + String.join(".", parts)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void databaseRoleOverridesTokenRoleAndBranchIsResolvedFromSubjectAccount()
            throws Exception {
        Branch otherBranch = branchRepository.saveAndFlush(
                new Branch("BR002", "Test Branch Two", BranchType.RESTAURANT_BRANCH)
        );
        AuthAccount otherAccount = accountRepository.saveAndFlush(
                new AuthAccount(
                        passwordEncoder.encode("other-test-password"),
                        Role.BRANCH_MANAGER
                )
        );
        Employee otherEmployee = new Employee(
                "TEST1002",
                "Other",
                "Branch Employee",
                "TEST-NIC-1002",
                EmployeePosition.BRANCH_MANAGER,
                otherBranch
        );
        otherEmployee.setAuthAccount(otherAccount);
        employeeRepository.saveAndFlush(otherEmployee);

        String token = signedToken(
                account.getId().toString(),
                Map.of(
                        "role", "OWNER",
                        "accountId", otherAccount.getId(),
                        "branchId", otherBranch.getId()
                )
        );

        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeId").value(STAFF_ID))
                .andExpect(jsonPath("$[0].branchCode").value("BR001"));
    }

    @Test
    void roleRestrictionsUseDatabaseRoleAndReturnForbiddenForInsufficientRole()
            throws Exception {
        account.setRole(Role.STOCK_KEEPER);
        accountRepository.saveAndFlush(account);

        String forgedOwnerRole = signedToken(
                account.getId().toString(),
                Map.of("role", "OWNER")
        );

        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + forgedOwnerRole))
                .andExpect(status().isForbidden());

        account.setRole(Role.OWNER);
        accountRepository.saveAndFlush(account);

        String ownerToken = signedToken(
                account.getId().toString(),
                Map.of("role", "BRANCH_MANAGER")
        );
        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void validSignedTokenWithUnknownSubjectIsUnauthorized()
            throws Exception {
        String unknownAccountToken = signedToken(
                "999999999",
                Map.of("role", "OWNER")
        );

        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer " + unknownAccountToken))
                .andExpect(status().isUnauthorized());
    }

    private String signedToken(String subject, Map<String, Object> claims) {
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)));
        claims.forEach(builder::claim);
        return builder.signWith(signingKey(jwtSecret)).compact();
    }

    private SecretKey signingKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
