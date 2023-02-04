import sqlalchemy as sa


meta = sa.MetaData(schema='bo')


private_deals = sa.Table(
    '//home/balance/dev/yb-ar/private-deals',
    meta,
    sa.Column('id', sa.Integer, sa.Sequence('s1'), primary_key=True, nullable=False),
    sa.Column('agency_rev_ratio', sa.Float, nullable=False),
    sa.Column('b_external_id', sa.Integer, nullable=False),
    sa.Column('name', sa.String(64), nullable=False),
)

deals_dict = sa.Table(
    '//home/balance/dev/yb-ar/adfox_deals_dict',
    meta,
    sa.Column('id', sa.Integer, sa.Sequence('s2'), primary_key=True, nullable=False),
    sa.Column('agencyRevenueRatio', sa.Float, nullable=False),
    sa.Column('dealExportId', sa.Integer, nullable=False),
    sa.Column('name', sa.String(64), nullable=False),
    sa.Column('type', sa.Integer, default=10),
)

balance_deal_notifications = sa.Table(
    '//home/balance/dev/yb-ar/deal-notification',
    meta,
    sa.Column('id', sa.Integer, sa.Sequence('s3'), primary_key=True, nullable=False),
    sa.Column('doc_number', sa.String(64), nullable=False),
    sa.Column('b_external_id', sa.Integer, nullable=False),
    sa.Column('doc_date', sa.DateTime, nullable=False),
)


direct_deal_notifications = sa.Table(
    '//home/direct/db/deal_notifications',
    meta,
    sa.Column('id', sa.Integer, sa.Sequence('s3'), primary_key=True, nullable=False),
    sa.Column('deal_id', sa.Integer, nullable=False),
    sa.Column('creation_time', sa.DateTime, nullable=False),
    sa.Column('client_notification_id', sa.String(64), nullable=False),
)
