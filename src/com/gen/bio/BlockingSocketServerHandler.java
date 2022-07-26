package com.gen.bio;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class BlockingSocketServerHandler implements Runnable {

	private Socket socket;

	public BlockingSocketServerHandler(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {

		InputStream in = null;
		OutputStream out = null;

		try {
			// 下面我们收取信息
			in = socket.getInputStream();
			out = socket.getOutputStream();
			Integer sourcePort = socket.getPort();
			int maxLen = 2048;
			byte[] contextBytes = new byte[maxLen];
			// 这里也会被阻塞，直到有数据准备好
			int realLen = in.read(contextBytes, 0, maxLen);
			// 读取信息
			String message = new String(contextBytes, 0, realLen);
			// 下面打印信息
			System.out.println("服务器收到来自于端口：" + sourcePort + "的信息：" + message);
			// 下面开始发送信息
			out.write("回发响应信息！".getBytes());
			// 关闭
			out.close();
			in.close();
			socket.close();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			CommonUtils.close(in);
			CommonUtils.close(out);
			if (this.socket != null) {
				CommonUtils.close(this.socket);
			}
		}
	}
}