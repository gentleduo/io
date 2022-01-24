package org.duo.bio;

import java.io.*;
import java.net.Socket;

public class SocketClient {

    public static void main(String[] args) {

        try {
            Socket client = new Socket("192.168.56.112", 9090);

            client.setSendBufferSize(20);
            client.setTcpNoDelay(false);
            OutputStream out = client.getOutputStream();

            InputStream in = System.in;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    byte[] bb = line.getBytes();
                    for (byte b : bb) {
                        out.write(b);
                    }
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }
}
