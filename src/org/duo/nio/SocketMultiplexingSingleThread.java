package org.duo.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 多路复用器监听的文件描述符的上限：
 *
 * /proc/sys/fs/epoll/max_user_watches (since Linux 2.6.28)
 * This  specifies  a limit on the total number of file descriptors that a user can register across all epoll instances on the system.  The limit is per
 *  real user ID.  Each registered file descriptor costs roughly 90 bytes on a 32-bit kernel, and roughly 160 bytes on a 64-bit kernel.   Currently,  the
 *  default value for max_user_watches is 1/25 (4%) of the available low memory, divided by the registration cost in bytes.
 *
 * 这指定了每个用户可以在系统上的所有epoll实例中注册的文件描述符总数的限制。在32位内核上大约需要90个字节，在64位内核上大约需要160个字节。
 * 目前，max_user_watches的默认值为可用低内存的1/25(4%)除以注册成本（以字节为单位）
 */
public class SocketMultiplexingSingleThread {

    private ServerSocketChannel server = null;
    // 内核的多路复用器被java抽象成了Selector (select poll epoll)
    // 可以根据JVM启动参数设置该java进程是使用poll还是epoll：
    // -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider
    // -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.PollSelectorProvider
    private Selector selector = null;
    int port = 9090;

    public void initServer() {
        try {
            // 以下的三步会在内核中创建一个listen状态的FD4(server)；
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            /*
            java会优先选择epoll，但是可以通过设置-D进行修正
            如果在epoll模型下，open相当于调用内核的epoll_create创建了一个新的文件描述符FD5(之后注册在该selector中的fd实际上是放在fd5中)
             */
            selector = Selector.open(); //
            /*
            如果使用的是select或者poll的模型：会在jvm里开辟一个数据将fd4放进去，
            如果使用的是epoll模型：那么相当于调用内核的epoll_ctl(fd5,ADD,fd4,EPOLLIN)
             */
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("服务器启动了......");
        try {
            while (true) {
                Set<SelectionKey> keys = selector.keys();
//                System.out.println(keys.size() + " size");
                /*
                1.调用多路复用器(select,poll or epoll)
                如果使用的是select,poll模型：select其实调的是内核的select(fds)方法
                如果使用的是epoll模型：其实调的是内核的epoll_wait
                2.select的参数可以带时间，表示阻塞多少毫秒；如果为零，则无限期阻塞； 不能为负
                可以调用此选择器的wakeup方法结束阻塞
                 */
                while (selector.select(500) > 0) {
                    // 返回有状态的文件描述符的集合
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = selectionKeys.iterator();
                    // 因此多路复用器返回的只是状态，应用程序还得通过系统调用将内核缓冲池中的数据读到应用程序自己的内存里面。
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isAcceptable()) {
                            /*
                            如果接受一个新的连接：
                            如果使用的是select,poll模型：因为它们内核没有空间，那么jvm会将它保存在和前面fd4一样的集合中
                            如果使用的是epoll模型：那么相当于调用内核的epoll_ctl(fd5,ADD,fd6,EPOLLIN)
                             */
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {

        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept();
            client.configureBlocking(false);
            ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("--------------------------------------------------");
            System.out.println("新的客户端：" + client.getRemoteAddress());
            System.out.println("--------------------------------------------------");
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
                } else if (read == 0) {
                    break;
                } else {
                    client.close();
                    break;
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

    }

    public static void main(String[] args) {

        SocketMultiplexingSingleThread thread = new SocketMultiplexingSingleThread();
        thread.start();
    }
}
