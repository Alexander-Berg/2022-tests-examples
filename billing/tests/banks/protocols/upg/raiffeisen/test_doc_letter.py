from datetime import date
from textwrap import dedent

import pytest

from bcl.banks.protocols.upg.exceptions import UpgValidationError
from bcl.banks.protocols.upg.raiffeisen.doc_letter import LetterInBank


def test_get_digest():

    doc = LetterInBank({
        'doc_ext_id': '1',
        'document_data': {
            'date': date(2021, 10, 3),
            'num': '100',
            'acc': 'acc12345678901234567',
            'inn': 'inn123',
            'org': 'org123',
        },
        'theme': 'тем_а',
        'recipient': 'полу_ч',
        'body': 'тел_о',
        'attachments': {
            'attachment': [
                {
                    'name': 'uno.txt',
                    'hint': 'some1',
                    'body': 'aa=='
                },
                {
                    'name': 'dos.zip',
                    'hint': 'xxxx',
                    'body': 'bb=='
                },
            ]
        }
    })

    digest_expected = dedent('''
        [Письмо в банк]
        Номер документа=100
        Дата документа=03.10.2021 00:00:00
        Наименование организации-создателя документа (отправителя)=org123
        Сообщение=тел_о
        Cчет клиента=acc12345678901234567
        Получатель=полу_ч
        Тема письма=тем_а
        Вложение 1=uno.txt
        SIFEU2WALERIU2309R2
        aa==
        SDKRJFH37G238R7==
        Вложение 2=dos.zip
        SIFEU2WALERIU2309R2
        bb==
        SDKRJFH37G238R7==
    ''').lstrip('\n')

    digest = doc.get_digest()
    assert digest == digest_expected

    # проверим сборку xml
    xml_expected = (
        '<upg:LetterInBank xmlns:upg="http://bssys.com/upg/request" xmlns:upgRaif="http://bssys.com/upg/request/raif" '
        'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" docExtId="1"><upg:DocData docDate="2021-10-03" '
        'docNum="100" accNum="acc12345678901234567" inn="inn123"><upg:OrgName>org123</upg:OrgName></upg:DocData>'
        '<upg:Theme>тем_а</upg:Theme><upg:Receiver>полу_ч</upg:Receiver><upg:AddInfo>тел_о</upg:AddInfo>'
        '<upg:Attachments><upg:Attachment><upg:AttachmentName>uno.txt</upg:AttachmentName>'
        '<upg:Description>some1</upg:Description><upg:Body>aa==</upg:Body></upg:Attachment><upg:Attachment>'
        '<upg:AttachmentName>dos.zip</upg:AttachmentName><upg:Description>xxxx</upg:Description>'
        '<upg:Body>bb==</upg:Body></upg:Attachment></upg:Attachments></upg:LetterInBank>')
    xml = doc.to_xml()
    assert xml == xml_expected


def test_xml_export():
    with pytest.raises(UpgValidationError):
        LetterInBank({})
