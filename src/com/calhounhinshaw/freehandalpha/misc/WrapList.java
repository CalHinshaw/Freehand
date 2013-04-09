package com.calhounhinshaw.freehandalpha.misc;

import java.util.ArrayList;
import java.util.Collection;

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
}