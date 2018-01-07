package company.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Ger användbara metoder för klienten
 */
public interface AccountIntf extends Remote{
    String getName() throws RemoteException;
    boolean verifyUser(String password) throws RemoteException;
    long getId() throws RemoteException;
}
