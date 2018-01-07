package company.server;

import company.common.AccountIntf;
import company.common.ControllerIntf;

import org.hibernate.collection.internal.PersistentSet;

import javax.persistence.RollbackException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Controller  extends UnicastRemoteObject implements ControllerIntf {
    //Database object
    DataDAO db = new DataDAO();
    //Core object
    PipeServer core = null;

    /**
     * Konstruktor
     * Av kontrollerns natur behövs inga initialiseringar
     * Vid skapandet av kontroller skapas Databas objekt vilket är en väldigt stor enhet
     * @throws RemoteException
     */
    protected Controller() throws RemoteException {
        super();
    }

    /**
     * Admin previligerad metod
     * @return
     */
    private String [] list (){
        java.lang.Object[] paths = core.list();
        String[] ret = new String[paths.length];
        int i = 0;
        for (java.lang.Object p : paths){
            ret[i] = p.toString();
            i++;
        }
        return ret;
    }

    /**
     * En nödvändig initialisering för att kontrollern ska fungera
     * @param server
     */
    protected void init(PipeServer server){
        //Vem är det som har kontroll
        this.core = server;
    }

    /**
     * Ger info om kärnan
     * @return
     * @throws RemoteException
     */
    @Override
    public String getMessage() throws RemoteException {
        return core.getMessage();
    }

    /**
     * Inte implementerad
     * @return
     * @throws RemoteException
     */
    @Override
    public String getDir() throws RemoteException {
        return null;
    }

    /**
     * Inte implementerad
     * @return
     * @throws RemoteException
     */
    @Override
    public String mkDir() throws RemoteException {
        return null;
    }

    /**
     * Används för att testa funktionalitet
     * @return
     * @throws RemoteException
     */
    public boolean creat() throws RemoteException {
        return core.creat("test");
    }

    /**
     * Skapa en ny fil till givet namn
     * @param name
     * @return
     * @throws RemoteException
     */
    @Override
    public boolean creat(String name) throws RemoteException {
        return core.creat(name);
    }

    /**
     * Raderar alla öppna filer kopplade till givet Account
     * @param acc
     * @return
     */
    @Override
    public boolean delete(AccountIntf acc){
        try {
            core.multiDelete(db.findAccountByName(acc.getId(), true).getUser());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Raderar fil med given path
     * @param path
     * @return
     * @throws RemoteException
     */
    @Override
    public boolean delete(String path) throws RemoteException {
        core.delete(path);
        return true;
    }

    /**
     * Använder satta rättigheter för en fil för att antingen ge tillåtelse att radera innehåll i filen
     * och ge möjlighet för giver Account att skriva nånting nytt till filen
     * @param acc
     * @param path
     * @return
     * @throws RemoteException
     */
    @Override
    public AccountIntf open(AccountIntf acc, String path) throws RemoteException {
        Item it = new Item(path);
        Thread t = new Thread(()->{
            db.persistItem(it);
        });
        Account account = db.findAccountByName(acc.getId(), false);
        int lean = core.open(path, account);
        System.out.println("Lean: " + lean);
        if(lean == 0) {
            Item item = null;
            try {
                //search in db
                Account account1 = db.searchItem(path);
                if(account1 == null){
                    throw new NullPointerException("no account");
                }
                System.out.println("This item belongs to = " + account1.getUser().name);
                Collection<Item> set = account1.getItem();
                Iterator<Item> ite = set.iterator();
                while (ite.hasNext()){
                    item = ite.next();
                    if(item.path.equals(path)){
                        break;
                    }
                }
                if(item == null){
                    throw new NullPointerException("no item");
                }else{
                    if(item.getPermissions().equals("public")) {
                        core.open(item, account);
                    }else{
                        return null;
                    }
                }
                //throw new NullPointerException("hej remove later");
            }catch (NullPointerException e){
                System.out.println("catched");
                e.printStackTrace();
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e1) {
                    //e1.printStackTrace();
                }
                account = db.findAccountByName(acc.getId(), false);
                Set h = account.getItem();
                if (h == null) {
                    h = new PersistentSet();
                    h.add(it);
                }else{
                    h.add(it);
                }

                account.setItem(h);
                db.commit();
                core.open(it, account);
            }catch (RollbackException e){
                //e.printStackTrace();
            }
        }else if(lean == 2){
            //everything is fine
        }
        return account;
    }

    /**
     * Updaterar lösenordet till ett användarnamn
     * Förutsätter att ancändaren vid ett tidigare skede har varit inloggad
     * @param user
     * @param pass
     * @return
     */
    @Override
    public boolean update(String user, String pass){
        Account account = null;
        account = core.getAccount(user);
        account.getUser().setPassword(pass);
        update(account, pass);
        return true;
    }

    /**
     * Updaterar lösenordet till en användare och gör lösenordet persistent
     * metoden kräver ett AccounrIntf som argument för att försäkra om behörighet
     * @param user
     * @param pass
     * @return
     */
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
        core.dispInfo();
        return true;
    }

    /**
     * Aktualiserar ett inloggningsförsök
     * Metoden är bunden till klienten som loggar in
     * @param s
     * @param mac
     * @return
     * @throws RemoteException
     */
    @Override
    public int login(String s, int mac) throws RemoteException {
        List accounts = db.findAccounts(s, true);
        System.out.println(Arrays.toString(accounts.toArray()));
        try{
            if (accounts.isEmpty()){
                Account acc = new Account(s);
                core.addActiveAcc(mac, acc);
                db.persistAccount(acc);
                return 0;
            }else if(accounts.size() == 1){
                core.addActiveAcc(mac, (Account) accounts.get(0));
                return 1;
            }else{
                return 2;
            }
        }catch(Exception e){
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Verifierar användarnamn till givert lösenord för att de åtkomst till objektet AccountIntf
     * @param user
     * @param pass
     * @return
     * @throws RemoteException
     */
    @Override
    public AccountIntf verifyUser(String user, String pass) throws RemoteException {
        Account loginAcc = core.getAccount(user);
        if(loginAcc.verifyUser(pass)){
            return (AccountIntf) loginAcc;
        }
        return null;
    }

    /**
     * Skriver input till öppna filer till giver account
     * @param acc
     * @param input
     * @return
     */
    @Override
    public boolean write(AccountIntf acc, String input){
        try {
            core.multiWrite(db.findAccountByName(acc.getId(), true).getUser(), input);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Söker filer kopplade till giver account
     * @param acc
     * @return
     */
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

    /**
     * Ser till att lokalt hanterad data inte längre är aktuell
     * @param acc
     * @return
     */
    @Override
    public boolean logout(AccountIntf acc){
        try {
            core.multiClose(db.findAccountByName(acc.getId(), true).getUser());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Hämtar account från databasen och ber kärnan att leverera filen med associerad path
     * @param acc
     * @param path
     * @return
     * @throws RemoteException
     */
    @Override
    public boolean view(AccountIntf acc, String path) throws RemoteException {
        Account account = db.findAccountByName(acc.getId(), true);
        core.view(account, path);
        return false;
    }

    /**
     * Stänger alla filer öppna i accountet OBS går ej att använda om samma account är inloggad
     * på flera ställen på samma maskin
     * @param acc
     * @return
     */
    public boolean close(AccountIntf acc){
        try {
            core.multiClose(db.findAccountByName(acc.getId(), true).getUser());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return true;
    }
}
