package pasar_fichero;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PReceive {

	public static void main(String[] args) {
		System.out.println("Listo para recibir");
		int port = 50505;

		try {
			DatagramSocket socket = new DatagramSocket(port);
			
			//Donde almacenamos los datos de datagrama del nombre
			byte[] receiveFileName = new byte[1024];
						
			DatagramPacket reciveFilePacket = new DatagramPacket(receiveFileName, receiveFileName.length);
			
			//Recibe el datagrama con el nombre del archivo
			socket.receive(reciveFilePacket);
			

			//Lectura del nombre en bytes
			byte[] dato = reciveFilePacket.getData();
			
			//Convertir el nombre del contenido del archivo
			String fileName = new String(dato, 0, reciveFilePacket.getLength());
			
			System.out.println("Nombre del archivo recibido --> "+fileName);

			System.out.println("Crea archivo");
			
			//Creando el archivo
			File f = new File("received_"+fileName);
			
			//Creando la secuencia a través de la cual escribimos el contenido del archivo
			FileOutputStream fos = new FileOutputStream(f);

			boolean ficheroLeidoCompletamente = false;
			int tamañoPaquete = 1024;//Orden de secuencias
			int tamañoIndicadorTrozo = 3;//La última secuencia

			while(!ficheroLeidoCompletamente){
				byte[] bufferMensaje = new byte[tamañoPaquete]; //Dónde se almacenan los datos del datagrama recibido
				byte[] bufferParteFichero = new byte[tamañoPaquete-tamañoIndicadorTrozo]; //Dónde almacenamos los datos que se escribirán en el archivo

				//Recibe el paquete y recupera los datos.
				DatagramPacket paqueteRecibido = new DatagramPacket(bufferMensaje, bufferMensaje.length);
				socket.receive(paqueteRecibido);
				bufferMensaje = paqueteRecibido.getData();//Datos a escribir en el archivo

				ByteBuffer bb = ByteBuffer.allocate(2);
				// le decimos que viene en little endian
				bb.order(ByteOrder.LITTLE_ENDIAN);
				// pongo el primer byte
				bb.put(bufferMensaje[0]);
				// pongo el segundo
				bb.put(bufferMensaje[1]);
				// ya puedo obtener el short del array de byte
				short numeroParteRecibida = bb.getShort(0);

				System.arraycopy(bufferMensaje,tamañoIndicadorTrozo,bufferParteFichero,0,bufferParteFichero.length);
				fos.write(bufferParteFichero);

				System.out.println("Received: Sequence number: "+numeroParteRecibida);

				boolean esUltimaParte = bufferMensaje[2] == (byte)1;

				InetAddress ipEmisor = paqueteRecibido.getAddress();
				int puerto = paqueteRecibido.getPort();

				byte[] acuseRecibo = new byte[2];
				for(int i = 0; i<2;i++){
					acuseRecibo[i] = (byte)(numeroParteRecibida >>> (i*8));
				}
				DatagramPacket paqueteAcuseRecibo = new DatagramPacket(acuseRecibo,acuseRecibo.length,ipEmisor,puerto);
				socket.send(paqueteAcuseRecibo);

				ficheroLeidoCompletamente = esUltimaParte;
			}
					
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}

}
