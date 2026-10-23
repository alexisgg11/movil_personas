package com.example.personas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FotoStorage {
    private final Path raiz;
    public FotoStorage(@Value("${app.photo-directory}") String directorio) throws IOException {
        raiz = Path.of(directorio).toAbsolutePath().normalize(); Files.createDirectories(raiz);
    }
    public String guardar(MultipartFile foto, String anterior) {
        if (foto == null || foto.isEmpty()) throw new ApiError(400, "Añade una fotografía.");
        if (foto.getSize() > 10 * 1024 * 1024) throw new ApiError(400, "La foto supera los 10 MB.");
        String nombre = UUID.randomUUID() + ".jpg";
        Path destino = raiz.resolve(nombre);
        try (ImageInputStream entrada = ImageIO.createImageInputStream(foto.getInputStream())) {
            var lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw new ApiError(400, "Selecciona una imagen JPEG o PNG válida.");
            var lector = lectores.next();
            BufferedImage original;
            try {
                lector.setInput(entrada);
                long pixeles = (long) lector.getWidth(0) * lector.getHeight(0);
                if (pixeles > 24_000_000) throw new ApiError(400, "La imagen es demasiado grande.");
                original = lector.read(0);
            } finally { lector.dispose(); }
            var salida = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
            var g = salida.createGraphics();
            try { g.setColor(java.awt.Color.WHITE); g.fillRect(0, 0, salida.getWidth(), salida.getHeight()); g.drawImage(original, 0, 0, null); }
            finally { g.dispose(); }
            if (!ImageIO.write(salida, "jpg", destino.toFile())) throw new IOException("JPEG");
        } catch (IOException | IllegalArgumentException e) {
            borrar(nombre); throw new ApiError(400, "No se pudo leer la fotografía.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED) borrar(nombre);
                else if (anterior != null) borrar(anterior);
            }
        });
        return nombre;
    }
    public byte[] leer(String nombre) {
        try { return Files.readAllBytes(ruta(nombre)); }
        catch (IOException e) { throw new ApiError(404, "Fotografía no disponible."); }
    }
    private Path ruta(String nombre) {
        if (nombre == null || !nombre.matches("[0-9a-f-]{36}\\.jpg")) throw new ApiError(404, "Fotografía no disponible.");
        return raiz.resolve(nombre);
    }
    private void borrar(String nombre) {
        try { Files.deleteIfExists(ruta(nombre)); } catch (IOException ignored) {}
    }
}
