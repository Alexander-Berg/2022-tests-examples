# coding: utf-8
"""Тесты для проверки генераторов писем.
Сгенерированное письмо получаем через balance_mailer.MessageData.from_message_mapper.
Генерация html и pdf при необходимости замокана с проверкой передаваемых параметров.
"""

import pytest
import pickle
import datetime
import marshal
import urllib
import mock

from balance import mapper
from balance import constants as cst
from balance import message_generator as generator
from balance.utils.formatting import pretty_date, pretty_period

from mailer import balance_mailer
from mailer import mailer_util as util

from tests import object_builder as ob


NOW = datetime.datetime.now()


class MessageDataTest(object):
    """Определяем дефолтные параметры для писем"""
    opcode = None
    protocol = pickle.HIGHEST_PROTOCOL
    rcpt_name = u'Balance Test Email'
    subject = u'Тестовое письмо'
    body = u'Уважаемый, клиент. Пишем Вам, потому что соскучились. С уважнием, Вася Пупкин.'
    attach_list = []
    data = None
    multipart = ''
    reply_to_email = None
    bcc = None
    send_dt = NOW

    def __init__(self, cfg):
        self.object_id = ob.get_big_number()
        self.sender = balance_mailer.u(util.get_conf(cfg, 'Mailer/Sender'))
        self.sender_name = balance_mailer.u(util.get_conf(cfg, 'Mailer/SenderName'))
        self.rcpt = balance_mailer.u(util.get_conf(cfg, 'MailErrors'))

        self.cfg = cfg


@pytest.fixture
def test_data(app):
    return MessageDataTest(app.cfg)


def _get_msg_mapper(test_data, session):
    params = {
        'opcode': test_data.opcode,
        'object_id': test_data.object_id,
        'recepient_name': test_data.rcpt_name,
        'recepient_address': test_data.rcpt,
        'data': test_data.data,
        'send_dt': test_data.send_dt,
    }
    for key, value in params.items():
        if value is None:
            del params[key]
    return ob.EmailMessageBuilder(**params).build(session).obj


