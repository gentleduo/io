package com.gen.aio;

public class AsyncSocketClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int port = 8080;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				// 采用默认值
			}

		}
		new Thread(new AsyncSocketClientHandler("127.0.0.1", port), "AIO-AsyncSocketClientHandler-001").start();

	}
}
