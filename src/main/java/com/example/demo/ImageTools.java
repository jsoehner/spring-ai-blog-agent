package com.example.demo;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import org.springframework.ai.tool.annotation.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

@Component
public class ImageTools {

    @Tool(description = "Scans a directory for images and extracts their EXIF metadata (Date Taken, GPS Coordinates). Returns a catalog of images to help suggest a descriptive folder name. Input is an absolute directory path.")
    public String scanImageMetadata(String directoryPath) {
        Path startPath = Paths.get(directoryPath).toAbsolutePath().normalize();
        Path baseDir = Paths.get(".").toAbsolutePath().normalize();
        if (!startPath.startsWith(baseDir)) {
            return "Error: Path traversal attempt detected! Directory must be inside the application workspace.";
        }
        if (!Files.exists(startPath) || !Files.isDirectory(startPath)) {
            return "Directory not found: " + directoryPath;
        }

        StringBuilder result = new StringBuilder("Image Catalog for " + directoryPath + ":\n");
        int count = 0;

        try (Stream<Path> paths = Files.walk(startPath)) {
            List<Path> imageFiles = paths.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.toString().toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".heic");
                    }).toList();

            for (Path path : imageFiles) {
                try {
                    Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
                    String date = "Unknown Date";
                    String location = "Unknown Location";

                    ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                    if (directory != null) {
                        Date dateObject = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                        if (dateObject != null) {
                            date = dateObject.toString();
                        }
                    }

                    GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
                    if (gpsDirectory != null && gpsDirectory.getGeoLocation() != null) {
                        location = "Lat: " + gpsDirectory.getGeoLocation().getLatitude() + ", Lon: " + gpsDirectory.getGeoLocation().getLongitude();
                    }

                    result.append("- File: ").append(path.getFileName())
                          .append(" | Date Taken: ").append(date)
                          .append(" | Location: ").append(location).append("\n");
                    count++;
                } catch (Exception e) {
                    result.append("- File: ").append(path.getFileName()).append(" | Error reading metadata\n");
                }
            }
        } catch (Exception e) {
            return "Failed to scan directory: " + e.getMessage();
        }

        result.append("\nTotal images scanned: ").append(count);
        return result.toString();
    }

    public record MoveRequest(String sourceDirectory, String targetDirectory) {}

    @Tool(description = "Creates a new folder and moves all images from a source directory into the new folder. Input should be the original source directory and the desired absolute target directory (e.g. /path/to/2024_Paris_Trip).")
    public String moveImages(MoveRequest request) {
        Path sourcePath = Paths.get(request.sourceDirectory()).toAbsolutePath().normalize();
        Path targetPath = Paths.get(request.targetDirectory()).toAbsolutePath().normalize();
        Path baseDir = Paths.get(".").toAbsolutePath().normalize();
        if (!sourcePath.startsWith(baseDir) || !targetPath.startsWith(baseDir)) {
            return "Error: Path traversal attempt detected! Directories must be inside the application workspace.";
        }
        if (!Files.exists(sourcePath) || !Files.isDirectory(sourcePath)) {
            return "Source directory not found: " + request.sourceDirectory();
        }
        
        try {
            if (!Files.exists(targetPath)) {
                Files.createDirectories(targetPath);
            }

            int count = 0;
            try (Stream<Path> paths = Files.walk(sourcePath)) {
                List<Path> imageFiles = paths.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.toString().toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".heic");
                    }).toList();
                
                for (Path file : imageFiles) {
                    Path destination = targetPath.resolve(file.getFileName());
                    Files.move(file, destination, StandardCopyOption.REPLACE_EXISTING);
                    count++;
                }
            }
            return "Successfully moved " + count + " images from " + sourcePath + " to " + targetPath.toAbsolutePath() + ".";
        } catch (Exception e) {
            return "Failed to move images: " + e.getMessage();
        }
    }
}
