package company.controller;

import client.RmiClient;
import company.common.AccountIntf;
import company.common.ControllerIntf;
import company.server.Account;
import company.server.DataDAO;
import company.server.Item;
import company.server.PipeServer;
import org.hibernate.collection.internal.PersistentSet;

import java.io.IOException;
import java.io.OutputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.function.BiConsumer;

public class Controller  extends UnicastRemoteObject implements ControllerIntf {
    DataDAO db = new DataDAO();
    PipeServer server = new PipeServer();
    String wDir = "";
    HashMap<String, OutputStream> openFiles = new HashMap<>();

    public Controller() throws RemoteException {
        super();    // required to avoid the 'rmic' step, see below
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
        return server.creat(wDir);
    }

    @Override
    public boolean creat(String path) throws RemoteException {
        return server.creat(path);

    }

    @Override
    public boolean delete(){
        openFiles.forEach(new BiConsumer<String, OutputStream>() {
            @Override
            public void accept(String s, OutputStream outputStream) {
                server.delete(s);
            }
        });
        return true;
    }

    @Override
    public boolean delete(String path) throws RemoteException {
        server.delete(path);
        return true;
    }

    @Override
    public Account open(AccountIntf accountIntf, String path) throws RemoteException {
        Account acc = null;
        Item item = new Item(path);
        db.persistItem(item);
        acc = db.findAccountByName(accountIntf.getUserName(), false);
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

    public boolean close(AccountIntf acc){
        try {
            server.multiClose(db.findAccountByName(acc.getUserName(), true).getUser());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean update(AccountIntf account) throws RemoteException {
        try {
            Account upd = db.findAccountByName(account.getUserName(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        db.commit();
        return true;
    }

    @Override
    public boolean update(AccountIntf acc, String pass){
        Account account = null;
        try {
            account = db.findAccountByName(acc.getUserName(), false);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        account.getUser().setPassword(pass);
        db.commit();
        return true;
    }
    @Override

    public int login(String s) throws RemoteException {
        List accounts = db.findAccounts(s, false);
        System.out.println(Arrays.toString(accounts.toArray()));
        try{
            if (accounts.isEmpty()){
                Account acc = new Account(s);
                Naming.rebind("//localhost/Account", acc);
                db.persistAccount(acc);
                return 0;
            }else if(accounts.size() == 1){
                Naming.rebind("//localhost/Account", (Account) accounts.get(0));
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
    public boolean write(AccountIntf acc, String input){
        try {
            server.multiWrite(db.findAccountByName(acc.getUserName(), true).getUser(), input);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    //ADMIN
    @Override
    public String [] list (){
        java.lang.Object[] paths = server.list();
        String[] ret = new String[paths.length];
        int i = 0;
        for (java.lang.Object p : paths){
            ret[i] = p.toString();
            i++;
        }
        return ret;
    }

    @Override
    public String [] list (AccountIntf acc){
        Object[] paths = null;
        try {
            if(acc != null) {
                Account account = db.findAccountByName(acc.getUserName(), true);
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

    public boolean logout(AccountIntf acc){
        try {
            server.multiClose(db.findAccountByName(acc.getUserName(), true).getUser());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }
}