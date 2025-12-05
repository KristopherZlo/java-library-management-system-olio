package lms.storage.file;
// TODO: review json schema
// TODO: add backup handling

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lms.exception.StorageException;
import lms.model.Book;
import lms.model.BookCopy;
import lms.model.Loan;
import lms.model.Member;
import lms.model.Reservation;
import lms.storage.LibraryStorage;
import lms.storage.Repository;

public class FileStorage implements LibraryStorage {
    private final Path base;
    private final Object transactionLock = new Object();
    private final List<JsonFileRepository<?, ?>> repositories = new ArrayList<>();
    private boolean inTransaction;
    private final JsonFileRepository<Book, String> bookRepository;
    private final JsonFileRepository<BookCopy, String> copyRepository;
    private final JsonFileRepository<Member, String> memberRepository;
    private final JsonFileRepository<Loan, String> loanRepository;
    private final JsonFileRepository<Reservation, String> reservationRepository;

    public FileStorage(String dataDir) {
        this.base = Paths.get(dataDir);
        recoverPendingTransaction();
        this.bookRepository = new JsonFileRepository<>(base.resolve("books.json"),
                new TypeReference<List<Book>>() { });
        this.copyRepository = new JsonFileRepository<>(base.resolve("copies.json"),
                new TypeReference<List<BookCopy>>() { });
        this.memberRepository = new JsonFileRepository<>(base.resolve("members.json"),
                new TypeReference<List<Member>>() { });
        this.loanRepository = new JsonFileRepository<>(base.resolve("loans.json"),
                new TypeReference<List<Loan>>() { });
        this.reservationRepository = new JsonFileRepository<>(base.resolve("reservations.json"),
                new TypeReference<List<Reservation>>() { });
        repositories.add(bookRepository);
        repositories.add(copyRepository);
        repositories.add(memberRepository);
        repositories.add(loanRepository);
        repositories.add(reservationRepository);
    }

    @Override
    public Repository<Book, String> books() {
        return bookRepository;
    }

    @Override
    public Repository<BookCopy, String> copies() {
        return copyRepository;
    }

    @Override
    public Repository<Member, String> members() {
        return memberRepository;
    }

    @Override
    public Repository<Loan, String> loans() {
        return loanRepository;
    }

    @Override
    public Repository<Reservation, String> reservations() {
        return reservationRepository;
    }

    @Override
    public <T> T runInTransaction(Supplier<T> action) {
        synchronized (transactionLock) {
            if (inTransaction) {
                return action.get();
            }
            inTransaction = true;
            List<RepoSnapshot> snapshots = snapshotRepositories();
            setAutoPersist(false);
            boolean actionCompleted = false;
            try {
                T result = action.get();
                actionCompleted = true;
                commitTransaction();
                return result;
            } catch (RuntimeException ex) {
                if (!actionCompleted) {
                    restoreSnapshots(snapshots);
                }
                throw ex;
            } finally {
                setAutoPersist(true);
                inTransaction = false;
            }
        }
    }

    @Override
    public void close() {
        // No-op for file storage.
    }

    private void setAutoPersist(boolean autoPersist) {
        for (JsonFileRepository<?, ?> repository : repositories) {
            repository.setAutoPersist(autoPersist);
        }
    }

    private List<RepoSnapshot> snapshotRepositories() {
        List<RepoSnapshot> snapshots = new ArrayList<>();
        for (JsonFileRepository<?, ?> repository : repositories) {
            snapshots.add(new RepoSnapshot(repository));
        }
        return snapshots;
    }

    private void restoreSnapshots(List<RepoSnapshot> snapshots) {
        for (RepoSnapshot snapshot : snapshots) {
            snapshot.restore();
        }
    }

    private void commitTransaction() {
        Path txDir = base.resolve(TX_DIR_NAME);
        Map<String, String> manifestEntries = new LinkedHashMap<>();
        try {
            Files.createDirectories(txDir);
            for (JsonFileRepository<?, ?> repository : repositories) {
                String fileName = repository.getFile().getFileName().toString();
                String tempName = fileName + ".new";
                Path tempFile = txDir.resolve(tempName);
                repository.writeTo(tempFile);
                manifestEntries.put(tempName, fileName);
            }
            Path manifest = txDir.resolve(MANIFEST_NAME);
            writeManifest(manifest, manifestEntries);
            for (Map.Entry<String, String> entry : manifestEntries.entrySet()) {
                Path source = txDir.resolve(entry.getKey());
                Path target = base.resolve(entry.getValue());
                moveReplacing(source, target);
            }
            deleteIfExists(manifest);
            deleteIfExists(txDir);
        } catch (IOException ex) {
            if (Files.exists(txDir.resolve(MANIFEST_NAME))) {
                recoverPendingTransaction();
            }
            throw new StorageException("Failed to commit file storage transaction", ex);
        }
    }

    private void recoverPendingTransaction() {
        Path txDir = base.resolve(TX_DIR_NAME);
        Path manifest = txDir.resolve(MANIFEST_NAME);
        if (!Files.exists(manifest)) {
            deleteIfExists(txDir);
            return;
        }
        try {
            List<String> lines = Files.readAllLines(manifest);
            for (String line : lines) {
                String[] parts = line.split("\\|", 2);
                if (parts.length != 2) {
                    continue;
                }
                Path source = txDir.resolve(parts[0]);
                Path target = base.resolve(parts[1]);
                if (Files.exists(source)) {
                    moveReplacing(source, target);
                }
            }
            deleteIfExists(manifest);
            deleteIfExists(txDir);
        } catch (IOException ex) {
            throw new StorageException("Failed to recover file storage transaction", ex);
        }
    }

    private void writeManifest(Path manifest, Map<String, String> entries) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            lines.add(entry.getKey() + "|" + entry.getValue());
        }
        Files.write(manifest, lines);
    }

    private void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Ignore cleanup failures.
        }
    }

    private static class RepoSnapshot {
        private final JsonFileRepository repository;
        private final Map snapshot;

        private RepoSnapshot(JsonFileRepository repository) {
            this.repository = repository;
            this.snapshot = repository.snapshot();
        }

        private void restore() {
            repository.restoreSnapshot(snapshot);
        }
    }
}