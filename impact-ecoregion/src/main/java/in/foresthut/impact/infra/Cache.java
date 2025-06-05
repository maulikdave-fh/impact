package in.foresthut.impact.infra;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cache {
	private static Cache instance; 
	private static Map<String, List<String>> splits = new HashMap<>();
	
	private Cache() {
		
	}
	
	public static synchronized Cache getInstance() {
		if (instance == null)
			instance = new Cache();
		return instance;
	}

	public synchronized void add(String id, List<String> parts) {
		splits.put(id, parts);
	}

	public List<String> splits(String id) {
		return splits.get(id);
	}
}
