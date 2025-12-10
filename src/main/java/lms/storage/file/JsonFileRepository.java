package lms.storage.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lms.exception.StorageException;
import lms.model.Identifiable;
import lms.storage.Repository;

public class JsonFileRepository<T extends Identifiable<ID>, ID> implements Repository<T, ID> {
    private final Path file;
    private final ObjectMapper mapper;
    private final TypeReference<List<T>> typeReference;
    private final Map<ID, T> store = new LinkedHashMap<>();
    private boolean autoPersist = true;

    public JsonFileRepository(Path file, TypeReference<List<T>> typeReference) {
        this.file = file;
        this.typeReference = typeReference;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        load();
    }

    @Override
    public void save(T entity) {
        store.put(entity.getId(), entity);
        if (autoPersist) {
            persist();
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(ID id) {
        store.remove(id);
        if (autoPersist) {
            persist();
        }
    }

    @Override
    public boolean existsById(ID id) {
        return store.containsKey(id);
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            List<T> items = mapper.readValue(file.toFile(), typeReference);
            store.clear();
            for (T item : items) {
                store.put(item.getId(), item);
            }
        } catch (IOException ex) {
            throw new StorageException("Failed to load " + file.getFileName(), ex);
        }
    }

    private void persist() {
        Path tempFile = null;
        try {
            Files.createDirectories(file.getParent());
            tempFile = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            writeTo(tempFile);
            moveReplacing(tempFile, file);
        } catch (IOException ex) {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Ignore cleanup errors for temp file.
                }
            }
            throw new StorageException("Failed to persist " + file.getFileName(), ex);
        }
    }

    Path getFile() {
        return file;
    }

    void setAutoPersist(boolean autoPersist) {
        this.autoPersist = autoPersist;
    }

    Map<ID, T> snapshot() {
        return new LinkedHashMap<>(store);
    }

    void restoreSnapshot(Map<ID, T> snapshot) {
        store.clear();
        store.putAll(snapshot);
    }

    void writeTo(Path target) {
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), store.values());
        } catch (IOException ex) {
            throw new StorageException("Failed to write " + target.getFileName(), ex);
        }
    }

    private void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}