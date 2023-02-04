import pytest
from django.core.files.uploadedfile import SimpleUploadedFile

from bcl.banks.common.letters import ProveLetter
from bcl.banks.party_raiff import DocCopyLetter, PayCurLetter
from bcl.banks.registry import Ing, Raiffeisen
from bcl.core.models import (
    Svo, Spd, Letter, SvoItem, SpdItem, DocRegistryRecord, states, Prove, Payment, Attachment, Role,
)
from bcl.exceptions import ValidationError

acc_num = '40702810500010000001'

@pytest.fixture
def common_checker(*, check_client_response, init_contract, init_doc_prove, table_request, get_assoc_acc_curr):

    class CommonChecker:

        def __init__(self, *, user, associate, url):
            self.associate = associate
            self.url = url

            _, acc, _ = get_assoc_acc_curr(Ing, account=acc_num)

            self.prove_1_1 = init_doc_prove(
                user=user, associate=associate, payments=[
                    {'t_name': 'uno', 'f_acc': acc_num},
                    {'t_name': 'dossss', 'f_acc': acc_num},
                    {'t_name': 'unote', 'f_acc': acc_num}
                ], account=acc
            )

            self.payment_1_1_1, self.payment_1_1_2, self.payment_1_1_3 = list(
                self.prove_1_1.payments.all().order_by('id'))

            self.prove_1_2 = init_doc_prove(
                user=user, associate=associate, payments=[
                    {'t_name': 'another', 'f_acc': acc_num}, {'t_name': 'uno2', 'f_acc': acc_num}
                ], account=acc)

            self.payment_1_2_1, self.payment_1_2_2 = list(
                self.prove_1_2.payments.all().order_by('id'))

        def run(self, *, doc_agent, doc_contract, from_registry: bool = False):
            url = self.url
            associate = self.associate
            payment_1_1_1 = self.payment_1_1_1
            payment_1_1_2 = self.payment_1_1_2
            payment_1_1_3 = self.payment_1_1_3
            payment_1_2_1 = self.payment_1_2_1
            payment_1_2_2 = self.payment_1_2_2

            type_alias = doc_agent.type_alias

            # список
            assert check_client_response(
                f'{url}', check_content=[
                    f'href="/associates/{associate.id}/docs/{type_alias}/{doc_agent.id}/"',
                    f'href="/associates/{associate.id}/docs/{type_alias}/{doc_contract.id}/"',
                ])

            # список c фильтрами
            assert check_client_response(
                f'{url}',
                # по номеру договора
                {'contract': doc_contract.contract.unumber},
                check_content=[
                    f'!href="/associates/{associate.id}/docs/{type_alias}/{doc_agent.id}/"',
                    f'href="/associates/{associate.id}/docs/{type_alias}/{doc_contract.id}/"',
                    f'href="/associates/{associate.id}/contracts/{doc_contract.contract.id}/"',
                ])

            assert check_client_response(
                f'{url}',
                # по названию контрагента
                {'counteragent': 'uno'},
                check_content=[
                    f'href="/associates/{associate.id}/docs/{type_alias}/{doc_agent.id}/"',
                    f'!href="/associates/{associate.id}/docs/{type_alias}/{doc_contract.id}/"',
                    '<div>•&nbsp;uno</div>',
                    '<div>•&nbsp;uno2</div>',
                    '<div>•&nbsp;unote</div>',
                    '!another',
                    '!dossss',
                    f'/payments/{payment_1_1_1.id}/edit/',
                    f'!/payments/{payment_1_1_2.id}/edit/',
                    f'/payments/{payment_1_1_3.id}/edit/',
                    f'!/payments/{payment_1_2_1.id}/edit/',
                    f'/payments/{payment_1_2_2.id}/edit/',
                ])

            # детализация
            if not from_registry:
                url_details = f'{url}{doc_agent.id}/'

                assert check_client_response(url_details, check_content=[
                    'id_account',
                    '<input type="text" name="date" '
                ])

                doc_agent.status = states.COMPLETE
                doc_agent.save()

                assert check_client_response(url_details, check_content=[
                    '!<input type="text" name="date" '
                ])

                if not isinstance(doc_agent, Prove):

                    def try_register():
                        assert table_request(
                            url=url,
                            realm='table-docs',
                            action='register',
                            items=[doc_agent.id],
                            associate=associate,
                        )
                        doc_agent.refresh_from_db()

                    def try_cancel():
                        assert table_request(
                            url=url,
                            realm='table-docs',
                            action='cancel',
                            items=[doc_agent.id],
                            associate=associate,
                        )
                        doc_agent.refresh_from_db()

                    # Заносим документ в реестр.
                    # сначала не выйдет, потому что документ не новый
                    try_register()
                    assert DocRegistryRecord.objects.count() == 0
                    assert doc_agent.is_complete

                    # когда документ новый, всё получится
                    doc_agent.status = states.NEW
                    doc_agent.save()
                    try_register()
                    assert DocRegistryRecord.objects.count() == 1
                    assert doc_agent.is_bundled

                    # Расформировываем.
                    # Сначала не получится — документ не новый.
                    try_cancel()
                    assert not doc_agent.hidden

                    # а теперь получится
                    doc_agent.status = states.NEW
                    doc_agent.save()
                    try_cancel()
                    assert doc_agent.hidden

    def common_checker_(*, user, associate, base_url):
        return CommonChecker(user=user, associate=associate, url=base_url)

    return common_checker_


