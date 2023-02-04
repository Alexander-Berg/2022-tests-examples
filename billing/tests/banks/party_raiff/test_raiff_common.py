from bcl.banks.common.letters import BaseLetter
from bcl.banks.party_raiff import SmsLetter
from bcl.banks.registry import Raiffeisen
from bcl.banks.party_raiff.common import *
from bcl.banks.protocols.upg.raiffeisen.doc_statements_raiff import StatementsRaif
from bcl.core.models import Service, Letter


def test_communicator(read_fixture, mock_post):

    tpl_result = '''
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
    <soap:Body>
        <getRequestStatusResponse xmlns="http://upg.sbns.bssys.com/">
            <return>%s</return>
        </getRequestStatusResponse>
    </soap:Body>
    </soap:Envelope>
    '''

    xml = read_fixture('response_many.xml', decode='utf-8')
    mock_post(tpl_result % xml)

    comm = RaiffCommunicator(associate=Raiffeisen)
    comm._session_id = '123123'

    result = comm.get_request_status('9150ca09-d23b-42d0-9ac8-62a342c38048')

    _, _, value = result[0].partition('<upg:StatementsRaif>')
    value, _, _ = value.rpartition('</upg:StatementsRaif>')
    result = StatementsRaif.from_xml(value)

    subdoc = result._data['statement']

    assert subdoc['account'] == '40802810500000000001'
    assert len(subdoc['transactions']['transaction']) == 9
    assert len(subdoc['signatures']['signature']) == 1
    assert subdoc['signatures']['signature'][0]['sn'] == '21D0B66A00000000042B'


def test_check_config(
    get_source_payment, build_payment_bundle, monkeypatch, make_org, get_assoc_acc_curr, get_bundle,
    read_fixture, init_user, mock_signer
):

    counter_sbp = 0
    secret_key = '12345'

    def mock_request(*args, **kwargs):
        nonlocal counter_sbp
        counter_sbp += 1
        assert secret_key in kwargs.get('headers', {'Authorization': ''}).get('Authorization')
        raise NotImplementedError

    monkeypatch.setattr(RaiffSBPHttpClient, 'request', mock_request)

    _, acc, _ = get_assoc_acc_curr(Raiffeisen, account={'number': '098765789087', 'sbp_payments': True})

    org = acc.org
    org.connection_id = 'probki'
    org.save()

    bundle = build_payment_bundle(
        associate=Raiffeisen, payment_dicts=[{'payout_type': 3, 't_acc': '79198762536', 'service_id': Service.TOLOKA}],
        account=acc, h2h=True
    )

    bundle.associate.automate_payments(bundle)

    assert counter_sbp == 1

    secret_key = '67890'
    organization = make_org(connection_id='taxi', name='testorg')

    _, acc, _ = get_assoc_acc_curr(
        associate=Raiffeisen, org=organization, account={'sbp_payments': True, 'number': '40702810600001430560'}
    )

    bundle = build_payment_bundle(
        associate=Raiffeisen,
        payment_dicts=[{'payout_type': 3, 't_acc': '79198762536', 'service_id': Service.TOLOKA, 'f_acc': acc.number}],
        account=acc, h2h=True
    )

    bundle.associate.automate_payments(bundle)

    assert counter_sbp == 2

    payment = get_source_payment(associate=Raiffeisen, attrs={'f_acc': acc.number})
    mock_signer(read_fixture('signature.txt', decode='utf-8'), 'serial')  # Фикстура подписи фиктивная!

    init_user()

    get_bundle(payment)

    assert counter_sbp == 2


def test_letter_render(init_user):
    # проверим, что методы доступных типов писем отрабатывают штатно.
    associate = Raiffeisen
    user = init_user()
    letter = Letter.objects.create(type_id=SmsLetter.id, associate_id=associate.id, user=user)

    for letter_cls in associate.letters_supported:
        letter_typed: BaseLetter = letter_cls(letter)
        assert letter_typed.get_tpl_recipient()
        assert letter_typed.get_tpl_subject()
        assert letter_typed.get_tpl_body()
