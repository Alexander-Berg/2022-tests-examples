from datetime import date
from textwrap import dedent

import luigi
import pytest
import cx_Oracle as cxO
import sqlalchemy as sqla

from dwh.grocery.targets import (
    ColumnMiningStrategy,
    DBViewTarget,
    OracleTableTarget,
    YTTableTarget,
)
from dwh.grocery.targets.db_targets.base import (
    BUNKER,
    ColumnMeta,
    ColumnHints,
)
from dwh.grocery.task.yt_export_task import YTFullExportTaskV2
from dwh.grocery.tools.parse_sql import (
    find_all_columns,
    find_all_tables,
)

from . import is_eq

dt = date(2019, 2, 1)
dt2 = date(2019, 3, 1)
awaps_dsp_ids = ", ".join(str(x) for x in [
    2,
    3,
    2522394,
    2563057,
    2563070,
    2563084,
    2563092,
    2563102,
    2563103,
    2563104,
    2563117,
    2563118,
    2563130,
    2563133,
    2563144,
    2563145,
    2563146,
    2563147,
    2563149,
    2563153,
    2563154,
    2563155,
    2563156,
    2563172,
    2563180,
    2563201,
    2563204,
    2563229,
    2563232,
])
CHMELEV = f"""
    select ps.place_id page_id,
        p.url,
        ps.dsp_id,
        ps.block_id,
        sum(ps.dsp_charge) dsp_charge,
        sum(ps.partner_reward) partner_reward,
        sum(ps.shows) showsiki
    from bo.t_partner_dsp_stat ps
    left join bo.t_place p
    on ps.place_id = p.id
    where ps.dt >= date'{dt}' and ps.dt < date'{dt2}' and ps.dsp_id in ({awaps_dsp_ids})
    --and (dsp_charge > 0 or partner_reward > 0 or shows > 0)
    group by ps.place_id, p.url, ps.dsp_id, ps.block_id
"""

CONTRACTS = f"""
select distinct pd.place_id pageid,
                (first_value(c.external_id) over(order by pd.place_id, c.id desc)) contract_eid
from bo.t_partner_act_data pd
join bs.t_contract2 c
on pd.partner_contract_id = c.id
where dt >= date'{dt}' and dt < date'{dt2}' and dsp_id in ({awaps_dsp_ids})
"""

SQL = """
    select arr.contract_id, arr.dkv_to_charge dkv, caf.discount_findt discdt
    from bo.v_ar_rewards arr join bo.v_contract_apex_full caf
    on arr.contract_id = caf.contract_id
    where arr.contract_id = {}
"""

HARD_SQL1 = """
with tmp as (
    select billing_line_id
    from apps.xxap_agent_reward_data@oebs_ro.yandex.ru ar
    join apps.xxap_cpa_paysystem_types_all@oebs_ro.yandex.ru cp
        on cp.paysys_type_id = ar.paysys_type_id
    where 1=1
        and period_end_date in (last_day(add_months(trunc(sysdate), -1)),
                                last_day(add_months(trunc(sysdate), -2)),
                                last_day(add_months(trunc(sysdate), -3)))
        and cp.org_id = 115098
        and cp.paysys_type_code not in ('YA_NETTING', 'CORRECTION_NETTING', 'YA_PROMOCODES')
        and ar.country_code = 'BY'
        and ar.org_id = 115098
),
tt as (
    select
        tt.contract_id,
        nvl(tt.service_order_id_str, o.service_order_id_str) service_order,
        sum(case when tt.total_sum < 0 then -1 * e.value_num else e.value_num end) sum_byn,
        tt.currency
    from tmp byn_tt
        join bo.t_thirdparty_transactions tt
            on byn_tt.billing_line_id = tt.id
        left join bo.t_extprops e
            on e.object_id = tt.id
            and e.classname = 'ThirdPartyTransaction'
            and e.attrname = 'reference_amount'
        left join bo.t_order o
            on tt.order_id = o.id
    group by
        tt.contract_id,
        tt.service_order_id_str,
        o.service_order_id_str,
        tt.currency
)
select /*+  parallel(10) */
    c.id contract_id,
    c.external_id contract,
    cl.id client_id,
    cl.name client,
    p.id person_id,
    pname.value_str person_name,
    ep.value_str local_name,
    pinn.value_str ynp,
    tt.service_order,
    tt.sum_byn,
    tt.currency
from tt
join bo.t_contract2 c
    on tt.contract_id = c.id
join bo.t_client cl
    on cl.id = c.client_id
join bo.t_person p
    on p.id = c.person_id
join bo.t_attribute_values pname
    on p.attribute_batch_id = pname.attribute_batch_id and pname.code = 'NAME'
join bo.t_attribute_values pinn
    on p.attribute_batch_id = pinn.attribute_batch_id and pinn.code = 'INN'
join bo.t_extprops ep
    on ep.object_id = p.id
    and ep.classname = 'Person'
    and ep.attrname = 'local_name'
"""

