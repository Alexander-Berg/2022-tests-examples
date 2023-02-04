# coding: utf-8

import datetime

from telepot import Bot

from btestlib import reporter
from btestlib import utils
from btestlib.secrets import Telegram, get_secret

TESTBALANCE_BOT_TOKEN = str(get_secret(*Telegram.TESTBALANCE_BOT_TOKEN))
OEBS_CHAT_ID = -1001136369947


def remind_oebs_to_open_next_month():
    with reporter.reporting(level=reporter.Level.NOTHING):
        current_month_last_working_day = utils.Date.current_month_last_working_day()
    reporter.log('current month last working day = {}'.format(current_month_last_working_day))

    today = datetime.date.today()
    reporter.log('today = {}'.format(today))

    if current_month_last_working_day == today:
        msg = u'Привет! Сегодня последний рабочий день месяца!\nОткройте, пожалуйста, следующий месяц в Дебиторах.'
        bot = Bot(TESTBALANCE_BOT_TOKEN)
        bot.sendMessage(OEBS_CHAT_ID, msg)


if __name__ == "__main__":
    remind_oebs_to_open_next_month()
