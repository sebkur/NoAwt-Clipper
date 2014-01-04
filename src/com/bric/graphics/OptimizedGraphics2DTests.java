/*
 * @(#)OptimizedGraphics2DTests.java
 *
 * $Date: 2012-07-03 01:10:05 -0500 (Tue, 03 Jul 2012) $
 *
 * Copyright (c) 2011 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * http://javagraphics.java.net/
 * 
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */
package com.bric.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.bric.awt.TransformedTexturePaint;
import com.bric.util.JVM;

/** A set of tests for the <code>OptimizedGraphics2D</code> class.
 * 
 * @name OptimizedGraphics2D
 * @title Graphics2D: Improving Performance
 * @release February 2009
 * @blurb It turns out some <code>Graphics2D</code> operations are painfully inefficient -- and we can easily intercept and divert those methods!
 * <p>This is becoming less of a problem now that Quartz is being phased out, but even without Quartz we can still make some improvements...
 * @see <a href="http://javagraphics.blogspot.com/2009/02/graphics2d-improving-performance.html">Graphics2D: Improving Performance</a>
 *
 */
public class OptimizedGraphics2DTests {
	
	public static void main(String[] args) {
		OptimizedGraphics2D.testingOptimizations = true;
		
		JTextArea textArea = new JTextArea();
		
		textArea.setText("Beginning tests on:\n\tOS = "+System.getProperty("os.name")+" ("+System.getProperty("os.version")+", "+System.getProperty("os.arch")+")\n\tJava Version = "+System.getProperty("java.version"));
		if(JVM.isMac) {
			boolean quartz = JVM.usingQuartz;
			textArea.setText(textArea.getText()+"\n"+"\tapple.awt.graphics.UseQuartz = "+quartz);
		}
		BufferedImage dest = new BufferedImage(600,600, BufferedImage.TYPE_INT_ARGB);
		

		Test[] tests = new Test[] {
			new GlyphVectorTest(),
			new FillClipTest(),
			new ImageBackgroundTest(),
			new ClipTest(),
			new ClipBoundsTest()
		};
		
		String[][] table = new String[tests.length+1][4];
		
		table[0][0] = "TEST NAME";
		table[0][1] = "NORMAL (ms)";
		table[0][2] = "OPTIMIZED (ms)";
		table[0][3] = "%";
		
		for(int a = 0; a<tests.length; a++) {
			
			long[] timesPlain = new long[10];
			long[] timesOptimized = new long[timesPlain.length];
			for(int b = 0; b<timesPlain.length; b++) {
				timesPlain[b] = System.currentTimeMillis();
				tests[a].run(dest, false);
				timesPlain[b] = System.currentTimeMillis()-timesPlain[b];
				
				timesOptimized[b] = System.currentTimeMillis();
				tests[a].run(dest, true);
				timesOptimized[b] = System.currentTimeMillis()-timesOptimized[b];
			}
			Arrays.sort(timesPlain);
			Arrays.sort(timesOptimized);
			
			long t1 = timesPlain[timesPlain.length/2];
			long t2 = timesOptimized[timesOptimized.length/2];
			table[a+1][1] = Long.toString(t1);
			table[a+1][2] = Long.toString(t2);
			float f = (t2*10000/t1);
			f = f/100f;
			table[a+1][3] = Float.toString(f)+"%";
			
			if(f>=100) {
				table[a+1][0] = "F "+tests[a].getName();
			} else {
				table[a+1][0] = "P "+tests[a].getName();
			}
		}
		
		for(int column = 0; column<table[0].length; column++) {
			int columnLength = 0;
			for(int row = 0; row<table.length; row++) {
				columnLength = Math.max(columnLength,table[row][column].length());
			}
			for(int row = 0; row<table.length; row++) {
				while(table[row][column].length()<columnLength+1) {
					table[row][column] = table[row][column]+" ";
				}
			}
		}
		
		for(int row = 0; row<table.length; row++) {
			StringBuffer sb = new StringBuffer();
			for(int column = 0; column<table[row].length; column++) {
				sb.append(table[row][column]);
			}
			textArea.setText(textArea.getText()+"\n"+sb);
		}
		
		textArea.setFont(new Font("Monospaced",0,13));
		JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new JScrollPane(textArea));
		frame.pack();
		frame.setVisible(true);
	}
}

abstract class Test {
	BufferedImage sampleImage = createImage(400,400);
	
