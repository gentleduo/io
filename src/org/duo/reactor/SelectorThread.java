package org.duo.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class SelectorThread implements Runnable {

    //每个线程对应一个selector，多线程情况下，并发客户端被分配到某一个selector上
    //注意：每个客户端只绑定一个selector
    Selector selector = null;
    LinkedBlockingQueue<Channel> linkedBlockingQueue = new LinkedBlockingQueue<>();

    SelectorThread() {
        try {
            selector = Selector.open();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Override
    public void run() {

        while (true) {
            try {
                /*
                如果当前线程执行select，没有事件到达，那么会进入阻塞状态
                而另外一个线程往相同的多路服务器中增加了对其他文件描述符的监听事件
                由于当前线程中只能监听执行select时刻的多路复用器中的文件描述符，无法监听之后由其他线程追加的文件描述符中的事件，因此有可能当前线程会进入永久阻塞的状态
                所以会有selector中会有wakeup方法，唤醒某个阻塞的文件描述符，并且返回值为0
                 */
                System.out.println(Thread.currentThread().getName() + " before select......" + selector.keys().size());
                int nums = selector.select(); //阻塞
                System.out.println(Thread.currentThread().getName() + " after select......" + selector.keys().size());
                // 处理selectedKeys
                if (nums > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keys.iterator();
                    // 同一文件描述符的R/W一定是在同一线程中处理
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        // 多线程中：新的客户端注册在哪里呢？
                        if (key.isAcceptable()) {
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);
                        } else if (key.isWritable()) {
                            writeHandler(key);
                        }
                    }
                }
                // 处理一些task
                if (!linkedBlockingQueue.isEmpty()) {
                    Channel channel = (Channel) linkedBlockingQueue.take();

                    if (channel instanceof ServerSocketChannel) {
                        ServerSocketChannel server = (ServerSocketChannel) channel;
                        server.register(selector, SelectionKey.OP_ACCEPT);
                    } else if (channel instanceof SocketChannel) {
                        SocketChannel client = (SocketChannel) channel;
                        ByteBuffer buffer = ByteBuffer.allocateDirect(4098);
                        client.register(selector, SelectionKey.OP_READ, buffer);
                    }
                }
            } catch (IOException | InterruptedException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private void acceptHandler(SelectionKey key) {

        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);

            // 多线程中需要选择一个多路复用器register
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {

        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (read == 0) { // 等于0：没有读取到任何数据
                    break;
                } else {//如果小于0：客户端断开了连接，或者出现异常等情况
                    System.out.println("client: " + client.getRemoteAddress() + " closed......");
                    key.cancel();
                    break;
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void writeHandler(SelectionKey key) {
    }
}
