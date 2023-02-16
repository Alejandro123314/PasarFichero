package pasar_fichero;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PSend {
	
	public static void main(String[] args) {
		String fileName = args[0];
		File f = new File(fileName);
		InetAddress address =null;
		int port = 50505;

		try {
			//Leyendo la IP del receptor
			address = InetAddress.getByName(args[1]);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		try {
			DatagramSocket socket = new DatagramSocket();
			
			//Creando el paquete para enviar el nombre de fichero
			DatagramPacket filePacket = new DatagramPacket(fileName.getBytes(), 
						   fileName.getBytes().length,address, port);
			
			//Enviamos el nombre del fichero
			socket.send(filePacket);
			
			//Leemos el Fichero
			FileInputStream fis = new FileInputStream(f);

			//Ir leyendo en trozos y mandando paquetes por cada trozo
			int tamaño = 1024;
			int tamañoIndicadorTrozo = 3; //Quitar 3 para marcar ultima parte
			int bytesRead = 0;
			
			//Tabla de char de tamaño 1024 bytes vacía para ir almacenando los trozos
			byte[] myBuffer = new byte[tamaño];

			short numeroParte = 0;
			int bytesLeidos;
			//Leemos 1021 bytes y los escribimos desplazados 2 bytes a la derecha (offset)
			while( ( bytesLeidos = fis.read(myBuffer,tamañoIndicadorTrozo,tamaño-tamañoIndicadorTrozo) ) > 0 ){
				// pone al principio del buffer el numero de parte del paquete
				for(int i = 0; i<2;i++){
					myBuffer[i] = (byte)(numeroParte >>> (i*8));
				}

				if(bytesLeidos < tamaño-tamañoIndicadorTrozo){
					// es la ultima parte
					myBuffer[2] = (byte)1;
				}
				else{
					// es una parte intermedia
					myBuffer[2] = (byte)0;
				}

				DatagramPacket paqueteParcial = new DatagramPacket(myBuffer,
						myBuffer.length,address, port);

				socket.send(paqueteParcial);

				boolean recibido = false;

				while(!recibido){
					byte[] parteRecibida = new byte[2];
					DatagramPacket paqueteRecepcion = new DatagramPacket(parteRecibida,parteRecibida.length);
					socket.setSoTimeout(250);
					socket.receive(paqueteRecepcion);

					parteRecibida = paqueteRecepcion.getData();

					// reservamos tamaño para 2 bytes (que es lo que ocupa un short)
					ByteBuffer bb = ByteBuffer.allocate(2);
					// le decimos que viene en little endian
					bb.order(ByteOrder.LITTLE_ENDIAN);
					// pongo el primer byte
					bb.put(parteRecibida[0]);
					// pongo el segundo
					bb.put(parteRecibida[1]);
					// ya puedo obtener el short del array de byte
					short numeroParteRecibida = bb.getShort(0);

					// si la parte que dice el otro extremo, concide con la parte que le hemos enviado
					if(numeroParteRecibida== numeroParte){
						// salimos del bucle
						recibido = true;
					}
					else{
						// enviamos de nuevo el paquete
						socket.send(paqueteParcial);
					}
				}
				numeroParte++;
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
