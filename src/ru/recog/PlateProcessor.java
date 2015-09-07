package ru.recog;

import java.io.File;
import java.util.List;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import ru.recog.feature.*;
import ru.recog.imgproc.*;
import ru.recog.nn.NNAnalysis;
import ru.recog.nn.NNWrapper;

public class PlateProcessor {
	
	
	private NNWrapper nn;
	private CompoundImageProcessor cip;
//	private MultipleFeatureExtractor mfx;
	
	public PlateProcessor(NNWrapper nn, CompoundImageProcessor cip) {
		this.nn = nn;
		this.cip = cip;
//		this.mfx = mfx;
	}
	
	
	public String getLPString(List<Mat> pieces) {
		StringBuilder sb = new StringBuilder();
		
		for (Mat piece : pieces) {
			Mat proc = cip.processImage(piece);
			
			Mat scaled = ImageUtils.scaleUp(proc, 3);
			sb.append(NNAnalysis.nnOutputToSymbol(nn.getNNOutputArray(scaled)));
//			lf.addImage(scaled, NNAnalysis.convertNNOutputToString(nn.getNNOutputArray(scaled)),1);
		}
		
		return sb.toString();
	}

	public static void main(String[] args) {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		
		NNWrapper nn = new NNWrapper("/Users/pps/dev/NNTrain/goodshit/Net496021.nnet", 
				new MultipleFeatureExtractor(new AreaFeatureExtractor(),
						new GravityGridFeatureExtractor(10, 20),
						new SymmetryFeatureExtractor(),
						new EdgeIntersectionFeatureExtractor(3, 3)));
		
		CompoundImageProcessor cip = new CompoundImageProcessor();
		cip.addImageProcessor(new Binarization(40, 255, Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU));
		cip.addImageProcessor(new Cropper());
		
		PlateProcessor pl = new PlateProcessor(nn, cip);
		
		
		File dir = new File("/Users/pps/dev/detected");
		LabelFrame lf = new LabelFrame("GOOD", true);
		
		int count = 0;
		
		for (String filestr : dir.list()) {
			count++;
			if (count > 250) break;
		
			String filename = new File(dir, filestr).getAbsolutePath();
			Mat m = Imgcodecs.imread(filename, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
			Mat m1 = Imgcodecs.imread(filename, Imgcodecs.CV_LOAD_IMAGE_COLOR);
			
			SegmentationResult result = Segmenter.segment(m);
			
			List<Mat> pieces = result.getRevisedSegments(); //FIXME
			
			for (int p : result.getCutPoints())
				Imgproc.line(m1, new Point(p, 0), new Point(p, m1.rows()-1), new Scalar(0,255,0));
			
			for (Rect r : result.getRevisedRectangles()) {
				Imgproc.line(m1, new Point(r.x, r.y), new Point(r.x+r.width,r.y), new Scalar(255,0,0));
				Imgproc.line(m1, new Point(r.x, r.y+r.height), new Point(r.x+r.width, r.y+r.height), new Scalar(255,0,0));
			}
			
			
			lf.addImage(m1, pl.getLPString(pieces), 5);

		}
		
	
		lf.pack();
		lf.setVisible(true);

	}

}
