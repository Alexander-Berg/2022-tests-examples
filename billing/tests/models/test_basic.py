from bcl.banks.registry import Unicredit
from bcl.core.models import Account, Payment, PaymentsBundle, states


def test_account_notify_balance_to_parsed():
    account = Account(notify_balance_to=' one@at.com, two@at.com,three@at.com, ')

    recipients = account.notify_balance_to_parsed

    assert recipients == ['one@at.com', 'two@at.com', 'three@at.com']


def test_bundled_payment(get_source_payment_mass, compose_bundles):

    payments = get_source_payment_mass(3, Unicredit)

    compose_bundles(Unicredit, payment_ids=[pay.id for pay in payments])
    for payment in payments:
        payment.refresh_from_db()
        assert payment.status == states.BUNDLED


def test_hide_account_related(get_source_payment_mass, compose_bundles):

    payments = get_source_payment_mass(3, Unicredit)

    account = Account.objects.all()
    assert len(account) == 1
    account = account[0]

    compose_bundles(Unicredit, payment_ids=[pay.id for pay in payments])

    assert Payment.objects.filter(hidden=1).count() == 0
    assert PaymentsBundle.objects.filter(hidden=1).count() == 0

    account.save()

    assert Payment.objects.filter(hidden=1).count() == 0
    assert PaymentsBundle.objects.filter(hidden=1).count() == 0

    account.hidden = 1
    account.save()

    assert Payment.objects.filter(hidden=1).count() == 3
    assert PaymentsBundle.objects.filter(hidden=1).count() == 1
