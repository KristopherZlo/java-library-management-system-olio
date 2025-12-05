package lms.storage;
// TODO: review transaction flow
// TODO: add metrics hooks

import java.util.List;
import java.util.Optional;
import lms.model.Identifiable;

public interface Repository<T extends Identifiable<ID>, ID> {


    List<T> findAll();

    void deleteById(ID id);

    boolean existsById(ID id);
}