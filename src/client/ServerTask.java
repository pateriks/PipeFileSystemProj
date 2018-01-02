package client;

import company.common.AccountIntf;
import company.common.ControllerIntf;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.RecursiveTask;

public class ServerTask extends RecursiveTask {
    private final String HOST = "192.168.0.16";


    private enum Tasks {
        cd("cd - Change Directory (cd + a directory from current directory)"),
        ls("ls - List (ls to list items in current directory flags: -r)"),
        help("help"),
        usr("usr - Username (usr + username)"),
        vi("vi - Viewer (vi + file)");

        private final String txt;
        Tasks(String s){
            this.txt = s;
        }
        @Override
        public String toString(){
            return this.txt;
        }
    }

    int id;
    String sQuery;
    LinkedList<String> handle= new LinkedList<>();
    SafeStandardOut out;
    ControllerIntf server;
    AccountIntf acc;
    Boolean bufferedWrite = false;
    String login = "please login";

    ServerTask(String s, SafeStandardOut out){
        id = new Random().nextInt(5);
        sQuery = s;
        this.out = out;
        try {
            server = (ControllerIntf) Naming.lookup("//".concat(HOST).concat("/PipeController"));
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void add(String s) throws Exception{
        try {
            if(Command.lockedMode){
                switch (s) {
                    case "usr":
                        login();
                        login = "welcome";
                        break;
                    case "help":
                        help();
                        break;
                    default:
                        out.println(login);
                }
            }else{
                switch (s) {
                    case "usr":
                        login();
                        break;
                    case "help":
                        help();
                        break;
                    case "creat":
                        creat();
                        break;
                    case "open":
                        open();
                        break;
                    case "close":
                        close();
                        break;
                    case "write":
                        write();
                        break;
                    case "ls":
                        list();
                        break;
                    case "lot":
                        logout();
                        break;
                    case "rem":
                        delete();
                    default:
                        args(s);
                }
            }
        }catch (IllegalArgumentException e){
            out.println("Unhandled argument");
        }
    }

    private void help (){
        out.println(Arrays.toString(Tasks.values()).replace(",", "\n"));
    }

    private void args(String s){
        handle.add(s);
        //TODO: Think about the purpose of this method
    }

    private synchronized void login() {
        try {
            int response = server.login(sQuery.split(" ")[1]);
            switch (response){
                case 1:
                    try {
                        acc = (AccountIntf) Naming.lookup("//".concat(HOST).concat("/Account"));
                    } catch (NotBoundException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    out.println("welcome " + acc.getName());
                    out.println("please enter your password");
                    while(true) {
                        String password = new Scanner(System.in).nextLine();
                        if (acc.verifyUser(password)) {
                            Command.user = acc.getName();
                            break;
                        } else {
                            out.println("wrong password please try again");
                        }
                    }
                    break;
                case 0:
                    try {
                        acc = (AccountIntf) Naming.lookup("//".concat(HOST).concat("/Account"));
                    } catch (NotBoundException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    out.println("welome new user " + acc.getName());
                    out.println("enter a password to use ");
                    String password = new Scanner(System.in).nextLine();
                    server.update(acc, password);
                    Command.user = acc.getName();
                    break;
                case 2:
                    out.println("2");
                    break;
                case -1: out.println("Exception");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void change(){
        //TODO: Implement
    }

    private void delete(){
        try {
            server.delete(RmiClient.acc);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void creat(){
        try {
            server.creat();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void open(){
        try {
            String path = sQuery.split(" ")[1];
            RmiClient.acc = server.open(RmiClient.acc, path);
            bufferedWrite = true;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void close(){
        try {
            server.close(RmiClient.acc);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void write(){
        bufferedWrite = true;
    }

    private void list(){
        try {
            String[] list = server.list(RmiClient.acc);
            for(String s : list){
                out.println(s);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void logout(){
        try {
            Naming.unbind("//".concat(HOST).concat("/Account"));
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            server.logout(RmiClient.acc);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Command.user =  "";
        Command.lockedMode = true;
    }

    protected Object compute(){
        //TODO: Long-running async tasks can go here
        if(bufferedWrite) {
            while (handle.peek() != null) {
                try {
                    server.write(RmiClient.acc, handle.poll());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            bufferedWrite = false;
        }
        if(Command.lockedMode) {
            return acc;
        }
        return null;
    }
}
