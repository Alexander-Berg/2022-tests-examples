# coding: utf-8

__author__ = 'a-vasin'

from datetime import datetime

from dateutil import parser
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
import btestlib.utils as utils

LIFETIME = relativedelta(weeks=2)


def test_remove_old_failed_reports():
    with reporter.step(u"Удаляем все отчеты об упавших тестах, которые старше, чем: {}".format(LIFETIME)):
        keys = get_report_keys()

        reporter.attach(u"Общее количество отчетов", utils.Presenter.pretty(len(keys)))

        to_delete_keys = [key for key in keys
                          if parser.parse(key.last_modified).replace(tzinfo=None) + LIFETIME < datetime.now()]

        for key in to_delete_keys:
            key.delete()

        reporter.attach(u"Количество удаленных отчетов", utils.Presenter.pretty(len(to_delete_keys)))


def get_report_keys():
    keys = utils.s3storage().bucket.get_all_keys()

    return [key for key in keys if key.name.startswith(u'failed_')]
