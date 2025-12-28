package org.example.Service;

import org.example.Tool.HibernateUtil;
import org.example.User;
import org.example.Tool.SecurityUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class UserService {

    /**
     * Authenticates a user based on username and password.
     *
     * @param username The user's username.
     * @param password The user's plain-text password.
     * @return The User object if authentication is successful, otherwise null.
     */
    public User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        User user = findByUsername(username);

        if (user != null) {
            // Hash the provided password and compare it with the stored hash
            String hashedPassword = SecurityUtil.hashPassword(password);
            if (hashedPassword.equals(user.getPassword())) {
                return user; // Authentication successful
            }
        }

        return null; // Authentication failed
    }

    /**
     * Registers a new user.
     *
     * @param username The username for the new user.
     * @param password The plain-text password for the new user.
     * @return The newly created User object if registration is successful, otherwise null (e.g., username exists).
     */
    public User register(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return null;
        }

        // 1. Check if user already exists
        if (findByUsername(username) != null) {
            return null; // Username is already taken
        }

        // 2. Create new user
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(SecurityUtil.hashPassword(password));
        newUser.setAdmin(false); // New users are not admins by default

        // 3. Save user to database
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.persist(newUser);
            session.getTransaction().commit();
            return newUser;
        } catch (Exception e) {
            e.printStackTrace();
            // Handle exceptions, maybe log them
            return null;
        }
    }

    /**
     * Finds a user by their username.
     *
     * @param username The username to search for.
     * @return The User object if found, otherwise null.
     */
    private User findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("FROM User WHERE username = :username", User.class);
            query.setParameter("username", username);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
