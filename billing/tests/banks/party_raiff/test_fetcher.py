from bcl.banks.party_raiff.syncers import DocumentFetcher, PaymentSyncer
from bcl.banks.registry import Raiffeisen


def test_fetcher(read_fixture, mock_post):
    mock_post(read_fixture('all_status_responses.xml', decode='utf-8'))

    fetcher = DocumentFetcher((
        '00000000-0000-0000-0000-0000000000af',
        '11111111-1111-1111-1111-1111111111af',
        '22222222-2222-2222-2222-2222222222af',
        '33333333-3333-3333-3333-3333333333af',
        '44444444-4444-4444-4444-4444444444af',
        '55555555-5555-5555-5555-5555555555af',
        '66666666-6666-6666-6666-6666666666af',
        '77777777-7777-7777-7777-7777777777af',
    ), associate=Raiffeisen)

    fetcher.run()

    assert len(fetcher.tickets) == 3
    assert len(fetcher.statements) == 1
    assert len(fetcher.errors) == 2
    assert len(fetcher.empty_responses) == 2

    ticket = fetcher.tickets['11111111-1111-1111-1111-1111111111af']
    assert ticket['doc_id'] == '99999999-9999-9999-9999-999999999999'

    errors = fetcher.errors['22222222-2222-2222-2222-2222222222af']
    assert errors.messages == ['00004: В поле "Счет клиента" счет указан неверно']
    errors = fetcher.errors['77777777-7777-7777-7777-7777777777af']
    assert errors.status == 103
    assert errors.messages == [
        '00001: invalid xml:SAXParseException:null:null[line:column]=[1:1] '
        'message = Content is not allowed in prolog.']

    statements = fetcher.statements['55555555-5555-5555-5555-5555555555af']

    assert set([st[0] for st in statements]) == {'40702810900000021641', '40702810500001400742'}

    tickets, errors = PaymentSyncer._filter_error_tickets(fetcher.tickets)

    assert len(errors) == 2
    assert errors['33333333-3333-3333-3333-3333333333af'].messages == ['ЭП/АСП неверна']
    assert errors['44444444-4444-4444-4444-4444444444af'].messages == [(
        '\nОшибка: Код валюты счета получателя не найден в справочнике валют'
        '\nОшибка: Код валюты счета получателя не соответствует национальной валюте 810'
    )]
