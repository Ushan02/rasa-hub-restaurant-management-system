package com.rasahub.auth_people_service.dto.employee;

import com.rasahub.auth_people_service.enums.EmployeePosition;
import com.rasahub.auth_people_service.enums.EmploymentStatus;

public class EmployeeResponse {

    private final Long id;
    private final String employeeId;
    private final String firstName;
    private final String lastName;
    private final String nic;
    private final String address;
    private final String email;
    private final String phone;
    private final EmployeePosition position;
    private final String branchCode;
    private final EmploymentStatus employmentStatus;
    private final boolean hasLoginAccount;

    public EmployeeResponse(Long id, String employeeId, String firstName, String lastName,
                            String nic, String address, String email, String phone,
                            EmployeePosition position, String branchCode,
                            EmploymentStatus employmentStatus, boolean hasLoginAccount) {
        this.id = id;
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.nic = nic;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.position = position;
        this.branchCode = branchCode;
        this.employmentStatus = employmentStatus;
        this.hasLoginAccount = hasLoginAccount;
    }

    public Long getId() {
        return id;
    }
    public String getEmployeeId() {
        return employeeId;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public String getNic() {
        return nic;
    }
    public String getAddress() {
        return address;
    }
    public String getEmail() {
        return email;
    }
    public String getPhone() {
        return phone;
    }
    public EmployeePosition getPosition() {
        return position;
    }
    public String getBranchCode() {
        return branchCode;
    }
    public EmploymentStatus getEmploymentStatus() {
        return employmentStatus;
    }
    public boolean isHasLoginAccount() {
        return hasLoginAccount;
    }
}