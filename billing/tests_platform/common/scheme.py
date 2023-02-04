"""
Объекты БД, необходимые для регрессионного тестирования
"""

import sqlalchemy as sa

from sqlalchemy.dialects.oracle import NUMBER

from agency_rewards.rewards.common import ARData


meta = sa.MetaData(schema='bo')

base_skv = sa.Table(
    ARData.get_table_name_base().replace('bo.', ''),
    meta,
    sa.Column('contract_id', sa.Integer, nullable=False),
    sa.Column('contract_eid', sa.String(64), nullable=False),
    sa.Column('discount_type', sa.Integer, nullable=False),
    sa.Column('nds', sa.Integer, nullable=False),
    sa.Column('currency_count', sa.Integer, nullable=False),
    sa.Column('nds_count', sa.Integer, nullable=False),
    sa.Column('client_id', sa.Integer, nullable=False),
    sa.Column('from_dt', sa.DateTime, nullable=False),
    sa.Column('till_dt', sa.DateTime, nullable=False),
    sa.Column('amt_rub', sa.Numeric),
    sa.Column('amt_w_nds_rub', sa.Numeric),
    sa.Column('excluded', sa.Integer),
    sa.Column('failed', sa.Integer),
)

prof_skv = sa.Table(
    ARData.get_table_name_prof().replace('bo.', ''),
    meta,
    sa.Column('contract_id', sa.Integer, nullable=False),
    sa.Column('contract_eid', sa.String(64), nullable=False),
    sa.Column('discount_type', sa.Integer, nullable=False),
    sa.Column('nds', sa.Integer, nullable=False),
    sa.Column('currency_count', sa.Integer, nullable=False),
    sa.Column('nds_count', sa.Integer, nullable=False),
    sa.Column('client_id', sa.Integer, nullable=False),
    sa.Column('from_dt', sa.DateTime, nullable=False),
    sa.Column('till_dt', sa.DateTime, nullable=False),
    sa.Column('amt_rub', sa.Numeric),
    sa.Column('amt_w_nds_rub', sa.Numeric),
    sa.Column('excluded', sa.Integer),
    sa.Column('failed', sa.Integer),
)


join_skv = sa.Table(
    ARData.get_table_name_joins().replace('bo.', ''),
    meta,
    sa.Column('contract_id', sa.Integer, nullable=False),
    sa.Column('contract_eid', sa.String(64), nullable=False),
    sa.Column('agency_id', sa.Integer, nullable=False),
    sa.Column('commission_type', sa.Integer, nullable=False),
    sa.Column('start_dt', sa.DateTime, nullable=False),
    sa.Column('finish_dt', sa.DateTime, nullable=False),
    sa.Column('sign_dt', sa.DateTime, nullable=False),
    sa.Column('linked_contract_id', sa.Integer),
    sa.Column('linked_agency_id', sa.Integer),
    sa.Column('cons_type', sa.Integer),
)


def _get_rewards_columns() -> list[sa.Column]:
    return [
        sa.Column('contract_id', sa.Integer, nullable=False),
        sa.Column('contract_eid', sa.String(64), nullable=False),
        sa.Column('from_dt', sa.DateTime, nullable=False),
        sa.Column('till_dt', sa.DateTime, nullable=False),
        sa.Column('nds', sa.Integer, nullable=False),
        sa.Column('currency', sa.String(16), nullable=False),
        sa.Column('nds', sa.Integer, nullable=False, default=1),
        sa.Column('discount_type', sa.Integer, nullable=False),
        sa.Column('reward_type', sa.Integer, nullable=False),
        sa.Column('turnover_to_charge', sa.Numeric),
        sa.Column('reward_to_charge', sa.Numeric),
        sa.Column('turnover_to_pay', sa.Numeric),
        sa.Column('turnover_to_pay_w_nds', sa.Numeric),
        sa.Column('reward_to_pay', sa.Numeric),
        sa.Column('reward_to_pay_src', sa.Numeric),
        sa.Column('insert_dt', sa.DateTime),
        sa.Column('tp', sa.String(16)),
        sa.Column('delkredere_to_charge', sa.Numeric),
        sa.Column('delkredere_to_pay', sa.Numeric),
        sa.Column('calc', sa.String(1024)),
    ]


rewards = sa.Table(
    ARData.get_table_name_rewards().replace('bo.', ''),
    meta,
    *_get_rewards_columns(),
)

# Всегда будет ходить именно во вьюху, даже в регрессии.
# Здесь workaround, так как в алхимии нельзя в метаданных две таблицы
# с одинаковым названием держать. А названия таблиц различаются только в регрессиях.
if ARData.get_view_name_rewards() in meta.tables:
    v_ar_rewards = rewards
else:
    v_ar_rewards = sa.Table(
        ARData.get_view_name_rewards().replace('bo.', ''),
        meta,
        *_get_rewards_columns(),
    )

