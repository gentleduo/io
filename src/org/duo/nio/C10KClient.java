package org.duo.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class C10KClient {

    public static void main(String[] args) throws IOException {

        LinkedList<SocketChannel> clients = new LinkedList<>();
        InetSocketAddress serverAddr = new InetSocketAddress("192.168.56.112", 9090);
        for (int i = 10000; i < 11000; i++) {

            try {
                SocketChannel client = SocketChannel.open();
                client.bind(new InetSocketAddress("192.168.56.1", i));
                client.connect(serverAddr);
                boolean c1 = client.isOpen();
                clients.add(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("client's size = " + clients.size());
        System.in.read();
    }
}
