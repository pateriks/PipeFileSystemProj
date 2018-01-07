package company.server;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class PipeServer {
    private static final String HOST = "192.168.0.16";
    private HashMap<String, HashMap<String, OutputStream>> rootMap = new HashMap<>();
    private HashMap<Integer, Account> activeAcs = new HashMap<>();
    private HashMap <String, Integer> keys = new HashMap<>();
    private HashMap <Integer, String> msgs = new HashMap<>();

    protected static final String MESSAGE = "Hello I am Pipe, how can I help you?";
    protected static final String ROT = "root/";

    public String getMessage() {
        return MESSAGE;
    }

    protected void multiClose(User user){
        if(user != null) {
            if (rootMap.containsKey(user.username)) {
                rootMap.get(user.username).forEach(new BiConsumer<String, OutputStream>() {
                    @Override
                    public void accept(String s, OutputStream outputStream) {
                        try {
                            if (outputStream != null) {
                                outputStream.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    protected void multiWrite(User user, String input){
        rootMap.get(user.username).forEach((s, outputStream) -> {
            try {
                outputStream.write(input.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    protected void multiDelete(User user){
        rootMap.get(user.username).forEach(new BiConsumer<String, OutputStream>() {
            @Override
            public void accept(String s, OutputStream outputStream) {
                delete(s);
            }
        });
    }

    protected Boolean mkDir(String s) {
        try {
            Path newDir = Files.createDirectory(Paths.get(s));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    protected Boolean mkRoot() {
        try {
            Path newDir = Files.createDirectory(Paths.get(ROT));
        } catch (Exception e){
            return false;
        }
        return true;
    }

    protected Boolean delete (String path){
        try {
            File file = new File((Paths.get(ROT.concat(path).concat(".txt")).toAbsolutePath().toString()));
            if(file.delete()){
                //////////////////////////////
                System.out.println("removed");
                //////////////////////////////
            }else{
                //////////////////////////////////
                System.out.println("not removed");
                //////////////////////////////////
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected Boolean creat(String s) {
        OutputStream t = null;
        try {
            t = Files.newOutputStream(Paths.get(ROT.concat(s.concat(".txt"))));
            t.write("".getBytes());
            t.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    protected boolean open(Item item, Account acc){
        User user = acc.getUser();
        OutputStream t = null;
        String s = item.path;
        addOpenFile(user, s);
        return true;
    }
    protected int open(String s, Account acc) {
        int ret = 0;
        acc.getItem();
        User user = acc.getUser();
        OutputStream t = null;
        Item item;
        if((item = getItem(acc, s)) != null) {
            //Are account the haser
            addOpenFile(user, s);
            ret = 1;
        }else if((item = getItem(s)) != null){
            if(item.getPermissions().equals("public")){
                System.out.println("permission allowed");
                addOpenFile(user, s);
            }else {
                System.out.println("permission not allowed");
            }
            ret = 2;
        }else{
            //Just make a new Item if not exists in db
        }
        return ret;
    }

    protected Boolean write(String s) {
        OutputStream t = null;
        try {
            t = Files.newOutputStream(Paths.get(ROT.concat(s.concat(".txt"))));
            t.write("".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    protected Object[] list (String path){
        try {
            Stream stream = Files.list(Paths.get(ROT.concat(path)));
            return stream.toArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Object[] list (){
        try {
            Stream stream = Files.list(Paths.get(ROT));
            return stream.toArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void addActiveAcc(int key, Account acc){
        activeAcs.put(key, acc);
        keys.put(acc.getUser().name, key);
    }

    protected Account getAccount(String user){
        return activeAcs.get(keys.get(user));
    }

    protected void dispInfo(){
        activeAcs.forEach(new BiConsumer<Integer, Account>() {
            @Override
            public void accept(Integer mac, Account acc) {
                System.out.println("MAC: " + mac + " Account " + acc.toString());
            }
        });
    }

    protected void view(Account acc, String path) {
        if (checkAccess(acc, path)) {
            System.out.println("view ok");
            TCP send = new TCP();
            try {
                send.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            path = path.concat(".txt");
            send.que.push(path);
        }
    }

    private void addOpenFile(User user, String s){
        OutputStream t;
        try {
            t = Files.newOutputStream(Paths.get(ROT.concat(s.concat(".txt"))));
            if (rootMap.containsKey(user.username)) {
                rootMap.get(user.username).put(s, t);
            } else {
                HashMap<String, OutputStream> n = new HashMap<>();
                n.put(s, t);
                rootMap.put(user.username, n);
            }
            t.write("".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkAccess(Account acc, String path){
        Iterator<Item> it = acc.getItem().iterator();
        boolean ok = false;
        while (it.hasNext()) {
            Item item = it.next();
            if (item.path.equals(path)) {

                ok = true;
            }
        }
        return ok;
    }

    private Item getItem(Account acc, String path){
        Iterator<Item> it = acc.getItem().iterator();
        Item item = null;
        boolean ok = false;
        while (it.hasNext()) {
            item = it.next();
            if (item.path.equals(path)) {
                ok = true;
            }
        }
        if(ok) {
            return item;
        }else{
            return null;
        }
    }

    private Item getItem(String path){
        Item item = null;
        boolean ok = false;
        Collection<Account> c = activeAcs.values();
        Iterator<Account> iterator = c.iterator();
        while(iterator.hasNext()) {
            Account acc = iterator.next();
            Iterator<Item> it;
            if(acc.getItem() != null) {
                it = acc.getItem().iterator();
                ok = false;
                while (it.hasNext()) {
                    item = it.next();
                    if (item.path.equals(path)) {
                        ok = true;
                        break;
                    }
                }
            }
            if(ok){
                int mac = keys.get(acc.getUser().name);
                break;
            }
        }
        if(ok) {
            return item;
        }else{
            return null;
        }
    }

    public static void main(String args[]) throws Exception {
        Controller controller = new Controller();
        controller.init(new PipeServer());
        /////////////////////////////////////////
        System.out.println("Server starting...");
        /////////////////////////////////////////
        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(1099);
            ///////////////////////////////////////////////////
            System.out.println("Registry at port 1099 created");
            ///////////////////////////////////////////////////
        } catch (RemoteException e) {
            //do nothing, error means registry already exists
            ///////////////////////////////////////////////
            System.out.println("Could not create registry");
            ///////////////////////////////////////////////
        }
        //Bind controller instance to the name "PipeController"
        try {
            Naming.rebind("//".concat(HOST).concat("/PipeController"), controller);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //////////////////////////////////////////////////////////
        System.out.println("Server bound and can now be accessed");
        //////////////////////////////////////////////////////////
    }
}
