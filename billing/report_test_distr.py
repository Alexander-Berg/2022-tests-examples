# -*- coding: utf-8 -*-

from __future__ import unicode_literals

from datetime import datetime

from rep.core import email_reports
from rep.utils.dateutils import get_last_day_prev_month

query = '''
with contracts as (
    select
      c.id contract_id,
      c.EXTERNAL_ID contract,
      c.CLIENT_ID,
      cur.CHAR_CODE currency,
      f.title firm,
      ca_type.VALUE_NUM contract_type,
      case
        when ca_type.VALUE_NUM in (1, 2) then ca_type.VALUE_NUM
        when ca_type.VALUE_NUM in (3, 4) then ca_supp.KEY_NUM
        else -1
      end activity_id
    from bo.t_contract2 c
    join bo.t_contract_collateral cc0
        on cc0.contract2_id = c.id
        and cc0.collateral_type_id is null
    join bo.t_contract_attributes ca_tm
        on ca_tm.collateral_id = cc0.id
        and ca_tm.code = 'TEST_MODE'
    left join bo.T_CONTRACT_ATTRIBUTES ca_f
        on cc0.id = ca_f.COLLATERAL_ID
        and ca_f.code = 'FIRM'
    left join bo.T_FIRM f on ca_f.VALUE_NUM = f.id
    left join bo.MV_CONTRACT_LAST_ATTR ca_cur
        on ca_cur.CONTRACT_ID = c.id
        and ca_cur.code = 'CURRENCY'
    left join bo.T_CURRENCY cur
        on ca_cur.VALUE_NUM = cur.NUM_CODE
    join bo.MV_CONTRACT_LAST_ATTR ca_type
        on ca_type.CONTRACT_ID = c.id
        and ca_type.code = 'CONTRACT_TYPE'
    left join bo.MV_CONTRACT_LAST_ATTR ca_supp
        on ca_supp.CONTRACT_ID = c.id
        and ca_supp.code = 'SUPPLEMENTS'
        and ca_supp.VALUE_NUM = 1
    where c.type = 'DISTRIBUTION'
        and ca_tm.value_num = 1
        and cc0.IS_CANCELLED is null
        and ca_type.VALUE_NUM != 5
),
rewards as (
  SELECT
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.CLIENT_ID,
    c.activity_id,
    round(sum(partner_reward_wo_nds), 2) reward
  from contracts c
  join bo.V_DISTR_REVSHARE_COMPLETION r
      on c.contract_id = r.CONTRACT_ID
  where c.activity_id = 1
      and dt >= add_months(trunc(sysdate, 'mm'), -13)
      and dt < trunc(sysdate, 'mm')
      and internal_type >= 100
  GROUP BY
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.activity_id,
    c.CLIENT_ID

  union ALL

  SELECT
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.CLIENT_ID,
    c.activity_id,
    round(sum(partner_reward_wo_nds), 2) reward
  from contracts c
  join bo.v_distr_fixed_completion r
      on c.contract_id = r.CONTRACT_ID
  where c.activity_id = 2
      and dt >= add_months(trunc(sysdate, 'mm'), -13)
      and dt < trunc(sysdate, 'mm')
      and internal_type >= 100
  GROUP BY
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.activity_id,
    c.CLIENT_ID

  union all

  SELECT
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.CLIENT_ID,
    c.activity_id,
    round(sum(partner_reward_wo_nds), 2) reward
  from contracts c
  join bo.v_distr_serphits r
      on c.contract_id = r.CONTRACT_ID
  where c.activity_id = 3
      and dt >= add_months(trunc(sysdate, 'mm'), -13)
      and dt < trunc(sysdate, 'mm')
      and internal_type >= 100
  GROUP BY
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.activity_id,
    c.CLIENT_ID

  union ALL

  SELECT
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.CLIENT_ID,
    c.activity_id,
    round(sum(money_wo_nds), 2) reward
  from contracts c
  join bo.v_partner_addapter_distr r
      on c.contract_id = r.CONTRACT_ID
  where c.activity_id = 4
      and dt >= add_months(trunc(sysdate, 'mm'), -13)
      and dt < trunc(sysdate, 'mm')
      and PAGE_ID = 4009
  GROUP BY
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.activity_id,
    c.CLIENT_ID

  union ALL

  SELECT
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.CLIENT_ID,
    c.activity_id,
    round(sum(money_wo_nds), 2) reward
  from contracts c
  join bo.v_partner_addapter_distr r
      on c.contract_id = r.CONTRACT_ID
  where c.activity_id = 5
      and dt >= add_months(trunc(sysdate, 'mm'), -13)
      and dt < trunc(sysdate, 'mm')
      and PAGE_ID = 4011
  GROUP BY
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.activity_id,
    c.CLIENT_ID

  union ALL

  SELECT
    c.contract_id,
    c.contract,
    c.firm,
    c.CURRENCY,
    c.CLIENT_ID,
    c.activity_id,
    null reward
  from contracts c
  where c.activity_id not in (1, 2, 3, 4, 5)
)
SELECT DISTINCT
  c.firm "Фирма",
  'Дистрибуция' "Вид договора",
  nvl(c.contract, c.contract_id) "Номер договора",
  c.CLIENT_ID "ID клиента",
  first_value(ps.LOGIN) over (PARTITION BY ps.CLIENT_ID ORDER BY ps.IS_MAIN desc) "Логин",
  c.currency "Валюта",
  'Да' "Тестовый режим",
  et.VALUE "Приложения",
  c.reward "Вознаграждение"
from rewards c
left join bo.T_ENUMS_TREE et
  on et.PARENT_ID = 900
  and et.CODE = c.activity_id
left join bo.T_PASSPORT ps on c.CLIENT_ID = ps.CLIENT_ID'''


class TestDistrReport(email_reports.JinjaTemplateMixin, email_reports.SimpleQueriesXLSReport):
    database_id = 'balance_ro'
    __mapper_args__ = {'polymorphic_identity': 'test_distr'}

    _sqls = [query]

    _subject_template = 'Тестовые договоры по дистрибуции с {{ from }} по {{ to }}'
    _body = 'Тестовые договоры по дистрибуции'

    def _additional_parameters(self, session, mnclose_task):
        last_day_prev_month = get_last_day_prev_month()
        return {
            'from': datetime(last_day_prev_month.year - 1, last_day_prev_month.month, 1).strftime('%d.%m.%Y'),
            'to': last_day_prev_month.strftime('%d.%m.%Y')
        }
