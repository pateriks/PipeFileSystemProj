package client;

import company.common.AccountIntf;
import company.common.ControllerIntf;
import company.server.Account;
import company.server.PipeServer;

import java.io.PrintWriter;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    Condition condition;
    LinkedList<String> handle= new LinkedList<>();
    SafeStandardOut out;
    ControllerIntf server;
    AccountIntf acc;
    Boolean bufferedWrite = false;
    boolean cNew = false;
    boolean graphics = false;
    String login = "please login";
    TCP connection;

    ServerTask(SafeStandardOut out, Condition c){
        id = new Random().nextInt(5);
        condition = c;
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

    public String add(String c, String a) throws Exception{
        if(Command.lockedMode){
            switch (c) {
                case "usr":
                    login(a);
                    login = "welcome";
                    return a;
                case "help":
                    help();
                    return a;
                case "new":
                    cNew = true;
                    return a;
                default:
                    args(c);
                    out.println(login);
                    if(a.equals("")){
                        return null;
                    }
                    args(a);
            }
        }else{
            switch (c) {
                case "help":
                    help();
                    return a;
                case "creat":
                    creat();
                    return a;
                case "open":
                    open(a);
                    return a;
                case "close":
                    close();
                    return a;
                case "write":
                    write();
                    return a;
                case "ls":
                    list();
                    return a;
                case "lot":
                    logout();
                    return a;
                case "rem":
                    delete();
                    return a;
                case "vi":
                    view(a);
                    return null;
                default:
                    args(c);
                    if(a.equals("")){
                        return null;
                    }
                    args(a);
            }
        }
        return null;
    }

    private void help (){
        out.println(Arrays.toString(Tasks.values()).replace(",", "\n"));
    }

    private void args(String s){
        handle.add(s);
    }

    private synchronized void login(String a) {
        try {
            InetAddress ip = null;
            try {
                ip = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            NetworkInterface network = null;
            try {
                network = NetworkInterface.getByInetAddress(ip);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            byte [] hw = new byte[0];
            try {
                hw = network.getHardwareAddress();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            int secret = 0;
            for(Byte b : hw) {
                secret += b.intValue();
            }
            out.println("" + secret);
            String user = a;
            int response = server.login(user, secret);

            switch (response){
                case 1:
                    if(cNew){
                        out.println("account exists");
                        break;
                    }
                    out.println("welcome " + user);
                    out.println("please enter your password");
                    for(int i = 0; i < 2; i++) {
                        String password = new Scanner(System.in).nextLine();
                        if ((acc = server.verifyUser(user, password)) != null) {
                            Command.user = acc.getName();
                            break;
                        } else {
                            out.println("wrong password please try again");
                        }
                    }
                    break;
                case 0:
                    out.println("welome new user " + user);
                    out.println("enter a password to use ");
                    String password = new Scanner(System.in).nextLine();
                    server.update(user, password);
                    acc = server.verifyUser(user, password);
                    Command.user = acc.getName();
                    break;
                case 2:
                    out.println("dont worry but you are not allowed to login");
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

    private void view(String path){
        connection = new TCP();
        connection.que.push(path);
        graphics = false;
        try {
            server.view(path);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        out.println(path);
        return;
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

    private void open(String a){
        try {
            String path = a;
            RmiClient.acc = server.open(RmiClient.acc, path);
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
            if(RmiClient.acc != null) {
                server.logout(RmiClient.acc);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Command.user =  "";
        Command.lockedMode = true;
    }

    private void render(){
        int HEIGHT = (int) Math.pow(2, 4);
        int WIDTH = (int) Math.pow(2, 7);
        out.println("");
        for(int j = 0; j < HEIGHT; j++){
            for(int i = 0; i < WIDTH; i++){
                out.print("x");
            }
            out.println("");
        }
    }

    protected Object compute(){
        //TODO: Long-running async tasks can go here
        Object ret = null;
        if(bufferedWrite) {
            /*try {
                condition.await(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            while (handle.peek() != null) {
                try {
                    server.write(RmiClient.acc, handle.poll());
                    server.write(RmiClient.acc, " ");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            bufferedWrite = false;
        }
        if(Command.lockedMode) {
            ret = acc;
        }
        if(graphics){
            render();
        }
        if(connection != null){
            connection.start();
            ret = getString(connection);
        }
        return ret;
    }
    private static String getString(TCP connection){
        String ret = null;
        StringBuilder sb = new StringBuilder();
        try {
            while (connection.open()) {
                sb = new StringBuilder();
                String append = connection.read();
                if(append==null){
                    throw new NullPointerException("not ready");
                }
                sb.append(append);
            }
            ret = sb.toString();
        }catch (NullPointerException e){
            e.printStackTrace();
            getString(connection);
        }
        ret = sb.toString();
        return ret;
    }
}
