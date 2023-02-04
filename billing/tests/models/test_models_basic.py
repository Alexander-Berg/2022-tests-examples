from bcl.banks.registry import Sber
from bcl.core.models import Account, Lock, Intent, RequestLog, Attachment


def test_account_notify_balance_to_parsed():
    account = Account(notify_balance_to=' one@at.com, two@at.com,three@at.com, ')

    recipients = account.notify_balance_to_parsed

    assert recipients == ['one@at.com', 'two@at.com', 'three@at.com']


def test_lock():

    name = 'mylock'

    # Здесь должен был быть полноценный тест блокировки, однако
    # в mongomock метод _find_and_modify работает не так, как монга,
    # в частности он не атомарен и не возвращает результата,
    # если производится insert, поэтому добавим задание перед тестом
    # и протестируем минимально.
    Lock(name=name).save()

    lock = Lock.acquire(name)
    assert lock
    assert Lock.objects.count() == 1

    lock = Lock.objects.get(name=name)
    assert not lock.released

    lock.release('allright')
    assert Lock.objects.count() == 1

    lock = Lock.objects.get(name=name)
    assert lock.result == 'allright'
    assert lock.released

    Lock.acquire(name)
    assert Lock.acquire(name) is None
    assert Lock.objects.count() == 1


def test_intent(time_shift):

    data = {'one': 1, 'two': 2}
    realm = Intent.REALM_DSS_SIGNING

    intent = Intent.register(realm, data, expire_after=120)

    assert intent.data == data
    # assert Intent.get(realm, uuid4()) is None  # Неизвестный uuid.

    intent_fetched = Intent.get(realm, intent.code)
    assert intent_fetched == intent

    with time_shift(4 * 60):
        # Намерения просрочены.
        assert Intent.get(realm, intent.code) is None

        assert Intent.objects.count() == 1
        Intent.cleanup_stale()
        assert Intent.objects.count() == 0


def test_request_log():
    out = RequestLog.add(
        associate=Sber,
        data='''
         > Content-Type: application/xml
         > X-Request-Id: 85a6769a-57d7-4465-8a74-83ff04aaf43f
         > Authorization: Basic NjQzMjIzOioqKio=

        '''
    )
    assert 'Authorization' in out.data
    assert 'NjQzMjIzOioqKio=' not in out.data


def test_attachment(init_user, get_assoc_acc_curr, init_uploaded):
    user = init_user()
    _, acc, curr = get_assoc_acc_curr(Sber)

    attach = Attachment.from_uploaded(
        uploaded=init_uploaded(),
        linked=acc,
        user=user,
    )
    assert str(attach) == attach.name == 'some.dat'
    assert attach.content_type == 'text/some'
    assert attach.linked == acc
    assert attach.user == user
    assert attach.title == ''
    assert not attach.hidden
