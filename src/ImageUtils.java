package com.odilotid.common.util.imgOptimizer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;

public class ImageUtils {

	public static double computeSimilarityRGB(BufferedImage img1, BufferedImage img2) throws IOException {
		return computeSimilarityRGB_Fastest(img1, img2);
	}
	//Optimized - Read Approach 8 :  http://chriskirk.blogspot.fr/2011/01/performance-comparison-of-java2d-image.html
	//And some personal optimization
	//Example image : 1.2 seconds to process
	public static double computeSimilarityRGB_Fastest(BufferedImage img1, BufferedImage img2) throws IOException {
		int width1 = img1.getWidth(null);
	    int width2 = img2.getWidth(null);
	    int height1 = img1.getHeight(null);
	    int height2 = img2.getHeight(null);
	    
	    if ((width1 != width2) || (height1 != height2)) {
	    	throw new IOException("Images have different sizes");
	    }
	    
	    DataBuffer db1   =  img1.getRaster().getDataBuffer();                                                 
	    DataBuffer db2 = img2.getRaster().getDataBuffer(); 	    
	    
	    
	    double diff = 0;
	    int size = db1.getSize(); //size = width * height * 3
	    double p = 0;
	    
	    //TODO: jpeg format v9 can use 12bit per channel, see: http://www.tomshardware.fr/articles/jpeg-lossless-12bit,1-46742.html
	    
	    if (size == (width1 * height1 * 3)) { //RGB 24bit per pixel - 3 bytes per pixel: 1 for R, 1 for G, 1 for B
	    	
		    for (int i = 0; i < size; i+= 3) {
				double deltaR = (db2.getElem(i) - db1.getElem(i));
				double deltaG = (db2.getElem(i+1) - db1.getElem(i+1));
				double deltaB = (db2.getElem(i+2) - db1.getElem(i+2));
				diff += Math.sqrt(((deltaR*deltaR) + (deltaG*deltaG) + (deltaB*deltaB)) / 65025.);
		    }
		    
		    double maxPixDiff = Math.sqrt(3); // max diff per color component is 1. So max diff on the 3 RGB component is 1+1+1.
		    double n = width1 * height1;
		    p = diff/(n*maxPixDiff);
		    
	    } else if (size ==  (width1 * height1)) { // Gray 8bit per pixel - Don't know if it's possible in jpeg, but just in case, code it! :)
	    	
		    for (int i = 0; i < size; ++i) {
				diff += (db2.getElem(i) - db1.getElem(i)) / 255;
		    }
		    p = diff / size;
	    }
	    

	    return p;
	}	
	
	//JPEG Copy input image to output image with the new quality, and copy too the EXIF data from input to output!
	public static void createJPEG(File input, File output, int quality) throws IOException {
		truncateFile(output);
		
		ImageInputStream iis = ImageIO.createImageInputStream(input);
		Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
		ImageReader reader = (ImageReader) readers.next();
		reader.setInput(iis, false);
		IIOMetadata metadata = reader.getImageMetadata(0);
		BufferedImage bi = reader.read(0);
		
		final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		writer.setOutput(new FileImageOutputStream(output));
		
		ImageWriteParam iwParam = writer.getDefaultWriteParam();
		iwParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwParam.setCompressionQuality(quality/100f);
		
		writer.write(null, new IIOImage(bi, null, metadata), iwParam);
		writer.dispose();
		
		reader.dispose();
		
		writeQualityInJPEG(output, quality);
	}
	
	//Save the input image as a jpeg file
	public static void createJPEG(BufferedImage input, File output, int quality) throws IOException {
		truncateFile(output);
		
		final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
		writer.setOutput(new FileImageOutputStream(output));
		
		ImageWriteParam iwParam = writer.getDefaultWriteParam();
		iwParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		iwParam.setCompressionQuality(quality/100f);
		
		writer.write(null, new IIOImage(input, null, null), iwParam);
		writer.dispose();
		
		writeQualityInJPEG(output, quality);
	}
	
	//Special trick to read the quality in the last byte of the file (because JFIF/EXIF do not have this info)
	public static int readQualityInJPEG(File input) throws IOException {
		if ((input == null) || (input.exists() == false)) {
			return -1;
		} else {
			FileInputStream in = new FileInputStream(input);
			in.getChannel().position(in.getChannel().size() - 2);
			int b1 = in.read();
			int b2 = in.read();
			int quality = -1;
			if ((b1 == 0xFF) && (b2 == 0xD9)) { //0xFFD9 it's the EOI (End Of Image jpeg tag), meaning JPEGOptimized does not append the quality byte
				quality = -1;
			} else {
				quality = b2;
			}
			in.close();
			return quality;
		}
	}
	
	//Special trick to write the quality in the last byte of the file (because JFIF/EXIF do not have this info)
	private static void writeQualityInJPEG(File output, int quality) throws IOException {
		FileOutputStream out = new FileOutputStream(output, true);
		out.write(quality & 0x7F);
		out.close();
	}

	private static void truncateFile(File file) throws IOException {
		if (file.exists()) {
			FileOutputStream fos = new FileOutputStream(file, true);
			FileChannel outChan = fos.getChannel();
			outChan.truncate(0);
			outChan.close();
			fos.close();
		}
	}

	public static String fileSize(long size) {
		if(size <= 0) return "0";
		final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
		return new DecimalFormat("#,##0.0#").format(size/Math.pow(1024, digitGroups)) + "" + units[digitGroups];
	}

	public static String rate(double rate) {
		return new DecimalFormat("#0.00%").format(rate);
	}

	public static String interval(final long l)  {
		final long hr = TimeUnit.MILLISECONDS.toHours(l);
		final long min = TimeUnit.MILLISECONDS.toMinutes(l - TimeUnit.HOURS.toMillis(hr));
		final long sec = TimeUnit.MILLISECONDS.toSeconds(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
		final long ms = TimeUnit.MILLISECONDS.toMillis(l - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
		return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
	}
	
}
