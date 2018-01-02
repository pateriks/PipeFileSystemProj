package company.common;

import com.sun.org.apache.regexp.internal.RE;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ControllerIntf extends Remote {
    String getMessage() throws RemoteException;
    String getDir() throws RemoteException;
    String mkDir() throws RemoteException;
    boolean update(AccountIntf account) throws RemoteException;
    boolean creat(String path) throws RemoteException;
    boolean creat() throws RemoteException;
    boolean update(AccountIntf acc, String pass) throws RemoteException;
    int login(String s) throws RemoteException;
    AccountIntf open(AccountIntf acc, String fullPath) throws RemoteException;
    boolean close(AccountIntf acc) throws RemoteException;
    boolean write(AccountIntf acc, String s) throws RemoteException;
    String[] list(AccountIntf acc) throws RemoteException;
    boolean delete(AccountIntf acc) throws RemoteException;
    boolean delete(String path) throws RemoteException;
    boolean logout(AccountIntf acc) throws RemoteException;
}