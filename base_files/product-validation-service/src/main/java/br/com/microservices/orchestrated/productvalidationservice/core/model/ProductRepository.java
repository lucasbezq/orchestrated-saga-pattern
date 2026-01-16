package br.com.microservices.orchestrated.productvalidationservice.core.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Boolean existsByCode(String code);

}