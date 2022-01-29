package org.duo.reactor;

public class MainThread {

    public static void main(String[] args) {

        // 创建I/O Thread：一个或多个：
        // 混杂模式：每个selector都会被分配client进行R/W
        // 也可以将listen单独绑定一个selector，client分配到其他的selector
        SelectorThreadGroup selectorThreadGroup = new SelectorThreadGroup(3);

        // 将监听的server注册到某一个selector上
        selectorThreadGroup.bind(9999);
    }
}
