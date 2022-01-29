package org.duo.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SelectorThreadGroup {

    SelectorThread[] selectorThread;
    ServerSocketChannel serverSocketChannel;
    AtomicInteger threadId = new AtomicInteger(0);

    SelectorThreadGroup(int num) {

        selectorThread = new SelectorThread[num];
        for (int i = 0; i < num; i++) {
            selectorThread[i] = new SelectorThread(this);
            // selectorThread运行后，会在执行select的地方阻塞，由于此时selector中还没有注册任何事件，如果不进行wakeup，那么会永久阻塞
            new Thread(selectorThread[i]).start();
        }
    }

    public void bind(int port) {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            // 注册到哪个selector上呢？
            nextSelector(serverSocketChannel);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * ServerSocketChannel和SocketChannel都复用这个方法
     *
     * @param channel
     */
    public void nextSelector(Channel channel) {

        // 混杂模式
//        SelectorThread selectorThread = next();
//
////        // channel有可能是server也有可能是client
////        ServerSocketChannel server = (ServerSocketChannel)channel;
////        try {
////            // 由于在selectorThread中已经调用了Selector的select方法，最终调用的是：sun.nio.ch.SelectorImpl.lockAndDoSelect
////            // 而ServerSocketChannel的register最终会调用也会sun.nio.ch.SelectorImpl#register
////            // 而lockAndDoSelect和register在SelectorImpl类中是同步方法，selectorThread在执行select方法的时候已经持有了该selector的锁，所以在这里在执行下面的方法时会因为获取不到锁而阻塞。
////            server.register(selectorThread.selector, SelectionKey.OP_ACCEPT);
////            // wakeup可以使selector的select方法立刻返回，从而释放锁
////            // 如果将wakeup放在register之前执行，那么有可能selectorThread在被唤醒后register线程还没有完成注册前就又执行到了select方法的地方，那么在register线程中注册的事件还是无法被监听到
////            // 如果wakeup放在register之后执行，那么有可能先执行selectorThread的select方法，然后再执行register线程的register方法，那么register线程将会被阻塞住，从而无法再执行wakeup方法
////            // 所以将wakeup放在register的前后都不行，涉及到多线程之间的通信最好使用队列
////            selectorThread.selector.wakeup();
////        } catch (ClosedChannelException e) {
////            e.printStackTrace();
////        }
//
//        // 1.通过队列传递数据
//        selectorThread.linkedBlockingQueue.add(channel);
//        // 2.通过打断阻塞，让对应的线程自己在打断后完成事件的注册
//        selectorThread.selector.wakeup();

        // listen单独绑定一个selector，client分配到其他的selector
        try {
            if (channel instanceof ServerSocketChannel) {
                selectorThread[0].linkedBlockingQueue.put(channel);
                selectorThread[0].selector.wakeup();
            } else if (channel instanceof SocketChannel) {
                SelectorThread selectorThread = next();
                selectorThread.linkedBlockingQueue.add(channel);
                selectorThread.selector.wakeup();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private SelectorThread next() {

//        // 混杂模式
//        int index = threadId.incrementAndGet() % selectorThread.length;
//        return selectorThread[index];

        // listen单独绑定一个selector，client分配到其他的selector
        int index = threadId.incrementAndGet() % (selectorThread.length - 1);
        return selectorThread[index + 1];
    }
}