@pytest.mark.email
class TestEmailMessageGenerator(object):
    """Внутри тестов переопределяем аттрибуты письма, затем проверяем, что отправляется правильное письмо"""

    def _check_msg_mapper(self, test_data, msg_mapper):
        assert test_data.opcode == msg_mapper.opcode
        assert test_data.object_id == msg_mapper.object_id
        assert test_data.rcpt_name == msg_mapper.recepient_name
        assert test_data.rcpt == msg_mapper.recepient_address
        assert test_data.data == msg_mapper.data
        assert test_data.send_dt == msg_mapper.send_dt

    def _check_msg_data(self, test_data, msg_data):
        # msg_data - объект balance_mailer.MessageData
        assert [test_data.rcpt] == msg_data.rcpt
        assert test_data.rcpt_name == msg_data.rcpt_name
        assert test_data.sender == msg_data.sender
        assert test_data.sender_name == msg_data.sender_name
        assert test_data.subject == msg_data.subject
        assert test_data.body == msg_data.body
        assert test_data.multipart == msg_data.multipart
        assert test_data.reply_to_email == msg_data.reply_to_email
        assert test_data.bcc == msg_data.bcc

        if test_data.attach_list is not None or msg_data.attach_list is not None:
            assert len(test_data.attach_list) == len(msg_data.attach_list)
            for attach_base, attach_data in zip(test_data.attach_list, msg_data.attach_list):
                assert attach_base.name == attach_data.name
                assert attach_base.type == attach_data.type
                assert attach_base.data == attach_data.data

    def _check(self, test_data, session):
        # Метод для проверки писем
        msg = _get_msg_mapper(test_data, session)
        self._check_msg_mapper(test_data, msg)

        msg_data = balance_mailer.MessageData.from_message_mapper(msg, test_data.cfg)
        self._check_msg_data(test_data, msg_data)

    def _set_invoice_common_params(self, test_data, invoice, session):
        test_data.multipart = ''
        test_data.object_id = invoice.id
        test_data.sender = 'info-noreply@support.yandex.com'

        firm = session.query(mapper.Firm).getone(invoice.firm_id)
        test_data.subject = u'{}: выставлен счет {} от {}'.format(
            firm.title,
            invoice.external_id,
            pretty_date(invoice.dt)
        )
        test_data.body = u'Здравствуйте, {}!\n' \
                    u'Счет находится в прикрепленном файле.\n\n' \
                    u'--\n' \
                    u'С уважением,\n' \
                    u'Яндекс.Баланс\n'.format(invoice.person.name)
        test_data.data = pickle.dumps((test_data.subject, test_data.body), protocol=test_data.protocol)

    def test_opcode_1(self, test_data, session):
        test_data.opcode = cst.INVOICE_FORM_CREATOR_MESSAGE_OPCODE
        invoice = ob.InvoiceBuilder(firm_id=cst.FirmId.AUTORU).build(session).obj
        self._set_invoice_common_params(test_data, invoice, session)
        test_data.reply_to = ("client@auto.ru", "client@auto.ru")
        attach_name = u'{}.{}'.format('invoice', 'html')
        attach_type = 'application/pdf'
        attach_data = 'Attachment text 1'
        test_data.attach_list = [
            generator.Attachment(attach_name, attach_type, attach_data),
        ]

        with mock.patch('balance.publisher.fetch.get_file_content') as mock_get_file_content:
            mock_get_file_content.return_value = attach_name, attach_type, attach_data
            self._check(test_data, session)
            mock_get_file_content.assert_called_with(session, test_data.object_id, 'html', 'invoice',
                                                     name_no_ext='invoice')

    def test_opcode_3(self, test_data, session):
        test_data.opcode = cst.INVOICE_MSWORD_CREATOR_MESSAGE_OPCODE
        invoice = ob.InvoiceBuilder().build(session).obj
        self._set_invoice_common_params(test_data, invoice, session)

        attach_name = u'{}.{}'.format(invoice.external_id, 'rtf')
        attach_type = 'application/msword'
        attach_data = 'Attachment text 1'
        test_data.attach_list = [
            generator.Attachment(attach_name, attach_type, attach_data),
        ]

        with mock.patch('balance.publisher.fetch.get_file_content') as mock_get_file_content:
            mock_get_file_content.return_value = attach_name, attach_type, attach_data
            self._check(test_data, session)
            mock_get_file_content.assert_called_with(session, test_data.object_id, 'rtf', 'invoice', name_no_ext=None)

    def test_opcode_4(self, test_data, session):
        test_data.opcode = cst.AUTOLIMITS_NOTIFICATION_MESSAGE_OPCODE
        manager = ob.ManagerWithChiefsBuilder().build(session).obj

        test_data.object_id = manager.manager_code
        external_id = u'ABC1234'
        client_name = u'Client Name'
        activity_type = ''
        old_turns = 35000
        new_turns = 55000
        credit_limit_currency = 'RUR'
        subclient = None
        test_data.data = marshal.dumps(
            [(external_id, client_name, activity_type, old_turns, new_turns, credit_limit_currency, subclient)],
        )

        test_data.subject = u"автоматически рассчитанные кредитные лимиты"
        test_data.body = u'''Добрый день!
Автоматически выданные лимиты на {} месяц {} года:

{}
--
С уважением,
администрация Баланса
{:%Y-%m-%d}
        '''.format(
            NOW.month,
            NOW.year,
            '\n'.join([
                u'Договор N %s с %s:\n' % (external_id, client_name),
                u' '.join([
                    u'\t',
                    u'лимит',
                    u'изменился с %s %s на %s %s' % (
                    old_turns, credit_limit_currency, new_turns, credit_limit_currency)
                ]),
            ]),
            NOW,
        )

        self._check(test_data, session)

    def test_opcode_5(self, test_data, session):
        test_data.opcode = cst.OVERDRAFT_DELAY_MESSAGE_CREATOR_MESSAGE_OPCODE
        test_data.subject = u"Неоплаченный овердрафт."
        test_data.body = u"""
Здравствуйте!

Вы являетесь клиентом компании Яндекс, приобретая наши услуги по размещению рекламы. При
выставлении счетов Вы воспользовались услугой "Отсрочка платежа"
(http://help.yandex.ru/direct/?id=990434#995282), предоставляемой клиентам Яндекc.Директ, но, к
сожалению, оплата по этим счетам на наш расчетный счет до сих пор не поступила.
Мы бы хотели с Вашей помощью разрешить это досадное недоразумение. Просим Вас перезвонить в
офис Яндекса по телефону +7 (495) 739-22-22 (добавочный 2328) или написать по электронной
почте info@balance.yandex.ru (с пометкой в теме письма <для Навального Вальдемара>) и
сообщить дополнительную информацию о деталях данного платежа.

К сожалению, до полного погашения задолженности, мы будем вынуждены начинать оказывать Вам
рекламные услуги только после фактического поступления денег нам на расчетный счет.

Пожалуйста, оплатите наши счета.

--
С уважением,
Яндекс.Баланс
"""
        self._check(test_data, session)

    def test_opcode_6(self, test_data, session):
        test_data.opcode = cst.KZ_EXPORTER_CREATOR_MESSAGE_OPCODE
        attach_content = u'Attach content'
        test_data.data = pickle.dumps(
            (test_data.send_dt, attach_content),
            protocol=test_data.protocol,
        )
        test_data.subject = u'[yandex-balance {}] Документы для экспорта в бухгалтерскую систему'.format(
            test_data.send_dt.strftime('%d.%m.%Y')
        )
        test_data.body = u'''Добрый день.

Документы для экспорта в бухгалтерскую систему. Дата экспорта - {}.

--
С уважением, Яндекс.Баланс'''.format(test_data.send_dt.strftime('%d.%m.%Y'))
        test_data.attach_list = [
            generator.Attachment(test_data.send_dt.strftime('%Y%m%d.zip'), 'application/zip', attach_content)
        ]

        self._check(test_data, session)

    def test_opcode_8(self, test_data, session):
        test_data.opcode = cst.DISPATCH_P_CONTRACT_CREATOR_MESSAGE_OPCODE
        test_data.sender = u'docs@partner.yandex.ru'
        test_data.sender_name = u'Рекламная Сеть Яндекса'
        test_data.subject = u'Рекламная Сеть Яндекса -- уведомление о получении копий/оригиналов договоров.'
        message_dict = {
            'person_greeting': u'Добрый день, %s.' % test_data.rcpt_name,
            'mail_date': test_data.send_dt.strftime("%d.%m.%Y"),
            'dispatched_text': u'Получены факсовые копии договоров:  РС-59869-02/18.',
        }
        test_data.body = u'''%(person_greeting)s

%(dispatched_text)s

С уважением,
Отдел Развития Рекламной Сети Яндекса
docs@partner.yandex.ru
%(mail_date)s
''' % message_dict
        test_data.data = u'recv_faxed: РС-59869-02/18'.encode('utf-8')

        self._check(test_data, session)

    def test_opcode_10(self, test_data, session):
        test_data.opcode = cst.GENERIC_MESSAGE_CREATOR_MESSAGE_OPCODE
        test_data.attach_list = [
            generator.Attachment('attach1', 'text/html', 'Attachment text 1'),
        ]
        test_data.data = pickle.dumps(
            (
                test_data.subject,
                test_data.body,
                (test_data.sender, test_data.sender_name),
                ((test_data.attach_list[0].name, test_data.attach_list[0].type, test_data.attach_list[0].data),),
            ),
            protocol=test_data.protocol
        )

        self._check(test_data, session)

    def test_opcode_12(self, test_data, session):
        test_data.opcode = cst.PERIOD_ERROR_MESSAGE_CREATOR_MESSAGE_OPCODE
        test_data.sender_name = test_data.sender
        contract_eid = ob.get_big_number()
        period_start_dt = test_data.send_dt
        period_end_dt = period_start_dt + datetime.timedelta(days=1)
        test_data.data = pickle.dumps(
            (contract_eid, period_start_dt, period_end_dt, test_data.body, test_data.sender),
            protocol=test_data.protocol,
        )
        test_data.subject = u"Ответ по отчету Комиссионера за %s по договору № %s" % (
            pretty_period(period_start_dt, period_end_dt),
            contract_eid
        )

        self._check(test_data, session)

    def test_opcode_13(self, test_data, session):
        test_data.opcode = cst.INVOICE_SCAN_MESSAGE_CREATOR_MESSAGE_OPCODE
        test_data.subject = u'Подтверждение оплаты счета'
        test_data.body = u"""Подтверждение оплаты счета."""

        name = test_data.sender_name
        address = test_data.sender
        fname = 'attach name'
        ctype = 'test/html'
        data = 'test data'
        test_data.attach_list = [generator.Attachment(fname, ctype, data)]
        test_data.data = pickle.dumps(
            (ctype, fname, data, name, address),
            protocol=test_data.protocol,
        )

        self._check(test_data, session)

    def test_opcode_14(self, test_data, session):
        test_data.opcode = cst.MONTHLY_ACT_REPORT_MESSAGE_OPCODE

        test_data.sender = 'sender@mail.com'
        test_data.sender_name = 'Sender Name'
        test_data.object_id = ob.get_big_number()
        attach_type = 'html'
        rep_type = 'act'
        dt = test_data.send_dt
        attach_name = 'filename'
        attach_data = 'Attach data 1'
        attach_name_2 = 'monthly_acts.xls'
        attach_type_2 = 'application/vnd.ms-excel'
        attach_data_2 = 'Attach data 2'
        test_data.data = pickle.dumps(
            {
                'subject': test_data.subject,
                'body': test_data.body,
                'sender': (test_data.sender, test_data.sender_name),
                'needs_registry': True,
                'attach_info': [{
                    'object_id': test_data.object_id,
                    'p_type': attach_type,
                    'rep_type': rep_type,
                    'dt': dt,
                    'name': attach_name,
                }],
            },
            protocol=test_data.protocol,
        )
        test_data.attach_list = [
            generator.Attachment(attach_name, attach_type, attach_data),
            generator.Attachment(attach_name_2, attach_type_2, attach_data_2),
        ]

        with mock.patch('balance.actions.generate_registry.get_agency_registry') as mock_get_agency_registry,\
                mock.patch('balance.publisher.fetch.get_file_content') as mock_get_file_content:

            mock_get_file_content.return_value = attach_name, attach_type, attach_data
            mock_get_agency_registry.return_value = attach_name_2, attach_type_2, attach_data_2

            self._check(test_data, session)

            mock_get_file_content.assert_called_once_with(session, test_data.object_id, attach_type,
                                                          rep_type, dt, attach_name)
            mock_get_agency_registry.assert_called_once_with(session, [test_data.object_id])

    def _trust_common_check(self, test_data, subj_title, data_title, data_html_title, session):
        lang = 'ru'
        order = ob.OrderBuilder(lang_id=lang).build(session).obj
        row = ob.BasketItemBuilder(order=order, quantity=1)
        request = ob.RequestBuilder(basket=ob.BasketBuilder(rows=[row]))
        invoice = ob.InvoiceBuilder(request=request)
        trust_payment = ob.TrustPaymentBuilder(
            invoice=invoice, request=request
        ).build(session).obj
        trust_payment.order = order
        session.flush()

        test_data.object_id = trust_payment.id
        test_data.sender = 'info-noreply@support.yandex.com'
        test_data.sender_name = u'Яндекс.Баланс'
        test_data.body = ''
        test_data.multipart = 'alternative'

        attach_data_success = 'attach data success'
        attach_data_success_html = 'attach data success html'
        templates = {
            'trust_payment/%s.mako' % subj_title: test_data.subject,
            'trust_payment/%s.mako' % data_title: attach_data_success,
            'trust_payment/%s.mako' % data_html_title: attach_data_success_html,
        }
        test_data.attach_list = [
            generator.Attachment('', 'text/plain', attach_data_success),
            generator.Attachment('', 'text/html', attach_data_success_html),
        ]

        def mock_render(templ_path, *a, **kwargs):
            render_order = kwargs.get('order', None)
            render_payment = kwargs.get('payment', None)
            render_ctx = kwargs.get('ctx', None)
            if order:
                assert render_order == order
            if render_payment:
                assert render_payment == trust_payment
            if render_ctx:
                assert render_ctx.lang == lang
                assert render_ctx.order == order
                assert render_ctx.payment == trust_payment
            return templates[templ_path]

        with mock.patch('butils.application.plugins.mako_helper.MakoRenderer.render', staticmethod(mock_render)):
            self._check(test_data, session)

    def test_opcode_15(self, test_data, session):
        test_data.opcode = cst.TRUST_PAYMENT_SUCCESS_MESSAGE_OPCODE
        self._trust_common_check(
            test_data=test_data,
            subj_title='payment_success_subj',
            data_title='payment_success',
            data_html_title='payment_success_html',
            session=session,
        )

    def test_opcode_16(self, test_data, session):
        test_data.opcode = cst.TRUST_PAYMENT_REFUND_MESSAGE_OPCODE
        self._trust_common_check(
            test_data=test_data,
            subj_title='payment_refund_subj',
            data_title='payment_refund',
            data_html_title='payment_refund_html',
            session=session,
        )

    def test_opcode_17(self, test_data, session):
        test_data.opcode = cst.PUBLISHED_ACT_MESSAGE_OPCODE
        test_data.sender = 'info-noreply@support.yandex.com'

        act_ids = [ob.get_big_number()]
        start_dt = NOW
        end_dt = start_dt + datetime.timedelta(days=1)
        first = 1
        last = 1000
        max = 5000
        test_data.data = pickle.dumps(
            (act_ids, start_dt, end_dt, first, last, max),
            protocol=test_data.protocol,
        )

        test_data.subject = u'Копии запрошенных документов за период с %s по %s, с %s по %s из %s' % (
            pretty_date(start_dt), pretty_date(end_dt), first, last, max)
        test_data.body = u'''Добрый день!
Копии запрошенных документов за период с %s по %s, с %s по %s из %s находятся во вложенных файлах.

С уважением,
Подразделение по сопровождению коммерческих проектов
%s'''% (pretty_date(start_dt), pretty_date(end_dt), first, last, max, pretty_date(datetime.datetime.now()))

        attach_name = 'attach_name.pdf'
        attach_type = 'application/pdf'
        attach_data = 'Attach data 1'
        test_data.attach_list = [generator.Attachment(attach_name, attach_type, attach_data)]

        with mock.patch('balance.publisher.fetch.get_file_content') as mock_get_file_content:
            mock_get_file_content.return_value = attach_name, attach_type, attach_data
            self._check(test_data, session)
            mock_get_file_content.assert_called_once_with(session, act_ids[0], 'pdf', 'oebs_act')

    def test_opcode_18(self, test_data, session):
        test_data.opcode = cst.GENERIC_MAKO_CREATOR_MESSAGE_OPCODE
        template_name = 'test template name'
        template_params = {'param1': 0, 'param2': 1, 'param3': 2}
        test_data.attach_list = [
            generator.Attachment('attach1', 'text/html', 'Attachment text 1'),
        ]
        test_data.data = pickle.dumps(
            (
                test_data.subject, (test_data.sender, test_data.sender_name),
                [(test_data.attach_list[0].name, test_data.attach_list[0].type, test_data.attach_list[0].data)],
                template_name, template_params
             ),
            protocol=test_data.protocol,
        )

        with mock.patch('mako.lookup.TemplateLookup.get_template') as mock_object:
            # замокиваем цепочку вызовов mock_get_template().get_def().render()
            mock_object.render.return_value = test_data.body  # глобальная переменна
            mock_object.get_def.return_value = mock_object
            mock_object.return_value = mock_object

            self._check(test_data, session)

            mock_object.assert_called_with(template_name)
            mock_object.get_def.assert_called_with('body')
            mock_object.render.assert_called_once_with(**template_params)

    def test_opcode_19(self, test_data, session):
        test_data.opcode = cst.HTML_MESSAGE_CREATOR_MESSAGE_OPCODE
        test_data.body = ''
        test_data.multipart = 'alternative'

        test_data.attach_list = [generator.Attachment('', 'text/html', test_data.body)]
        test_data.data = pickle.dumps(
            (test_data.subject, test_data.body, (test_data.sender, test_data.sender_name)),
            protocol=test_data.protocol
        )

        self._check(test_data, session)

    def test_opcode_20(self, test_data, session):
        from billing.contract_iface.cmeta import general
        agency = ob.ClientBuilder(is_agency=True)
        client = ob.ClientBuilder(is_agency=False, agency=agency)
        person = ob.PersonBuilder(client=agency)
        contract = ob.ContractBuilder(
            client=client, person=person,
            commission=0, finish_dt=session.now()
        ).build(session).obj
        collateral_type = general.collateral_types[1006]  # пролонгация+односторонние акты
        finish_dt = session.now() + datetime.timedelta(days=30*3)
        collateral = ob.CollateralBuilder(contract=contract, collateral_type=collateral_type,
                                          finish_dt=finish_dt).build(session).obj
        session.flush()

        test_data.opcode = cst.PRINT_TEMPLATE_MESSAGE_CREATOR_OPCODE
        test_data.object_id = collateral.id
        test_data.bcc = test_data.rcpt
        test_data.multipart = 'mixed'

        # Замокиваем методы, которые вызываются в PrintFormMessageCreator для получения элементов письма
        collateral.print_tpl_email_from = test_data.sender_name = test_data.reply_to_email = test_data.sender
        collateral.print_tpl_email_subject = test_data.subject
        collateral.print_tpl_email_body = test_data.body

        test_data.data = pickle.dumps(
            (contract.id, 'contract'),
            protocol=test_data.protocol,
        )
        pdf_file_name = u'Допсоглашение %s' % collateral.num
        pdf_content = 'Pdf content'
        test_data.attach_list = [generator.Attachment('%s.pdf' % pdf_file_name, 'application/pdf', pdf_content)]

        class MockWikiHandler(object):
            def __init__(self, session, object_id, object_type):
                assert object_id == contract.id
                assert object_type == 'contract'
            def render_plain_text(self, text):
                return text
            def pdf_binary_content(self):
                return pdf_content

        with mock.patch('balance.publisher.wiki_handler.WikiHandler', MockWikiHandler):
            self._check(test_data, session)


@pytest.mark.email
class TestEmailUtil(object):

    @pytest.mark.parametrize(
        'email, answer',
        [
            (u'admin@соседи.рус', True),
            (u'привет@привет.рф', False),
            (u'alena-titova@yandex-team.ru', True),
            (u'sale6@полиадапро.рф', True),
            (u'ФаФа-оо_оо@собаКа-рф.рф.ку.КУ__ку', False),
            (u'aa888AAaa@aa88FF.рф', True),
            (u'+90@yand.ru', False),
            (u'test/test@ya.com', False),
        ],
    )
    def test_check_email(self, email, answer):
        assert bool(util.check_email(email)) is answer
