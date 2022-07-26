package com.gen.nio;

import java.io.Closeable;
import java.io.IOException;

public class CommonUtils {

	public static int getActualLength(byte[] data) {
		int i = 0;
		for (; i < data.length; i++) {
			if (data[i] == '\0')
				break;
		}
		return i;
	}

	/**
	 * byte[]转int
	 * 
	 * @param bytes
	 * @return
	 */
	public static int byteArrayToInt(byte[] bytes) {
		int value = 0;
		// 由高位到低位
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (bytes[i] & 0x000000FF) << shift;// 往高位游
		}
		return value;
	}

	/**
	 * int到byte[]
	 * 
	 * @param i
	 * @return
	 */
	public static byte[] intToBytes(int value) {
		byte[] result = new byte[4];
		// 由高位到低位
		result[0] = (byte) ((value >> 24) & 0xFF);
		result[1] = (byte) ((value >> 16) & 0xFF);
		result[2] = (byte) ((value >> 8) & 0xFF);
		result[3] = (byte) (value & 0xFF);
		return result;
	}

	public static void close(Closeable closable) {
		if (closable != null) {
			try {
				closable.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
