import static java.lang.System.out;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.String;

public class Rip {
	public static void main(final String[] args) throws IOException {
		String pass;

		// Lee password
		do {
			@SuppressWarnings("resource")
			Scanner in = new Scanner(System.in);
			out.print("Introduzca contraseña (16 caracteres como máximo): ");
			pass = in.nextLine();
		} while (pass.length() > 16);

		final String passs = pass;

		final String mip;
		final int mipuerto;

		// llamada a función que elige qué IP usar como mi IP dependiendo si hay
		// parámetros de entrada o no
		mip = queIp(args);
		// llamada a función que elige qué puerto usar como mi puerto
		// dependiendo si hay parámetros de entrada o no
		mipuerto = quePuerto(args);

		// Mi máscara siempre es 255.255.255.255
		String mimask = mask255(32);

		// crear tabla de encaminamiento, array de vectores
		final ArrayList<VectorD> tabla = new ArrayList<VectorD>();
		// crear tabla de direcciones de vecinos
		final ArrayList<Vecino> listVecino = new ArrayList<Vecino>();
		// crear tabala de triggered updates
		final ArrayList<VectorD> tabla_trig = new ArrayList<VectorD>();

		// Abrir archivo de configuración y realizar operaciones de inicio
		// Llenar tabla con datos de rutas y anunciar mi ruta a los vecinos
		String linea;
		FileReader f = new FileReader("ripconf-" + mip + ".topo");

		BufferedReader bf = new BufferedReader(f);

		while ((linea = bf.readLine()) != null) {

			if (linea.contains("/")) { // ruta conectada. añadir a tabla

				String[] parts = linea.split("/");
				String ipruta = parts[0];
				int mascararuta = Integer.parseInt(parts[1]);
				String mascruta = mask255(mascararuta);

				VectorD vect = new VectorD(ipruta, mascruta, 1, mip, 6);

				tabla.add(vect);
			}

			else if (linea.contains(":")) { // vecino con puerto
				String[] parts = linea.split(":");
				String ip_vecino = parts[0];
				int puerto_vecino = Integer.parseInt(parts[1]);

				VectorD vect = new VectorD(ip_vecino, mask255(32), 1, ip_vecino, 6);
				tabla.add(vect);

				Vecino vec = new Vecino(ip_vecino, puerto_vecino);
				listVecino.add(vec);
			}

			else {
				// Vecino sin puerto
				Vecino veci = new Vecino(linea, 5512);
				listVecino.add(veci);
				// añadir vecino a mi tabla
				VectorD vect = new VectorD(linea, mask255(32), 1, linea, 6);
				tabla.add(vect);

			}
		}
		bf.close();

		// AÑADE MI RUTA A LA TABLA

		VectorD vect = new VectorD(mip, mimask, 0, mip, 6);
		tabla.add(vect);

		// ********************************************************************************************
		// Mandar tabla cada 10 segundos

		TimerTask timerEnvioTabla = new TimerTask() {
			public void run() {

				thread_envio hilo = new thread_envio(listVecino, tabla, mip, passs);

				hilo.start();
			}
		};
		// Aquí se pone en marcha el timer.
		Timer timer0 = new Timer();
		timer0.scheduleAtFixedRate(timerEnvioTabla, 1000, 5000);

		// **************************************************************************************

		// Imprimir tabla por pantalla cada 5 segundos
		TimerTask timerTablaPantalla = new TimerTask() {
			public void run() {
				System.out.println(new GregorianCalendar().getTime());
				out.println("*********************************************************************");
				out.println("IP Destino\tMascara\t\t\tMetric\tNext Hop\tTimer");
				Iterator<VectorD> nombreIterator = tabla.iterator();
				while (nombreIterator.hasNext()) {
					VectorD elemento = nombreIterator.next();

					System.out.println(elemento.getIpDestino() + "\t" + elemento.getMascara() + "\t" + "\t"
							+ elemento.getMetric() + "\t" + elemento.getNextHop() + "\t" + elemento.getTime());
				}
				out.println();

			}
		};
		Timer timer1 = new Timer();
		timer1.scheduleAtFixedRate(timerTablaPantalla, 0, 5000);
		// //*******************************************************************************

		// Estar atento a recibir
		while (true) {
			// Abrir socket UDP
			InetAddress miaddr = InetAddress.getByName(mip);
			DatagramSocket sock = new DatagramSocket(mipuerto, miaddr);
			// Leer paquete
			byte[] incomingData = new byte[1024];
			DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);

			sock.receive(incomingPacket);
			byte[] data = incomingPacket.getData();
			InetAddress addrVecino = incomingPacket.getAddress();
			String ipVecino = addrVecino.getHostAddress();

			// Comprobar si la pass es correcta

			byte[] passbuf = new byte[16];
			byte[] bytepass = pass.getBytes();

			for (int i = 0; i < bytepass.length; i++) {
				passbuf[i] = bytepass[i];

			}
			String pas = new String(passbuf);
			boolean incorrect = false;
			byte[] p = Arrays.copyOfRange(data, 8, 24);
			String pi = new String(p);

			if (!pi.equals(pas)) {
				incorrect = true;

			}

			if (incorrect == true) {
				sock.close();
				continue;
			}

			// Decodificar y guardar paquete recibido
			ArrayList<VectorD> tblVecino = new ArrayList<VectorD>();
			tblVecino.clear();
			for (int i = 28; i < data.length; i = i + 4) {
				byte[] op = Arrays.copyOfRange(data, i, i + 4);
				InetAddress ip_recib = InetAddress.getByAddress(op);

				i = i + 4;
				op = Arrays.copyOfRange(data, i, i + 4);
				InetAddress mask_recib = InetAddress.getByAddress(op);

				i = i + 4;
				op = Arrays.copyOfRange(data, i, i + 4);
				InetAddress nh_recib = InetAddress.getByAddress(op);

				i = i + 4;
				int metric_recib = data[i + 3];

				i = i + 4;
				if (ip_recib.getHostAddress().equals("0.0.0.0") & metric_recib == 0) {
					break;
				}
				VectorD vecto = new VectorD(ip_recib.getHostAddress(), mask_recib.getHostAddress(), metric_recib,
						nh_recib.getHostAddress(), 6);
				tblVecino.add(vecto);

			}

			boolean flag_trig = false;
			boolean flag1 = false;

			// Comienza las comparaciones recorriendo la tabla recibida del
			// vecino
			for (int i = 0; i < tblVecino.size(); i++) {
				// Flag indicador de nuevo destino
				flag1 = false;

				VectorD vectorVecino = tblVecino.get(i);
				String ipDestVecino = vectorVecino.getIpDestino();

				// Para cada entrada de la tabla recibida recorre mi tabla
				for (int j = 0; j < tabla.size(); j++) {

					VectorD vectorHost = tabla.get(j);
					String ipDestHost = vectorHost.getIpDestino();

					// si hay IpDestino/Mask iguales, compara costes
					if (ipDestHost.equals(ipDestVecino) && vectorHost.getMascara().equals(vectorVecino.getMascara())) {
						flag1 = true;

						// Actualiza timer
						if ((vectorVecino.getMetric() + 1) == vectorHost.getMetric()
								& vectorHost.getNextHop().equals(ipVecino) && vectorVecino.getMetric() < 15) {
							vectorHost.setTime(6);
						}

						if (vectorHost.getNextHop().equals(ipVecino)) {
							// caso recibo coste 16 de next-hop para esa ruta(por primera vez, que indica que la ruta ya
							// está a infinito)
							if (vectorVecino.getMetric() > 15 && vectorHost.getMetric() < 15) {
								vectorHost.setMascara(vectorVecino.getMascara());
								vectorHost.setNexthop(ipVecino);
								vectorHost.setMetric(16);
								vectorHost.setTime(2);
								// Siempre que actualiza Coste ruta => triggered update
								flag_trig = true;
								tabla_trig.add(new VectorD(ipDestHost, vectorHost.getMascara(), vectorHost.getMetric(),
										ipVecino, vectorHost.getTime()));
							}
						}
						if ((vectorVecino.getMetric() + 1) < vectorHost.getMetric()) {
							// caso recibo coste mejor
							vectorHost.setMetric(vectorVecino.getMetric() + 1);
							vectorHost.setMascara(vectorVecino.getMascara());
							vectorHost.setNexthop(ipVecino);
							vectorHost.setTime(6);

							// Siempre que actualiza Coste ruta => triggered update
							flag_trig = true;
							tabla_trig.add(new VectorD(ipDestHost, vectorHost.getMascara(), vectorHost.getMetric(),
									ipVecino, vectorHost.getTime()));

						}
					}

				}

				// si es nuevo destino y su métrica no supera el diámetro máximo
				// se añade a la tabla
				if (flag1 == false & (vectorVecino.getMetric() < 14)) {
					VectorD vec = new VectorD(ipDestVecino, vectorVecino.getMascara(), vectorVecino.getMetric() + 1,
							ipVecino, 6);
					tabla.add(vec);
					// Cambio en la tabla=> hay triggered update
					flag_trig = true;
					tabla_trig.add(vec);
				}
			}

			// Recorre mi tabla para eliminar enlaces caídos
			for (int j = 0; j < tabla.size(); j++) {

				VectorD vectorHost = tabla.get(j);

				if (vectorHost.getMetric() > 15 & vectorHost.getTime() < 1) {
					tabla.remove(j);
					j--;
				}
			}

			sock.close();

			// Triggered updates
			if (flag_trig == true && !tabla_trig.isEmpty()) {
				try {
					sendDatTabla(listVecino, tabla_trig, mip, pass);

					// Después de enviar triggered updates, vaciamos la tabla de triggered
					tabla_trig.clear();
				} catch (IOException e) {

				}
			}

		}

	}// Fin main

	// ************************************************************************************************************************
	// Función que elige Ip, si hay parámetros de entrada
	public static String queIp(String[] argu) throws SocketException {
		if (argu.length != 0) {
			String mip1 = argu[0];
			if (mip1.contains(":")) {
				String[] parts = mip1.split(":");
				mip1 = parts[0];
			}
			return mip1;
		} else {
			String mip1 = miIP();
			return mip1;
		}
	}

	// ************************************************************************************************************************
	// Función que elige puerto, si hay parámetros de entrada
	public static int quePuerto(String[] argu) throws SocketException {
		if (argu.length != 0) {
			String mip1 = argu[0];
			int puerto = 5512;
			if (mip1.contains(":")) {
				String[] parts = mip1.split(":");
				puerto = Integer.parseInt(parts[1]);
			}
			return puerto;
		} else {
			return 5512;
		}
	}

	// ************************************************************************************************************************
	// envía mi tabla a las IPs destino que tengo en el array de vecinos
	public static void sendDatTabla(ArrayList<Vecino> lista, ArrayList<VectorD> tbl, String ip, String password)
			throws IOException {
		InetAddress miaddr = InetAddress.getByName(ip);
		DatagramSocket sock = new DatagramSocket(4200, miaddr);

		// Recorre tabla buscando IPS vecinos en la lista
		Iterator<Vecino> nombreIterator = lista.iterator();
		while (nombreIterator.hasNext()) {
			Vecino elemento = nombreIterator.next();

			// Filtramos vectores para split-horizon
			@SuppressWarnings("unchecked")
			ArrayList<VectorD> tbl_horizon = (ArrayList<VectorD>) tbl.clone();
			Iterator<VectorD> iter_horizon = tbl_horizon.iterator();
			while (iter_horizon.hasNext()) {
				VectorD vect_horizon = iter_horizon.next();

				if (vect_horizon.getNextHop().equals(elemento.getIp())) {
					iter_horizon.remove();
				}
			}

			// Creamos paquete RIP
			byte[] buf = ripacket(tbl_horizon, password);
			// Abrimos socket y enviamos paquete
			InetAddress address = InetAddress.getByName(elemento.getIp());
			DatagramPacket pack = new DatagramPacket(buf, buf.length, address, elemento.getPuerto());
			sock.send(pack);
		}
		sock.close();

	}

	// *******************************************************************************

	// Función que devuelve mi IPv4 de eth0
	public static String miIP() throws SocketException {
		NetworkInterface ni = NetworkInterface.getByName("eth0");

		Enumeration<InetAddress> inetA = ni.getInetAddresses();

		// Desplazo 2 en la enumeration para tomar la IPV4
		inetA.nextElement();
		inetA.nextElement();

		InetAddress i = inetA.nextElement();
		return i.getHostAddress();

	}

	// *******************************************************************************
	// Función que devuelve máscaras en formato 255.255.255.255
	public static String mask255(int maskdecimal) throws SocketException, UnknownHostException {

		int mask = 0xffffffff << (32 - maskdecimal);
		int value = mask;
		byte[] bytes = new byte[] { (byte) (value >>> 24), (byte) (value >> 16 & 0xff), (byte) (value >> 8 & 0xff),
				(byte) (value & 0xff) };
		InetAddress netAddr = InetAddress.getByAddress(bytes);

		return netAddr.getHostAddress();

	}

	// *******************************************************************************
	// Crear paquete RIP
	public static byte[] ripacket(ArrayList<VectorD> tabla, String password) throws IOException {
		// Cabecera RIP
		byte[] cabecera = { (byte) 2, (byte) 2, (byte) 0, (byte) 0, (byte) 255, (byte) 255, (byte) 0, (byte) 2 };
		// Genera Password Paquete RIP
		byte[] passbuf = new byte[16];
		String pass = password;
		byte[] bytepass = pass.getBytes();

		for (int i = 0; i < bytepass.length; i++) {
			passbuf[i] = bytepass[i];

		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(cabecera);
		outputStream.write(passbuf);

		// Recorre tabla para hacer el paquete
		Iterator<VectorD> nombreIterator = tabla.iterator();
		while (nombreIterator.hasNext()) {
			VectorD elemento = nombreIterator.next();
			byte[] a = null;
			byte[] cab = { (byte) 0, (byte) 2, (byte) 0, (byte) 0 };
			outputStream.write(cab);
			InetAddress addr = InetAddress.getByName(elemento.getIpDestino());
			a = addr.getAddress();
			outputStream.write(a);
			InetAddress mask = InetAddress.getByName(elemento.getMascara());
			a = mask.getAddress();
			outputStream.write(a);
			byte[] nexthop = { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
			outputStream.write(nexthop);
			byte[] metric = { (byte) 0, (byte) 0, (byte) 0, (byte) elemento.getMetric() };
			outputStream.write(metric);
		}

		return outputStream.toByteArray();

	}

}