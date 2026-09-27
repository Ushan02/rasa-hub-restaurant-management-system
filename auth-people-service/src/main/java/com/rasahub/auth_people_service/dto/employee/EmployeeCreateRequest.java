package com.rasahub.auth_people_service.dto.employee;

import com.rasahub.auth_people_service.enums.EmployeePosition;
import jakarta.validation.constraints.*;

public class EmployeeCreateRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 60, message = "First name must be under 60 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 60, message = "Last name must be under 60 characters")
    private String lastName;

    @NotBlank(message = "NIC is required")
    @Pattern(regexp = "^([0-9]{9}[vVxX]|[0-9]{12})$",
            message = "NIC must be a valid old (9 digits + V/X) or new (12 digits) format")
    private String nic;

    @Size(max = 150, message = "Address must be under 150 characters")
    private String address;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must be under 100 characters")
    private String email;

    @Pattern(regexp = "^0[0-9]{9}$", message = "Phone must be 10 digits starting with 0")
    private String phone;

    @NotNull(message = "Position is required")
    private EmployeePosition position;

    @NotNull(message = "Branch ID is required")
    @Positive(message = "Branch ID must be positive")
    private Long branchId;

    private boolean createLoginAccount;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getNic() {
        return nic;
    }
    public void setNic(String nic) {
        this.nic = nic;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public EmployeePosition getPosition() {
        return position;
    }
    public void setPosition(EmployeePosition position) {
        this.position = position;
    }
    public Long getBranchId() {
        return branchId;
    }
    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }
    public boolean isCreateLoginAccount() {
        return createLoginAccount;
    }

    public void setCreateLoginAccount(boolean createLoginAccount) {
        this.createLoginAccount = createLoginAccount;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}