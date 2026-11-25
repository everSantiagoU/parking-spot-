package com.parkingspot.infrastructure.adapter.out.persistence;

import com.parkingspot.application.port.Transactions;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Adaptador de transacciones con Spring: la aplicación no depende de anotaciones de Spring. */
@Component
public class SpringTransactions implements Transactions {
    private final TransactionTemplate template;

    public SpringTransactions(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
