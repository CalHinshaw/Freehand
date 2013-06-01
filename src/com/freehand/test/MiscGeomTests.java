package com.freehand.test;

import android.graphics.RectF;
import android.test.AndroidTestCase;
import android.util.Log;

import com.freehand.ink.MiscGeom;
import com.freehand.ink.Point;

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
	
	public void testCircleSegmentIntersection () {
		Point[] result;
		Point c, T, H;
		float r;
		
		c = new Point(0, 0);
		r = 1;
		T = new Point (0, 0);
		H = new Point (2, 0);
		
		result = MiscGeom.circleSegmentIntersection(c, r, T, H);
		Assert.assertEquals(1, result[0].x, 0.00000001f);
		Assert.assertEquals(0, result[0].y, 0.00000001f);
		Assert.assertNull(result[1]);
		
		c = new Point(0, 0);
		r = 1.0f;
		T = new Point (0.1f, 0.1f);
		H = new Point (2, 2);
		
		result = MiscGeom.circleSegmentIntersection(c, r, T, H);
		Assert.assertEquals(r*Math.cos(Math.PI/4), result[0].x, 0.00001f);
		Assert.assertEquals(r*Math.sin(Math.PI/4), result[0].y, 0.00001f);
		Assert.assertNull(result[1]);
		
		c = new Point(0, 0);
		r = 1.0f;
		T = new Point (0.1f, (float) (r*Math.sin(Math.PI/4)));
		H = new Point (2, (float) (r*Math.sin(Math.PI/4)));
		
		result = MiscGeom.circleSegmentIntersection(c, r, T, H);
		Assert.assertEquals(r*Math.cos(Math.PI/4), result[0].x, 0.00001f);
		Assert.assertEquals(r*Math.sin(Math.PI/4), result[0].y, 0.00001f);
		Assert.assertNull(result[1]);
		
		c = new Point(0, 1);
		r = 1.0f;
		T = new Point (0.1f, (float) (r*Math.sin(Math.PI/4)) + 1);
		H = new Point (2, (float) (r*Math.sin(Math.PI/4)) + 1);
		
		result = MiscGeom.circleSegmentIntersection(c, r, T, H);
		Assert.assertEquals(r*Math.cos(Math.PI/4), result[0].x, 0.00001f);
		Assert.assertEquals(r*Math.sin(Math.PI/4)+1, result[0].y, 0.00001f);
		Assert.assertNull(result[1]);
		
		
		c = new Point(0, 0);
		r = 1;
		T = new Point (0.5f, 0);
		H = new Point (-2, 0);
		
		result = MiscGeom.circleSegmentIntersection(c, r, T, H);
		Assert.assertEquals(-1, result[0].x, 0.00000001f);
		Assert.assertEquals(0, result[0].y, 0.00000001f);
		Assert.assertNull(result[1]);
		
		c = new Point(0, 0);
		r = 1;
		T = new Point (0, 0);
		H = new Point (-2, -2);
		
		result = MiscGeom.circleSegmentIntersection(c, r, T, H);
		Assert.assertEquals(-Math.sqrt(2)/2, result[0].x, 0.000001f);
		Assert.assertEquals(-Math.sqrt(2)/2, result[0].y, 0.000001f);
		Assert.assertNull(result[1]);
		
	}
	
	
	
	public void testCheckCircleContainment () {
		Point refP, checkP;
		float refR, checkR;
		
		refP = new Point(0, 0);
		checkP = new Point(5, 5);
		refR = 1;
		checkR = 1;
		Assert.assertFalse(MiscGeom.checkCircleContainment(refP, refR, checkP, checkR));
		
		refP = new Point(5, 5);
		checkP = new Point(5, 5);
		refR = 0.5f;
		checkR = 1;
		Assert.assertFalse(MiscGeom.checkCircleContainment(refP, refR, checkP, checkR));
		
		refP = new Point(5, 5);
		checkP = new Point(5, 5);
		refR = 1;
		checkR = 0.5f;
		Assert.assertTrue(MiscGeom.checkCircleContainment(refP, refR, checkP, checkR));
		
		refP = new Point(5, 5);
		checkP = new Point(6, 5);
		refR = 2;
		checkR = 0.5f;
		Assert.assertTrue(MiscGeom.checkCircleContainment(refP, refR, checkP, checkR));
		
		refP = new Point(5, 5);
		checkP = new Point(6, 5);
		refR = 2;
		checkR = 1;
		Assert.assertTrue(MiscGeom.checkCircleContainment(refP, refR, checkP, checkR));
		
		refP = new Point(5, 5);
		checkP = new Point(6, 5);
		refR = 2;
		checkR = 1.001f;
		Assert.assertFalse(MiscGeom.checkCircleContainment(refP, refR, checkP, checkR));
	}
	
	public void testRectF () {
		RectF a = new RectF(0, 0, 1, 1);
		RectF b = new RectF(0.25f, 0.25f, 0.75f, 0.75f);
		
		Assert.assertTrue(RectF.intersects(a, b));
	}
	
	
	
}