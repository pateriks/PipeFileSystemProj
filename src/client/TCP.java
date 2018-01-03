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
    protected LinkedList<String> que = new LinkedList<>();
    SocketChannel sC;
    Selector selector;
    String last = "null";
    LinkedList<String> buffer = new LinkedList<>();
    boolean done = false;

    protected void start (){
        channelSetup();
        que.push("hello");
        new Thread(this).start();
    }

    @Override
    public void run() {
        boolean send = true;
        while(send) {
            try {
                if (selector.select() > 0) {
                    Set set = selector.selectedKeys();
                    Iterator iterator = set.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = (SelectionKey)
                                iterator.next();
                        iterator.remove();
                        if (key.isConnectable()) {
                            processConnect();
                            System.out.println("process connect");
                        }
                        if (key.isReadable()) {
                            String msg = processRead(key);
                            if(msg.equals("resend")){
                                System.out.println("resend");
                                que.push(last);
                            }else {
                                if(que.peek() != null){
                                    que.poll();
                                }
                                String [] res = msg.split("#");
                                for(String s : res) {
                                    System.out.println("[Server]: " + s);
                                    if(s.equals("dOnE")){
                                        done = true;
                                    }
                                }
                                buffer.push(msg);
                            }
                        }
                        if (key.isWritable()) {
                            if (que.peek() != null) {
                                last = que.poll();
                                send = sendStringToServer(last, key);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!send) {
            try {
                sC.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("terminated successfully");
        } else {
            System.out.println("terminated without close");
        }
    }

    private boolean sendStringToServer(String s, SelectionKey key){
        if (s.equalsIgnoreCase("bye")) {
            return false;
        }
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.wrap(s.getBytes());
        try {
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void channelSetup(){
        try {
            InetAddress hostIP = InetAddress.getByName("localhost");
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
                Thread.sleep(1000);
                sC.close();
                selector.close();
                channelSetup();
                this.run();
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
        return null;
    }
}