HARD_SQL2 = """
with contracts as (
    select
      c.id,
      c.external_id,
      c.client_id,
      cl.name                                   client,
      to_char(finish_dt.value_dt, 'YYYY-mm-dd') finish_dt,
      listagg(s.name, ', ')
      within group (
        order by s.id)                          services
    from bo.t_contract2 c
      join bo.t_client cl on c.client_id = cl.id
      join bo.t_contract_collateral col0 on col0.contract2_id = c.id
                                            and col0.collateral_type_id is null
      join bo.t_contract_attributes firm_ca on firm_ca.collateral_id = col0.id
                                               and firm_ca.code = upper('firm')
      join bo.t_firm f on firm_ca.value_num = f.id
      join bo.mv_contract_signed_attr payment_type
        on payment_type.contract_id = c.id
           and payment_type.code = upper('payment_type')
      left join bo.mv_contract_signed_attr finish_dt
        on finish_dt.contract_id = c.id
           and finish_dt.code = upper('finish_dt')
      left join bo.mv_contract_signed_attr ca_serv
        on ca_serv.contract_id = c.id
           and ca_serv.code = 'SERVICES'
           and ca_serv.value_num is not null
      left join bo.t_service s on ca_serv.key_num = s.id
    where 1 = 1
          and cl.is_agency = 1
          and firm_ca.value_num = 111
          and (finish_dt.value_dt is null or finish_dt.value_dt >= trunc(sysdate, 'MM'))
          and payment_type.value_num = 3
    group by
      c.id,
      c.external_id,
      c.client_id,
      cl.name,
      finish_dt.value_dt
),
    contract_attributes as (
      select distinct
        /*+materialize*/
        c.id contract_id,
        ca.code,
        ca.value_num,
        ca.value_str
      from contracts c
        join bo.mv_contract_signed_attr ca
          on ca.contract_id = c.id
      where ca.code in ('CREDIT_TYPE', 'PAYMENT_TERM', 'PAYMENT_TERM_MAX', 'CREDIT_LIMIT_SINGLE')
  ),
    contract_data as (
      select
        /*+materialize*/
        c.id                          contract_id,
        c.external_id                 contract_eid,
        c.client_id                   agency_id,
        c.client                      agency,
        c.services,
        ca_credit_type.value_num      credit_type,
        ca_payment_term.value_num     payment_term,
        ca_payment_term_max.value_num payment_term_max,
        ca_credit_limit.value_num     credit_limit
      from contracts c
        left join contract_attributes ca_credit_type
          on ca_credit_type.contract_id = c.id
             and ca_credit_type.code = 'CREDIT_TYPE'
        left join contract_attributes ca_payment_term
          on ca_payment_term.contract_id = c.id
             and ca_payment_term.code = 'PAYMENT_TERM'
        left join contract_attributes ca_payment_term_max
          on ca_payment_term_max.contract_id = c.id
             and ca_payment_term_max.code = 'PAYMENT_TERM_MAX'
        left join contract_attributes ca_credit_limit
          on ca_credit_limit.contract_id = c.id
             and ca_credit_limit.code = 'CREDIT_LIMIT_SINGLE'
  ),
    clients_credit_data as (
      select
        /*+materialize*/
        c.id                                                                     contract_id,
        clients_limits.key_num                                                   client_id,
        json_value(clients_limits.value_str, '$.client_limit_currency')          client_limit_currency,
        to_number(json_value(clients_limits.value_str, '$.client_payment_term')) client_payment_term,
        to_number(json_value(clients_limits.value_str, '$.client_limit'))        client_limit,
        to_number(json_value(clients_limits.value_str, '$.client_credit_type'))  client_credit_type_num
      from contracts c
        join bo.mv_contract_signed_attr clients_limits
          on clients_limits.contract_id = c.id
             and clients_limits.code = 'CLIENT_LIMITS'
  ),
    clients_brands_all as (
      select
        /*+materialize*/
        c.contract_id,
        c.client_id,
        nvl(b.brand_client_id, c.client_id) brand_client_id
      from clients_credit_data c
        left join bo.mv_client_direct_brand b
          on b.dt = trunc(sysdate)
             and b.client_id = c.client_id
  ),
    clients_brands as (
      select
        /*+materialize*/
        cba.contract_id,
        cba.client_id,
        cba.brand_client_id,
        min(ps.login)
        keep (dense_rank first
          order by ps.is_main desc nulls last) login
      from clients_brands_all cba
        left join bo.t_passport ps on ps.client_id = cba.client_id
      group by cba.contract_id, cba.client_id, cba.brand_client_id
  ),
    clients_pas as (
      select
        cl.contract_id,
        cl.client_id,
        cl.login,
        i.id fict_pa_id
      from clients_brands cl
        join bo.t_extprops ep
          on ep.classname = 'PersonalAccount'
             and ep.attrname = 'subclient_id'
             and ep.value_num = cl.brand_client_id
        join bo.t_invoice i on ep.object_id = i.id
      where i.contract_id = cl.contract_id
  ),
    turnover_fpa as (
      select
        /*+materialize*/ *
      from (
        select
          cl.contract_id,
          cl.client_id,
          cl.login,
          cl.fict_pa_id,
          trunc(a.dt, 'MM') month,
          i.currency,
          sum(a.amount)     amount
        from clients_pas cl
          join bo.t_invoice fpa on cl.fict_pa_id = fpa.id
          join bo.t_invoice_repayment ir on fpa.id = ir.invoice_id
          join bo.t_invoice i on ir.repayment_invoice_id = i.id
          join bo.t_act a on i.id = a.invoice_id
        where 1 = 1
              and a.dt >= add_months(trunc(sysdate, 'MM'), -1)
              and a.dt < trunc(sysdate, 'MM')
        group by cl.client_id, cl.contract_id, cl.login, cl.fict_pa_id, trunc(a.dt, 'MM'), i.currency)
  ),
    turnover_total as (
      select
        /*+materialize*/ *
      from (
        select
          cl.contract_id,
          cl.client_id,
          cl.login,
          trunc(a.dt, 'MM') month,
          i.currency,
          sum(at.amount)    amount
        from clients_brands cl
          join bo.t_invoice i on i.contract_id = cl.contract_id
          join bo.t_act a on i.id = a.invoice_id
          join bo.t_act_trans at on at.act_id = a.id
          join bo.t_consume q on q.id = at.consume_id
          join bo.t_order o on o.id = q.parent_order_id
        where 1 = 1
              and cl.brand_client_id = o.client_id
              and a.dt >= add_months(trunc(sysdate, 'MM'), -1)
              and a.dt < trunc(sysdate, 'MM')
        group by cl.client_id, cl.contract_id, cl.login, trunc(a.dt, 'MM'), i.currency)
  ),
    united_turnover as (
      select
        to_char(nvl(tt.month, tf.month), 'YYYY-mm') month,
        nvl(tt.client_id, tf.client_id)             client_id,
        nvl(tt.login, tf.login)                     login,
        nvl(tt.currency, tf.currency)               currency,
        sum(tt.amount)                              amount_total,
        sum(tf.amount)                              amount_ind
      from turnover_total tt
        full outer join turnover_fpa tf on tt.contract_id = tf.contract_id
                                           and tt.client_id = tf.client_id
                                           and tt.month = tf.month
                                           and tt.currency = tf.currency
      group by
        to_char(nvl(tt.month, tf.month), 'YYYY-mm'),
        nvl(tt.client_id, tf.client_id),
        nvl(tt.login, tf.login),
        nvl(tt.currency, tf.currency)
  )
select
  cl.client_id,
  cli.name                                    client,
  ut.login,
  c.agency_id,
  c.agency,
  nvl(cl.client_payment_term, c.payment_term) client_payment_term,
  case
  when nvl(cl.client_credit_type_num, c.credit_type) = 1
    then round((30 + nvl(cl.client_payment_term, c.payment_term)) / 30, 2)
  when nvl(cl.client_credit_type_num, c.credit_type) = 2
    then 1
  else
    null
  end as                                      payment_term_coef,
  c.payment_term_max,
  case
  when nvl(cl.client_credit_type_num, c.credit_type) = 1
    then round(nvl(cl.client_limit, c.credit_limit) * (30 + nvl(cl.client_payment_term, c.payment_term)) / 30, 2)
  when nvl(cl.client_credit_type_num, c.credit_type) = 2
    then nvl(cl.client_limit, c.credit_limit)
  else
    null
  end as                                      credit_limit,
  ut.month,
  ut.currency,
  ut.amount_ind,
  ut.amount_total
from contract_data c
  join clients_credit_data cl on cl.contract_id = c.contract_id
  join bo.t_client cli on cli.id = cl.client_id
  left join united_turnover ut on ut.client_id = cl.client_id
order by c.agency_id, cli.id, ut.month, ut.currency
"""

