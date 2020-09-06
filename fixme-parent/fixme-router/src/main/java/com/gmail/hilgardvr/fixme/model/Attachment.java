package com.gmail.hilgardvr.fixme.model;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.ByteBuffer;
import java.net.SocketAddress;

public class Attachment {
	public AsynchronousServerSocketChannel server;
	public int clientId;
	public String toMarketId;
	public int mapToId;
	public AsynchronousSocketChannel client;
	public SocketAddress clientAddress;
	public ByteBuffer buffer;
}