	protected BufferedImage createImage(int w,int h) {
		BufferedImage bi = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
		//color the image randomly
		int[] row = new int[bi.getWidth()];
		for(int y = 0; y<bi.getHeight(); y++) {
			for(int b = 0; b<row.length; b++) {
				row[b] = (int)(Math.random()*0xFFFFFFFF);
			}
			bi.getRaster().setDataElements(0,y,bi.getWidth(),1,row);
		}
		return bi;
	}
	
	public abstract void run(BufferedImage dest,boolean optimize);
	public abstract String getName();
}

class ClipBoundsTest extends Test {

	@Override
	public String getName() {
		return "getClipBounds() Test";
	}

	@Override
	public void run(BufferedImage dest, boolean optimize) {
		Graphics2D g = dest.createGraphics();
		if(optimize) {
			g = new OptimizedGraphics2D(g,OptimizedGraphics2D.OPTIMIZE_CLIP_BOUNDS);
		}
		g.setClip(new Ellipse2D.Float(.5f,5.5f,dest.getWidth()/2,dest.getHeight()/2));
		
		Rectangle destRect = new Rectangle();
		for(int a = 0; a<100000; a++) {
			g.getClipBounds(destRect);
		}
		g.dispose();
	}
	
}

class GlyphVectorTest extends Test {

	@Override
	public String getName() {
		return "drawGlyphVector() Test";
	}
	
	Font font = new Font("Verdana",0,32);

	@Override
	public void run(BufferedImage dest, boolean optimize) {
		Random random = new Random(0);
		Graphics2D g = dest.createGraphics();
		GlyphVector glyphVector = font.createGlyphVector(g.getFontRenderContext(), "Testing Glyph Vectors!");
		if(optimize) {
			g = new OptimizedGraphics2D(g);
		}
		g.setPaint(new TransformedTexturePaint(sampleImage,
					new Rectangle2D.Float(5,5,100,100),
					new AffineTransform(1,.1,.1,1,0,0)));
		
		for(int a = 0; a<1000; a++) {
			g.drawGlyphVector(glyphVector, 
					(int)(400*random.nextDouble()), 
					(int)(400*random.nextDouble()));
		}
		
		g.dispose();
	}
}

class ClipTest extends Test {

	@Override
	public String getName() {
		return "clip() Test";
	}

	@Override
	public void run(BufferedImage dest, boolean optimize) {
		Graphics2D g = dest.createGraphics();
		if(optimize)
			g = new OptimizedGraphics2D(g);
		
		Random random = new Random(0);
		
		Rectangle2D rect = new Rectangle2D.Float();
		Ellipse2D oval = new Ellipse2D.Float();
		for(int a = 0; a<1000; a++) {
			rect.setFrame(random.nextInt(200),random.nextInt(200),random.nextInt(200),random.nextInt(200));
			oval.setFrame(random.nextInt(200),random.nextInt(200),random.nextInt(200),random.nextInt(200));
			if(a%2==0) {
				g.setClip(rect);
				g.clip(oval);
			} else {
				g.setClip(oval);
				g.clip(rect);
			}
		}
		g.dispose();
	}
	
}

class ImageBackgroundTest extends Test {

	@Override
	public String getName() {
		return "drawImage() Background Test";
	}

	@Override
	public void run(BufferedImage dest, boolean optimize) {
		Graphics2D g = dest.createGraphics();
		if(optimize)
			g = new OptimizedGraphics2D(g);
		
		g.setPaint(new TransformedTexturePaint(sampleImage,
				new Rectangle2D.Float(5,5,100,100),
				new AffineTransform(1,.1,.1,1,0,0)));
		
		Random random = new Random(100);
		for(int a = 0; a<100; a++) {
			g.drawImage(sampleImage,random.nextInt(100),random.nextInt(100),null);
		}
		g.dispose();
	}
}

class FillClipTest extends Test {

	@Override
	public String getName() {
		return "fill() Clipping Test";
	}

	@Override
	public void run(BufferedImage dest, boolean optimize) {
		Graphics2D g = dest.createGraphics();
		if(optimize)
			g = new OptimizedGraphics2D(g,OptimizedGraphics2D.OPTIMIZE_CLIPPED_SHAPES);
		
		g.setColor(Color.blue);
		
		g.setClip(0,0,dest.getWidth()/2,dest.getHeight()/2);
		
		Random random = new Random(0);
		for(int a = 0; a<10000; a++) {
			g.fillRoundRect(dest.getWidth()/2+random.nextInt(100),
					dest.getHeight()/2+random.nextInt(100),
					10+random.nextInt(50),
					10+random.nextInt(50),
					5,5);
		}
		g.dispose();
	}
	
}
