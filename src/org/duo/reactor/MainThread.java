package org.duo.reactor;

public class MainThread {

    public static void main(String[] args) {

        // 创建I/O Thread：一个或多个：
        SelectorThreadGroup selectorThreadGroup = new SelectorThreadGroup(1);
        // 混杂模式：只有一个线程负责accept，每个都会被分配client进行R/W
        // SelectorThreadGroup selectorThreadGroup = new SelectorThreadGroup(3);

        // 将监听的server注册到某一个selector上
        selectorThreadGroup.bind(9999);

    }
}
