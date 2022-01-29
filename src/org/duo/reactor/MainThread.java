package org.duo.reactor;

public class MainThread {

    public static void main(String[] args) {

        // 创建I/O Thread：一个或多个：
        // 混杂模式：每个selector都会被分配client进行R/W
        // 也可以将listen单独绑定一个boss group，client绑定一个worker group
        SelectorThreadGroup bossGroup = new SelectorThreadGroup(3);
        SelectorThreadGroup workerGroup = new SelectorThreadGroup(3);
        bossGroup.setWorker(workerGroup);
        // 将监听的server注册到某一个selector上
        bossGroup.bind(9999);
//        bossGroup.bind(7777);
//        bossGroup.bind(6666);
    }
}
