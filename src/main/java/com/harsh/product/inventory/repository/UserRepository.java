package com.harsh.product.inventory.repository;

import com.harsh.product.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
