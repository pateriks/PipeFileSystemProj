package company.server;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class HandlerTCP implements Runnable {

    private LinkedList <String> args = new <String> LinkedList();
    private StringBuilder hidden;
    private SocketChannel sC;
    private Map<Integer, ForkJoinTask<Integer>> pendingTasks;
    String fromClient;
    
    public HandlerTCP(SocketChannel k, Map t, String f){
        System.out.println("handler created");
        sC = k;
        pendingTasks = t;
        fromClient = f;
    }

    public HandlerTCP(SocketChannel k){
        System.out.println("handler created");
        sC = k;
    }

    public void run(){
        System.out.println("computing");
        String ret;
        if(fromClient == null || fromClient.equals("null")){
            ret = getResponse("no request string");
        }else if(fromClient.equals("resend")){
            ret = "resend";
        }else {
            ret = getResponse(fromClient);
        }
        if(!ret.equals("no send")) {
            ByteBuffer buffer = ByteBuffer.wrap(ret.getBytes());
            try {
                System.out.println(ret);
                sC.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("finished");
        }
    }
    

    
    private String sendNotImpl(){
        return "not implemented";
    }
    
    private String sendString(){
        try {
            return readFromFile(fromClient).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private boolean isCommand(String s) {
        return (s.length()>0);
    }
    
    private boolean isLetter(String s) {
        return (s.length()==1);
    }

    private byte[] readFile(String filePathRelativeToRootDir) throws IOException {
        File file = new File(new File(PipeServer.ROT), filePathRelativeToRootDir);
        try (FileInputStream fromFile = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            fromFile.read(buf);
            return buf;
        }catch (Exception e){
            System.out.println(e);

        }
        return null;
    }

    private StringBuilder readFromFile(String s) throws FileNotFoundException{
        Scanner sc = new Scanner(new File(s));
        StringBuilder words = new StringBuilder();
        while(sc.hasNextLine()){
            String word = sc.nextLine();
            words.append("\n");
            words.append(word);
        }
        return words;
    }
    
    public String getResponse(String s) {
        return "";
    }
}
