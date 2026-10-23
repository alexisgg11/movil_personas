package com.example.personas;

import com.example.personas.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class FotoStorageTest {
    @TempDir Path directory;
    @AfterEach void clear() { if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization(); }
    @Test void rollbackDeletesNewPhotoAndKeepsOldPhoto() throws Exception {
        var storage = new FotoStorage(directory.toString());
        var previous = directory.resolve("00000000-0000-0000-0000-000000000000.jpg");
        Files.write(previous, new byte[]{1});
        TransactionSynchronizationManager.initSynchronization();
        var photo = storage.guardar("01234567", photo(), previous.getFileName().toString());
        for (var sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertFalse(Files.exists(directory.resolve(photo)));
        assertFalse(Files.exists(directory.resolve("01234567")));
        assertTrue(Files.exists(previous));
    }
    @Test void commitKeepsNewPhotoAndDeletesReplacedPhoto() throws Exception {
        var storage = new FotoStorage(directory.toString());
        var previous = directory.resolve("00000000-0000-0000-0000-000000000000.jpg");
        Files.write(previous, new byte[]{1});
        TransactionSynchronizationManager.initSynchronization();
        var photo = storage.guardar("01234567", photo(), previous.getFileName().toString());
        for (var sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        assertTrue(Files.exists(directory.resolve(photo)));
        assertTrue(photo.startsWith("01234567/"));
        assertFalse(Files.exists(previous));
    }
    @Test void invalidImageDoesNotLeaveFiles() throws Exception {
        var storage = new FotoStorage(directory.toString());
        assertThrows(ApiError.class, () -> storage.guardar("01234567", new MockMultipartFile("foto", "foto.jpg", "image/jpeg", new byte[]{1,2,3}), null));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }
    @Test void legacyPhotoRemainsReadable() throws Exception {
        var storage = new FotoStorage(directory.toString());
        var name = "00000000-0000-0000-0000-000000000000.jpg";
        Files.write(directory.resolve(name), new byte[]{1,2,3});
        assertArrayEquals(new byte[]{1,2,3}, storage.leer(name));
    }
    @Test void traversalCannotReadFilesOutsideThePhotoDirectory() throws Exception {
        var storage = new FotoStorage(directory.toString());
        assertThrows(ApiError.class, () -> storage.leer("../secret.jpg"));
        assertThrows(ApiError.class, () -> storage.leer("01234567/../../secret.jpg"));
    }
    @Test void aPhotoFromAnotherPersonCannotBeReplaced() throws Exception {
        var storage = new FotoStorage(directory.toString());
        assertThrows(ApiError.class, () -> storage.guardar("01234567", photo(),
            "02345678/00000000-0000-0000-0000-000000000000.jpg"));
    }
    private MockMultipartFile photo() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("foto", "foto.png", "image/png", output.toByteArray());
    }
}
