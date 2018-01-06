package company.server;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

public class DataDAO {
    //FINAL INITILATIONS
    private final EntityManagerFactory emf;
    private final ThreadLocal<EntityManager> threadLEM = new ThreadLocal<>();
    //Construct
    public DataDAO() {
        emf = Persistence.createEntityManagerFactory("Pipe");
    }
    //Searchers
    public Account findAccountByName(long user, boolean bool) {
        if (user == 0) {
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

    public Account searchItem(String sPath){
        boolean bool = true;
        try {
            EntityManager em = beginTransaction();
            try {
                return em.createNamedQuery("findItemByPath", Account.class).
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
    //Used by user after search
    public void commit() {
        commitTransaction();
    }
    //New user to persist
    public void persistUser(User user){
        try {
            EntityManager em = beginTransaction();
            em.persist(user);
        } finally {
            commitTransaction();
        }
    }
    //New item to persist
    public void persistItem(Item item){
        try {
            EntityManager em = beginTransaction();
            em.persist(item);
        } finally {
            commitTransaction();
        }
    }
    //New account to persist
    public void persistAccount(Account acc){
        try {
            EntityManager em = beginTransaction();
            em.persist(acc);
        } finally {
            commitTransaction();
        }
    }
    //Private methods
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