package com.calhounhinshaw.freehandalpha.misc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WrapList<E> extends ArrayList<E> {
	private static final long serialVersionUID = 1L;
	
	public WrapList (int size) {
		super(size);
	}
	
	public WrapList () {
		super();
	}

	private int wrap (int index) {
		if (index < 0) {
			return super.size() + (index % super.size());
		} else {
			return index % super.size();
		}
	}
	
	@Override
	public E get (int index) {
		return super.get(wrap(index));
	}
	
	@Override
	public void add (int index, E element) {
		super.add(wrap(index), element);
	}
	
	@Override
	public boolean addAll (int index, Collection<? extends E> c) {
		return super.addAll(wrap(index), c);
	}
	
	@Override
	public E remove (int index) {
		return super.remove(wrap(index));
	}
	
	public void addRangeToList (List<E> l, int start, int end, boolean direction) {
		if (direction == true) {
			if (start > end) {
				end += this.size();
			}
			
			for (int i = start; i <= end; i++) {
				l.add(this.get(i));
			}
		} else {
			if (end < start) {
				start += this.size();
			}
			
			for (int i = start; i >= end; i--) {
				l.add(this.get(i));
			}
		}
	}
	
	public WrapList<E> getWrapSublist (int start, int end, boolean direction) {
		if (direction == true) {
			if (end < start) {
				end += this.size();
			}
			
			WrapList<E> toReturn = new WrapList<E>(end-start);
			
			for (int i = start; i <= end; i++) {
				toReturn.add(this.get(i));
			}
			return toReturn;
		} else {
			if (start < end) {
				start += this.size();
			}
			
			WrapList<E> toReturn = new WrapList<E>(start-end);
			
			for (int i = start; i >= end; i--) {
				toReturn.add(this.get(i));
			}
			
			return toReturn;
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
}