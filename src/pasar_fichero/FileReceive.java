package pasar_fichero;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class FileReceive {

	public static void main(String[] args) {
		System.out.println("Ready to receive!");
		int port = 50505; // Change this to the desired port
		String serverRoute = "E:\\WorkSpace"; // Change this to the desired directory
																				// route (Where the file will be stored)
		createFile(port, serverRoute); // Creating the file
	}

	public static void createFile(int port, String serverRoute) {
		try {
			DatagramSocket socket = new DatagramSocket(port);
			byte[] receiveFileName = new byte[1024]; // Where we store the data of datagram of the name
			DatagramPacket receiveFileNamePacket = new DatagramPacket(receiveFileName, receiveFileName.length);

			socket.receive(receiveFileNamePacket); // Receive the datagram with the name of the file
			System.out.println("Receiving file name");
			byte[] data = receiveFileNamePacket.getData(); // Reading the name in bytes
			String fileName = new String(data, 0, receiveFileNamePacket.getLength()); // Converting the
		//	sendAck(1, socket, receiveFileNamePacket.getAddress(), port);
			DatagramPacket respuesta = new DatagramPacket(data, receiveFileNamePacket.getLength(), receiveFileNamePacket.getAddress(), port);
			socket.send(respuesta);
			System.out.println("Creating file");
			File f = new File(serverRoute + "\\" + fileName); // Creating the file
			FileOutputStream outToFile = new FileOutputStream(f); // Creating the stream through which we
			receiveFile(outToFile, socket); // Receiving the file
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private static void receiveFile(FileOutputStream outToFile, DatagramSocket socket) throws IOException {
		System.out.println("Receiving file");
		boolean flag; // Have we reached end of file
		int sequenceNumber = 0; // Order of sequences
		int foundLast = 0; // The las sequence found
		while (true) {
			byte[] message = new byte[1024]; // Where the data from the received datagram is stored
			byte[] fileByteArray = new byte[1021]; // Where we store the data to be writen to the file
			// Receive packet and retrieve the data
			DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
			socket.receive(receivedPacket);
			message = receivedPacket.getData(); // Data to be written to the file
			// Get port and address for sending acknowledgment
			InetAddress address = receivedPacket.getAddress();
			int port = receivedPacket.getPort();
			// Retrieve sequence number
			sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);

			// Check if we reached last datagram (end of file)
			flag = (message[2] & 0xff) == 1;
			// If sequence number is the last seen + 1, then it is correct
			// We get the data from the message and write the ack that it has been received
			// correctly

			if (sequenceNumber == (foundLast + 1)) {
				// set the last sequence number to be the one we just received
				foundLast = sequenceNumber;
				// Retrieve data from message
				System.arraycopy(message, 3, fileByteArray, 0, 1021);
				// Write the retrieved data to the file and print received data sequence number
				String datoFichero = new String(fileByteArray);
				
				outToFile.write(datoFichero.trim().getBytes());
				System.out.println("Received: sequence number:" + foundLast);

				System.out.println("Expected sequenceNumber+" + ". DISCARDING");
				// Re send the acknowledgement
				sendAck(foundLast, socket, address, port);
			} else {

				System.out.println("Expected sequence number: " + (foundLast + 1) + " but received " + sequenceNumber
						+ ". DISCARDING");
				sendAck(foundLast, socket, address, port);

			}

			System.out.println("Received: sequence number:" + foundLast);
			// Check for last datagram
			if (flag) {
				outToFile.close();
				break;
			}
		}
	}

	private static void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port)
			throws IOException {
		// send acknowledgement
		// + (foundLast + 1) + "but received
		byte[] ackPacket = new byte[2];
		ackPacket[0] = (byte) (foundLast >> 8);
		ackPacket[1] = (byte) (foundLast);
		// the datagram packet to be sent
		DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
		socket.send(acknowledgement);
		System.out.println("Sent ack: Sequence Number = " + foundLast);
	}

}