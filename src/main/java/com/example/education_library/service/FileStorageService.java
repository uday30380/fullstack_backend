package com.example.education_library.service;

import com.example.education_library.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path storageLocation;

    @Autowired
    public FileStorageService(AppProperties appProperties) {
        this.storageLocation = Paths.get(appProperties.getStorage().getUploadDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    public String storeFile(MultipartFile file) {
        return storeFile(file, null);
    }

    public String storeFile(MultipartFile file, String subDirectory) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";

        if (originalFileName.contains("..")) {
            throw new RuntimeException("Invalid file path");
        }

        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }

        String fileName = UUID.randomUUID() + extension;

        try {
            Path targetDirectory = ensureDirectory(subDirectory);
            Path targetLocation = targetDirectory.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return normalizeRelativePath((subDirectory == null || subDirectory.isBlank()) ? fileName : subDirectory + "/" + fileName);
        } catch (IOException e) {
            throw new RuntimeException("Could not store file " + fileName, e);
        }
    }

    public Resource loadAsResource(String relativePath) {
        try {
            Path filePath = resolve(relativePath);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            }

            throw new RuntimeException("Could not read file " + relativePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file " + relativePath, e);
        }
    }

    public Path resolve(String relativePath) {
        String normalized = normalizeRelativePath(relativePath);
        Path resolvedPath = storageLocation.resolve(normalized).normalize();

        if (!resolvedPath.startsWith(storageLocation)) {
            throw new RuntimeException("Invalid file path");
        }

        return resolvedPath;
    }

    public void deleteFile(String fileName) {
        try {
            Files.deleteIfExists(resolve(fileName));
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + fileName + ". Reason: " + e.getMessage());
        }
    }

    private Path ensureDirectory(String subDirectory) throws IOException {
        Path directory = (subDirectory == null || subDirectory.isBlank())
                ? storageLocation
                : resolve(subDirectory);
        Files.createDirectories(directory);
        return directory;
    }

    private String normalizeRelativePath(String relativePath) {
        return relativePath == null ? "" : relativePath.replace("\\", "/").replaceFirst("^/+", "");
    }
}
