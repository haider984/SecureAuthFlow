package com.demo.springsecurityclient.repository;

import com.demo.springsecurityclient.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long>{
    VerificationToken findByToken(String token);
    //, CrudRepository<VerificationToken, Long>
    //void save(VerificationToken verificationToken);
}
