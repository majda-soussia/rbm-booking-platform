package com.rbm.service;

import com.rbm.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_SIZE = 2L * 1024 * 1024;   // 2 Mo
    private static final String URL_PREFIX = "/uploads/salles/";

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize().resolve("salles");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier d'upload : " + root, e);
        }
    }

    public String storeSallePhoto(Long salleId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Le fichier est vide");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("La photo ne doit pas dépasser 2 Mo");
        }
        String extension = switch (String.valueOf(file.getContentType())) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw new BusinessException("Format non supporté : seuls JPG et PNG sont acceptés");
        };

        String filename = "salle-" + salleId + "-" + UUID.randomUUID() + "." + extension;
        try {
            Files.copy(file.getInputStream(), root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("Échec de l'enregistrement de la photo");
        }
        return URL_PREFIX + filename;
    }

    public void delete(String photoUrl) {
        if (photoUrl == null || !photoUrl.startsWith(URL_PREFIX)) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(Paths.get(photoUrl).getFileName().toString()));
        } catch (IOException ignored) {
            // une photo orpheline n'est pas bloquante
        }
    }

    public Optional<Path> resolve(String photoUrl) {
        if (photoUrl == null || !photoUrl.startsWith(URL_PREFIX)) {
            return Optional.empty();
        }
        Path file = root.resolve(Paths.get(photoUrl).getFileName().toString());
        return Files.exists(file) ? Optional.of(file) : Optional.empty();
    }
}