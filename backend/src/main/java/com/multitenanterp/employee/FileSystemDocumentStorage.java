package com.multitenanterp.employee;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@ConditionalOnProperty(name="app.storage.provider",havingValue="filesystem",matchIfMissing=true)
public class FileSystemDocumentStorage implements DocumentStorage {
    private final Path root;

    public FileSystemDocumentStorage(@Value("${app.storage.employee-documents-root:./data/employee-documents}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override public void store(String key, InputStream content) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override public Resource load(String key) {
        return new FileSystemResource(resolve(key));
    }

    @Override public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid storage key");
        return resolved;
    }
}