def test_svo(init_user, get_assoc_acc_curr, check_client_response, common_checker, init_contract):

    associate, acc, _ = get_assoc_acc_curr(Ing)
    url = f'/associates/{associate.id}/docs/svo/'
    user = init_user()

    checker = common_checker(user=user, associate=associate, base_url=url)

    doc_1 = Svo.objects.create(associate_id=associate.id, user=user, account=acc)
    doc_1.set_items([
        SvoItem(prove=checker.prove_1_1, payment=checker.payment_1_1_1),
        SvoItem(prove=checker.prove_1_2, payment=checker.payment_1_2_2),
    ])

    doc_2 = Svo.objects.create(associate_id=associate.id, user=user, account=acc, contract=init_contract())

    # общие проверки
    checker.run(doc_agent=doc_1, doc_contract=doc_2)

    # детализация
    assert check_client_response(f'{url}{doc_2.id}/', check_content='name="correction" value="0"')


def test_spd(init_user, get_assoc_acc_curr, check_client_response, common_checker, init_contract):

    associate, acc, _ = get_assoc_acc_curr(Ing)
    url = f'/associates/{associate.id}/docs/spd/'
    user = init_user()

    checker = common_checker(user=user, associate=associate, base_url=url)

    doc_1 = Spd.objects.create(associate_id=associate.id, user=user, account=acc)
    doc_1.set_items([
        SpdItem(prove=checker.prove_1_1),
        SpdItem(prove=checker.prove_1_2),
    ])

    doc_2 = Spd.objects.create(associate_id=associate.id, user=user, account=acc, contract=init_contract())

    # общие проверки
    checker.run(doc_agent=doc_1, doc_contract=doc_2)


def test_letter(init_user, get_assoc_acc_curr, check_client_response, common_checker, init_contract):

    associate, acc, _ = get_assoc_acc_curr(Ing)
    url = f'/associates/{associate.id}/docs/letter/'
    user = init_user()

    checker = common_checker(user=user, associate=associate, base_url=url)

    doc_1 = Letter.objects.create(
        type_id=ProveLetter.id,
        associate_id=associate.id, user=user, account=acc)
    doc_1.proves.add(checker.prove_1_1, checker.prove_1_2)

    doc_2 = Letter.objects.create(
        type_id=ProveLetter.id,
        associate_id=associate.id, user=user, account=acc, contract=init_contract(),
        initial=doc_1,
    )

    # общие проверки
    checker.run(doc_agent=doc_1, doc_contract=doc_2)

    # детализация. связи между письмами
    assert check_client_response(
        f'{url}{doc_1.id}/',
        check_content=f'Ответы:</label><ul><li><a href="/associates/{associate.id}/docs/letter/{doc_2.id}/')

    assert check_client_response(
        f'{url}{doc_2.id}/',
        check_content=f'Исходное письмо" disabled><a href="/associates/{associate.id}/docs/letter/{doc_1.id}/')


