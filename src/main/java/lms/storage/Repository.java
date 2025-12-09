package lms.storage;

import java.util.List;
import java.util.Optional;
import lms.model.Identifiable;

public interface Repository<T extends Identifiable<ID>, ID> {
    void save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    void deleteById(ID id);

    boolean existsById(ID id);
}