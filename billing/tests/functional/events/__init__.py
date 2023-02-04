import hamcrest as hm


def expected_client_transactions(client_transactions, additional_transaction_fields=None):
    additional_transaction_fields = additional_transaction_fields or dict()
    return [
        hm.has_entries({
            'client_id': b['client_id'],
            'transactions': hm.contains_inanyorder(*[hm.has_entries({**t, **additional_transaction_fields})
                                                     for t in b['transactions']]),
        })
        for b in client_transactions
    ]
