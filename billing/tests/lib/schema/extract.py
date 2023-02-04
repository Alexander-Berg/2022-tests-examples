import typing as tp


def tariffer_payload_external_id(processor_response: dict[str, tp.Any]) -> list[int]:
    event = processor_response['data']['result']['event']

    return [event['tariffer_payload']['external_id']]


def trust_external_ids(processor_response: dict[str, tp.Any]) -> list[int]:
    extracted = []
    payment_event = processor_response['data']['result']['event']

    _extract_payment_row_ids(payment_event, extracted)

    return extracted


def _extract_payment_row_ids(payment: dict[str, tp.Any], extracted: list[int]):
    composite_components = payment.get('composite_components')
    if composite_components:
        for component in composite_components:
            _extract_payment_row_ids(component, extracted)
    else:
        for row in payment.get('rows') or []:
            extracted.append(row['id'])

    for refund in payment.get('refunds') or []:
        _extract_payment_row_ids(refund, extracted)
