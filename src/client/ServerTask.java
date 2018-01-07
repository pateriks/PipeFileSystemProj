package client;

import company.common.AccountIntf;
import company.common.ControllerIntf;

import java.net.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.RecursiveTask;

public class ServerTask extends RecursiveTask {

    static String HOST = "localhost";

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



    private LinkedList<String> handle = new LinkedList<>();
    private SafeStandardOut out = null;
    private ControllerIntf server;
    private AccountIntf account;
    private boolean bufferedWrite = false;
    boolean cNew = false;
    private String login = "please login";
    private TCP connection;

    ServerTask(SafeStandardOut out){
        this.out = out;
        try {
            server = (ControllerIntf) Naming.lookup("//".concat(HOST).concat("/PipeController"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    ServerTask(){
        try {
            server = (ControllerIntf) Naming.lookup("//".concat(HOST).concat("/PipeController"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void setHost(String host){
        HOST = host;
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
                case "debug":
                    RmiClient.lock.lock();
                    RmiClient.complete.signal();
                    RmiClient.lock.unlock();
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
                        if ((account = server.verifyUser(user, password)) != null) {
                            Command.user = account.getName();
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
                    account = server.verifyUser(user, password);
                    Command.user = account.getName();
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
        try {
            server.view(RmiClient.acc, path);
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
            AccountIntf acc = server.open(RmiClient.acc, path);
            if(acc != null) {
                RmiClient.acc = acc;
            }else{
                out.println("file exists under private access");
            }
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
                RmiClient.acc = null;
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

    private static String getString(TCP connection){
        String ret = null;
        StringBuilder sb = new StringBuilder("");
        while (connection.open()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String append = connection.read();
            sb.append(append);
        }
        ret = sb.toString();
        return ret;
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
        }if(Command.lockedMode) {
            ret = account;
        }if(connection != null){
            connection.start();
            out.println("getting string");
            ret = getString(connection);
        }
        if(ret == null){
            ret = "nothing to do";
        }
        return ret;
    }
}
