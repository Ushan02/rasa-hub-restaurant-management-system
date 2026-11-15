package com.rasahub.product_inventory_service.dto.lowstock;

import jakarta.validation.constraints.Size;

public record AlertRemarksRequest(
        @Size(max = 500)
        String remarks
) {
}
