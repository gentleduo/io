import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class OSFileIO {

    static byte[] data = "123456789\n".getBytes(StandardCharsets.UTF_8);
    static String path = "/opt/io/out.txt";

    public static void main(String[] args) throws Exception {

        switch (args[0]) {
            case "0":
                testBasicFileIO();
                break;
            case "1":
                testBufferedFileIO();
                break;
            default:
                whatByteBuffer();
        }
    }

    public static void testBasicFileIO() throws Exception {
        File file = new File(path);
        FileOutputStream out = new FileOutputStream(file);
//        while (true) {
//            out.write(data);
//        }
        for (int i = 0; i < 1000000; i++) {
            out.write(data);
        }
        System.in.read();
    }

    public static void testBufferedFileIO() throws Exception {
        File file = new File(path);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
//        while (true) {
//            out.write(data);
//        }
        for (int i = 0; i < 1000000; i++) {
            out.write(data);
        }
        System.in.read();
    }

    public static void testRandomAccessFileWrite() throws Exception {

        // RandomAccessFile与之前的FileOutputStream不同点在于，支持随机写，也就是说FileOutputStream的写入只能追加到文件的末尾，而RandomAccessFile可以指定偏移量写入
        RandomAccessFile raf = new RandomAccessFile(path, "rw");
        raf.write("hello world\n".getBytes());
        raf.write("hello gentleduo\n".getBytes());
        System.out.println("write---------------------------------");
        System.in.read();

        raf.seek(4);
        raf.write("ooxx".getBytes());

        System.out.println("seek---------------------------------");
        System.in.read();

        FileChannel rafChannel = raf.getChannel();
        // 只有文件系统的的FileChannel上有map方法（本质是通过系统调用，调用内核的mmap方法），如果是serversocket或者是socket上面是没有map方法的，只有文件（块设备）才可以做内存映射。
        // 块设备：是可以来回自由寻址的，能够自由读取文件中前面的某一块或者后面的某一块
        // 内存映射：将内核的pagecache和文件数据页映射起来
        // map：通过系统调用，使用内核中的mmap方法得到一个堆外的并且和文件映射的bytebuffer
        MappedByteBuffer map = rafChannel.map(FileChannel.MapMode.READ_WRITE, 0, 4096);
        // 这里通过调用MappedByteBuffer的put方法就没有系统调用了，因为MappedByteBuffer和文件做了映射，所以数据会直接到达内核的pagecache。
        // 其他的方式是需要out.write()这样的系统调用才能要进程的数据进入内核的pagecache。也就是说必须有用户态、内核态的切换
        // 但是mmap的内存映射，依然是内核的pagecache体系所约束的，换而言之，也是会丢数据的。（JDK的API没有能力脱离pagecache的约束）
        // 可以是用C语言开发JNI，使用linux内核的Direct IO，但是这并不是忽略linux内核的pagecache，而是把pagecache交给程序自己，开辟一个数组当作pagecache，用代码逻辑来维护一致性等。。
        map.put("@@@".getBytes());
        System.out.println("map--put---------------------------------");
        System.in.read();
    }

    public static void whatByteBuffer() {

        // 通过socket写入的时候，会创建一个堆外的、java进程内的bytebuffer，然后将数据从堆内的bytebuffer拷贝到堆外的bytebuffer，最后写入内核中（因此直接申请堆外的bytebuffer会效率更高）
        // 所以java是必须先从jvm的虚拟堆内将数据拷贝到java进程的堆内然后再将数据拷贝到内核空间中，
        ByteBuffer buffer = ByteBuffer.allocate(1024);//堆内分配
//        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);//对外分配

        System.out.println("position:" + buffer.position());
        System.out.println("limit:" + buffer.limit());
        System.out.println("capacity:" + buffer.capacity());
        System.out.println("mark:" + buffer);

        buffer.put("123".getBytes());
        System.out.println("-----------------------put:123-----------------------");
        System.out.println("mark:" + buffer);

        buffer.flip(); // 写 ==> 读
        System.out.println("-----------------------flip-----------------------");
        System.out.println("mark:" + buffer);

        buffer.get();
        System.out.println("-----------------------get-----------------------");
        System.out.println("mark:" + buffer);

        // 将position与limit之间的内容移到0与(limit - position)之间的区域，position的值变为limit - position，limit的值变为capacity；
        // compact主要用于在读取还没有结束的时候又想向buffer中写入输入，但是又想保留没有读取的数据。如果直接clear的话那没有读取的数据就被覆盖了
        // 例如当前capacity是6当前指针指向2即下标0、1位置的数据已经写出，此时执行compact方法就是将下标2、3、4、5的数据移动到下标0、1、2、3的位置，指针指向下标4的位置，然后从4的位置继续写入数据。写完后，把指针移动到0，再写出，然后再执行compact操作，如此反复......
        buffer.compact();
        System.out.println("-----------------------compact-----------------------");
        System.out.println("mark:" + buffer);

        buffer.clear();
        System.out.println("-----------------------clear-----------------------");
        System.out.println("mark:" + buffer);
    }
}