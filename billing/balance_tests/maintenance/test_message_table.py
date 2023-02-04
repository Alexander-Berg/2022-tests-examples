# coding: utf-8
__author__ = 'sandyk'
import balance.balance_db as db
import btestlib.reporter as reporter
from btestlib import utils


# будем проверять отсутствие неотправленных сообщений в t_message
# за период - (сейчас - сутки, сейчас - 15 мин)
# 15 мин - даем на разгребание очереди
# не рассматриваем сообщения с opcode 15,16 - это сообщения trust'a
# и там в каких-то случаях отправка и не должна происходить
# на rate специально не смотрим, чтобы отловить случай когда будут зависать с rate=0


email_query = """
select error as error
from bo.t_export ex
         join bo.t_message m on m.id = ex.object_id
where 1 = 1
  and ex.state != 1
  and ex.classname = 'EmailMessage'
  AND m.opcode NOT IN (7, 15, 16)
  AND m.dt BETWEEN (sysdate - 1) AND (sysdate - 1 / 96)
  AND (m.SEND_DT IS NULL OR m.SEND_DT < sysdate)
"""


def check_mesages(message_count):
    # если все упавшие сообщения с opcode =1,3 и их не много,
    # то считаем что это проявление известной ошибки BALANCE-20009
    known_error_BALANCE_20009 = db.balance().execute('''SELECT count(*) AS count FROM t_message
                                                WHERE sent = 0 AND opcode IN (1, 3) AND dt
                                                BETWEEN (sysdate - 1) AND (sysdate - 1 / 96)''')[0]['count']
    return (message_count == known_error_BALANCE_20009) and (message_count < 30)


def check_spam(messages):
    spam_msg = 'Message rejected under suspicion of SPAM'
    return all(spam_msg in m for m in messages)


def test_message_table():
    # 7 нужно убрать когда починят BALANCE-18842
    messages = [x['error'] for x in db.balance().execute(email_query)]
    message_count = len(messages)

    with reporter.step(u"Проверяем количество неотправленных сообщений в t_message"):
        if message_count > 0:
            if check_mesages(message_count):
                with reporter.step(
                        u'В t_message {message_count} неотправленных сообщений из-за известной ошибки ' \
                        u'BALANCE-20009'.format(message_count=message_count)):
                    pass
            if check_spam(messages):
                with reporter.step(
                        u'В t_message {message_count} неотправленных сообщений из-за ' \
                        u'спам блокировок'.format(message_count=message_count)):
                    pass
            else:
                raise utils.ServiceError(
                    u'В t_message {message_count} неотправленных сообщений'.format(message_count=message_count))
        else:
            with reporter.step(u'В t_message нет неотправленных сообщений за рассматриваемый период'):
                pass
