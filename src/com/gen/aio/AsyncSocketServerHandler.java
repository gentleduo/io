package com.gen.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.CountDownLatch;

public class AsyncSocketServerHandler implements Runnable {

	private int port;

	CountDownLatch latch;
	AsynchronousServerSocketChannel asynchronousServerSocketChannel;

	public AsyncSocketServerHandler(int port) {
		this.port = port;
		try {
			asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
			asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
			System.out.println("The async socket server is start in port : " + port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		latch = new CountDownLatch(1);
		doAccept();
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/*
	 * AsynchronousServerSocketChannel创建成功后，类似于ServerSocket，也是调用accept()
	 * 方法来接受来自客户端的连接，由于异步IO实际的IO操作是交给操作系统来做的，用户进程只负责通知操作系统进行IO和接受操作系统IO完成的通知。
	 * 所以异步的ServerChannel调用accept()方法后，当前线程不会阻塞，程序也不知道accept()
	 * 方法什么时候能够接收到客户端请求并且操作系统完成网络IO，为解决这个问题，AIO为accept方法提供两个版本：
	 *
	 * Future<AsynchronousSocketChannel> accept()
	 * 开始接收客户端请求，如果当前线程需要进行网络IO（即获得AsynchronousSocketChannel），
	 * 则应该调用该方法返回的Future对象的get()方法，但是get方法会阻塞该线程，所以这种方式是阻塞式的异步IO。
	 *
	 * <A> void accept(A attachment, CompletionHandler<AsynchronousSocketChannel,?
	 * super A> handler) 开始接受来自客户端请求，连接成功或失败都会触发CompletionHandler对象的相应方法。
	 * 其中AsynchronousSocketChannel就代表该CompletionHandler处理器在处理连接成功时的result是AsynchronousSocketChannel的实例。
	 * 
	 * 而CompletionHandler接口中定义了两个方法：
	 * completed(V result , A attachment)：当连接成功时触发该方法，该方法的第一个参数表示连接成功时返回的AsynchronousSocketChannel对象，
	 * 第二个参数代表发起连接操作时传入的附加参数。 
	 * faild(Throwable exc, Aattachment)：当连接失败时触发该方法，第一个参数代表连接操作失败引发的异常或错误。
	 */
	public void doAccept() {
		asynchronousServerSocketChannel.accept(this, new AcceptCompletionHandler());
	}
}
