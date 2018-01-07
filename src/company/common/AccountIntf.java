package company.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AccountIntf extends Remote{
    String getName() throws RemoteException;
    boolean verifyUser(String password) throws RemoteException;
    long getId() throws RemoteException;
}
