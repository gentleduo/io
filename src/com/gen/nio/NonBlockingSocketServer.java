package com.gen.nio;

import java.io.IOException;

public class NonBlockingSocketServer {
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		int port = 8080;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				// 采用默认值
			}
		}
		NonBlockingSocketServerHandler timeServer = new NonBlockingSocketServerHandler(port);
		new Thread(timeServer, "NIO-NonBlockingSocketServerHandler-001").start();
	}
}