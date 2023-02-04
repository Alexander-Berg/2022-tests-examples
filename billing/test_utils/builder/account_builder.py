from decimal import Decimal


def gen_loc(namespace: str, loc_type: str, **kwargs):
    loc = kwargs
    loc.update({'namespace': namespace, 'type': loc_type})
    return loc


def gen_account(
    namespace: str,
    account_type: str,
    client_id: int,
    contract_id: int,
    debit: Decimal = Decimal('0.00'),
    credit: Decimal = Decimal('0.00'),
    currency: str = 'RUB',
    ts: int = 0,
):
    if not (debit or credit):
        raise ValueError('One of credit or debit is required')

    return {
        'loc': gen_loc(
            namespace,
            account_type,
            client_id=client_id,
            contract_id=contract_id,
            currency=currency,
        ),
        'debit': debit or '0',
        'credit': credit or '0',
        'dt': ts,
    }
