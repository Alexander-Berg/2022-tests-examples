import pytest

from bcl.banks.party_raiff.common import SignUtils
from bcl.banks.registry import Raiffeisen
from bcl.core.models import BundleSign, PaymentsBundle


@pytest.fixture(autouse=True)
def patch_communicator(monkeypatch):
    """Добавляет идентификатор сессии коммуникатору, чтобы он не слал запросы для аутентификации."""

    from bcl.banks.party_raiff.common import RaiffCommunicator  # Иначе Аркадийный запускальщик не запустит тесты.

    monkeypatch.setattr(
        RaiffCommunicator, '_session_id',
        property(lambda self: '123123', lambda self, x: None),
        raising=False
    )


@pytest.fixture
def get_bundle(get_payment_bundle, read_fixture, mock_post, get_signing_right, init_user):

    # Ответ на запрос sendRequests
    send_response = read_fixture('payment/sendrequest_response.xml', decode='utf-8')

    get_signing_right(Raiffeisen.id, 'serial')
    user = init_user()

    def get_bundle_(payment, account=None):
        # Создаётся пакет
        payment_bundle = get_payment_bundle((payment,), h2h=True, account=account)  # type: PaymentsBundle

        Raiffeisen.payment_dispatcher.get_creator(payment_bundle).create_bundle()

        # Пакет подписывается
        signature = SignUtils.sign_detached_multiple(payment_bundle.digests)

        digital_signature = BundleSign(
            bundle=payment_bundle,
            user=user,
        )
        digital_signature.value = signature.as_bytes()
        digital_signature.save()

        payment_bundle = PaymentsBundle.objects.all().order_by('-id').first()

        # Пакет отправляется
        mock_post(send_response, url_filter=lambda url: 'upg' in url)
        Raiffeisen.payment_sender(payment_bundle).prepare_send_sync()
        return payment_bundle, signature

    return get_bundle_
