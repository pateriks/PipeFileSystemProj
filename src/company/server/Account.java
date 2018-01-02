package company.server;

import company.common.AccountIntf;

import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;
import javax.persistence.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

@NamedQueries({
    @NamedQuery(
        name = "findAccountByName",
        query = "SELECT acct FROM Account acct WHERE acct.accountId LIKE :userName"
    ),
    @NamedQuery(
            name = "findAccounts",
            query = "SELECT acct FROM Account acct WHERE acct.user.name LIKE :userName"
    )
})

@Entity(name = "Account")
public class Account extends UnicastRemoteObject implements AccountIntf {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long accountId;

    @OneToOne@MapsId
    private User user;

    @OneToMany@MapsId
    private Set<Item> item;
    //Mandatory non argument construct used outside of package
    protected Account() throws RemoteException {
        super();
        user = null;
    }
    //Construct used in package
    protected Account(String s) throws RemoteException {
        super();
        user = new User(s);
    }
    //Overrides can be accessed by user

    @Override
    public void getPassword(String email) throws RemoteException {
        return;
    }

    @Override
    public String getUserName() throws RemoteException {
        return user.username;
    }

    @Override
    public long getId() throws RemoteException {
        return accountId;
    }

    @Override
    public String getName() throws RemoteException {
        return user.name;
    }

    @Override
    public boolean hasUser() throws RemoteException {
        return (!user.password.equals(""));
    }

    @Override
    public boolean verifyUser(String password) throws RemoteException {
        return password.equals(this.user.password);
    }

    @Override
    public void setUser(String password) throws RemoteException {
        user.setPassword(password);
    }

    public User getUser(){
        return user;
    }

    public void setUser (User user){
        this.user = user;
    }

    public Set<Item> getItem() {
        return item;
    }

    public void setItem(Set<Item> item) {
        this.item = item;
    }
}
