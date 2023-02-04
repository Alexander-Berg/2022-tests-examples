# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import sys
import argparse

import sqlalchemy as sa
import jinja2

from rep.application import Application

from rep import settings
from rep.utils.reports_import import import_all, import_all_db
from rep.utils import mail
from rep.db import mapper as rep_mapper
from rep.core.config import Config
from butils import logger

log = logger.get_logger()


report_template = '''
{% for name, queries in info %}
Выгрузка {{ name }}:
{% for q_name, is_ok, exc in queries %}
Запрос {{ q_name }}:
{%- if is_ok %} ОК
{% else %} ошибка {{ exc }}
{% endif %}
{% endfor %}

{% endfor %}'''

msg_title = 'Невыполненные запросы регулярных выгрузок'


def test_execution(session, sql):
    rep_mapper.execute_sql(session, sql)


class QueriesTestApp(Application):
    database_id = 'balance_ro'
    name = b'reports_test_queries'

    def main(self, reports=None, emails=None, report_successful=False):
        rep_session = self.new_session(database_id=self.database_id)

        db_reports = rep_session.query(rep_mapper.BaseReportSource). \
            filter(~rep_mapper.BaseReportSource.source.is_(None)). \
            with_entities(rep_mapper.BaseReportSource.id)
        db_reports = [r for r, in db_reports if not reports or r in reports]
        py_reports = rep_session.query(rep_mapper.BaseReportSource). \
            filter(rep_mapper.BaseReportSource.source.is_(None)). \
            with_entities(rep_mapper.BaseReportSource.id)
        py_reports = [r for r, in py_reports if not reports or r in reports]
        if db_reports:
            import_all_db(rep_session, db_reports)
        if py_reports:
            import_all(py_reports)
        #if Config(rep_session).get('LOAD_FROM_DB', False):
        #    import_all_db(rep_session, reports or [])
        #else:
        #    import_all(reports or [])

        jinja_env = jinja2.Environment(trim_blocks=True, lstrip_blocks=True)
        template = jinja_env.from_string(report_template)


        reports_classes = rep_session.query(rep_mapper.BaseEmailReport).filter(rep_mapper.BaseEmailReport.hidden == 0)
        if reports:
            reports_classes = reports_classes.filter(rep_mapper.BaseEmailReport.id.in_(reports))
        reports_classes = reports_classes.all()

        log_info = []
        for i, report_cls in enumerate(reports_classes):
            log.info('Processing report %s/%s - %s' % (i + 1, len(reports_classes), report_cls.id))

            info_provider = getattr(report_cls, 'info_provider', None)
            if not info_provider:
                continue
            queries = info_provider.get_queries_w_names()

            session = self.new_session(database_id=report_cls.database_id)
            queries_res = []
            for q_name, query in queries:
                try:
                    test_execution(session, query)
                except sa.exc.SQLAlchemyError:
                    log.exception('Sql exception:')
                    error = unicode(sys.exc_info()[1])
                    queries_res.append((q_name, False, error.split('\n')[0]))
                else:
                    if report_successful:
                        queries_res.append((q_name, True, ''))
            session.close()

            if queries_res:
                log_info.append((report_cls.id, queries_res))

        if emails:
            sender = mail.Sender()
            sender.really_send(msg_title, template.render(info=log_info), rcpt=emails, bcc_emails=[])
        else:
            log.info(template.render(info=log_info))


def main(pycron_args=None):
    app = QueriesTestApp(settings.app_cfg_path)

    parser = argparse.ArgumentParser()
    parser.add_argument('reports', nargs='*')
    parser.add_argument('-e', '--emails', nargs='*')
    parser.add_argument('-s', '--report-successful', action='store_const', const=True)

    args = parser.parse_args(pycron_args)
    app.main(args.reports, args.emails, args.report_successful)


if __name__ == '__main__':
    main()