HARD_SQL3 = """
with debts as (
  select
      ca_manager.value_num manager_code,
      cl.id client_id,
      cl.name client,
      p.id person_id,
      pname.value_str person,
      c.external_id contract,
      i.id invoice_id,
      i.external_id invoice,
      i.receipt_sum_1c,
      m.name manager,
      country.region_name country,
      kladr.formal_name region,
      sum(case when ceil(sysdate - nvl(a.payment_term_dt, a.dt)) <= 0 then at.amount - at.paid_amount else 0 end) debt00,
      sum(case when ceil(sysdate - nvl(a.payment_term_dt, a.dt)) between 1 and 30 then at.amount - at.paid_amount else 0 end) debt130,
      sum(case when ceil(sysdate - nvl(a.payment_term_dt, a.dt)) between 31 and 60 then at.amount - at.paid_amount else 0 end) debt3060,
      sum(case when ceil(sysdate - nvl(a.payment_term_dt, a.dt)) between 61 and 90 then at.amount - at.paid_amount else 0 end) debt6090,
      sum(case when ceil(sysdate - nvl(a.payment_term_dt, a.dt)) >= 91 then at.amount - at.paid_amount else 0 end) debtover90,
      sum(at.amount) act_sum,
      sum(at.amount - at.paid_amount) debt_sum,
      sum(nvl2(bda.id, at.amount, 0)) bad_debt,
      sum(decode(bda.our_fault, 1, at.amount, 0)) our_fault,
      sum(case
        when a.payment_term_dt >= sysdate - 1 then 0
        else at.amount - at.paid_amount
      end) overdue_debt
    from bo.t_act a
    join bo.t_act_trans at on a.id = at.act_id
    join bo.t_consume q on at.consume_id = q.id
    join bo.t_order o on q.parent_order_id = o.id
    join bo.t_invoice i on i.id = a.invoice_id
    join bo.t_person p on p.id = i.person_id
    join bo.t_contract_attributes pname on pname.attribute_batch_id = p.attribute_batch_id
        and pname.code = 'NAME'
    join bo.t_client cl on cl.id = a.client_id
    join bo.t_contract2 c on c.id = i.contract_id
    join bo.t_contract_collateral col0
      on c.id = col0.contract2_id
      and col0.collateral_type_id is null
    join bo.t_contract_attributes ca_firm
      on ca_firm.collateral_id = col0.id
      and ca_firm.code = 'FIRM'
    left join bo.t_contract_attributes ca_manager
      on ca_manager.collateral_id = col0.id
      and ca_manager.code = 'MANAGER_CODE'
    left join bo.t_contract_attributes ca_reg
      on ca_reg.collateral_id = col0.id
      and ca_reg.code = 'REGION'
    left join bo.t_fias kladr
      on kladr.kladr_code = substr(ca_reg.value_str, 0, 11)
    left join bo.t_contract_attributes ca_country
      on ca_country.collateral_id = col0.id
      and ca_country.code = 'COUNTRY'
    left join bo.t_country country
      on country.region_id = ca_country.value_num
    left join bo.t_bad_debt_act bda
      on bda.act_id = a.id
      and bda.hidden = 0
    left join bo.t_manager m
      on m.manager_code = ca_manager.value_num
    where 1=1
      and o.service_id in (135, 650)
    group by
      cl.id,
      cl.name,
      p.id,
      pname.value_str,
      c.external_id,
      i.id,
      i.external_id,
      i.receipt_sum_1c,
      ca_manager.value_num,
      m.name,
      country.region_name,
      kladr.formal_name
    order by
      ca_manager.value_num,
      cl.id,
      c.external_id
)
select
  manager_code,
  client_id,
  client,
  person_id,
  person,
  contract,
  invoice,
  manager,
  country,
  region,
  debt00 "0-0",
  debt130 "1-30",
  debt3060 "30-60",
  debt6090 "60-90",
  debtover90 ">90",
  act_sum,
  debt_sum,
  bad_debt,
  greatest(debt_sum - bad_debt, 0) clean_debt,
  our_fault,
  overdue_debt,
  greatest(overdue_debt - bad_debt, 0) overdue_clean_debt,
  debts.receipt_sum_1c - nvl(sum(p.receipt_sum_1c), 0) + nvl(sum(p.receipt_sum), 0) invoice_payments_sum
from debts
left join bo.t_payment p
  on p.invoice_id = debts.invoice_id
group by
    manager_code,
    client_id,
    client,
    person_id,
    person,
    contract,
    invoice,
    manager,
    country,
    region,
    debt00,
    debt130,
    debt3060,
    debt6090,
    debtover90,
    act_sum,
    debt_sum,
    bad_debt,
    greatest(debt_sum - bad_debt, 0), our_fault,
    overdue_debt,
    greatest(overdue_debt - bad_debt, 0),
    debts.receipt_sum_1c
"""

