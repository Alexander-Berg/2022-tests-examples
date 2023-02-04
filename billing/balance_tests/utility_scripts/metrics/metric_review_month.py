# -*- coding: utf-8 -*-

import processor_review
from btestlib.utils import Date
from mutils import Steps, Utils

SOURCE = 'balance_qa_review_month'
TPL = 'one_day.%(sender_host)s.%(source)s.%(name)s %(metric)s %(time)s'


def do():
    NOW = int(Utils.to_timestamp(Date.get_last_day_of_previous_month()))
    review_data = processor_review.get_month_metrics()
    for queue in review_data:
        Steps.sender(SOURCE, queue, review_data[queue], NOW, tpl=TPL)


if __name__ == "__main__":
    do()
    pass