payments = sa.Table(
    ARData.get_table_name_payments().replace('bo.', ''),
    meta,
    sa.Column('contract_id', sa.Integer, nullable=False),
    sa.Column('contract_eid', sa.String(64), nullable=False),
    sa.Column('invoice_id', sa.Integer, nullable=False),
    sa.Column('invoice_dt', sa.DateTime, nullable=False),
    sa.Column('invoice_type', sa.String(256), nullable=False),
    sa.Column('from_dt', sa.DateTime, nullable=False),
    sa.Column('till_dt', sa.DateTime, nullable=False),
    sa.Column('commission_type', sa.Integer, nullable=False),
    sa.Column('is_fully_paid', sa.Integer, nullable=False),
    sa.Column('discount_type', sa.Integer),
    sa.Column('nds', sa.Integer, default=1),
    sa.Column('currency', sa.String(8), default='RUR'),
    sa.Column('client_id', sa.Integer),
    sa.Column('amt', sa.Numeric),
    sa.Column('amt_w_nds', sa.Numeric),
    sa.Column('is_early_payment', sa.Boolean),
    sa.Column('is_early_payment_true', sa.Boolean),
    sa.Column('payment_control_type', sa.Integer, default=0),
    sa.Column('invoice_total_sum', sa.Numeric),
    sa.Column('invoice_total_sum_w_nds', sa.Numeric),
)

v_opt_2015_acts_last_month = sa.Table(
    't_ar_rgrs_acts_last_month',
    meta,
    sa.Column('contract_id', sa.Integer, nullable=False),
    sa.Column('contract_eid', sa.String(64), nullable=False),
    sa.Column('contract_from_dt', sa.Date),
    sa.Column('contract_till_dt', sa.Date),
    sa.Column('invoice_id', sa.Integer, nullable=False),
    sa.Column('invoice_type', sa.String(256), nullable=False),
    sa.Column('act_id', sa.Integer, nullable=False),
    sa.Column('commission_type', sa.Integer, nullable=False),
    sa.Column('discount_type', sa.Integer),
    sa.Column('agency_id', sa.Integer, nullable=False),
    sa.Column('service_id', sa.Integer, nullable=False),
    sa.Column('service_order_id', sa.Integer, nullable=False),
    sa.Column('brand_id', sa.Integer),
    sa.Column('amt', sa.Numeric),
    sa.Column('payment_control_type', sa.Numeric),
)


def _get_rewards_history_columns() -> list[sa.Column]:
    return [
        sa.Column('contract_id', sa.Integer, nullable=False),
        sa.Column('contract_eid', sa.String(64), nullable=False),
        sa.Column('reward_type', sa.Integer, nullable=False),
        sa.Column('discount_type', sa.Integer, nullable=True),
        sa.Column('turnover_to_pay', NUMBER(38, 5)),
        sa.Column('turnover_to_charge', sa.Numeric),
        sa.Column('reward_to_pay', sa.Numeric),
        sa.Column('reward_to_charge', sa.Numeric),
        sa.Column('delkredere_to_charge', sa.Numeric),
        sa.Column('delkredere_to_pay', sa.Numeric),
        sa.Column('currency', sa.String(8), default='RUR'),
        sa.Column('from_dt', sa.DateTime, nullable=False),
        sa.Column('till_dt', sa.DateTime, nullable=False),
        sa.Column('insert_dt', sa.DateTime, nullable=False),
        sa.Column('tp', sa.String(16), nullable=True),
        sa.Column('calc', sa.String(1024)),
    ]


rewards_history = sa.Table(
    ARData.get_table_name_rewards_history().replace('bo.', ''),
    meta,
    *_get_rewards_history_columns(),
)

# Всегда будет ходить именно во вьюху, даже в регрессии.
# Здесь workaround, так как в алхимии нельзя в метаданных две таблицы
# с одинаковым названием держать. А названия таблиц различаются только в регрессиях.
if ARData.get_view_name_rewards_history() in meta.tables:
    v_ar_rewards_history = rewards_history
else:
    v_ar_rewards_history = sa.Table(
        ARData.get_view_name_rewards_history().replace('bo.', ''),
        meta,
        *_get_rewards_history_columns(),
    )

v_opt_2015_acts_2_month_ago = sa.Table(
    't_ar_rgrs_acts_2_months_ago',
    meta,
    sa.Column('contract_id', sa.Integer, nullable=False),
    sa.Column('contract_eid', sa.String(64), nullable=False),
    sa.Column('contract_from_dt', sa.Date),
    sa.Column('contract_till_dt', sa.Date),
    sa.Column('invoice_id', sa.Integer, nullable=False),
    sa.Column('invoice_type', sa.String(256), nullable=False),
    sa.Column('act_id', sa.Integer, nullable=False),
    sa.Column('commission_type', sa.Integer, nullable=False),
    sa.Column('discount_type', sa.Integer),
    sa.Column('agency_id', sa.Integer, nullable=False),
    sa.Column('service_id', sa.Integer, nullable=False),
    sa.Column('service_order_id', sa.Integer, nullable=False),
    sa.Column('brand_id', sa.Integer),
    sa.Column('amt', sa.Numeric),
)

