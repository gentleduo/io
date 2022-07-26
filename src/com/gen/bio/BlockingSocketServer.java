package com.gen.bio;

import java.net.ServerSocket;
import java.net.Socket;

public class BlockingSocketServer {

	public static void main(String[] args) throws Exception {

		ServerSocket serverSocket = new ServerSocket(8080);
		try {
			while (true) {
				Socket socket = serverSocket.accept();
				// 当然业务处理过程可以交给一个线程（这里可以使用线程池）,并且线程的创建是很耗资源的。
				// 最终改变不了.accept()只能一个一个接受socket的情况,并且被阻塞的情况
				BlockingSocketServerHandler socketServerThread = new BlockingSocketServerHandler(socket);
				new Thread(socketServerThread).start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			CommonUtils.close(serverSocket);
		}

	}

}
