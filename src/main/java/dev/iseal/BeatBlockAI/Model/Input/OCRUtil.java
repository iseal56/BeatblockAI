package dev.iseal.BeatBlockAI.Model.Input;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.LeptonicaFrameConverter;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.libfreenect._freenect_device;
import org.bytedeco.opencv.opencv_core.GpuMat;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_text.OCRTesseract;
import org.bytedeco.opencv.presets.opencv_core;
import org.bytedeco.tesseract.Tesseract;

import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCRUtil {

    String pathToTessData = "/home/iseal/Downloads/tessdata/tessData-4.1.0";
    BufferedImage lastImage;
    String lastOCR;

    private String computeOCR(BufferedImage screen) {

        if (lastImage != null && areImagesEqual(screen, lastImage)) {
            return lastOCR;
        }

        BytePointer outText = new BytePointer();
        Tesseract tesseract = new Tesseract();
        OCRTesseract api = OCRTesseract.create();

        // Convert BufferedImage to Mat
        Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
        LeptonicaFrameConverter leptonicaFrameConverter = new LeptonicaFrameConverter();

        PIX pix = leptonicaFrameConverter.convert(java2DFrameConverter.convert(screen));
        if (pix == null) {
            throw new IllegalArgumentException("Conversion from BufferedImage to PIX failed.");
        }
        Mat mat = new Mat(pix);

        try {
            api.run(mat, outText, null, null, null, 0);
        } catch (Exception e) {
            throw new IllegalStateException("OCR processing failed.", e);
        }

        String ocrResult;
        if (outText == null || outText.getString().isEmpty()) {
            ocrResult = "";
            System.out.println("OCR failed to produce any output.");
        } else {
            ocrResult = outText.getString();
        }

        api.deallocate();
        outText.deallocate();

        lastImage = screen;
        lastOCR = ocrResult;
        return ocrResult;
    }

    private boolean areImagesEqual(BufferedImage imgA, BufferedImage imgB) {
        if (imgA == null || imgB == null) {
            return false;
        }
        // The images must be the same size.
        if (imgA.getWidth() != imgB.getWidth() || imgA.getHeight() != imgB.getHeight()) {
            return false;
        }

        int width  = imgA.getWidth();
        int height = imgA.getHeight();

        // Loop over every pixel.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Compare the pixels for equality.
                if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean doesScreenContainEndGame(BufferedImage screen) {
        String result = computeOCR(screen);
        return result.contains("Grade:");
    }

    public int getMisses(BufferedImage screen) {
        String result = computeOCR(screen);
        Pattern pattern = Pattern.compile("Misses:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(result);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    public int getBarelies(BufferedImage screen) {
        String result = computeOCR(screen);
        Pattern pattern = Pattern.compile("Barelies:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(result);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    public int getGrade(BufferedImage screen) {
    String result = computeOCR(screen);
    Pattern pattern = Pattern.compile("Grade:\\s*(S\\+|S|A\\+|A|B\\+|B|C\\+|C|D\\+|D|E\\+|E|F|P)");
    Matcher matcher = pattern.matcher(result);
    int gradeValue = 0;
    if (matcher.find()) {
        String grade = matcher.group(1);
        gradeValue = switch (grade) {
            case "P" -> 120;
            case "S+" -> 110;
            case "S" -> 100;
            case "A+" -> 90;
            case "A" -> 80;
            case "B+" -> 70;
            case "B" -> 60;
            case "C+" -> 50;
            case "C" -> 30;
            case "D+" -> 10;
            case "D" -> 0;
            case "E+" -> -10;
            case "E" -> -30;
            case "F" -> -50;
            default -> gradeValue;
        };
    }
    return gradeValue;
}
}
