public class VectorD {

	private String ipdestino;
	private String mascara;
	private int metric;
	private String nexthop;
	private int time;

	public VectorD(String valorIpDestino, String valorMascara, int valorMetric, String valorNextHop, int time) {
		ipdestino = valorIpDestino;
		mascara = valorMascara;
		metric = valorMetric;
		nexthop = valorNextHop;
		this.time = time;
	}

	public String getIpDestino() {
		return ipdestino;
	}

	public String getMascara() {
		return mascara;
	}

	public int getMetric() {
		return metric;
	}

	public String getNextHop() {
		return nexthop;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public void setIpdestino(String ipdestino) {
		this.ipdestino = ipdestino;
	}

	public void setMascara(String mascara) {
		this.mascara = mascara;
	}

	public void setMetric(int metric) {
		this.metric = metric;
	}

	public void setNexthop(String nexthop) {
		this.nexthop = nexthop;
	}

}