agency_stats = sa.Table(
    't_ar_rgrs_agency_stats',
    meta,
    sa.Column('agency_id', sa.Integer, nullable=False),
    sa.Column('act_id', sa.Integer, nullable=False),
    sa.Column('client_id', sa.Integer, nullable=False),
    sa.Column('service_id', sa.Integer, nullable=False),
    sa.Column('service_order_id', sa.Integer, nullable=False),
    sa.Column('amt', sa.Numeric),
)

domains_stats = sa.Table(
    't_ar_rgrs_domain',
    meta,
    sa.Column('billing_export_id', sa.Integer, nullable=False),
    sa.Column('service_order_id', sa.Integer, nullable=False),
    sa.Column('service_id', sa.Integer, nullable=False),
    sa.Column('is_blacklist', sa.Integer, nullable=False),
    sa.Column('is_gray', sa.Integer, nullable=False),
    sa.Column('domain', sa.String(1024), nullable=True),
    sa.Column('cost', sa.Integer, nullable=False),
)


v_ar_acts_q_ext = sa.Table(
    't_ar_rgrs_acts_q_ext',
    meta,
    sa.Column('contract_eid', sa.String(64), nullable=False),
    sa.Column('contract_id', sa.Integer, nullable=False),
    sa.Column('agency_id', sa.Integer, nullable=False),
    sa.Column('discount_type', sa.Integer, nullable=False),
    sa.Column('commission_type', sa.Integer, nullable=False),
    sa.Column('from_dt', sa.DateTime, nullable=False),
    sa.Column('till_dt', sa.DateTime, nullable=False),
    sa.Column('amt_q', NUMBER(precision=38, scale=10), nullable=False),
    sa.Column('amt_prev_q', NUMBER(precision=38, scale=10), nullable=False),
    sa.Column('amt', NUMBER(precision=38, scale=10), nullable=False),
    sa.Column('amt_w_nds', NUMBER(precision=38, scale=10), nullable=False),
    sa.Column('failed', sa.Integer, nullable=False),
    sa.Column('failed_bok', sa.Integer, nullable=False),
)

v_ar_acts_hy = sa.Table(
    't_ar_rgrs_acts_hy',
    meta,
    sa.Column('contract_eid', sa.String(64), nullable=False),
    sa.Column('contract_id', sa.Integer, nullable=False),
    sa.Column('discount_type', sa.Integer, nullable=False),
    sa.Column('commission_type', sa.Integer, nullable=False),
    sa.Column('amt_cons', NUMBER(precision=38, scale=10), nullable=False),
    sa.Column('amt', NUMBER(precision=38, scale=10), nullable=False),
    sa.Column('amt_w_nds', NUMBER(precision=38, scale=10), nullable=False),
    sa.Column('failed_bok', sa.Integer, nullable=False),
)


fin_docs = sa.Table(
    't_ar_rgrs_fin_docs',
    meta,
    sa.Column('contract_eid', sa.String(64), nullable=False),
    sa.Column('agency_id', sa.Integer, nullable=False),
    sa.Column('from_dt', sa.String(10), nullable=False),
    sa.Column('receive_dt', sa.String(10), nullable=False),
)


regtest_data = sa.Table(
    't_ar_rgrs_data',
    meta,
    sa.Column('module_name', sa.String(200), nullable=False),
    sa.Column('class_name', sa.String(200), nullable=False),
    sa.Column('field', sa.String(200), nullable=False),
    sa.Column('value', sa.String(200), nullable=False),
)


commission_corrections = sa.Table(
    't_commission_correction',
    meta,
    sa.Column('type', sa.String(2000), nullable=False),
    sa.Column('from_dt', sa.DateTime, nullable=False),
    sa.Column('till_dt', sa.DateTime, nullable=False),
    sa.Column('contract_id', sa.Integer, nullable=False),
    sa.Column('contract_eid', sa.String(2000), nullable=False),
    sa.Column('currency', sa.String(2000), nullable=False),
    sa.Column('nds', sa.Integer, nullable=False),
    sa.Column('reward_to_charge', sa.Numeric),
    sa.Column('delkredere_to_charge', sa.Numeric),
    sa.Column('dkv_to_charge', sa.Numeric),
    sa.Column('reward_to_pay', sa.Numeric),
    sa.Column('reward_to_pay_src', sa.Numeric),
    sa.Column('delkredere_to_pay', sa.Numeric),
    sa.Column('dkv_to_pay', sa.Numeric),
    sa.Column('dsc', sa.String(2000)),
    sa.Column('turnover_to_charge', sa.Numeric),
    sa.Column('turnover_to_pay', sa.Numeric),
    sa.Column('turnover_to_pay_w_nds', sa.Numeric),
    sa.Column('nds', sa.Integer, default=1),
    sa.Column('discount_type', sa.Integer),
    sa.Column('reward_type', sa.Integer),
)
