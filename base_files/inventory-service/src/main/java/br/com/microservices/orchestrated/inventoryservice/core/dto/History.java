package br.com.microservices.orchestrated.inventoryservice.core.dto;

import br.com.microservices.orchestrated.inventoryservice.core.enums.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class History {

    private String source;
    private SagaStatus status;
    private String message;
    private String createdAt;

}
