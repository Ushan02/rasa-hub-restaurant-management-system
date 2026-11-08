
package com.rasahub.product_inventory_service.dto.stocklocation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StockLocationUpdateRequest {

    @NotBlank(message = "Location name is required")
    @Size(
            max = 150,
            message = "Location name must not exceed 150 characters"
    )
    private String locationName;

    public StockLocationUpdateRequest() {
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
