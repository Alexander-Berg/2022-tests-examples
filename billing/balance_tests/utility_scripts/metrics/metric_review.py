# -*- coding: utf-8 -*-

import datetime

import processor_review
from mutils import Steps, Utils

SOURCE = 'balance_qa_review'
TPL = 'one_hour.%(sender_host)s.%(source)s.%(name)s %(metric)s %(time)s'

def do():
    NOW = int(Utils.to_timestamp(datetime.datetime.now().replace(minute=0, second=0, microsecond=0)))
    review_data = processor_review.get_metrics()
    for queue in review_data:
        Steps.sender(SOURCE, queue, review_data[queue], NOW, tpl=TPL)

if __name__ == "__main__":
    do()
    pass