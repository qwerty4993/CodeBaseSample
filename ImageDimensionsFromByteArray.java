package com.example.helloworld.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

public class ImageDimensionsFromByteArray {
    public static String getImageDimensions(byte[] imageData) {
        String imageDimensions = null;
        if (imageData != null) {
            try {
                // Create a ByteArrayInputStream from the image data byte array
                InputStream bis = new ByteArrayInputStream(imageData);
                // Use ImageIO to read the image data from the ByteArrayInputStream
                BufferedImage image = ImageIO.read(bis);
                if (image != null) {
                    // Get the width and height of the image
                    int width = image.getWidth();
                    int height = image.getHeight();
                    imageDimensions = width + " x " + height;
                    System.out.println("Image Dimensions (Width x Height): " + width + " x " + height);
                } else {
                    System.out.println("Error: Could not read the image.");
                }

                bis.close(); // Close the ByteArrayInputStream
                return imageDimensions;
            } catch (IOException e) {
                System.out.println("Error reading image data: " + e.getMessage());
            }
        } else {
            System.out.println("Error: Image data is null or empty.");
        }
        return imageDimensions;
    }

    public static void main(String[] args) {
        byte[] imageData = getFileContent();
        getImageDimensions(imageData);
    }


    private static byte[] getFileContent() {
        String imagePath = "/Users/charankumar/Downloads/01_Rechtwinkliges_Dreieck-inverser_Satz.gif"; // Replace with your image file path

        try {
            // Read image file to byte array
            byte[] imageData = Files.readAllBytes(Paths.get(imagePath));

            // Use the image data as needed (e.g., encode to Base64, process, etc.)
            System.out.println("Image converted to byte array successfully.");

            // Optionally, you can display the size of the byte array
            System.out.println("Image byte array size: " + imageData.length + " bytes");
            return imageData;
        } catch (IOException e) {
            System.out.println("Error reading image file: " + e.getMessage());
            return null;
        }
    }

}