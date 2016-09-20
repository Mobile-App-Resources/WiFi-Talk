/* This class is created to provide message communication with two devices via Wi-Fi
	Message packets are sent and received with sockets*/

package com.esb_pc.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;


public class Channel implements Runnable
{
	private DatagramSocket socket;
	private boolean running;

	private OnSocketListener onSocketListener; // creating onSocketListener object


	public Channel(OnSocketListener onSocketListener) // assigning onSocketListener object to Channel
	{
		this.onSocketListener = onSocketListener;
	}
	
	public void bind(int port) throws SocketException // binding the user defined port to socket
	{
		socket = new DatagramSocket(port);
	}
	
	public void start() // creating and running Thread object for text communication
	{
		Thread thread = new Thread(this);
		thread.start();

	}
	
	public void stop() // if the program will stop terminate the process
	{
		running = false;
		socket.close();
	}

	@Override
	public void run() // run function which is overrided from Runnable interface
	{
		byte[] buffer = new byte[1024]; // creating buffer for holding messages
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length); // assigning characters which are holded in buffer to packet

		running = true;
		while(running) // receiving of messages while process is running
		{
			try
			{
				socket.receive(packet);

				String msg = new String(buffer, 0, packet.getLength()); // write the message to msg string
				// System.out.println(msg);

				if(null != onSocketListener)
					onSocketListener.onReceived(msg); // get the messages while socket is on
			} 
			catch (IOException e)
			{
				break;
			}
		}
	}

	public void sendTo(final InetSocketAddress address, final String msg) // sending messages to defined IP address
	{
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{

				byte[] buffer = msg.getBytes();

				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				packet.setSocketAddress(address);

				try //send messages to socket
				{
					socket.send(packet);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(runnable); // creating and running thread for sending messages
		thread.start();
	}
}
