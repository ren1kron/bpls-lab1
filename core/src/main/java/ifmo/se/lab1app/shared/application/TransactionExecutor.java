package ifmo.se.lab1app.shared.application;

import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

@Component
public class TransactionExecutor {

    private final TransactionOperations writeTransactions;
    private final TransactionOperations readTransactions;

    public TransactionExecutor(
            @Qualifier("writeTransactionTemplate") TransactionOperations writeTransactions,
            @Qualifier("readTransactionTemplate") TransactionOperations readTransactions
    ) {
        this.writeTransactions = writeTransactions;
        this.readTransactions = readTransactions;
    }

    public <T> T write(Supplier<T> callback) {
        return writeTransactions.execute(status -> callback.get());
    }

    public void write(Runnable callback) {
        writeTransactions.execute(status -> {
            callback.run();
            return null;
        });
    }

    public <T> T read(Supplier<T> callback) {
        return readTransactions.execute(status -> callback.get());
    }
}