MIN_CONTRACT_ID = 46665

M_CON_STR = "meta/meta"
B_CON_STR = "balance/balance"


class TestOracleViewTarget:

    def test_oracle_view_target_exists(self):
        full = SQL.format(MIN_CONTRACT_ID)
        # empty = SQL.format(MIN_CONTRACT_ID - 1)

        full_view = DBViewTarget((M_CON_STR, full))
        assert full_view.exists()

        # our view always exists

        # empty_view = DBViewTarget((M_CON_STR, empty))
        # assert not empty_view.exists()

    def test_oracle_view_target_export(self, yt_test_root):

        view_yt_table = yt_test_root / YTTableTarget("oracle_view_test/opex_contracts")

        view = DBViewTarget((M_CON_STR, SQL.format(MIN_CONTRACT_ID)))

        task = YTFullExportTaskV2(
            source=view,
            target=view_yt_table,
        )

        is_success = luigi.build([task], local_scheduler=True)
        assert is_success
        assert is_eq(view_yt_table, view)

    def test_create_from_record(self):
        record = {
            "_schema": f"{BUNKER}{DBViewTarget.bunker_schema}",
            "yb_con": M_CON_STR,
            "text": CHMELEV,
            "dumb": False,
            "lazy": False,
            "columns": [],
        }
        exp = DBViewTarget.exportable_from_record(record)
        assert exp.sql == dedent(record['text'])
        assert exp.yb_connection_string == record['yb_con']

    @pytest.mark.skip(reason="AttributeError: module 'cx_Oracle' has no attribute 'DB_TYPE_NUMBER'")
    def test_columns_from_inspector(self):
        view = DBViewTarget((M_CON_STR, "SELECT id, internal_rate, payment_date from t_invoice"))

        columns = [
            ColumnMeta('ID', cxO.DB_TYPE_NUMBER),
            ColumnMeta('INTERNAL_RATE', cxO.DB_TYPE_NUMBER),
            ColumnMeta('PAYMENT_DATE', cxO.DB_TYPE_DATE),
        ]
        mined_columns = view.mine_columns(ColumnMiningStrategy.inspect)
        assert columns == mined_columns

    @pytest.mark.skip(reason="AttributeError: module 'cx_Oracle' has no attribute 'DB_TYPE_NUMBER'")
    def test_columns_from_inspector_with_hints(self):
        view = DBViewTarget(
            (M_CON_STR, "SELECT id, internal_rate, payment_date from t_invoice"),
            hints=[
                {"name": "id", "type": "default"},
                {"name": "internal_rate", "type": "float"},
                {"name": "payment_date", "type": "date"},
                {"name": "ololo", "type": "date"},
            ]
        )

        columns = [
            ColumnMeta('ID', cxO.DB_TYPE_NUMBER),
            ColumnMeta('INTERNAL_RATE', cxO.DB_TYPE_NUMBER, ColumnHints.float),
            ColumnMeta('PAYMENT_DATE', cxO.DB_TYPE_DATE, ColumnHints.date),
            ColumnMeta('ololo', sqla.DateTime(), ColumnHints.date),
        ]
        mined_columns = view.mine_columns(ColumnMiningStrategy.inspect | ColumnMiningStrategy.hints)
        assert columns == mined_columns

    def test_columns_from_hints(self):
        view = DBViewTarget(
            (M_CON_STR, "SELECT id, internal_rate, payment_date from t_invoice"),
            hints=[
                {"name": "id", "type": "default"},
                {"name": "internal_rate", "type": "float"},
                {"name": "payment_date", "type": "date"},
            ]
        )

        columns = [
            ColumnMeta('id', None),
            ColumnMeta('internal_rate', sqla.Numeric(), ColumnHints.float),
            ColumnMeta('payment_date', sqla.DateTime(), ColumnHints.date),
        ]
        mined_columns = view.mine_columns(ColumnMiningStrategy.hints)
        assert columns == mined_columns

    def test_columns_from_parsing(self):
        full = SQL.format(MIN_CONTRACT_ID)

        v_ar_rewards = OracleTableTarget(f"{M_CON_STR}:bo.v_ar_rewards")
        v_contract_apex_ful = OracleTableTarget(f"{M_CON_STR}:bo.v_contract_apex_full")

        view = DBViewTarget((M_CON_STR, full))

        columns = view.mine_columns(ColumnMiningStrategy.parse)
        real_columns = [
            v_ar_rewards.column('contract_id'),
            v_ar_rewards.column('dkv_to_charge'),
            v_contract_apex_ful.column('discount_findt')
        ]
        assert columns == real_columns

    # def test_music_payments(self):
    #     d: DBViewTarget = DBViewTarget.exportable_from_uri("bunker://dwh/test/queries/music_payments")
    #     print(d.columns)
    #     print(d.get_yt_schema())
    #     print(d.get_read_sql())
    #     # assert False

    def test_serialization(self):
        d: DBViewTarget = DBViewTarget.exportable_from_uri("bunker://dwh/test/queries/geoproduct_sales_report")
        sd: DBViewTarget = eval(repr(d))
        assert d.sql == sd.sql
        assert d.commentary == sd.commentary
        assert d.hints == sd.hints
        assert d.mining_strategy == sd.mining_strategy
        assert d.columns == sd.columns