def test_letter_new(check_client_response, get_assoc_acc_curr):

    associate, acc, _ = get_assoc_acc_curr(Raiffeisen)

    # Добавляем Письмо через интерфейс.
    assert check_client_response(
        f'/associates/{associate.id}/docs/letter/add/',
        {
            'account': acc.id,
            'type_id': PayCurLetter.id,
            'subject': 'one',
            'recipient': 'two',
            'date': '06.10.2021',
            'body': 'some',
            'attachments-TOTAL_FORMS': 1,
            'attachments-INITIAL_FORMS': 0,
            'attachments-MIN_NUM_FORMS': 0,
            'attachments-MAX_NUM_FORMS': 1000,
            'attachments-0-title': 'qq',
            'attachments-0-DELETE': '',
            'attachments-0-id': '',
            '__submit': 'siteform',
        },
        files={
            'attachments-0-attached': SimpleUploadedFile('myfile.txt', b'zzz'),
        },
        method='post',
    )
    letter_1 = Letter.objects.last()
    assert letter_1.associate.id == associate.id
    assert letter_1.is_dir_out
    assert letter_1.subject == 'one'
    assert letter_1.recipient == 'two'
    assert letter_1.body == 'some'
    assert letter_1.type_id == PayCurLetter.id
    assert letter_1.user_id
    attach = Attachment.objects.first()
    assert attach.linked == letter_1
    assert attach.title == 'qq'
    assert attach.name == 'myfile.txt'
    assert attach.content == b'zzz'

    # проверим ajax-запрос на предоставление шаблонного заполнения полей
    assert check_client_response(
        f'/associates/{associate.id}/docs/letter/add/',
        {
            'action': 'get_preset',
            'type_id': DocCopyLetter.id,
            'context': {
                'payment': '',
                'account': acc.id,
            },
        },
        method='json',
        check_content=[
            '{"subject": "\\u0417',
            'fakedorg',  # проверка подстановки значения для макроса
        ]
    )


def test_letter_from_payment(
    init_user, get_assoc_acc_curr, check_client_response, common_checker,
    init_contract, table_request, get_source_payment, spawn_prove, check_prove_invalidation
):
    associate, acc, _ = get_assoc_acc_curr(Raiffeisen)
    aid = associate.id
    contract = init_contract()
    payment_1 = get_source_payment({'f_acc': acc.number, 'contract_id': contract.id}, associate=associate)

    # Страница нового письмо с базовым платежом.
    assert check_client_response(
        f'/associates/{aid}/docs/letter/add/',
        {
            'payment': payment_1.id,
        },
        method='post',
        check_content='name="payment"'
    )

    # Добавляем Письмо через интерфейс.
    assert check_client_response(
        f'/associates/{associate.id}/docs/letter/add/',
        {
            'account': acc.id,
            'type_id': PayCurLetter.id,
            'subject': 'one',
            'recipient': 'two',
            'date': '06.10.2021',
            'body': 'some',
            'attachments-TOTAL_FORMS': 1,
            'attachments-INITIAL_FORMS': 0,
            'payment': payment_1.id,
            '__submit': 'siteform',
        },
        method='post',
    )
    payment_1.refresh_from_db()

    letters = list(payment_1.letters.all())
    assert len(letters) == 1
    assert letters[0].payment_id == payment_1.id


@pytest.fixture
def spawn_prove(init_doc_prove, init_contract):
    def spawn_prove_(*, associate, acc, user, contract=None, payments=None):

        return init_doc_prove(
            user=user, associate=associate, payments=payments or [{'t_name': 'another'}],
            type_id=Prove.TYPE_INVOICE,
            contract=contract or init_contract(),
            account=acc
        )

    return spawn_prove_


@pytest.fixture
def check_prove_invalidation(table_request):
    def check_prove_invalidation_(*, url, associate, prove):
        # Проверка отбраковки пд
        assert table_request(
            url=url,
            realm='table-docs',
            action='invalidate',
            items=[prove.id],
            associate=associate,
        )
        prove.refresh_from_db()
        assert prove.status == states.USER_INVALIDATED

    return check_prove_invalidation_


