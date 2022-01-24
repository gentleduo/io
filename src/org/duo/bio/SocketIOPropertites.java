package org.duo.bio;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.util.Arrays;

public class SocketIOPropertites {

    private static final int RECEIVE_BUFFER = 10;
    private static final int SO_TIMEOUT = 0;
    private static final boolean REUSE_ADDR = false;
    // 请求的连接数量非常大，资源不够时，允许排队的连接数
    private static final int BACK_LOG = 2;
    private static final boolean CLI_KEEPALIVE = false;
    // 是否优先发送数据进行试探
    private static final boolean CLI_OOB = false;
    // 通过ss -natp可以看到Recv-Q，Recv-Q和CLI_REC_BUF的关系？
    private static final int CLI_REC_BUF = 20;
    private static final boolean CLI_REUSE_ADDR = false;
    // 通过ss -natp可以看到Send-Q，Send-Q和CLI_SEND_BUF的关系？
    private static final int CLI_SEND_BUF = 20;
    private static final boolean CLI_LINGER = true;
    private static final int CLI_LINGER_N = 0;
    // 客户端读取数据的超时时间，0：表示阻塞等待
    private static final int CLI_TIMEOUT = 0;
    private static final boolean CLI_NO_DELAY = false;

//    StandardSocketOptions.TCP_NODELAY

    public static void main(String[] args) {
        ServerSocket server = null;
        try {
            server = new ServerSocket();
            server.bind(new InetSocketAddress(9090), BACK_LOG);
            server.setReceiveBufferSize(RECEIVE_BUFFER);
            server.setReuseAddress(REUSE_ADDR);
            server.setSoTimeout(SO_TIMEOUT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("server up use 9090!");

        while (true) {
            try {

                System.in.read();

                Socket client = server.accept();
                System.out.println("client port:" + client.getPort());

                client.setKeepAlive(CLI_KEEPALIVE);
                client.setOOBInline(CLI_OOB);
                client.setReceiveBufferSize(CLI_REC_BUF);
                client.setReuseAddress(CLI_REUSE_ADDR);
                client.setSendBufferSize(CLI_SEND_BUF);
                client.setSoLinger(CLI_LINGER, CLI_LINGER_N);
                client.setSoTimeout(CLI_TIMEOUT);
                client.setTcpNoDelay(CLI_NO_DELAY);

                new Thread(() -> {
                    while (true) {
                        try {
                            InputStream in = client.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                            char[] data = new char[1024];
                            int num = reader.read(data);

                            if (num > 0) {
                                System.out.println("client read some data is :" + num + " val :" + new String(data, 0, num));
                            } else if (num == 0) {
                                System.out.println("client read nothing!");
                                continue;
                            } else {
                                System.out.println("client read -1...");
                                client.close();
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
