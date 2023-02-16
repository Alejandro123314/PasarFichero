package pasar_fichero;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class FileSend {
	
	private static DatagramSocket socket;
	private static int port = 50505;
	private static String host;
	private static String nfile;

	private static void ready() {
		try {
			if(socket != null) {
				throw new RuntimeException("El socket esta siendo usado");
			}
			 socket = new DatagramSocket();
			InetAddress address = InetAddress.getByName(host);
			String fileName;
			System.out.println(nfile);
			File f = new File(nfile);
			fileName = f.getName();
			System.out.println(fileName);
			byte[] fileNameBytes = fileName.getBytes();
			DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, address, port);
			socket.send(fileStatPacket);
			boolean ackRec;

			int intentos = 0;
			boolean mantener = true;
			
			while (mantener) {
				byte[] ack = new byte[2];
				DatagramPacket ackpack = new DatagramPacket(ack, ack.length, address, port);

				try {
					socket.setSoTimeout(50);
					socket.receive(ackpack);					
					ackRec = true;
				} catch (SocketTimeoutException e) {
					System.out.println("Socket timed out waiting for ack");
					ackRec = false;
				}

				if (ackRec) {
					System.out.println("Ack received: Nombre fichero recibido = " + fileName);
					mantener = false;
				} else {
					intentos++;
					if(intentos == 3)
						throw new RuntimeException("No se pudo completar el envío de " + fileName);
					socket.send(ackpack);
					System.out.println("Enviando nombre fichero " + fileName);
				}
			}
			
			int sequencerNumber = 0;
			FileInputStream fis = new FileInputStream(f);
			
			byte[] fileByteArray = new byte[1024];
			while (fis.read(fileByteArray) != -1) {
				
				sequencerNumber++;
				sendFile(socket, fileByteArray, address, port, sequencerNumber);
			}
			fis.close();
			socket.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int port, int sequencerNumber)
			throws IOException {
		System.out.println("Sending file");
		//FileOutputStream fos = new FileOutputStream(fis);
		boolean flag;
		int ackSequence = 0;

		for (int i = 0; i < fileByteArray.length; i = i + 1021) {
			
			byte[] message = new byte[1024];
			message[0] = (byte) (sequencerNumber >> 8);
			message[1] = (byte) (sequencerNumber);

			if ((i + 1021) >= fileByteArray.length) {
				flag = true;
				message[2] = (byte) (1);
			} else {
				flag = false;
				message[2] = (byte) (0);
			}

			if (!flag) {
				System.arraycopy(fileByteArray, i, message, 3, 1021);
				//fos.write(fileByteArray);
			} else {
				System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
				//fos.write(fileByteArray);
			}

			DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, port);
			socket.send(sendPacket);
			System.out.println("Sent: Sequence number = " + sequencerNumber);

			boolean ackRec;

			int intentos = 0;
			boolean mantener2 = true;
			
			while (mantener2) {
				byte[] ack = new byte[2];
				DatagramPacket ackpack = new DatagramPacket(ack, ack.length, address, port);

				try {
					socket.setSoTimeout(50);
					socket.receive(ackpack);
					ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
					ackRec = true;
				} catch (SocketTimeoutException e) {
					System.out.println("Socket timed out waiting for ack");
					ackRec = false;
					intentos++;
				}

				if ((ackSequence == sequencerNumber) && (ackRec)) {
					System.out.println("Ack received: Sequence Number = " + ackSequence);
					mantener2 = false;
				} else {
					intentos++;
					if(intentos == 3)
						throw new RuntimeException("No se pudo completar el envío de " + sequencerNumber);
					socket.send(ackpack);
					System.out.println("Resending: Sequence Number = " + sequencerNumber);
				}
			}
			//fos.close();
		}
	}

	public static void main(String[] args) {
		nfile = args[0];
		host = args[1];
		ready();
	}

}
