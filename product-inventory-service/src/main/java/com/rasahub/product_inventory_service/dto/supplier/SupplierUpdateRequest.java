
package com.rasahub.product_inventory_service.dto.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SupplierUpdateRequest {

    @Pattern(
            regexp = "(?s).*\\S.*",
            message = "Supplier name must not be blank"
    )
    @Size(
            max = 150,
            message = "Supplier name must not exceed 150 characters"
    )
    private String supplierName;

    @Size(
            min = 10,
            max = 10,
            message = "Phone number must contain exactly 10 digits"
    )
    @Pattern(
            regexp = "^(?:" +
                    "(?:070|071|072|075|076|077|078)[0-9]{7}" +
                    "|074[0-4][0-9]{6}" +
                    "|0(?:11|21|23|24|25|26|27|" +
                    "31|32|33|34|35|36|37|38|" +
                    "41|45|47|51|52|54|55|57|" +
                    "63|65|66|67|81|91)[0-9]{7}" +
                    ")$",
            message = "Invalid Sri Lankan mobile or landline number"
    )
    private String phoneNumber;

    @Email(message = "Invalid email address")
    @Size(
            max = 150,
            message = "Email must not exceed 150 characters"
    )
    private String email;

    @Size(
            max = 500,
            message = "Address must not exceed 500 characters"
    )
    private String address;

    public SupplierUpdateRequest() {
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
