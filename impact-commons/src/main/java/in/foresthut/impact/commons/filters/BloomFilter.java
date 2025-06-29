package in.foresthut.impact.commons.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;

public class BloomFilter {
	private final BitSet bitSet;
	private final int size;
	private final List<Function<String, Integer>> hashFunctions;

	/**
	 * Constructs a BloomFilter.
	 * 
	 * @param size          The size of the bit array.
	 * @param hashFunctions A list of hash functions to use.
	 */
	public BloomFilter(int size) {
		this.size = size;
		this.bitSet = new BitSet(size);
		this.hashFunctions = new ArrayList<>(Arrays.asList(s -> s.hashCode(), s -> s.length() * 31, // Another simple
																									// hash based on
																									// length
				s -> s.charAt(0) * 17)); // Another simple hash based on first char
	}

	/**
	 * Adds an element to the Bloom filter.
	 * 
	 * @param element The element to add.
	 */
	public void add(String element) {
		for (Function<String, Integer> hashFunc : hashFunctions) {
			int hash = hashFunc.apply(element);
			bitSet.set(Math.abs(hash % size)); // Use absolute value to handle negative hash codes
		}
	}

	/**
	 * Checks if an element might be in the Bloom filter. Returns true if the
	 * element might be present, false if it's definitely not present.
	 * 
	 * @param element The element to check.
	 * @return true if the element might be present, false otherwise.
	 */
	public boolean mightContain(String element) {
		for (Function<String, Integer> hashFunc : hashFunctions) {
			int hash = hashFunc.apply(element);
			if (!bitSet.get(Math.abs(hash % size))) {
				return false; // If any bit is not set, the element is definitely not in the set.
			}
		}
		return true; // All bits are set, so the element might be in the set (or it's a false
						// positive).
	}
	
	/**
	 * Checks if the filter has any elements
	 * 
	 * @return true if the filter is empty, false otherwise
	 */
	public boolean isEmpty() {
		return bitSet.isEmpty();
	}
}
