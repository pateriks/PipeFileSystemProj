package company.server;

import company.common.AccountIntf;
import company.common.ControllerIntf;

import org.hibernate.collection.internal.PersistentSet;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Controller  extends UnicastRemoteObject implements ControllerIntf {
    //Database object
    DataDAO db = new DataDAO();
    //Core object
    PipeServer server = null;
    //Host ip

    //Non argument construct used outside of package
    protected Controller() throws RemoteException {
        super();
    }
    //Vem är det som har kontroll
    protected void init(PipeServer server){
        this.server = server;
    }

    @Override
    public String getMessage() throws RemoteException {
        return server.getMessage();
    }

    @Override
    public String getDir() throws RemoteException {
        return null;
    }

    @Override
    public String mkDir() throws RemoteException {
        return null;
    }

    @Override
    public boolean creat() throws RemoteException {
        return server.creat("test");
    }

    @Override
    public boolean creat(String path) throws RemoteException {
        return server.creat(path);
    }

    @Override
    public boolean delete(AccountIntf acc){
        try {
            server.multiDelete(db.findAccountByName(acc.getId(), true).getUser());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean delete(String path) throws RemoteException {
        server.delete(path);
        return true;
    }

    @Override
    public AccountIntf open(AccountIntf accountIntf, String path) throws RemoteException {
        Account acc = null;
        Item item = new Item(path);
        db.persistItem(item);
        acc = db.findAccountByName(accountIntf.getId(), false);
        server.open(path, acc.getUser());
        Set h = acc.getItem();
        h.add(item);
        if(h == null){
            h = new PersistentSet();
            h.add(item);
        }
        acc.setItem(h);
        db.commit();
        return acc;
    }

    @Override
    public boolean update(AccountIntf account) throws RemoteException {
        try {
            Account upd = db.findAccountByName(account.getId(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        db.commit();
        return true;
    }

    @Override
    public boolean update(String user, String pass){
        Account account = null;
        account = server.getAccount(user);
        account.getUser().setPassword(pass);
        return true;
    }

    @Override
    public boolean update(AccountIntf user, String pass){
        Account acc = null;
        try {
            acc = db.findAccountByName(user.getId(), false);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        acc.getUser().setPassword(pass);
        db.commit();
        return true;
    }

    @Override
    public int login(String s, int mac) throws RemoteException {
        List accounts = db.findAccounts(s, false);
        System.out.println(Arrays.toString(accounts.toArray()));
        try{
            if (accounts.isEmpty()){
                Account acc = new Account(s);
                server.addActiveAcc(mac, acc);
                db.persistAccount(acc);
                return 0;
            }else if(accounts.size() == 1){
                server.addActiveAcc(mac, (Account) accounts.get(0));
                db.commit();
                return 1;
            }else{
                db.commit();
                return 2;
            }
        }catch(Exception e){
            db.commit();
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public AccountIntf verifyUser(String user, String pass) throws RemoteException {
        Account loginAcc = server.getAccount(user);
        if(loginAcc.verifyUser(pass)){
            return (AccountIntf) loginAcc;
        }
        return null;
    }

    @Override
    public boolean write(AccountIntf acc, String input){
        try {
            server.multiWrite(db.findAccountByName(acc.getId(), true).getUser(), input);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public String [] list (AccountIntf acc){
        Object[] paths = null;
        try {
            if(acc != null) {
                Account account = db.findAccountByName(acc.getId(), true);
                if(account != null){
                    PersistentSet set = (PersistentSet) account.getItem();
                    paths = set.toArray();
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        String[] ret = new String[paths.length];
        int i = 0;
        for (java.lang.Object p : paths){
            ret[i] = p.toString();
            i++;
        }
        return ret;
    }

    @Override
    public boolean logout(AccountIntf acc){
        try {
            try {
                Naming.unbind("//".concat("localhost").concat("/Account"));
            } catch (NotBoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            server.multiClose(db.findAccountByName(acc.getId(), true).getUser());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean close(AccountIntf acc){
        try {
            server.multiClose(db.findAccountByName(acc.getId(), true).getUser());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    //Admin - Usage
    private String [] list (){
        java.lang.Object[] paths = server.list();
        String[] ret = new String[paths.length];
        int i = 0;
        for (java.lang.Object p : paths){
            ret[i] = p.toString();
            i++;
        }
        return ret;
    }
}
