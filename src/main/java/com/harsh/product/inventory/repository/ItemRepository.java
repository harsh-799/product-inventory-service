package com.harsh.product.inventory.repository;

import com.harsh.product.inventory.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
