from billing.library.python.calculator.models.personal_account import ServiceCode


def gen_personal_account(
    contract_id: int,
    external_id: str,
    iso_currency: str,
    service_code: ServiceCode,
    hidden: int,
    postpay: int,
):
    return {
        'id': contract_id,
        'contract_id': contract_id,
        'external_id': external_id,
        'iso_currency': iso_currency,
        'type': 'personal_account',
        'service_code': service_code,
        'hidden': hidden,
        'postpay': postpay,
    }


def gen_generic_personal_account(
    contract_id: int,
    client_id: int,
    external_id: str = '2134/7482',
    iso_currency: str = 'RUB',
    service_code: ServiceCode = ServiceCode.EMPTY,
    hidden: int = 0,
    postpay: int = 1,
    version: int = 1,
):
    return {
        'id': contract_id,
        'contract_id': contract_id,
        'client_id': client_id,
        'version': version,
        'obj': gen_personal_account(
            contract_id=contract_id,
            external_id=external_id,
            iso_currency=iso_currency,
            service_code=service_code,
            hidden=hidden,
            postpay=postpay,
        ),
    }
