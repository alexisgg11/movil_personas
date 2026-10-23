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
        var photo = storage.guardar(photo(), previous.getFileName().toString());
        for (var sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertFalse(Files.exists(directory.resolve(photo)));
        assertTrue(Files.exists(previous));
    }
    @Test void commitKeepsNewPhotoAndDeletesReplacedPhoto() throws Exception {
        var storage = new FotoStorage(directory.toString());
        var previous = directory.resolve("00000000-0000-0000-0000-000000000000.jpg");
        Files.write(previous, new byte[]{1});
        TransactionSynchronizationManager.initSynchronization();
        var photo = storage.guardar(photo(), previous.getFileName().toString());
        for (var sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        assertTrue(Files.exists(directory.resolve(photo)));
        assertFalse(Files.exists(previous));
    }
    @Test void invalidImageDoesNotLeaveFiles() throws Exception {
        var storage = new FotoStorage(directory.toString());
        assertThrows(ApiError.class, () -> storage.guardar(new MockMultipartFile("foto", "foto.jpg", "image/jpeg", new byte[]{1,2,3}), null));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }
    private MockMultipartFile photo() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("foto", "foto.png", "image/png", output.toByteArray());
    }
}
