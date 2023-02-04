import time
import logging

from billing.tools.telebot.telebot.bots import base

log = logging.getLogger('bots.test')


class Test(base.Bot):
    HELP = {'Тестируем АПИ2.0': 'посчитай'}

    @base.match_keywords(keys=['посчитай'])
    def process(self, msg):
        if msg['text_lower'] != 'посчитай':
            return

        res = self.telegram.send_message(msg, u"Итак: base")

        log.debug("Test: " + repr(res))

        for i in range(0, 10):
            time.sleep(1)
            self.telegram.update_message(msg, res, u"Итак: {}".format(i))

        return
