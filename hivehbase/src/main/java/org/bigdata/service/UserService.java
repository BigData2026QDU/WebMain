package org.bigdata.service;

import org.bigdata.tool.HibernateUtil;
import org.bigdata.entity.User;
import org.bigdata.util.SecurityUtil; // 保留本地实现
import org.hibernate.Session;
import org.hibernate.query.Query;

public class UserService {

    public User login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        User user = findByUsername(username);

        if (user != null) {
            String hashedPassword = SecurityUtil.hashPassword(password);
            if (hashedPassword.equals(user.getPassword())) {
                return user;
            }
        }

        return null;
    }

    public User register(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return null;
        }

        if (findByUsername(username) != null) {
            return null;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(SecurityUtil.hashPassword(password));
        newUser.setAdmin(false);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.beginTransaction();
            session.persist(newUser);
            session.getTransaction().commit();
            return newUser;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register user: " + summarizeException(e), e);
        }
    }

    private User findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("FROM User WHERE username = :username", User.class);
            query.setParameter("username", username);
            return query.uniqueResult();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to query user by username: " + summarizeException(e), e);
        }
    }

    private static String summarizeException(Throwable throwable) {
        Throwable current = throwable;
        StringBuilder summary = new StringBuilder();
        while (current != null) {
            if (summary.length() > 0) {
                summary.append(" <- ");
            }
            summary.append(current.getClass().getName());
            String message = current.getMessage();
            if (message != null && !message.trim().isEmpty()) {
                summary.append(": ").append(message.trim());
            }
            Throwable next = current.getCause();
            if (next == null || next == current) {
                break;
            }
            current = next;
        }
        return summary.toString();
    }
}
