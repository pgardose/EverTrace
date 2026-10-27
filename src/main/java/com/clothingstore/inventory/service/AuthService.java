package com.clothingstore.inventory.service;

import com.clothingstore.inventory.dao.UserDao;
import com.clothingstore.inventory.model.User;
import com.clothingstore.inventory.util.PasswordHasher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Login rules. The first time the app runs there are no accounts, so the
 * login screen offers to create the owner account; after that, no one can
 * create accounts without already being signed in.
 */
public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofSeconds(60);
    public static final int MIN_PASSWORD_LENGTH = 8;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,30}");

    // Checked against when the username doesn't exist, so "no such user" takes as long as
    // "wrong password" and response time doesn't reveal which usernames are real.
    private static final String DUMMY_HASH = PasswordHasher.hash("not-a-real-password".toCharArray());

    private static final class Attempts {
        int failures;
        Instant lockedUntil;
    }

    private final UserDao userDao;
    private final Clock clock;
    private final Map<String, Attempts> attemptsByUser = new HashMap<>();

    public AuthService(UserDao userDao) {
        this(userDao, Clock.systemDefaultZone());
    }

    public AuthService(UserDao userDao, Clock clock) {
        this.userDao = userDao;
        this.clock = clock;
    }

    public boolean isSetupRequired() {
        return !userDao.anyUserExists();
    }

    /** Creates the very first (owner) account. Refuses once any account exists. */
    public User registerFirstUser(String username, char[] password, char[] confirm) {
        if (!isSetupRequired()) {
            throw new AuthException("An account already exists. Please sign in.");
        }
        String cleanName = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(cleanName).matches()) {
            throw new AuthException("Username must be 3-30 characters: letters, numbers, dot, dash or underscore.");
        }
        validateNewPassword(password, confirm, cleanName);
        return userDao.insert(cleanName, PasswordHasher.hash(password));
    }

    /** Returns the user on success, throws AuthException (with a user-safe message) otherwise. */
    public User login(String username, char[] password) {
        String key = normalize(username);
        if (key.isEmpty() || password == null || password.length == 0) {
            throw new AuthException("Enter your username and password.");
        }

        Instant now = clock.instant();
        Attempts attempts = attemptsByUser.computeIfAbsent(key, k -> new Attempts());

        if (attempts.lockedUntil != null) {
            if (now.isBefore(attempts.lockedUntil)) {
                long seconds = Duration.between(now, attempts.lockedUntil).toSeconds() + 1;
                throw new AuthException("Too many failed attempts. Try again in " + seconds + " seconds.");
            }
            attempts.lockedUntil = null; // lock expired
        }

        Optional<User> user = userDao.findByUsername(key);
        boolean passwordMatches = PasswordHasher.verify(password, user.map(User::passwordHash).orElse(DUMMY_HASH));

        if (user.isEmpty() || !passwordMatches) {
            attempts.failures++;
            if (attempts.failures >= MAX_FAILED_ATTEMPTS) {
                attempts.failures = 0;
                attempts.lockedUntil = now.plus(LOCK_DURATION);
                throw new AuthException("Too many failed attempts. Locked for "
                        + LOCK_DURATION.toSeconds() + " seconds.");
            }
            throw new AuthException("Invalid username or password.");
        }

        attemptsByUser.remove(key);
        return user.get();
    }

    public void changePassword(User user, char[] current, char[] newPassword, char[] confirm) {
        // Re-read from the database so the check uses the latest stored hash.
        User stored = userDao.findByUsername(user.username())
                .orElseThrow(() -> new AuthException("This account no longer exists."));
        if (!PasswordHasher.verify(current, stored.passwordHash())) {
            throw new AuthException("Current password is incorrect.");
        }
        validateNewPassword(newPassword, confirm, stored.username());
        if (Arrays.equals(current, newPassword)) {
            throw new AuthException("New password must be different from the current one.");
        }
        userDao.updatePassword(stored.userId(), PasswordHasher.hash(newPassword));
    }

    private void validateNewPassword(char[] password, char[] confirm, String username) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new AuthException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasLetter || !hasDigit) {
            throw new AuthException("Password must contain at least one letter and one number.");
        }
        if (new String(password).equalsIgnoreCase(username)) {
            throw new AuthException("Password can't be the same as the username.");
        }
        if (!Arrays.equals(password, confirm)) {
            throw new AuthException("Passwords don't match.");
        }
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
