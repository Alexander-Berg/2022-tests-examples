# -*- coding: utf-8 -*-

"report_test_ticket_report"

from __future__ import unicode_literals


from rep.core import email_reports
from rep.utils.dateutils import get_last_day_prev_month

query = """
select 'you are the best' from dual
"""


class TestTicketReport(email_reports.JinjaTemplateMixin, email_reports.SimpleQueriesZippedXLSXTicketReport):
    database_id = 'balance_ro'

    __mapper_args__ = {'polymorphic_identity': 'test_ticket_report'}

    _assignee = 'barsukovpt'

    _sqls = [query]

    _file_names = ['You ve got mail']

    _summary_template = 'Тестовый тикет для TicketReport {{ date }}'

    _description = '''
Добрый день!
Выгрузка во вложении.

С уважением,
Группа сопровождения биллинговой системы'''.strip()

    def _additional_parameters(self, session, mnclose_task):
        return {'date': get_last_day_prev_month().strftime('%m.%Y')}
