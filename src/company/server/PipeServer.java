package company.server;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class PipeServer {

    public static final String MESSAGE = "Hello I am Pipe, how can I help you?";
    private static final String ROT = "root/";

    private HashMap<String, HashMap<String, OutputStream>> rootMap = new HashMap<>();
    private HashMap<Integer, Account> activeAcs = new HashMap<>();
    private HashMap <String, Integer> keys = new HashMap<>();

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
        rootMap.get(user.username).forEach(new BiConsumer<String, OutputStream>() {
            @Override
            public void accept(String s, OutputStream outputStream) {
                try {
                    outputStream.write(input.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
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

    protected OutputStream open(String s, User user) {
        OutputStream t = null;
        try {
            t = Files.newOutputStream(Paths.get(ROT.concat(s.concat(".txt"))));
            if(rootMap.containsKey(user.username)){
                rootMap.get(user.username).put(s, t);
            }else {
                HashMap<String, OutputStream> n = new HashMap<>();
                n.put(s, t);
                rootMap.put(user.username, n);
            }
            t.write("".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t;
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
            Naming.rebind("//localhost/PipeController", controller);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //////////////////////////////////////////////////////////
        System.out.println("Server bound and can now be accessed");
        //////////////////////////////////////////////////////////
    }
}
