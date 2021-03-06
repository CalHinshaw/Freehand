package com.freehand.test;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

import com.freehand.editor.canvas.ICanvScreenConverter;
import com.freehand.editor.canvas.Note;
import com.freehand.editor.canvas.Pen;
import com.freehand.ink.MiscGeom;
import com.freehand.ink.Point;

public class VisualTests {
	
	public static void mathTests (Canvas c) {
		c.drawColor(0xffffffff);
		
		Paint p = new Paint();
		p.setColor(Color.BLUE);
		p.setStyle(Paint.Style.STROKE);
		p.setAntiAlias(true);
		
		Point c1 = new Point(300, 700);
		float r1 = 40;
		Point c2 = new Point(330, 750);
		float r2 = 100;
		
		c.drawCircle(c1.x, c1.y, r1, p);
		c.drawCircle(c2.x, c2.y, r2, p);
		
		Point[] tangents = MiscGeom.calcExternalBitangentPoints(c1, r1, c2, r2);
		if (tangents != null) {
			Point v1 = new Point(tangents[0].x - tangents[1].x, tangents[0].y - tangents[1].y);
			Point v2 = new Point(tangents[2].x - tangents[3].x, tangents[2].y - tangents[3].y);
			
			p.setColor(Color.RED);
			c.drawLine(tangents[0].x - 1000*v1.x, tangents[0].y - 1000*v1.y, tangents[0].x + 1000*v1.x, tangents[0].y + 1000*v1.y, p);
			c.drawLine(tangents[2].x - 1000*v2.x, tangents[2].y - 1000*v2.y, tangents[2].x + 1000*v2.x, tangents[2].y + 1000*v2.y, p);
		}
		
		Point[] intersections = MiscGeom.circleCircleIntersection(c1, r1, c2, r2);
		if (intersections != null) {
			p.setColor(Color.GREEN);
			p.setStyle(Paint.Style.FILL);
			
			c.drawCircle(intersections[0].x, intersections[0].y, 3, p);
			c.drawCircle(intersections[1].x, intersections[1].y, 3, p);
		}
	}
	
	
	
	
	
	public static void penBacktrackTests (final Canvas c) {
		c.drawColor(Color.WHITE);
		
		List<Point> pts = new ArrayList<Point>();
		List<Float> pressures = new ArrayList<Float>();
		
		pts.add(new Point(100, 300));
		pressures.add(0.2f);
		
		pts.add(new Point(198, 300));
		pressures.add(0.2f);
		
		pts.add(new Point(202, 300));
		pressures.add(1.0f);
		
		pts.add(new Point(300, 300));
		pressures.add(1.0f);
		
		penTester(c, pts, pressures, 10.0f, true);
	}
	
	
	public static void breakContainmentTests (final Canvas c) {
		c.drawColor(Color.WHITE);
		
		breakContainmentCleanDoubleBackTest(c);
		breakContainmentDirtyForwardTest(c);
		breakContainmentCircIntersectionTest(c);
		breakContainmentCircIntersectionBackInTest(c);
	}
	
	public static void breakContainmentCleanDoubleBackTest (final Canvas c) {
		List<Point> pts = new ArrayList<Point>();
		List<Float> pressures = new ArrayList<Float>();
		
		pts.add(new Point(300, 300));
		pressures.add(1.0f);
		
		pts.add(new Point(202, 300));
		pressures.add(1.0f);
		
		pts.add(new Point(198, 300));
		pressures.add(0.2f);
		
		pts.add(new Point(199, 302));
		pressures.add(0.2f);
		
		pts.add(new Point(400, 200));
		pressures.add(0.2f);
		
		penTester(c, pts, pressures, 10.0f, true);
	}
	
	
	public static void breakContainmentCircIntersectionTest (final Canvas c) {
		List<Point> pts = new ArrayList<Point>();
		List<Float> pressures = new ArrayList<Float>();
		
		pts.add(new Point(600, 600));
		pressures.add(1.0f);
		
		pts.add(new Point(502, 600));
		pressures.add(1.0f);
		
		pts.add(new Point(498, 600));
		pressures.add(0.2f);
		
		pts.add(new Point(493, 602));
		pressures.add(0.2f);
		
		pts.add(new Point(400, 600));
		pressures.add(0.2f);
		
		penTester(c, pts, pressures, 10.0f, true);
	}
	
	
	public static void breakContainmentCircIntersectionBackInTest (final Canvas c) {
		List<Point> pts = new ArrayList<Point>();
		List<Float> pressures = new ArrayList<Float>();
		
		pts.add(new Point(300, 600));
		pressures.add(1.0f);
		
		pts.add(new Point(202, 600));
		pressures.add(1.0f);
		
		pts.add(new Point(198, 600));
		pressures.add(0.2f);
		
		pts.add(new Point(193, 602));
		pressures.add(0.2f);
		
		pts.add(new Point(198, 595));
		pressures.add(0.2f);
		
		pts.add(new Point(100, 600));
		pressures.add(0.2f);
		
		penTester(c, pts, pressures, 10.0f, true);
	}
	
	
	public static void breakContainmentDirtyForwardTest(final Canvas c) {
		List<Point> pts = new ArrayList<Point>();
		List<Float> pressures = new ArrayList<Float>();
		
		pts.add(new Point(600, 300));
		pressures.add(1.0f);
		
		pts.add(new Point(502, 300));
		pressures.add(1.0f);
		
		pts.add(new Point(498, 300));
		pressures.add(0.2f);
		
		pts.add(new Point(491, 302));
		pressures.add(0.2f);
		
		pts.add(new Point(491, 296));
		pressures.add(0.2f);
		
		pts.add(new Point(400, 300));
		pressures.add(0.2f);
		
		penTester(c, pts, pressures, 10.0f, true);
	}
	
	
	public static void standardPenTests (final Canvas c) {
		c.drawColor(Color.WHITE);
		
		straitPenTest(c);
	}
	
	
	public static void straitPenTest (final Canvas c) {
		List<Point> pts = new ArrayList<Point>();
		List<Float> pressures = new ArrayList<Float>();
		
		pts.add(new Point(300, 300));
		pressures.add(1.0f);
		
		pts.add(new Point(301, 301));
		pressures.add(1.0f);
		
		pts.add(new Point(302, 302));
		pressures.add(1.0f);
		
		pts.add(new Point(303, 301));
		pressures.add(1.0f);
		
		pts.add(new Point(304, 300));
		pressures.add(1.0f);
		
		pts.add(new Point(320, 300));
		pressures.add(1.0f);
		
		penTester(c, pts, pressures, 10.0f, false);
	}
	
	
	
	
	