def test_prove(
    init_user, get_assoc_acc_curr, check_client_response, common_checker,
    init_contract, table_request, spawn_prove, check_prove_invalidation, get_document
):

    associate, acc, _ = get_assoc_acc_curr(Ing, account=acc_num)
    url = f'/associates/{associate.id}/docs/prove/'
    init_user(robot=True)
    user = init_user()

    checker = common_checker(user=user, associate=associate, base_url=url)

    doc_1 = checker.prove_1_1
    doc_1.account = acc
    doc_1.save()

    doc_2 = spawn_prove(associate=associate, acc=acc, user=user)
    contract_2 = init_contract()
    doc_3 = spawn_prove(associate=associate, acc=acc, user=user, contract=contract_2)
    doc_4 = spawn_prove(associate=associate, acc=acc, user=user, contract=contract_2)

    # общие проверки
    checker.run(doc_agent=doc_1, doc_contract=doc_2)

    # фильтрация по типу
    assert check_client_response(
        f'{url}',
        # по номеру договора
        {'type': doc_2.type_id},
        check_content=[
            f'!href="/associates/{associate.id}/docs/prove/{doc_1.id}/"',
            f'href="/associates/{associate.id}/docs/prove/{doc_2.id}/"',
        ])

    # для проверки данных колонки Отправлялся
    letter = Letter.objects.create(
        type_id=ProveLetter.id, associate_id=associate.id, user=user, account=acc,
        status=states.COMPLETE,  # Письмо отправлялось, а в нём и ПД.
    )
    letter.proves.add(doc_1)

    assert check_client_response(f'{url}', check_content=[
        '<td>Акт<',
        '>нет</td>',
        '>да</td>',
    ])

    # Проверка формирования СПД
    with pytest.raises(ValidationError):
        table_request(
            url=url,
            realm='table-docs',
            action='create-spd',
            items=[doc_1.id, doc_2.id, doc_3.id],
            associate=associate,
        )
    doc_1.attachment_add(name='one.txt', content=b'data1', content_type='text/plain')
    doc_2.attachment_add(name='one.txt', content=b'data1', content_type='text/plain')
    doc_3.attachment_add(name='one.txt', content=b'data1', content_type='text/plain')

    assert table_request(
            url=url,
            realm='table-docs',
            action='create-spd',
            items=[doc_1.id, doc_2.id, doc_3.id],
            associate=associate,
        )
    assert Spd.objects.count() == 2

    check_prove_invalidation(url=url, associate=associate, prove=doc_4)


def test_svo_and_letter_from_payment(
    get_assoc_acc_curr, table_request, init_contract, get_source_payment, spawn_prove, init_user):
    associate, acc, _ = get_assoc_acc_curr(Ing)
    user = init_user()

    contract = init_contract()
    payment_1 = get_source_payment({'f_acc': acc.number, 'contract_id': contract.id}, associate=associate)
    prove_1 = spawn_prove(associate=associate, acc=acc, user=user, payments=[payment_1])
    prove_2 = spawn_prove(associate=associate, acc=acc, user=user, payments=[payment_1])
    prove_3 = spawn_prove(associate=associate, acc=acc, user=user, payments=[payment_1])
    url = f'/associates/{associate.id}/payments/{payment_1.id}/edit/'

    # Проверка формирования СПД
    assert table_request(
        url=url,
        realm='table-docs',
        action='create-svo',
        items=[prove_1.id, prove_2.id],
        associate=associate,
    )
    svos = list(Svo.objects.all())
    assert len(svos) == 1
    assert list(svos[0].items.values_list('prove_id', flat=True)) == [prove_1.id, prove_2.id]

    # Проверка формирования Письма.
    assert table_request(
        url=url,
        realm='table-docs',
        action='create-letter',
        items=[prove_1.id, prove_3.id],
        associate=associate,
    )
    letters = list(Letter.objects.all())
    assert len(svos) == 1
    assert list(letters[0].proves.values_list('id', flat=True)) == [prove_1.id, prove_3.id]


