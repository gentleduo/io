package com.gen.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class AsyncSocketClientHandler implements CompletionHandler<Void, AsyncSocketClientHandler>, Runnable {

	private AsynchronousSocketChannel client;
	private String host;
	private int port;
	private CountDownLatch latch;

	public AsyncSocketClientHandler(String host, int port) {
		this.host = host;
		this.port = port;
		try {
			client = AsynchronousSocketChannel.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		latch = new CountDownLatch(1);
		client.connect(new InetSocketAddress(host, port), this, this);
		try {
			latch.await();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			// The documentation on close says that it causes AsynchronousCloseException or
			// ClosedChannelException on the other side.
			// To cause completed(-1) the client should call shutdownInput.
			// However, I would treat AsynchronousCloseException and ClosedChannelException
			// as normal shutdown, along with completed(-1).
			// close上的文档说它在另一端导致 AsynchronousCloseException 或 ClosedChannelException
			// 要导致完成（-1），客户端应该调用 shutdownInput。
			// 但是，我会将 AsynchronousCloseException 和 ClosedChannelException 视为正常关闭，以及
			// Completed(-1)。
			client.shutdownInput();
			// client.shutdownOutput();
			// client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void completed(Void result, AsyncSocketClientHandler attachment) {

		String requestStr = "name:1张1三1,age:18,sex:1男1,over";
		Map<String, Object> info = new HashMap();

		ByteBuffer sendBuffer = null;
		try {
			sendBuffer = ByteBuffer.wrap(requestStr.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		int bodyLength = 0;
		try {
			bodyLength = requestStr.getBytes("utf-8").length;
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		ByteBuffer writeBuffer = ByteBuffer.allocate(8);
		writeBuffer.put(CommonUtils.intToBytes(bodyLength));
		for (int i = writeBuffer.position(), j = sendBuffer.position(); i < writeBuffer.capacity()
				&& j < sendBuffer.limit(); i++, j++) {
			writeBuffer.put(sendBuffer.get());
		}

		writeBuffer.flip();
		info.put("write", writeBuffer);
		info.put("send", sendBuffer);

		client.write(writeBuffer, info, new CompletionHandler<Integer, Object>() {
			@Override
			public void completed(Integer result, Object attachment) {
				Map<String, Object> info = (Map<String, Object>) attachment;
				ByteBuffer writeBuffer = (ByteBuffer) info.get("write");
				ByteBuffer sendBuffer = (ByteBuffer) info.get("send");
				if (sendBuffer.hasRemaining()) {
					writeBuffer.clear();
					for (int i = 0, j = sendBuffer.position(); i < writeBuffer.capacity()
							&& j < sendBuffer.limit(); i++, j++) {
						writeBuffer.put(sendBuffer.get());
					}
					writeBuffer.flip();
					client.write(writeBuffer, info, this);
				} else {
					ByteBuffer readBuffer = ByteBuffer.allocate(1024);
					client.read(readBuffer, readBuffer, new CompletionHandler<Integer, ByteBuffer>() {
						@Override
						public void completed(Integer result, ByteBuffer buffer) {
							buffer.flip();
							byte[] bytes = new byte[buffer.remaining()];
							buffer.get(bytes);
							String body;
							try {
								body = new String(bytes, "UTF-8");
								System.out.println("Now is : " + body);
								latch.countDown();
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void failed(Throwable exc, ByteBuffer attachment) {
							try {
								client.shutdownInput();
								// client.shutdownOutput();
								// client.close();
								latch.countDown();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
				}
			}

			@Override
			public void failed(Throwable exc, Object attachment) {
				try {
					client.shutdownInput();
					// client.shutdownOutput();
					// client.close();
					latch.countDown();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void failed(Throwable exc, AsyncSocketClientHandler attachment) {
		exc.printStackTrace();
		try {
			client.shutdownInput();
			// client.shutdownOutput();
			// client.close();
			latch.countDown();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
