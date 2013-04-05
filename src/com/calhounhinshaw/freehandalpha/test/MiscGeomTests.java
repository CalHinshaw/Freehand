package com.calhounhinshaw.freehandalpha.test;

import android.test.AndroidTestCase;

import com.calhounhinshaw.freehandalpha.ink.MiscGeom;
import com.calhounhinshaw.freehandalpha.ink.Point;

import junit.framework.Assert;

public class MiscGeomTests extends AndroidTestCase {

	public void testIntersectLineIntoSegment () {
		// Test to make sure non-intersection is working
		Point l1 = new Point(0, 0);
		Point l2 = new Point(0, 1);
		Point s1 = new Point(1, 0);
		Point s2 = new Point(2, 0);
		
		Point result = MiscGeom.intersectLineIntoSegment(l1, l2, s1, s2);
		Assert.assertNull(result);
		
		// Test to make sure segment intersection is working
		l1 = new Point(0, -1);
		l2 = new Point(0, 1);
		s1 = new Point(-1, 0);
		s2 = new Point(1, 0);
		
		result = MiscGeom.intersectLineIntoSegment(l1, l2, s1, s2);
		Assert.assertTrue(result.x == 0 && result.y == 0);
		
		// Test to make sure the line intersects the segment
		l1 = new Point(0, -1);
		l2 = new Point(0, 1);
		s1 = new Point(-1, 3);
		s2 = new Point(1, 3);
		
		result = MiscGeom.intersectLineIntoSegment(l1, l2, s1, s2);
		Assert.assertTrue(result.x == 0 && result.y == 3);
		
		// Test to make sure the segment doesn't intersect the line
		l1 = new Point(-1, 3);
		l2 = new Point(1, 3);
		s1 = new Point(0, -1);
		s2 = new Point(0, 1);
		
		result = MiscGeom.intersectLineIntoSegment(l1, l2, s1, s2);
		Assert.assertNull(result);
	}
	
	
//	public void testFindCircleIndex () {
////		Point[] circle = 	{new Point (0, 1),
////							 new Point(0.7071f, 0.7071f),
////							 new Point (1, 0),
////							 new Point(0.7071f, -0.7071f),
////							 new Point (0, -1),
////							 new Point(-0.7071f, -0.7071f),
////							 new Point (-1, 0),
////							 new Point(-0.7071f, 0.7071f)};
////		
////		Point center = new Point(0, 0);
////		
////		Point test = new Point(0.7071f, 0.7071f);
////		int result = Geometry.findAdjacentCircleIndex(test, 1, center, true);
////		Assert.assertEquals(1, result);
////		
////		test = new Point(-0.7071f, 0.7071f);
////		result = Geometry.findAdjacentCircleIndex(test, 1, center, true);
////		Assert.assertEquals(0, result);
////		
////		test = new Point(0, 1);
////		result = Geometry.findAdjacentCircleIndex(test, 1, center, true);
////		Assert.assertEquals(0, result);
////		
////		test = new Point(1, 0);
////		result = Geometry.findAdjacentCircleIndex(test, 1, center, true);
////		Assert.assertEquals(2, result);
////		
////		test = new Point(-0.7071f, -0.706f);
////		result = Geometry.findAdjacentCircleIndex(test, 1, center, true);
////		Assert.assertEquals(6, result);
//	}
	
//	public void testTraceCircularPath () {
//		Point center;
//		float radius;
//		boolean clockwise;
//		Point from;
//		Point to;
//		List<Point> result;
//		
//		center = new Point(0, 0);
//		radius = 1;
//		clockwise = true;
//		from = new Point(0.1f, 0.9f);
//		to = new Point (0.1f, -0.9f);
//		result = MiscGeom.traceCircularPath(center, radius, clockwise, from, to);
//		Assert.assertEquals(3, result.size());
//		
//		center = new Point(0, 0);
//		radius = 1;
//		clockwise = false;
//		from = new Point(0.1f, 0.9f);
//		to = new Point (0.1f, -0.9f);
//		result = MiscGeom.traceCircularPath(center, radius, clockwise, from, to);
//		Assert.assertEquals(5, result.size());
//		
//	}
}