def test_payment_no_docs(check_client_response, get_source_payment, get_assoc_acc_curr):
    associate, acc, _ = get_assoc_acc_curr(Ing)

    payment_1 = get_source_payment({'f_acc': acc.number}, associate=associate)
    url = f'/associates/{associate.id}/payments/{payment_1.id}/edit/'

    # кнопки нет
    assert check_client_response(url, check_content=[
        '!Не требовать документы</button',
    ])

    # покажем кнопку
    payment_1.invalidate('hmm', mode=Payment.INV_DOCS_REQ)
    assert check_client_response(url, check_content=[
        'Не требовать документы</button',
    ])

    # проверим проставление метки
    assert check_client_response(url, {'action': 'ignore-docs'}, method='post')
    payment_1.refresh_from_db()
    assert payment_1.invalidation == Payment.INV_DOCS_IGNORE


def test_prove_from_payment(
    init_user, get_assoc_acc_curr, check_client_response, common_checker,
    init_contract, table_request, get_source_payment, spawn_prove, check_prove_invalidation
):
    associate, acc, _ = get_assoc_acc_curr(Ing)
    aid = associate.id

    contract = init_contract()
    payment_1 = get_source_payment({'f_acc': acc.number, 'contract_id': contract.id}, associate=associate)
    payment_2 = get_source_payment({'f_acc': acc.number, 'contract_id': contract.id}, associate=associate)
    payment_3 = get_source_payment({'f_acc': acc.number}, associate=associate)
    url = f'/associates/{associate.id}/payments/{payment_1.id}/edit/'
    user = init_user()

    payments = [payment_1]
    prove_1 = spawn_prove(associate=associate, acc=acc, user=user, payments=payments, contract=contract)
    prove_2 = spawn_prove(associate=associate, acc=acc, user=user, contract=contract)
    prove_3 = spawn_prove(associate=associate, acc=acc, user=user, payments=[payment_3])

    # список связанных с платежом
    assert check_client_response(url, check_content=[
        f'href="/associates/{aid}/docs/prove/{prove_1.id}/">',
        # не привязан к платежу, но виден из-за привязки к контракту
        f'href="/associates/{aid}/docs/prove/{prove_2.id}/">',
        f'!href="/associates/{aid}/docs/prove/{prove_3.id}/">',
    ])
    check_prove_invalidation(url=url, associate=associate, prove=prove_2)

    # дополнительная ПД в списке
    assert check_client_response(
        url,
        {'payment': f'{payment_3.number}'},
        check_content=[
            f'href="/associates/{aid}/docs/prove/{prove_1.id}/">',
            f'href="/associates/{aid}/docs/prove/{prove_2.id}/">',
            f'href="/associates/{aid}/docs/prove/{prove_3.id}/">',
        ])

    # Проверяем форму создания ПД.
    assert check_client_response(
        f'/associates/{aid}/docs/prove/add/',
        {
            'payment': payment_1.id,
        },
        method='post',
        check_content=[
            '</a> — 152.00 RUB',  # отрисуем ещё не привязанный, но базовй платёж
        ]
    )

    # Добавляем ПД через интерфейс.
    response = check_client_response(
        f'/associates/{aid}/docs/prove/add/',
        {
            'type_id': 2,
            'contract_num': 'zxc111',
            'number': 3213,
            'date': '17.05.2020',
            'kind_code': 222,
            'delivery': 1,
            'summ': '11.00',
            'currency_id': 643,
            'contract_summ': '445.00',
            'attachments-TOTAL_FORMS': 1,
            'attachments-INITIAL_FORMS': 0,
            'attachments-MIN_NUM_FORMS': 0,
            'attachments-MAX_NUM_FORMS': 1000,
            'attachments-0-title': 'qq',
            'attachments-0-DELETE': '',
            'attachments-0-id': '',
            'payment': payment_1.id,
            '__submit': 'siteform',
        },
        files={
            'attachments-0-attached': SimpleUploadedFile('myfile.txt', b'zzz'),
        },
        method='post',
    )
    assert response
    assert response.url.endswith('/edit/')  # перенаправляем на страницу исходного платежа
    prove_4 = Prove.objects.last()
    assert prove_4.id > prove_3.id
    attach = Attachment.objects.first()
    assert attach.linked == prove_4
    assert attach.title == 'qq'
    assert attach.name == 'myfile.txt'
    assert attach.content == b'zzz'


