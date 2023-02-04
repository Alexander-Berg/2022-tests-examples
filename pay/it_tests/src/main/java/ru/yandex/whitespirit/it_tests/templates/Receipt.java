package ru.yandex.whitespirit.it_tests.templates;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.Getter;

import ru.yandex.whitespirit.it_tests.utils.Constants;

@Builder
@Getter
public class Receipt {
    @Builder.Default
    private Firm firm = Constants.HORNS_AND_HOOVES;
    @Builder.Default
    private ReceiptType receiptType = ReceiptType.INCOME;
    @Builder.Default
    private Optional<FiscalDocumentType> fiscalDocumentType = Optional.empty();
    @Builder.Default
    private TaxationType taxationType = TaxationType.OSN;
    @Builder.Default
    private AgentType agentType = AgentType.NONE_AGENT;
    @Builder.Default
    private List<Row> rows = Collections.emptyList();
    @Builder.Default
    private List<Payment> payments = Collections.emptyList();
}
