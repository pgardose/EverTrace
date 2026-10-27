package com.clothingstore.inventory.model;

/** A login account. passwordHash is the PBKDF2 string from PasswordHasher, never a plain password. */
public record User(int userId, String username, String passwordHash) {}
