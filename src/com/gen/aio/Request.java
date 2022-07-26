package com.gen.aio;

import java.nio.ByteBuffer;

public class Request {

	private int headLength;

	private int bodyLength;

	private int receivedDataLen;

	private ByteBuffer dataBuffer;

	private byte[] headByteArray;

	private ByteBuffer bodyBuffer;

	private String type;

	public Request(int headLength, ByteBuffer dataBuffer) {

		super();
		this.headLength = headLength;
		this.dataBuffer = dataBuffer;
		this.headByteArray = new byte[this.headLength];
	}

	public int getHeadLength() {
		return headLength;
	}

	public void setHeadLength(int headLength) {
		this.headLength = headLength;
	}

	public int getBodyLength() {
		return bodyLength;
	}

	public void setBodyLength(int bodyLength) {
		this.bodyLength = bodyLength;
	}

	public int getReceivedDataLen() {
		return receivedDataLen;
	}

	public void setReceivedDataLen(int receivedDataLen) {
		this.receivedDataLen = receivedDataLen;
	}

	public ByteBuffer getDataBuffer() {
		return dataBuffer;
	}

	public void setDataBuffer(ByteBuffer dataBuffer) {
		this.dataBuffer = dataBuffer;
	}

	public byte[] getHeadByteArray() {
		return headByteArray;
	}

	public void setHeadByteArray(byte[] headByteArray) {
		this.headByteArray = headByteArray;
	}

	public ByteBuffer getBodyBuffer() {
		return bodyBuffer;
	}

	public void setBodyBuffer(ByteBuffer bodyBuffer) {
		this.bodyBuffer = bodyBuffer;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}