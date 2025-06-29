package in.foresthut.impact.commons.filters;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BloomFilterTest {
	@Test
	void test_whenElementExistsInFilter_shouldReturnTrue() {
		BloomFilter bf=new BloomFilter(10);
		
		bf.add("apple");
		bf.add("orange");
		
		assertTrue(bf.mightContain("apple"));
	}
	
	@Test
	void test_whenElementDoesNotExistInFilter_shouldReturnFalse() {
		BloomFilter bf=new BloomFilter(3);
		
		bf.add("apple");
		bf.add("orange");
		bf.add("lemon");
		bf.add("jackfruit");
		bf.add("mango");
			
		assertTrue(bf.mightContain("banana"));
	}
}
