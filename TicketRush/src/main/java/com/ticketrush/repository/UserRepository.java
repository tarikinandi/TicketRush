package com.ticketrush.repository;

import com.ticketrush.domain.TicketEvent;
import com.ticketrush.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

}
