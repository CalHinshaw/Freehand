package com.freehand.misc;

import java.util.List;

public class Util {
	
	/**
	 * Will wrap around for any index. For example, if list.size() == 10 and index is 13,
	 * fullWrap will return 3.
	 * @return
	 */
	public static int fWrap (final List<? extends Object> list, final int index) {
		if (index < 0) {
			return list.size() + (index % list.size());
		} else {
			return index % list.size();
		}
	}
	
	/**
	 * Will wrap around from -list.size() <= index < 2*list.size(). Is much faster than fullWrap because
	 * it doesn't use the modulo operator.
	 * @param list
	 * @param index
	 * @return
	 */
	public static int pWrap (final List<? extends Object> list, final int index) {
		if (index < 0) {
			return list.size() + index;
		} else if (index >= list.size()) {
			return index - list.size();
		} else {
			return index;
		}
	}
	
	public static <E> void addRangeToList (final List<E> target, final List<E> source, int start, int end, boolean direction) {
		if (direction == true) {
			while (start > end) {
				end += source.size();
			}
			
			for (int i = start; i <= end; i++) {
				target.add(source.get(i));
			}
		} else {
			while (end < start) {
				start += source.size();
			}
			
			for (int i = start; i >= end; i--) {
				target.add(source.get(i));
			}
		}
	}
}