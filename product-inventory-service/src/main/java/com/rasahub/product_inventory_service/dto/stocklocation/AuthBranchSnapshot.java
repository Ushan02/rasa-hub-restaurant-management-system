
package com.rasahub.product_inventory_service.dto.stocklocation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthBranchSnapshot(
        String branchCode,
        String name,
        String type,
        Boolean active
) {
}