class TestParsing:

    def test_tables_extraction(self):
        assert find_all_tables(CHMELEV) == {
            "ps": "bo.t_partner_dsp_stat",
            "p": "bo.t_place",
        }
        assert find_all_tables(CONTRACTS) == {
            "pd": "bo.t_partner_act_data",
            "c": "bs.t_contract2",
        }

    def test_columns_extraction(self):
        assert find_all_columns(CHMELEV) == [
            ('page_id', "ps", "place_id"),
            ('url', 'p', 'url'),
            ('dsp_id', 'ps', 'dsp_id'),
            ('block_id', 'ps', 'block_id'),
            ('dsp_charge', 'ps', 'dsp_charge'),
            ('partner_reward', 'ps', 'partner_reward'),
            ('showsiki', 'ps', 'shows'),
        ]
        assert find_all_columns(CONTRACTS) == [
            ("pageid", "pd", "place_id"),
            ("contract_eid", "c", "external_id"),
        ]

    @pytest.mark.bad
    @pytest.mark.skip(reason="not fixed")
    @pytest.mark.parametrize(
        'sql',
        [
            HARD_SQL1,
            HARD_SQL2,
            HARD_SQL3,
        ]
    )
    def test_hard_sql_parsing(self, sql):
        DBViewTarget.find_all_columns(sql)
