package com.clothingstore.inventory.dao;

import java.util.List;
import java.util.Optional;

/**
 * Shared CRUD contract for every DAO. Using generics here means
 * ItemDao, OrderDao, and CategoryDao all share one contract without
 * duplicating findById/findAll/save/delete signatures.
 */
public interface GenericDao<T, ID> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    void deleteById(ID id);
}
