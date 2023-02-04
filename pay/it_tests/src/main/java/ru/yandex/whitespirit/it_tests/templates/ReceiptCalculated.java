package ru.yandex.whitespirit.it_tests.templates;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import one.util.streamex.DoubleStreamEx;
import one.util.streamex.StreamEx;

@Builder
@Getter
public class ReceiptCalculated {
    private int id;
    private String fn;
    private String fp;
    private String qrDt;
    @Builder.Default
    private ReceiptType receiptType = ReceiptType.INCOME;
    @Builder.Default
    private List<Row> rows = Collections.emptyList();
    @Builder.Default
    private PaymentType paymentType = PaymentType.CARD;

    public double getTotalSum() {
        return rows.stream()
                .mapToDouble(Row::getAmount)
                .sum();
    }

    public List<TaxTotals> getTaxTotals() {
        return StreamEx.of(rows)
                .mapToEntry(Row::getTaxType, row -> row.getTaxType().getPct() == 0 ? row.getAmount() : row.getTaxAmount())
                .sortedBy(Map.Entry::getKey)
                .collapseKeys()
                .mapValues(DoubleStreamEx::of)
                .mapValues(DoubleStreamEx::reverseSorted)
                .mapValues(DoubleStreamEx::sum)
                .mapKeyValue(TaxTotals::new)
                .toImmutableList();
    }
}
