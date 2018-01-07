package company.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Ger användbara metoder för klienten
 */
public interface ControllerIntf extends Remote {
    String getMessage()                                 throws RemoteException;
    String getDir()                                     throws RemoteException;
    String mkDir()                                      throws RemoteException;
    boolean creat(String path)                          throws RemoteException;
    boolean creat()                                     throws RemoteException;
    boolean update(AccountIntf acc, String pass)        throws RemoteException;
    boolean update(String user, String pass)            throws RemoteException;
    int login(String s, int mac)                        throws RemoteException;
    AccountIntf verifyUser(String s, String pass)       throws RemoteException;
    AccountIntf open(AccountIntf acc, String fullPath)  throws RemoteException;
    boolean close(AccountIntf acc)                      throws RemoteException;
    boolean write(AccountIntf acc, String s)            throws RemoteException;
    String[] list(AccountIntf acc)                      throws RemoteException;
    boolean delete(AccountIntf acc)                     throws RemoteException;
    boolean delete(String path)                         throws RemoteException;
    boolean logout(AccountIntf acc)                     throws RemoteException;
    boolean view(AccountIntf acc, String path)          throws RemoteException;
}