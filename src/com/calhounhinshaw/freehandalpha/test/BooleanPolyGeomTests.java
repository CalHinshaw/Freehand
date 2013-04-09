package com.calhounhinshaw.freehandalpha.test;

import java.util.ArrayList;

import com.calhounhinshaw.freehandalpha.ink.BooleanPolyGeom;
import com.calhounhinshaw.freehandalpha.ink.Point;
import com.calhounhinshaw.freehandalpha.ink.Vertex;
import com.calhounhinshaw.freehandalpha.misc.WrapList;

import android.test.AndroidTestCase;
import android.util.Log;

import junit.framework.Assert;

public class BooleanPolyGeomTests extends AndroidTestCase {
	private WrapList<Point> square1 = new WrapList<Point>();
	private WrapList<Point> square2 = new WrapList<Point>();
	private WrapList<Point> square3 = new WrapList<Point>();
	private WrapList<Point> square4 = new WrapList<Point>();
	private WrapList<Point> tri1 = new WrapList<Point>();
	private WrapList<Point> tri2 = new WrapList<Point>();
	
	@Override
	protected void setUp () {
		square1.add(new Point(0, 1));
		square1.add(new Point(1, 1));
		square1.add(new Point(1, 0));
		square1.add(new Point(0, 0));
		square1.add(new Point(0, 1));
		
		square2.add(new Point(3, 4));
		square2.add(new Point(4, 4));
		square2.add(new Point(4, 3));
		square2.add(new Point(3, 3));
		square2.add(new Point(3, 4));
		
		square3.add(new Point(-1, 0.5f));
		square3.add(new Point(2, 0.5f));
		square3.add(new Point(2, -1));
		square3.add(new Point(-1, -1));
		square3.add(new Point(-1, 0.5f));
		
		square4.add(new Point(1, 2));
		square4.add(new Point(2, 2));
		square4.add(new Point(2, 1));
		square4.add(new Point(1, 1));
		square4.add(new Point(1, 2));
		
		tri1.add(new Point(0, 0));
		tri1.add(new Point(1, -1));
		tri1.add(new Point(-1, -1));
		tri1.add(new Point(0, 0));
		
		tri2.add(new Point(0, 0));
		tri2.add(new Point(1, 1));
		tri2.add(new Point(-1, 1));
		tri2.add(new Point(0, 0));
	}
	
	
	
	public void testIntersectPolys () {
		ArrayList<Vertex> result;
		
		// Don't touch at all
		result = BooleanPolyGeom.intersectPolys(square1, square2);
		Assert.assertEquals(0, result.size());
		
		// Line-on-line intersection
		result = BooleanPolyGeom.intersectPolys(square1, square3);
		Assert.assertEquals(2, result.size());
		Point int1 = new Point(0, 0.5f);
		Point int2 = new Point(1, 0.5f);
		boolean int1Found = false;
		boolean int2Found = false;
		
		for (Vertex v : result) {
			if (v.intersection.equals(int1)) {
				int1Found = true;
			} else if (v.intersection.equals(int2)) {
				int2Found = true;
			}
		}
		
		Assert.assertTrue(int1Found);
		Assert.assertTrue(int2Found);
		
		// point-on-point squares
		result = BooleanPolyGeom.intersectPolys(square1, square4);
		Assert.assertEquals(4, result.size());
		
		
		
		// Point-on-point triangles
		result = BooleanPolyGeom.intersectPolys(tri1, tri2);
		Assert.assertEquals(4, result.size());
	}
	
	
	
	
	
	
	
	
	
}