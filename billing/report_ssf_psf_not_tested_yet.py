# -*- coding: utf-8 -*-

"""
MNCLOSE-1130
"""
from __future__ import unicode_literals

from rep.core import email_reports
from rep.utils.dateutils import get_last_day_prev_month

query = """
SELECT DISTINCT hou.NAME "Операционная единица",
  ap.INVOICE_TYPE_LOOKUP_CODE "Тип сч-ф",
  FDSC.NAME "Категория",
  PH.SEGMENT1 "№ ЗП",
  p.vendor_name "Контрагент",
  p.SEGMENT1 "№ поставщика", 
  pvs.VENDOR_SITE_CODE "Отделение",
  ap.INVOICE_DATE "Дата счета-фактуры",
  ap.INVOICE_NUM "№ счета-фактуры ",
  ap.INVOICE_CURRENCY_CODE "Валюта счета",
  ap.INVOICE_AMOUNT "Сумма по сч.-фактуре",
  ap.GL_DATE "Дата ГК",
  us.user_name "Пользователь,что завел сч-ф",
  decode (AP_INVOICES_PKG.GET_APPROVAL_STATUS (AP.INVOICE_ID,
   AP.INVOICE_AMOUNT, AP.PAYMENT_STATUS_FLAG,
   AP.INVOICE_TYPE_LOOKUP_CODE), 
   'NEVER APPROVED', 'Ранее не проверялось',
   'NEEDS REAPPROVAL', 'Требуется повторная проверка')
FROM apps.AP_INVOICES_all ap,
  apps.GL_CODE_COMBINATIONS gcc,
  apps.fnd_user us,
  apps.hr_operating_units hou,
  apps.FND_DOC_SEQUENCE_CATEGORIES FDSC,
  apps.PO_HEADERS_all PH,
  apps.po_vendors p, 
  apps.po_vendor_sites_all pvs
WHERE 1=1
and ap.VENDOR_SITE_ID = pvs.VENDOR_SITE_ID
and p.VENDOR_ID = pvs.VENDOR_ID
AND ap.VENDOR_ID = p.VENDOR_ID
AND ap.PAYMENT_STATUS_FLAG IN ('N','P')
AND ap.ACCTS_PAY_CODE_COMBINATION_ID = gcc.CODE_COMBINATION_ID
AND gcc.SEGMENT1 IN ('IL10','NL10','US10','NL90','NL20','NL31','AM31','NL32','CH10','FI10','DE10','CH20')
AND ap.CANCELLED_DATE      IS NULL
AND ap.INVOICE_AMOUNT      !=0
AND ap.GL_DATE >= date '2012-01-01' and ap.GL_DATE < trunc(sysdate, 'MM')
AND ap.CREATED_BY                = us.user_id
AND hou.ORGANIZATION_ID          = ap.ORG_ID
AND AP_INVOICES_PKG.GET_APPROVAL_STATUS (AP.INVOICE_ID,
    AP.INVOICE_AMOUNT, AP.PAYMENT_STATUS_FLAG,
    AP.INVOICE_TYPE_LOOKUP_CODE) not in  ('APPROVED','CANCELLED','UNPAID','AVAILABLE','FULL','PERMANENT','UNAPPROVED') -- Статус = "Проверено","Отменено",'Не оплачено','Доступно','Полностью учтено','Постоянная предоплата','Не проверено'
AND ap.DOC_CATEGORY_CODE         = FDSC.CODE
AND ap.QUICK_PO_HEADER_ID        = PH.PO_HEADER_ID (+)
Order by 9
"""


class SsfPspNotTestedYetReport(email_reports.JinjaTemplateMixin, email_reports.SimpleQueriesXLSReport):
    database_id = 'oebs'
    __mapper_args__ = {'polymorphic_identity': 'ssf_psf_not_tested_yet'}

    _sqls = [query]

    _subject_template = 'Регулярный запрос на выгрузку перечня ССФ и ПСФ со статусами «На перепроверку» и «Ранее не проверялось» на {{ date }}'
    _body = '''
Добрый день!
Выгрузка во вложении.

С уважением,
Группа сопровождения биллинговой системы'''.strip()

    def _additional_parameters(self, session, mnclose_task):
        prev_month = get_last_day_prev_month()
        return {
            'date': prev_month.strftime('%m.%Y'),
            }