	private static void penTester (final Canvas c, final List<Point> pts, final List<Float> pressures, final float size, final boolean showNumbers) {
		final Pen pen = new Pen(new Note(), new testCSConv(), null, 1.0f, Color.BLACK, 10.0f, true);
		final MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, pts.get(0).x, pts.get(0).y, pressures.get(0), 0, 0, 1, 1, 1, 0);
		for (int i = 1; i < pts.size(); i++) {
			event.addBatch(0, pts.get(i).x, pts.get(i).y, pressures.get(i), 0, 0);
		}
		
		pen.onMotionEvent(event);
		pen.draw(c);
//		drawCircles(c, pts, pressures, 10.0f, showNumbers);
	}
	
	
	private static void drawCircles (final Canvas c, final List<Point> pts, final List<Float> pressures, final float size, final boolean showNumbers) {
		final Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(0);
		
		for (int i = 0; i < pts.size(); i++) {
			c.drawCircle(pts.get(i).x, pts.get(i).y, pressures.get(i)*size, paint);
			if (showNumbers == true) {
				c.drawText(Integer.toString(i), pts.get(i).x, pts.get(i).y, paint);
			}
		}
	}
	
	
	private static class testCSConv implements ICanvScreenConverter {
		public float canvToScreenDist(float canvasDist) { return 0; }
		public float screenToCanvDist(float screenDist) { return 0; }
		public Rect canvRectToScreenRect(RectF canvRect) { return null; }
	}
	
	

//	private void visualPolyTests (Canvas c) {
	
	//	private Paint debugPaint1 = new Paint();
	//	private Paint debugPaint2 = new Paint();
	//	private Paint inPaint = new Paint();
	//	private Paint outPaint = new Paint();
	
	
	//	debugPaint1.setColor(Color.BLACK);
	//	debugPaint1.setStyle(Paint.Style.STROKE);
	//	debugPaint1.setStrokeWidth(0);
	//	debugPaint1.setAntiAlias(true);
	//	
	//	debugPaint2.setColor(0xa0ff0000);
	//	debugPaint2.setStyle(Paint.Style.STROKE);
	//	debugPaint2.setStrokeWidth(1);
	//	debugPaint2.setAntiAlias(true);
	//	
	//	inPaint.setColor(Color.GREEN);
	//	inPaint.setStyle(Paint.Style.FILL);
	//	inPaint.setAntiAlias(true);
	//	
	//	outPaint.setColor(Color.BLUE);
	//	outPaint.setStyle(Paint.Style.FILL);
	//	outPaint.setAntiAlias(true);
	
	
	
	
