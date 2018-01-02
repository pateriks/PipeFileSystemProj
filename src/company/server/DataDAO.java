package company.server;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

public class DataDAO {
    private final EntityManagerFactory emf;
    private final ThreadLocal<EntityManager> threadLEM = new ThreadLocal<>();

    public DataDAO() {
        emf = Persistence.createEntityManagerFactory("Pipe");
    }

    public Account findAccountByName(String user, boolean bool) {
        if (user == null) {
            return null;
        }
        try {
            try {
                EntityManager em = beginTransaction();
                return em.createNamedQuery("findAccountByName", Account.class).
                        setParameter("userName", user).getSingleResult();
            } catch (NoResultException r) {
                return null;
            }
        } finally {
            if (bool) {
                commitTransaction();
            }
        }
    }

    public List<Account> findAccounts(String user, boolean bool) {
        try {
            EntityManager em = beginTransaction();
            try {
                return em.createNamedQuery("findAccounts", Account.class).
                        setParameter("userName", user).getResultList();
            } catch (NoResultException noSuchAccount) {
                return new ArrayList<>();
            }
        } finally {
            if(bool) {
                commitTransaction();
            }
        }
    }

    public Item findItem(String sPath, boolean bool){
        try {
            EntityManager em = beginTransaction();
            try {
                return em.createNamedQuery("findItemByPath", Item.class).
                        setParameter("sPath", sPath).getSingleResult();
            } catch (NoResultException noSuchAccount) {
                return null;
            }
        } finally {
            if(bool) {
                commitTransaction();
            }
        }
    }

    public void commit() {
        commitTransaction();
    }

    public void persistUser(User user){
        try {
            EntityManager em = beginTransaction();
            em.persist(user);
        } finally {
            commitTransaction();
        }
    }

    public void persistItem(Item item){
        try {
            EntityManager em = beginTransaction();
            em.persist(item);
        } finally {
            commitTransaction();
        }
    }

    public void persistAccount(Account acc){
        try {
            EntityManager em = beginTransaction();
            em.persist(acc);
        } finally {
            commitTransaction();
        }
    }

    private EntityManager beginTransaction() {
        EntityManager em = emf.createEntityManager();
        threadLEM.set(em);
        EntityTransaction transaction = em.getTransaction();
        if (!transaction.isActive()) {
            transaction.begin();
        }
        return em;
    }

    private void commitTransaction(){
        EntityManager em = threadLEM.get();
        if(em == null){
            return;
        }
        EntityTransaction transaction = em.getTransaction();
        transaction.commit();
    }
}