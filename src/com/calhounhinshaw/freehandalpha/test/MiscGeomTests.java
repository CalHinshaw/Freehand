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
}