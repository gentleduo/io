package com.gen.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NonBlockingSocketServerHandler implements Runnable {

	private Selector selector;

	private ServerSocketChannel serverSocketChannel;

	/**
	 * 
	 * client通过SocketChannel多次传给服务器的信息通过SocketChannel的hashcode作为key，信息的stringbuffer作为value，存储到服务器端的一个内存区域MESSAGEHASHCONTEXT。
	 */
	private static final ConcurrentMap<Integer, StringBuffer> MESSAGEHASHCONTEXT = new ConcurrentHashMap<Integer, StringBuffer>();

	/**
	 * 初始化多路复用器、绑定监听端口
	 * 
	 * @param port
	 */
	public NonBlockingSocketServerHandler(int port) {
		try {
			selector = Selector.open();
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			// 第二个参数是，限制最大连接数。
			serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);
			// 注意、服务器通道只能注册SelectionKey.OP_ACCEPT事件
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("The nonblocking socket server is start in port : " + port);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				// 如果条件成立，说明本次询问selector，并没有获取到任何准备好的、感兴趣的事件
				// java程序对多路复用IO的支持也包括了阻塞模式 和非阻塞模式两种。
				if (selector.select(100) == 0) {
					// ================================================
					// 这里视业务情况，可以做一些然并卵的事情
					// ================================================
					continue;
				}
				// 这里就是本次询问操作系统，所获取到的“所关心的事件”的事件类型（每一个通道都是独立的）
				Iterator<SelectionKey> selecionKeys = selector.selectedKeys().iterator();

				while (selecionKeys.hasNext()) {
					SelectionKey readyKey = selecionKeys.next();
					// 这个已经处理的readyKey一定要移除。如果不移除，就会一直存在在selector.selectedKeys集合中
					// 待到下一次selector.select() > 0时，这个readyKey又会被处理一次
					selecionKeys.remove();

					SelectableChannel selectableChannel = readyKey.channel();
					if (readyKey.isValid() && readyKey.isAcceptable()) {
						System.out.println("======channel通道已经准备好=======");
						/*
						 * 当server socket channel通道已经准备好，就可以从server socket channel中获取socketchannel了
						 * 拿到socket channel后，要做的事情就是马上到selector注册这个socket channel感兴趣的事情。 否则无法监听到这个socket
						 * channel到达的数据
						 */
						ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectableChannel;
						SocketChannel socketChannel = serverSocketChannel.accept();
						registerSocketChannel(socketChannel, selector);

					} else if (readyKey.isValid() && readyKey.isConnectable()) {
						System.out.println("======socket channel 建立连接=======");
					} else if (readyKey.isValid() && readyKey.isReadable()) {
						System.out.println("======socket channel 数据准备完成，可以去读取=======");
						readSocketChannel(readyKey);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			CommonUtils.close(selector);
		}

	}

	/**
	 * 在ServerSocketChannel接收到一个新的 TCP请求并建立好连接后。 就会向程序返回一个新的SocketChannel。
	 * 但是这个新的SocketChannel并没有在selector“选择器/代理器”中注册，
	 * 所以程序还没法通过selector通知这个SocketChannel 的事件。
	 * 于是我们拿到新的SocketChannel后，要做的第一个事情就是到selector“选择器/代理器”中注册这个 SocketChannel感兴趣的事件
	 * 
	 * @param socketChannel
	 *            新的SocketChannel
	 * @param selector
	 *            选择器/代理器
	 * @throws Exception
	 */
	private void registerSocketChannel(SocketChannel socketChannel, Selector selector) throws Exception {
		socketChannel.configureBlocking(false);
		// SocketChannel通道可以且只可以注册三种事件SelectionKey.OP_READ | SelectionKey.OP_WRITE |
		// SelectionKey.OP_CONNECT
		// 最后一个参数attachment是为这个SocketChannel分配的缓存区，并存放在readyKey中
		socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(16));
	}

	/**
	 * 这个方法用于读取从客户端传来的信息。 并且通過特殊结束符确定是否完成传输。 如果传输完成，则返回一个true的标记。
	 * 
	 * @param socketChannel
	 * @throws Exception
	 */
	private void readSocketChannel(SelectionKey readyKey) throws Exception {
		SocketChannel clientSocketChannel = (SocketChannel) readyKey.channel();
		// 获取客户端的IP地址和使用的端口
		InetSocketAddress sourceSocketAddress = (InetSocketAddress) clientSocketChannel.getRemoteAddress();
		String ip = sourceSocketAddress.getHostName();
		Integer resoucePort = sourceSocketAddress.getPort();

		// 拿到这个SocketChannel使用的缓存区，准备读取数据
		ByteBuffer contextBytes = (ByteBuffer) readyKey.attachment();
		// 将通道的数据出来然后写入到缓存区。
		int realLen = 0;
		StringBuffer message = new StringBuffer();
		while ((realLen = clientSocketChannel.read(contextBytes)) != 0) {

			// 将buffer切换成读模式
			contextBytes.flip();
			int position = contextBytes.position();
			int capacity = contextBytes.capacity();
			byte[] messageBytes = new byte[capacity];
			contextBytes.get(messageBytes, position, realLen);

			// 这种方式也是可以读取数据的，而且不用关心position的位置。
			// 因为是目前contextBytes所有的数据全部转出为一个byte数组。
			// 使用这种方式时，一定要自己控制好读取的最终位置（realLen很重要）
			// byte[] messageBytes = contextBytes.array();

			String messageEncode = new String(messageBytes, 0, realLen, "UTF-8");
			message.append(messageEncode);

			// 再切换成“写”模式，直接情况缓存的方式，最快捷
			contextBytes.clear();
		}

		// 如果发现本次接收的信息中有over关键字，说明信息接收完了
		// if (URLDecoder.decode(message.toString(), "UTF-8").indexOf("over") != -1) {
		if (message.toString().indexOf("over") != -1) {
			// 则从messageHashContext中，取出之前已经收到的信息，组合成完整的信息
			Integer channelUUID = clientSocketChannel.hashCode();
			System.out.println("地址:" + ip + ",端口:" + resoucePort + "的客户端发来的信息======message : " + message);
			StringBuffer completeMessage;
			// 清空MESSAGEHASHCONTEXT中的历史记录
			StringBuffer historyMessage = MESSAGEHASHCONTEXT.remove(channelUUID);
			if (historyMessage == null) {
				completeMessage = message;
			} else {
				completeMessage = historyMessage.append(message);
			}
			System.out.println("地址:" + ip + ",端口:" + resoucePort + "的客户端发来的完整信息======completeMessage : "
					+ URLDecoder.decode(completeMessage.toString(), "UTF-8"));

			// ======================================================
			// 当然接受完成后，可以在这里正式处理业务了
			// ======================================================

			// 回发数据，并关闭channel
			// ByteBuffer sendBuffer = ByteBuffer.wrap(URLEncoder.encode("处理完成",
			// "UTF-8").getBytes());
			ByteBuffer sendBuffer = ByteBuffer.wrap("处理完成".getBytes("utf-8"));
			clientSocketChannel.write(sendBuffer);
			clientSocketChannel.close();
		} else {
			// 如果没有发现有“over”关键字，说明还没有接受完，则将本次接受到的信息存入messageHashContext
			System.out.println("地址:" + ip + ",端口:" + resoucePort + "客户端信息还未接受完，继续接受======message : "
					+ URLDecoder.decode(message.toString(), "UTF-8"));
			// 每一个channel对象都是独立的，所以可以使用对象的hash值，作为唯一标示
			Integer channelUUID = clientSocketChannel.hashCode();

			// 然后获取这个channel下以前已经达到的message信息
			StringBuffer historyMessage = MESSAGEHASHCONTEXT.get(channelUUID);
			if (historyMessage == null) {
				historyMessage = new StringBuffer();
				MESSAGEHASHCONTEXT.put(channelUUID, historyMessage.append(message));
			}
		}
	}
}