//		WrapList<Point> square1 = new WrapList<Point>();
//		WrapList<Point> square3 = new WrapList<Point>();
//		
//		square1.add(new Point(-100, -400));
//		square1.add(new Point(-100, 50));
//		square1.add(new Point(-50, 25));
//		square1.add(new Point(0, 50));
//		square1.add(new Point(0, -400));
//		
//		square3.add(new Point(200, 40f));
//		square3.add(new Point(200, -90));
//		square3.add(new Point(-200, -90));
//		square3.add(new Point(-200, 40f));
//		
//		drawDebugPolys(c, square1, square3);
//		
//		
//		WrapList<Point> shape1 = new WrapList<Point>();
//		WrapList<Point> shape2 = new WrapList<Point>();
//		
//		shape1.add(new Point(-100, 100));
//		shape1.add(new Point(-100, 550));
//		shape1.add(new Point(-50, 525));
//		shape1.add(new Point(0, 550));
//		shape1.add(new Point(0, 100));
//		
//		shape2.add(new Point(0, 550f));
//		shape2.add(new Point(0, 400));
//		shape2.add(new Point(-200, 400));
//		shape2.add(new Point(-200, 550f));
//		
//		drawDebugPolys(c, shape1, shape2);
//		
//		
//		WrapList<Point> hole1 = new WrapList<Point>();
//		WrapList<Point> hole2 = new WrapList<Point>();
//		
//		hole1.add(new Point(500, 700));
//		hole1.add(new Point(550, 750));
//		hole1.add(new Point(700, 700));
//		hole1.add(new Point(700, 500));
//		hole1.add(new Point(500, 500));
//		
//		hole2.add(new Point(550, 600));
//		hole2.add(new Point(550, 450));
//		hole2.add(new Point(650, 450));
//		hole2.add(new Point(650, 600));
//		hole2.add(new Point(700, 600));
//		hole2.add(new Point(700, 400));
//		hole2.add(new Point(500, 400));
//		
//		drawDebugPolys(c, hole1, hole2);
//		
//		
//		WrapList<Point> inside1 = new WrapList<Point>();
//		WrapList<Point> inside2 = new WrapList<Point>();
//		
//		inside1.add(new Point(600, 100));
//		inside1.add(new Point(500, 100));
//		inside1.add(new Point(550, 200));
//		
//		inside2.add(new Point(550, 150));
//		inside2.add(new Point(545, 150));
//		inside2.add(new Point(547.5f, 155));
//		
//		drawDebugPolys(c, inside1, inside2);
//		
//		
//		ArrayList<Point> points = new ArrayList<Point>();
//		ArrayList<Float> sizes = new ArrayList<Float>();
//		
//		points.add(new Point(100, 600));
//		points.add(new Point(110, 600));
//		points.add(new Point(110, 615));
//		points.add(new Point(90, 625));
//		points.add(new Point(90, 650));
//		points.add(new Point(90, 650));
//		points.add(new Point(100, 640));
//		points.add(new Point(91, 590));
//		
//		sizes.add(8f);
//		sizes.add(7f);
//		sizes.add(4f);
//		sizes.add(4f);
//		sizes.add(5f);
//		sizes.add(3f);
//		sizes.add(6f);
//		sizes.add(3f);
//		
//		WrapList<Point> poly = BooleanPolyGeom.buildPolygon(points, sizes);
//		
//		currentPath.reset();
//		currentPath.moveTo(poly.get(0).x, poly.get(0).y);
//		for (int i = 0; i <= poly.size(); i++) {
//			currentPath.lineTo(poly.get(i).x, poly.get(i).y);
//		}
//		c.drawPath(currentPath, currentPaint);
//		
//		
//		currentPath.reset();
//		
//		currentPath.moveTo(100, 800);
//		currentPath.lineTo(100, 800);
//		currentPath.lineTo(100, 1000);
//		currentPath.lineTo(300, 1000);
//		currentPath.lineTo(300, 1000);
//		currentPath.lineTo(300, 900);
//		currentPath.lineTo(50, 900);
//		currentPath.lineTo(50, 930);
//		currentPath.lineTo(250, 930);
//		currentPath.lineTo(250, 960);
//		currentPath.lineTo(150, 960);
//		currentPath.lineTo(150, 800);
//		
//		c.drawPath(currentPath, currentPaint);
//		
//	}
//	
//	
//	
//	private void drawDebugPolys (Canvas c, WrapList<Point> poly1, WrapList<Point> poly2) {
//
//		WrapList<Vertex> graph = BooleanPolyGeom.buildPolyGraph(poly1, poly2);
//
//		WrapList<Point> union = BooleanPolyGeom.union(graph, poly1, poly2);
//		
//		currentPath.reset();
//		currentPath.moveTo(poly1.get(0).x, poly1.get(0).y);
//		for (int i = 0; i <= poly1.size(); i++) {
//			currentPath.lineTo(poly1.get(i).x, poly1.get(i).y);
//		}
//		c.drawPath(currentPath, debugPaint1);
//		
//		currentPath.reset();
//		currentPath.moveTo(poly2.get(0).x, poly2.get(0).y);
//		for (int i = 0; i <= poly2.size(); i++) {
//			currentPath.lineTo(poly2.get(i).x, poly2.get(i).y);
//		}
//		c.drawPath(currentPath, debugPaint1);
//		
//		for (int i = 0; i < union.size(); i++) {
//			c.drawLine(union.get(i).x, union.get(i).y, union.get(i+1).x, union.get(i+1).y, debugPaint2);
//		}
//		
//		if (graph.size() > 1) {
//			c.drawCircle(graph.get(0).intersection.x, graph.get(0).intersection.y, 1.5f, inPaint);
//		}
//		
//		
//		for (Vertex v : graph) {
//			if (v.poly1Entry == true) {
//				c.drawCircle(v.intersection.x, v.intersection.y, 0.75f, inPaint);
//			} else {
//				c.drawCircle(v.intersection.x, v.intersection.y, 0.75f, outPaint);
//			}
//		}
//	}
}