package com.freehand.test;

import java.util.ArrayList;

import com.freehand.ink.BooleanPolyGeom;
import com.freehand.ink.Point;
import com.freehand.ink.Vertex;
import com.freehand.misc.WrapList;

import android.test.AndroidTestCase;
import android.util.Log;

import junit.framework.Assert;

public class WrapListTests extends AndroidTestCase {
	
	private WrapList<Integer> test = new WrapList<Integer>(10);
	
	@Override
	protected void setUp () {
		test.add(0);
		test.add(1);
		test.add(2);
		test.add(3);
		test.add(4);
		test.add(5);
		test.add(6);
		test.add(7);
		test.add(8);
	}
	
	public void testAddRangeToList () {
		ArrayList<Integer> result = new ArrayList<Integer>(15);
		
		test.addRangeToList(result, 0, test.size()-1, true);
		Assert.assertEquals(test.size(), result.size());
		result.clear();
		
		test.addRangeToList(result, 1, 4, true);
		Assert.assertEquals(4, result.size());
		Assert.assertEquals(new Integer(1), result.get(0));
		result.clear();
		
		test.addRangeToList(result, 5, 2, true);
		for (Integer i : result) {
			Log.d("PEN", i.toString());
		}
	}
}