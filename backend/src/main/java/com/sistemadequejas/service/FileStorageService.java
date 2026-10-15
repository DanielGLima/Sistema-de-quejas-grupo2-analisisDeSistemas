package com.sistemadequejas.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

// CU-05: guarda evidencias (fotos/PDF) en disco local, maximo 2 MB por archivo.
@Service
public class FileStorageService {

    private static final long TAMANO_MAXIMO_BYTES = 2 * 1024 * 1024;
    private static final List<String> TIPOS_PERMITIDOS = List.of("jpg", "jpeg", "png", "pdf");

    private final Path directorioBase = Paths.get("uploads").toAbsolutePath().normalize();

    public FileStorageService() {
        try {
            Files.createDirectories(directorioBase);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear el directorio de subida de archivos", e);
        }
    }

    private static final String MENSAJE_ARCHIVO_INVALIDO =
            "El archivo adjunto supera los 2 MB o no corresponde a un formato permitido (PDF/Imagen)";

    // Valida tamano (max 2 MB), extension (JPG/PNG/PDF) y que el contenido corresponda a ese formato.
    public String validar(MultipartFile archivo) {
        if (archivo.isEmpty() || archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MENSAJE_ARCHIVO_INVALIDO);
        }

        String extension = obtenerExtension(archivo.getOriginalFilename()).toLowerCase();
        if (!TIPOS_PERMITIDOS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MENSAJE_ARCHIVO_INVALIDO);
        }

        try (java.io.InputStream entrada = archivo.getInputStream()) {
            byte[] cabecera = entrada.readNBytes(8);
            boolean coincide = switch (extension) {
                case "png" -> cabecera.length >= 4 && (cabecera[0] & 0xFF) == 0x89 && cabecera[1] == 'P' && cabecera[2] == 'N' && cabecera[3] == 'G';
                case "jpg", "jpeg" -> cabecera.length >= 3 && (cabecera[0] & 0xFF) == 0xFF && (cabecera[1] & 0xFF) == 0xD8 && (cabecera[2] & 0xFF) == 0xFF;
                case "pdf" -> cabecera.length >= 4 && cabecera[0] == '%' && cabecera[1] == 'P' && cabecera[2] == 'D' && cabecera[3] == 'F';
                default -> false;
            };
            if (!coincide) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MENSAJE_ARCHIVO_INVALIDO);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MENSAJE_ARCHIVO_INVALIDO);
        }
        return extension;
    }

    public String guardar(MultipartFile archivo) {
        String extension = validar(archivo);

        String nombreArchivo = UUID.randomUUID() + "." + extension;
        Path destino = directorioBase.resolve(nombreArchivo);

        try {
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el archivo adjunto");
        }

        return nombreArchivo;
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1);
    }
}
