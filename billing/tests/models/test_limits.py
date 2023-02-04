import pytest

from bcl.banks.registry import JpMorgan
from bcl.core.models import states
from bcl.core.models.limits import LIMITS


@pytest.mark.skipif(not LIMITS, reason='Ограничения не заданы.')
def test_bundling(build_payment_bundle, get_assoc_acc_curr, get_source_payment):

    associate = JpMorgan

    _, acc1, _ = get_assoc_acc_curr(associate, account=list(LIMITS.keys())[0])

    payment_over_limit = get_source_payment({
        'f_acc': acc1.number,
        'summ': list(LIMITS.values())[0] + 1,  # превышение
    }, associate=associate)

    bundle = build_payment_bundle(associate, payment_dicts=[
        payment_over_limit,
        {'f_acc': acc1.number},  # этот платёж уложился в ограничения
    ])

    assert bundle.payments_count == 1
    payment_over_limit.refresh_from_db()
    assert 'Превышены ограничения' in payment_over_limit.processing_notes
    assert payment_over_limit.status == states.OVER_LIMITS

    # проставили статус
    payment_over_limit.set_status(states.REVISER_OK)
    bundle = build_payment_bundle(associate, payment_dicts=[
        payment_over_limit,
    ])
    assert bundle.payments_count == 1
    payment_over_limit.refresh_from_db()
    assert 'Превышены ограничения' not in payment_over_limit.processing_notes
    assert payment_over_limit.is_exported
