package com.gen.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AsyncSocketServerHandler> {

	/**
	 * AsynchronousServerSocketChannel调用accept成功后的异步回调函数
	 * 
	 * @param result
	 *            连接成功时返回的AsynchronousSocketChannel对象
	 * @param attachment
	 *            发起连接操作时传入的附加参数。
	 * 
	 */
	public void completed(AsynchronousSocketChannel result, AsyncSocketServerHandler attachment) {
		
		if (attachment.asynchronousServerSocketChannel.isOpen()) {
			attachment.asynchronousServerSocketChannel.accept(attachment, this);
		}
		AsynchronousSocketChannel clientChannel = result;
		if (clientChannel != null && clientChannel.isOpen()) {
			InetSocketAddress address = null;
			try {
				address = (InetSocketAddress) clientChannel.getRemoteAddress();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.out.println("成功和客户端:[" + address.getAddress() + ":" + address.getPort() + "]建立连接");
			ReadCompletionHandler handler = new ReadCompletionHandler(clientChannel);
			Request request = new Request(4, ByteBuffer.allocate(8));
			request.setType("read");
			clientChannel.read(request.getDataBuffer(), request, handler);
		}
	}

	/**
	 * AsynchronousServerSocketChannel调用accept失败后的异步回调函数
	 * 
	 * @param exc
	 *            连接操作失败引发的异常或错误
	 * @param attachment
	 *            发起连接操作时传入的附加参数。
	 * 
	 */
	public void failed(Throwable exc, AsyncSocketServerHandler attachment) {
		exc.printStackTrace();
		attachment.latch.countDown();
	}
}