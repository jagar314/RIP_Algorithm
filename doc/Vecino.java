public class Vecino {

	private String ip;
	private int puerto;

	public Vecino(String valor_ip, int valor_puerto) {

		ip = valor_ip;
		puerto = valor_puerto;
	}

	public String getIp() {
		return ip;
	}

	public int getPuerto() {
		return puerto;
	}

	public void setMascara(String ip) {
		this.ip = ip;
	}

	public void setmetric(int puerto) {
		this.puerto = puerto;
	}

}
