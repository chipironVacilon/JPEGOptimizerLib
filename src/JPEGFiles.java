package com.odilotid.common.util.imgOptimizer;

import org.apache.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class JPEGFiles {

	private static Logger logger = Logger.getLogger(JPEGFiles.class);

	private File _src;
	private File _dst;
	
	private long _originalSrcSize;
	
	public JPEGFiles(File src) {
		_src = src;
		_dst = new File(src.getAbsolutePath());
		_originalSrcSize = _src.length();
	}

	public File getSrc() {
		return _src;
	}

	public Double getEarnRate() {
		Double earn = null;
		if (_dst != null && _src != null) {
			if (_dst.exists() && _src.exists()) {
				earn = 1. - (_dst.length() / (double)_originalSrcSize);
			}
		}
		return earn;
	}
	
	public Long getEarnSize() {
		Long earn = null;
		if (_dst != null && _src != null) {
			if (_dst.exists() && _src.exists()) {
				earn = _originalSrcSize - _dst.length();
			}
		}
		return earn;
	}

	private boolean optimize(BufferedImage img1, File tmp, int quality, double maxVisualDiff) throws IOException {
		logger.debug("   Trying quality " + quality + "%");
		
		if (tmp.exists()) {
			tmp.delete();
		}
		
		long start1 = System.currentTimeMillis();
		ImageUtils.createJPEG(_src, tmp, quality);
		long end1 = System.currentTimeMillis();
		logger.debug("   * Size : " + ImageUtils.fileSize(tmp.length()) + "\t (" + ImageUtils.interval(end1-start1) + ")");
		
		long start2 = System.currentTimeMillis();
		BufferedImage img2 = ImageIO.read(tmp);
		double diff = ImageUtils.computeSimilarityRGB(img1, img2);
		long end2 = System.currentTimeMillis();
		
		img2 = null;
		logger.debug("   * Diff : " + ImageUtils.rate(diff) + "\t (" + ImageUtils.interval(end2-start2) + ")");
		diff *= 100.;			
		if (diff < maxVisualDiff) {
			logger.debug("   [OK] Visual diff is correct.");
			return true;
		} else {
			logger.debug("   [KO] Visual diff is too important, try a better quality.");
			return false;
		}
	}
	
	public void optimize(File dstDir, double maxVisualDiff) throws IOException {
		logger.info("Max Diff : " + maxVisualDiff);
		logger.info("Optimizing " + _src.getAbsolutePath() + " (" + ImageUtils.fileSize(_originalSrcSize) + ")");

		File tmp = new File(dstDir.getParentFile(), "JpegOptimizer.tmp.jpg");
		BufferedImage img1 = ImageIO.read(_src);
		int minQ = 0;
		int maxQ = 100;
		int foundQuality = -1;
		while (minQ <= maxQ) {
			logger.debug(" - Dichotomic search between (" + minQ + ", " + maxQ + ") qualities :");
			int quality = (int)Math.floor((minQ + maxQ) / 2.);
			if (optimize(img1, tmp, quality, maxVisualDiff) == true) {
				foundQuality = quality;
				maxQ = quality-1;
			} else {
				minQ = quality+1;
			}
		}
		img1 = null;
		String filePath = _src.getAbsolutePath();
		_src.renameTo(new File(filePath+".old.jpg"));
		File optFile = new File(filePath);
		if ((foundQuality >= 0) && (foundQuality < 100)) {
			ImageUtils.createJPEG(tmp, optFile, foundQuality);
			logger.info("Optimization complete. Original size: "+ImageUtils.fileSize(_originalSrcSize)+". Current Size: "+ ImageUtils.fileSize(optFile.length()));
			logger.info("Image size reduced "+ Math.round(this.getEarnRate()*100) +"% ("+ImageUtils.fileSize(this.getEarnSize())+")");
			File old = new File(filePath+".old.jpg");
			old.delete();
		} else {
			logger.info(" - [KO] Unable to optimize the file");
		}
		tmp.delete();
	}
}
