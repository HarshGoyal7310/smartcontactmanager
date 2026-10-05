package com.smart.smartcontactmanager.dao;

import com.smart.smartcontactmanager.entities.Contact;
import com.smart.smartcontactmanager.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Integer> {

//    // Pagination support ke sath logged-in user ke contacts fetch karna
    @Query("from Contact as c where c.user.id = :userId")
    public Page<Contact> findContactsByUser(@Param("userId") int userId, Pageable pageable);

    // Starred/Favorite contacts fetch karne ke liye with Pagination
    @Query("from Contact as c where c.user.id = :userId and c.favorite = true")
    public Page<Contact> findFavoriteContactsByUser(@Param("userId") int userId, Pageable pageable);
    // Live search functionality ke liye (User ke contacts me se search)
    public List<Contact> findByNameContainingAndUser(String name, User user);

}