package eu.dnetlib.pace.distance.algo;

import com.wcohen.ss.AbstractStringDistance;
import eu.dnetlib.pace.distance.DistanceClass;
import eu.dnetlib.pace.distance.SecondStringDistanceAlgo;

import java.util.Map;

//case class JaroWinkler(w: Double) extends SecondStringDistanceAlgo(w, new com.wcohen.ss.JaroWinkler())
@DistanceClass("JaroWinkler")
public class JaroWinkler extends SecondStringDistanceAlgo {

	public JaroWinkler(){
		super();
	}

	public JaroWinkler(Map<String, Number> params){
		super(params);
	}

	public JaroWinkler(double weight) {
		super(weight, new com.wcohen.ss.JaroWinkler());
	}

	protected JaroWinkler(double weight, AbstractStringDistance ssalgo) {
		super(weight, ssalgo);
	}
	
	@Override
	public double distance(String a, String b) {
		String ca = cleanup(a);
		String cb = cleanup(b);

		return normalize(ssalgo.score(ca, cb));
	}		

	@Override
	public double getWeight() {
		return super.weight;
	}

	@Override
	protected double normalize(double d) {
		return d;
	}

}
