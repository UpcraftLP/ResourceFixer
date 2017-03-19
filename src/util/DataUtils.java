package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataUtils {

	public static <T> List<T[]> divideArray(T[] source, int chunksize) {

	    List<T[]> result = new ArrayList<T[]>();
	    int start = 0;
	    while (start < source.length) {
	        int end = Math.min(source.length, start + chunksize);
	        result.add(Arrays.copyOfRange(source, start, end));
	        start += chunksize;
	    }
	    return result;
	}
}
