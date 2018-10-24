package eu.dnetlib.pace.distance.algo;

import com.wcohen.ss.AbstractStringDistance;
import eu.dnetlib.pace.distance.DistanceClass;
import eu.dnetlib.pace.distance.SecondStringDistanceAlgo;

import java.util.Map;

@DistanceClass("AlwaysMatch")
public class AlwaysMatch extends SecondStringDistanceAlgo {

	public AlwaysMatch(){
		super();
	}

	public AlwaysMatch(final Map<String, Number> params){
		super(params);
	}

	public AlwaysMatch(final double weight) {
		super(weight, new com.wcohen.ss.JaroWinkler());
	}

	protected AlwaysMatch(final double weight, final AbstractStringDistance ssalgo) {
		super(weight, ssalgo);
	}

	@Override
	public double distance(final String a, final String b) {
		return 1.0;
	}

	@Override
	public double getWeight() {
		return super.weight;
	}

	@Override
	protected double normalize(final double d) {
		return d;
	}

}
