package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class TCP implements Runnable{
    private long idling = 0;
    protected LinkedList<String> que = new LinkedList<>();
    SocketChannel sC;
    Selector selector;
    String last = "null";
    LinkedList<String> buffer = new LinkedList<>();
    boolean done = false;
    public boolean send = true;

    protected void start (){
        channelSetup();
        new Thread(this).start();
    }

    @Override
    public void run() {
        //System.out.println("ready to work");
        boolean once = true;
        while(send) {
            try {
                if (selector.select() > 0) {
                    Set set = selector.selectedKeys();
                    Iterator iterator = set.iterator();
                    while (iterator.hasNext()) {
                        //System.out.println("iterate");
                        SelectionKey key = (SelectionKey) iterator.next();
                        iterator.remove();
                        if (key.isConnectable()) {
                            processConnect();
                            //System.out.println("process connect");
                        }
                        if (key.isReadable()) {
                            String msg = processRead(key);

                            if(msg.equals("resend")){
                                que.push(last);
                            }else {
                                if(que.peek() != null){
                                    que.poll();
                                }
                                String [] res = msg.split("#");
                                for(String s : res) {
                                    //System.out.println("[Server]: " + s);
                                    if (s.equals("bye")) {
                                        done = true;
                                        //System.out.println("closing");
                                        que.push("bye");
                                    }else{
                                        buffer.push(msg);
                                    }
                                }
                            }
                        }
                        if (key.isWritable()) {
                            if (que.peek() != null) {
                                last = que.poll();
                                //System.out.println(last);
                                send = sendStringToServer(last, key);
                            }
                            //System.out.println("process write");
                        }
                        if(!send){
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!send) {
            try {
                selector.close();
                sC.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //System.out.println("terminated successfully");
        } else {
            //System.out.println("terminated without close");
        }
    }

    private boolean sendStringToServer(String s, SelectionKey key){
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        try {
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (s.equalsIgnoreCase("bye")) {
            done = true;
            return false;
        }
        return true;
    }

    private void channelSetup(){
        try {
            InetAddress hostIP = InetAddress.getByName(RmiClient.HOST);
            selector = Selector.open();
            sC = SocketChannel.open();
            sC.configureBlocking(false);
            sC.connect(new InetSocketAddress(hostIP, 8888));
            int operations = SelectionKey.OP_CONNECT|SelectionKey.OP_READ|SelectionKey.OP_WRITE;
            sC.register(selector, operations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processConnect(){
        try {
            sC.finishConnect();
        } catch (IOException e) {
            try {
                sC.close();
                selector.close();
                System.out.println("idle + " + Thread.currentThread().toString());
                idling++;
                if(!(idling>2)) {
                    Thread.sleep(10);
                    channelSetup();
                    this.run();
                }else{
                    send = false;
                }
            }catch (Exception e1){
            }

        }
    }

    public static String processRead(SelectionKey key) {
        SocketChannel sChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            sChannel.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.flip();
        Charset charset = Charset.forName("UTF-8");
        CharsetDecoder decoder = charset.newDecoder();
        CharBuffer charBuffer = null;
        try {
            charBuffer = decoder.decode(buffer);
        } catch (CharacterCodingException e) {
            e.printStackTrace();
        }
        String msg = charBuffer.toString();
        return msg;
    }

    public boolean open(){
        return !done;
    }

    public String read(){
        if(buffer.peek() != null) {
            return buffer.poll();
        }
        return "";
    }
}