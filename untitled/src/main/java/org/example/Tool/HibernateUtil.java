package org.example.Tool;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class HibernateUtil {
    private static SessionFactory sessionFactory;
    private static final Object lock = new Object();

    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (lock) {
                if (sessionFactory == null) {
                    sessionFactory = new Configuration().configure().buildSessionFactory();
                }
            }
        }
        return sessionFactory;
    }

    public static void executeInTransaction(Consumer<Session> action) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                action.accept(session);
                transaction.commit();
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new RuntimeException("Transaction failed", e);
            }
        }
    }

    public static <T> T executeQuery(Function<Session, T> query) {
        try (Session session = getSessionFactory().openSession()) {
            return query.apply(session);
        } catch (Exception e) {
            throw new RuntimeException("Query failed", e);
        }
    }

    public static void save(Object entity) {
        executeInTransaction(session -> session.persist(entity));
    }

    public static void update(Object entity) {
        executeInTransaction(session -> session.merge(entity));
    }

    public static void delete(Object entity) {
        executeInTransaction(session -> session.remove(entity));
    }

    public static <T> T findById(Class<T> entityClass, Object id) {
        return executeQuery(session -> session.get(entityClass, id));
    }

    public static <T> List<T> findAll(Class<T> entityClass) {
        return executeQuery(session ->
            session.createQuery("FROM " + entityClass.getSimpleName(), entityClass).list()
        );
    }

    public static <T> List<T> executeHQL(String hql, Class<T> resultClass, Object... params) {
        return executeQuery(session -> {
            Query<T> query = session.createQuery(hql, resultClass);
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i, params[i]);
            }
            return query.list();
        });
    }

    public static int executeUpdate(String hql, Object... params) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                Query<?> query = session.createQuery(hql);
                for (int i = 0; i < params.length; i++) {
                    query.setParameter(i, params[i]);
                }
                int result = query.executeUpdate();
                transaction.commit();
                return result;
            } catch (Exception e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw new RuntimeException("Update failed", e);
            }
        }
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}