def test_registry(init_user, get_assoc_acc_curr, check_client_response, common_checker, init_contract, table_request):

    associate, acc, _ = get_assoc_acc_curr(Ing)
    url = f'/associates/{associate.id}/docs/'

    # ограничим пользователя организацией, чтобы проверить работу фильтрации.
    user = init_user()
    role = Role.SUPER_TREASURER
    user.roles = [role]
    user.restrictions = {role: {'org': [acc.org_id]}}
    user.save()

    checker = common_checker(user=user, associate=associate, base_url=url)

    doc_1 = Spd.objects.create(associate_id=associate.id, user=user, account=acc)
    doc_1.set_items([
        SpdItem(prove=checker.prove_1_1),
        SpdItem(prove=checker.prove_1_2),
    ])
    doc_2 = Spd.objects.create(associate_id=associate.id, user=user, account=acc, contract=init_contract())
    doc_3 = Svo.objects.create(associate_id=associate.id, user=user, account=acc)

    registered = []
    for doc in [doc_1, doc_2, doc_3]:
        registered.append(DocRegistryRecord.add_doc(doc=doc, associate=associate, user=user))
    doc_1_reg, doc_2_reg, doc_3_reg = registered

    # общие проверки
    checker.run(doc_agent=doc_1, doc_contract=doc_2, from_registry=True)

    # фильтр по типу
    assert check_client_response(
        f'{url}',
        {'type': doc_3.type_alias},
        check_content=[
            f'!href="/associates/{associate.id}/docs/{doc_1.type_alias}/{doc_1.id}/"'
            f'!href="/associates/{associate.id}/docs/{doc_2.type_alias}/{doc_2.id}/"'
            f'href="/associates/{associate.id}/docs/{doc_3.type_alias}/{doc_3.id}/"'
        ])

    # Удаление документа.
    assert table_request(
        url=url,
        realm='table-docs',
        action='delete',
        items=[doc_3_reg.id],
        associate=associate,
    )
    doc_3_reg.refresh_from_db()
    assert doc_3_reg.hidden

    # Постановка на отправку.
    assert table_request(
        url=url,
        realm='table-docs',
        action='schedule',
        items=[doc_2_reg.id],
        associate=associate,
    )
    doc_1_reg.refresh_from_db()
    doc_2_reg.refresh_from_db()
    assert doc_2_reg.is_processing_ready
    assert doc_1_reg.is_new


def test_contract(
    get_assoc_acc_curr, init_user, init_doc_prove, get_source_payment, check_client_response, init_contract):

    contract = init_contract()
    associate, acc, _ = get_assoc_acc_curr(Ing)
    user = init_user()

    aid = associate.id
    svo = Svo.objects.create(associate_id=aid, contract=contract, user=user, account=acc)
    spd = Spd.objects.create(associate_id=aid, contract=contract, user=user, account=acc)
    prove = init_doc_prove(user=user, account=acc, contract=contract)
    letter = Letter.objects.create(
        type_id=ProveLetter.id, associate_id=aid, contract=contract, user=user, account=acc)
    payment = get_source_payment(attrs={'contract': contract, 'account': acc})

    # детализация
    assert check_client_response(f'/associates/3/contracts/{contract.id}/', check_content=[
        f'href="/associates/{aid}/docs/svo/{svo.id}/">',
        f'href="/associates/{aid}/docs/prove/{prove.id}/">',
        f'href="/associates/{aid}/docs/spd/{spd.id}/">',
        f'href="/associates/{aid}/docs/letter/{letter.id}/">',
        f'href="/associates/9/payments/{payment.id}/edit/">',
    ])


def test_getfile(check_client_response, get_assoc_acc_curr, init_user):

    associate, acc, _ = get_assoc_acc_curr(Ing)
    user = init_user()

    doc_1 = DocRegistryRecord.add_doc(
        doc=Spd.objects.create(associate_id=associate.id, user=user, account=acc),
        associate=associate,
        user=user
    )
    doc_1.content = 'somevalue'
    doc_1.save()

    assert check_client_response(f'/get_file?n=doc_{doc_1.id}', check_content=lambda data: data == b'somevalue')
