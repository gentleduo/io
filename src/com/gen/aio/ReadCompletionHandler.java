package com.gen.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ReadCompletionHandler implements CompletionHandler<Integer, Object> {

	private AsynchronousSocketChannel clientChannel;

	public ReadCompletionHandler(AsynchronousSocketChannel channel) {
		this.clientChannel = channel;
	}

	/**
	 * AsynchronousSocketChannel调用I/O操作成功后的异步回调函数
	 * 此时数据已从内核空间拷贝到进程空间，一般会将数据存放在启动I/O操作时传入的附件参数:attachment中
	 * 
	 * @param result
	 *            I/O完成时的返回结果
	 * @param attachment
	 *            启动I/O操作时传入的附加参数
	 * 
	 */
	public void completed(Integer result, Object attachment) {

		InetSocketAddress address = null;
		try {
			address = (InetSocketAddress) clientChannel.getRemoteAddress();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("收到客户端:[" + address.getAddress() + ":" + address.getPort() + "]发来的数据");
		// 请求中前面request.getHeadLength()位表示表头，表头存放本次请求body中数据的byte数
		Request request = (Request) attachment;
		String type = request.getType();
		int receivedDataLen = request.getReceivedDataLen();
		// dataBuffer表示channel中每次I/O操作的缓存区大小
		ByteBuffer dataBuffer = request.getDataBuffer();
		int headLength = request.getHeadLength();
		byte[] headByteArray = request.getHeadByteArray();
		int bodyLength = request.getBodyLength();
		ByteBuffer bodyBuffer = request.getBodyBuffer();

		if ("read".equals(type)) {
			// 写完数据，需要开始读的时候，将postion复位到0，并将limit设为当前postion
			dataBuffer.flip();
			// hasRemaining() ---是否还有可用的空间
			// remaining() ---- 查看还有多少可用的空间
			// 由于可用空间的计算方式为:limit - position,因此写模式的时候表示查看还有多少可以写入的空间，读模式的时候表示未读数据的容量
			int remaining = dataBuffer.remaining();
			request.setReceivedDataLen(remaining + receivedDataLen);
			if (receivedDataLen == 0) {
				for (int i = CommonUtils.getActualLength(headByteArray), j = dataBuffer
						.position(); i < headByteArray.length && j < dataBuffer.limit(); i++, j++) {
					// 从buffer里读一个字节，并把postion移动一位。上限是limit，即写入数据的最后位置。
					headByteArray[i] = dataBuffer.get();
				}
				if (remaining >= headLength) {
					request.setBodyLength(CommonUtils.byteArrayToInt(headByteArray));
					request.setBodyBuffer(ByteBuffer.allocate(request.getBodyLength()));
					for (int i = dataBuffer.position(), j = 0; i < dataBuffer.limit()
							&& j < request.getBodyLength(); i++, j++) {
						// 写模式下，往buffer里写一个字节，并把postion移动一位。写模式下，一般limit与capacity相等。
						request.getBodyBuffer().put(dataBuffer.get());
					}
				}
				request.setType("read");
				// 读完数据，需要开始写的时候，将position置为0，limit设置为postion并不清除buffer内容。
				dataBuffer.clear();
				clientChannel.read(dataBuffer, request, this);
			} else {
				// 到上一次为止，收到的字节数少于包头的长度
				if (receivedDataLen < headLength) {
					for (int i = CommonUtils.getActualLength(headByteArray), j = dataBuffer
							.position(); i < headByteArray.length && j < dataBuffer.limit(); i++, j++) {
						headByteArray[i] = dataBuffer.get();
					}
					// 本次读取操作后，能获得完整的包头信息
					if (request.getReceivedDataLen() >= headLength) {
						request.setBodyLength(CommonUtils.byteArrayToInt(headByteArray));
						request.setBodyBuffer(ByteBuffer.allocate(request.getBodyLength()));
						for (int i = dataBuffer.position(), j = receivedDataLen - headLength; i < dataBuffer.limit()
								&& j < request.getBodyLength(); i++, j++) {
							request.getBodyBuffer().put(dataBuffer.get());
						}
						// 包体部分读取完成
						if (bodyLength == bodyBuffer.position()) {

							bodyBuffer.flip();
							byte[] bytes = new byte[bodyBuffer.remaining()];
							bodyBuffer.get(bytes);
							String body = "";
							try {
								body = new String(bytes, "UTF-8");
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
							System.out.println(body);
							request = new Request(4, ByteBuffer.allocate(8));
							dataBuffer = request.getDataBuffer();
							request.setType("write");
							dataBuffer.put("finished".getBytes());
							dataBuffer.flip();
							clientChannel.write(dataBuffer, request, this);
						} else {
							request.setType("read");
							dataBuffer.clear();
							clientChannel.read(dataBuffer, request, this);
						}
						// 本次读取操作后，未获得完整的包头信息
					} else {
						request.setType("read");
						dataBuffer.clear();
						clientChannel.read(dataBuffer, request, this);
					}
					// 已经读出包头
				} else {
					for (int i = dataBuffer.position(), j = receivedDataLen - headLength; i < dataBuffer.limit()
							&& j < bodyLength; i++, j++) {
						bodyBuffer.put(dataBuffer.get());
					}
					// 包体部分读取完成
					if (bodyLength == bodyBuffer.position()) {
						bodyBuffer.flip();
						byte[] bytes = new byte[bodyBuffer.remaining()];
						bodyBuffer.get(bytes);
						String body = "";
						try {
							body = new String(bytes, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						System.out.println(body);
						request = new Request(4, ByteBuffer.allocate(8));
						dataBuffer = request.getDataBuffer();
						request.setType("write");
						dataBuffer.put("finished".getBytes());
						dataBuffer.flip();
						clientChannel.write(dataBuffer, request, this);
					} else {
						request.setType("read");
						dataBuffer.clear();
						clientChannel.read(dataBuffer, request, this);
					}
				}
			}

		} else if ("write".equals(type)) {

			request.setType("read");
			dataBuffer.clear();
			clientChannel.read(dataBuffer, request, this);
		}
	}

	/**
	 * AsynchronousSocketChannel调用I/O操作失败后的异步回调函数
	 * 
	 * @param exc
	 *            I/O操作失败引发的异常或错误
	 * @param attachment
	 *            启动I/O操作时传入的附加参数
	 * 
	 */
	public void failed(Throwable exc, Object attachment) {
		// 处理错误
		try {
			this.clientChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
