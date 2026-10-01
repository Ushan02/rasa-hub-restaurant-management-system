package com.rasahub.auth_people_service.dto.customer;

public class CustomerResponse {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String email;
    private final boolean marketingConsent;
    private final boolean hasLoginAccount;

    public CustomerResponse(
            Long id,
            String firstName,
            String lastName,
            String phone,
            String email,
            boolean marketingConsent,
            boolean hasLoginAccount
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.marketingConsent = marketingConsent;
        this.hasLoginAccount = hasLoginAccount;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public boolean isMarketingConsent() {
        return marketingConsent;
    }

    public boolean isHasLoginAccount() {
        return hasLoginAccount;
    }
}