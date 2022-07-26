package com.gen.aio;

import java.io.IOException;

public class AsyncSocketServer {

	public static void main(String[] args) throws IOException {
		int port = 8080;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				// 采用默认值
			}
		}
		AsyncSocketServerHandler timeServer = new AsyncSocketServerHandler(port);
		new Thread(timeServer, "AIO-AsyncSocketServerHandler-001").start();
	}
}
