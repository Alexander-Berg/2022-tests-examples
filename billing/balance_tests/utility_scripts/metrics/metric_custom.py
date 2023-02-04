# -*- coding: utf-8 -*-
__author__ = 'torvald'

import datetime
import json

from mutils import Steps, Utils

NOW_FORMAT = '%Y-%m-%d %H:%M:%S'
TODAY_FORMAT = '%Y-%m-%d'

date_converter = {'now()': lambda: datetime.datetime.now().strftime(NOW_FORMAT),
                  'today()': lambda: datetime.date.today().strftime(TODAY_FORMAT)}


def do(value_json, source):

    # Для отправки статистики за сегодня добавляем поддержку строки "now()" или now()
    for str in date_converter.keys():
        if str in value_json:
            dt = date_converter[str]()
            value_json = value_json.replace(str, dt)

    value_json = json.loads(value_json)

    # Source переданный явно переопределит значение из value_json
    if source:
        value_json['source'] = source

    for point in value_json['data']:

        time = None
        for format in [NOW_FORMAT, TODAY_FORMAT]:
            try:
                time = datetime.datetime.strptime(point['time'], format)
            except ValueError:
                pass

        if not time:
            raise Exception

        time = int(Utils.to_timestamp(time))
        for metric in point['metrics']:
            Steps.sender(value_json['source'], metric['name'], metric['metric'], time)


if __name__ == "__main__":
    example = {"data": [{"time": "2015-08-01", "metrics": [{"name": "sut", "metric": 2}, {"name": "noise", "metric": 3}, {"name": "auto", "metric": 4}]}], "source": "balance_qa_metrics_autostatus_manual"}
    example2 = '{"data": [{"time": "now()", "metrics": [{"name": "sut", "metric": 2}, {"name": "noise", "metric": 3}, {"name": "auto", "metric": 4}]}], "source": "balance_qa_metrics_autostatus_manual"}'

    # import sys
    # argv = sys.argv
    # argv.append('--source')
    # argv.append('balance_qa_metrics_autostatus_manual')
    # argv.append('--name')
    # argv.append('test')
    # argv.append('--metric')
    # argv.append('34')
    # argv.append('--time')
    # argv.append('2015-08-01T00:00:00')
    #
    # parser = argparse.ArgumentParser(description='Custom BALANCE metric')
    # parser.add_argument('--source', default='balance_qa_metrics_autostatus_manual', help='source')
    # parser.add_argument('--name', help='name')
    # parser.add_argument('--metric', type=int, help='integer value')
    # parser.add_argument('--time', help='time')
    # args = parser.parse_args()

    do(example2, 'blablabla')
    pass
