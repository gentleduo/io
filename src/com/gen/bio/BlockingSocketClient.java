package com.gen.bio;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class BlockingSocketClient {

	public static void main(String[] args) {

		Socket socket = null;
		OutputStream clientRequest = null;
		InputStream clientResponse = null;

		try {
			socket = new Socket("localhost", 8080);
			clientRequest = socket.getOutputStream();
			clientResponse = socket.getInputStream();

			Thread.sleep(15000);
			// 发送请求信息
			clientRequest.write("这是客户端的请求。".getBytes());
			clientRequest.flush();
			int maxLen = 1024;
			byte[] contextBytes = new byte[maxLen];
			int realLen;
			String message = "";
			// 程序执行到这里，会一直等待服务器返回信息（注意，前提是in和out都不能close，如果close了就收不到服务器的反馈了）
			while ((realLen = clientResponse.read(contextBytes, 0, maxLen)) != -1) {
				message += new String(contextBytes, 0, realLen);
			}
			System.out.println("接收到来自服务器的信息:" + message);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			CommonUtils.close(clientRequest);
			CommonUtils.close(clientResponse);
		}

	}
}
