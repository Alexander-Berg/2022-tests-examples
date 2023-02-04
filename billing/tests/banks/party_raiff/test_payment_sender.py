import requests

from bcl.banks.registry import Raiffeisen
from bcl.core.models import states, BundleSign
from bcl.core.tasks import process_bundles


def test_retry(get_payment_bundle, get_source_payment, monkeypatch, read_fixture, mock_post, time_shift, init_user):

    response = type(str('response'), (object,), {'text': 'Bad Gateway', 'status_code': 502})
    monkeypatch.setattr(requests, 'post', lambda *args, **kwargs: response())

    bundle = get_payment_bundle([get_source_payment(associate=Raiffeisen)])
    org = bundle.account.org
    org.connection_id = 'test'
    org.save()

    sig_params = dict(
        level=1,
        bundle=bundle,
        user=init_user(),
    )

    # Добавляем две подписи одного уровня.
    for _ in range(2):
        sign = BundleSign(**sig_params)
        sign.value = read_fixture('signature.txt')
        sign.save()

    bundle.schedule()

    process_bundles(0)
    bundle.refresh_from_db()

    assert bundle.status == states.FOR_DELIVERY

    mock_post(read_fixture('payment/sendrequest_response.xml', decode='utf-8'))

    monkeypatch.setattr(bundle.associate.payment_sender, 'autosign', lambda *args, **kwargs: None)

    with time_shift(60):
        process_bundles(0)

    bundle.refresh_from_db()
    assert bundle.status == states.EXPORTED_H2H
