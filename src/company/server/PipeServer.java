package company.server;


import company.controller.Controller;

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
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class PipeServer {
    public static final String MESSAGE = "Hello I am Pipe, how can I help you?";
    public String rot = "root/";
    private HashMap<String, HashMap<String, OutputStream>> rootMap = new HashMap<>();

    public String getMessage() {
        return MESSAGE;
    }

    public void multiClose(User user){

        rootMap.get(user).forEach(new BiConsumer<String, OutputStream>() {
            @Override
            public void accept(String s, OutputStream outputStream) {
                try {
                    if(outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void multiWrite(User user, String input){
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

    public Boolean mkDir(String s) {
        try {
            Path newDir = Files.createDirectory(Paths.get(s));
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }



    public Boolean delete (String path){
        try {
            File file = new File((Paths.get(rot.concat(path).concat(".txt")).toAbsolutePath().toString()));
            if(file.delete()){
                System.out.println("removed");
            }else{
                System.out.println("not removed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Boolean creat(String s) {
        OutputStream t = null;
        try {
            t = Files.newOutputStream(Paths.get(rot.concat(s.concat(".txt"))));
            t.write("".getBytes());
            t.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public OutputStream open(String s, User user) {
        OutputStream t = null;
        try {
            t = Files.newOutputStream(Paths.get(rot.concat(s.concat(".txt"))));
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

    public Boolean write(String s) {
        OutputStream t = null;
        try {
            t = Files.newOutputStream(Paths.get(rot.concat(s.concat(".txt"))));
            t.write("".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public Object[] list (String path){
        try {
            Stream stream = Files.list(Paths.get(rot.concat(path)));
            return stream.toArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object[] list (){
        try {
            Stream stream = Files.list(Paths.get(rot));
            return stream.toArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String args[]) throws Exception {
        Controller controller = new Controller();
        System.out.println("Pipe server started");

        try { //special exception handler for registry creation
            LocateRegistry.createRegistry(1099);

            System.out.println("java RMI registry created.");
        } catch (RemoteException e) {

            //do nothing, error means registry already exists
            System.out.println("java RMI registry already exists.");
        }
        //Instantiate PipeServer
        //Bind this object instance to the name "PipeController"
        try {
            Naming.rebind("//localhost/PipeController", controller);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        System.out.println("Server bound in registry");
    }
}
