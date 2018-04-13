import java.util.*;

public class thread_envio extends Thread {
	ArrayList<Vecino> listaVecino;
	ArrayList<VectorD> mitabla;
	String mip, pass;
	boolean m_bRunThread = true;

	public thread_envio() {
		super();
	}

	thread_envio(ArrayList<Vecino> listaVecino, ArrayList<VectorD> mitabla, String mip, String pass) {
		this.listaVecino = listaVecino;
		this.mitabla = mitabla;
		this.mip = mip;
		this.pass = pass;
	}

	public void run() {

		final ArrayList<VectorD> tabla_tri = new ArrayList<VectorD>();

		// Cada envío(10s) decrementa timer a todos los vectores (excepto en los que soy next-hop)
		for (int j = 0; j < mitabla.size(); j++) {
			VectorD vectorHost = mitabla.get(j);

			
			if (!vectorHost.getNextHop().equals(mip)) {
				vectorHost.setTime(vectorHost.getTime() - 1);
			}
			
			if (vectorHost.getTime() < 1 & vectorHost.getMetric() > 15) {
				mitabla.remove(j);
				j--;
			}
			
			
			if (vectorHost.getTime() < 1 & vectorHost.getMetric() < 16) {
				vectorHost.setMetric(16);
				vectorHost.setTime(2);
				tabla_tri.add(vectorHost);
			}
			
			
		}
		
		try {
			if(!tabla_tri.isEmpty()){
			Rip.sendDatTabla(listaVecino, tabla_tri, mip, pass);
			tabla_tri.clear();
			}} catch (Exception e) {
		}
		
//		for (int k = 0; k < mitabla.size(); k++) {
//			//
//			// // La primera vez que llega a 0 el Timer de cada entrada =>2 para garbage collector y coste=>16
//			VectorD vectorHost = mitabla.get(k);
//			// if (vectorHost.getTime() == 0 & vectorHost.getMetric() < 16) {
//			// vectorHost.setMetric(16);
//			// vectorHost.setTime(2);
//			// }
//			//
//			// Segunda vez que Timer llega a 0 => Remove entrada
//			if (vectorHost.getTime() < 1 & vectorHost.getMetric() > 15) {
//				mitabla.remove(k);
//				k--;
//			}
//		}


		try {
			// Sleep para offset random en envío. El valor proporcional a usar sería DE 0 a 5s/3. Usamos 3s
			Thread.sleep(new Random().nextInt(3000));

			Rip.sendDatTabla(listaVecino, mitabla, mip, pass);

		} catch (Exception e) {

		}
	}
}
