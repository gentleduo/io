package org.duo.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedList;

public class ServerSocketIO {

    public static void main(String[] args) throws IOException, InterruptedException {

        LinkedList<SocketChannel> clients = new LinkedList<>();

        ServerSocketChannel ss = ServerSocketChannel.open(); // listen socket
        ss.bind(new InetSocketAddress(9090));
        ss.configureBlocking(false); // 重点 OS NONBLOCKING

//        ss.setOption(StandardSocketOptions.TCP_NODELAY, false);

        while (true) {
//            Thread.sleep(1000);
            SocketChannel client = ss.accept();//连接socket
            if (client == null) {
//                System.out.println("null......");
            } else {
                client.configureBlocking(false); // 重点
                int port = client.socket().getPort();
                System.out.println("client port = " + port);
                clients.add(client);
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);

            for (SocketChannel c : clients) {
                int num = c.read(buffer); //非阻塞
                if (num > 0) {
                    buffer.flip();
                    byte[] aaa = new byte[buffer.limit()];
                    buffer.get(aaa);
                    String b = new String(aaa);
                    System.out.println(c.socket().getPort() + " : " + b);
                    buffer.clear();
                }
            }
        }
    }
}
