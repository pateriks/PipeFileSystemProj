package company.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AccountIntf extends Remote{
    String getUserName() throws RemoteException;
    String getName() throws RemoteException;
    boolean verifyUser(String password) throws RemoteException;
    String getPassword(String email) throws RemoteException;
    boolean hasUser() throws RemoteException;
    void setUser(String password) throws RemoteException;
}
