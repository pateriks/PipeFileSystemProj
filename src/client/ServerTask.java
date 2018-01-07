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
    private boolean out = false;
    private ControllerIntf server;
    private AccountIntf account;
    private boolean bufferedWrite = false;
    private boolean cNew = false;
    private String login = "please login";
    private TCP connection;
    private NonBlockingInterpreter cmd;

    ServerTask(boolean out, NonBlockingInterpreter cmd){
        this.cmd = cmd;
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
        if(cmd.lockedMode){
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
                    println(login);
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
        println(Arrays.toString(Tasks.values()).replace(",", "\n"));
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
            println("" + secret);
            String user = a;
            int response = server.login(user, secret);

            switch (response){
                case 1:
                    if(cNew){
                        println("account exists");
                        break;
                    }
                    println("welcome " + user);
                    println("please enter your password");
                    for(int i = 0; i < 2; i++) {
                        String password = new Scanner(System.in).nextLine();
                        if ((account = server.verifyUser(user, password)) != null) {
                           login = "welcome";
                           cmd.lockedMode = false;
                           break;
                        } else {
                            println("wrong password please try again");
                        }
                    }
                    break;
                case 0:
                    println("welome new user " + user);
                    println("enter a password to use ");
                    String password = new Scanner(System.in).nextLine();
                    server.update(user, password);
                    account = server.verifyUser(user, password);
                    login = "welcome";
                    cmd.lockedMode = false;
                    break;
                case 2:
                    println("dont worry but you are not allowed to login");
                    break;
                case -1:
                    println("Exception");
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
        connection.setHost(HOST);
        try {
            server.view(RmiClient.account, path);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        println(path);
        return;
    }

    private void delete(){
        try {
            server.delete(RmiClient.account);
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
            AccountIntf acc = server.open(RmiClient.account, path);
            if(acc != null) {
                RmiClient.account = acc;
            }else{
                println("file exists under private access");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void close(){
        try {
            server.close(RmiClient.account);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void write(){
        bufferedWrite = true;
    }

    private void list(){
        try {
            String[] list = server.list(RmiClient.account);
            for(String s : list){
                println(s);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void logout(){
        try {
            if(RmiClient.account != null) {
                server.logout(RmiClient.account);
                RmiClient.account = null;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        cmd.lockedMode = true;
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

    /**
     * Kollar om interna utskrifter är tillåtna
     * @param s
     */
    private void println(String s){
        if(out){
            cmd.out.println(s);
        }
    }

    /**
     * Uppgifter som tar lång tid hanteras här
     * Returvärdet måste vara unikt för varje typ av uppgift dvs om det är en login uppgift så gers returvärdet
     * @AccountIntf
     * Om uppgiften inte returnerat inom en sekund tappas kontrollen över tråden
     * @return
     */
    protected Object compute(){
        //TODO: Long-running async tasks can go here
        //Default resultat
        Object res = null;
        //Skriva till en fil på servern
        if(bufferedWrite) {
            while (handle.peek() != null) {
                try {
                    server.write(RmiClient.account, handle.poll());
                    server.write(RmiClient.account, " ");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            bufferedWrite = false;
        //Login åtgärd som lyckats
        }if(login.equals("welcome")) {
            res = account;
        //TCP åtgärd returvärde String
        }if(connection != null){
            connection.start();
            println("getting content");
            res = getString(connection);
        }
        //Resultat returneras efter en lyckad bearbetning
        return res;
    }
}
