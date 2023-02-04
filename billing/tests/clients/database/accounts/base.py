import sqlalchemy as sa

metadata = sa.MetaData(schema='acc')

state_account = sa.Table(
    't_state_account', metadata,
    sa.Column('id', sa.BigInteger),
    sa.Column('type', sa.String),
    sa.Column('state', sa.JSON),
    sa.Column('dt', sa.DateTime),
    sa.Column('attribute_1', sa.String),
    sa.Column('attribute_2', sa.String),
    sa.Column('attribute_3', sa.String),
    sa.Column('attribute_4', sa.String),
    sa.Column('attribute_5', sa.String),
    sa.Column('shard_key', sa.String),
    sa.Column('namespace', sa.String),
)


event_batch = sa.Table(
    't_event_batch', metadata,
    sa.Column('id', sa.BigInteger),
    sa.Column('dt', sa.DateTime),
    sa.Column('event_type', sa.String),
    sa.Column('external_id', sa.String),
    sa.Column('info', sa.JSON),
    sa.Column('shard_key', sa.String),
    sa.Column('event_count', sa.Integer),
)


account = sa.Table(
    't_account', metadata,
    sa.Column('id', sa.BigInteger),
    sa.Column('type', sa.String),
    sa.Column('aggregated_sequence_pos', sa.BigInteger),
    sa.Column('attribute_1', sa.String),
    sa.Column('attribute_2', sa.String),
    sa.Column('attribute_3', sa.String),
    sa.Column('attribute_4', sa.String),
    sa.Column('attribute_5', sa.String),
    sa.Column('shard_key', sa.String),
    sa.Column('namespace', sa.String),
)

event = sa.Table(
    't_event', metadata,
    sa.Column('id', sa.BigInteger),
    sa.Column('event_batch_id', sa.BigInteger, sa.ForeignKey('t_event_batch.id')),
    sa.Column('account_id', sa.BigInteger, sa.ForeignKey('t_account.id')),
    sa.Column('dt', sa.DateTime),
    sa.Column('type', sa.String),
    sa.Column('amount', sa.DECIMAL),
    sa.Column('attribute_1', sa.String),
    sa.Column('attribute_2', sa.String),
    sa.Column('attribute_3', sa.String),
    sa.Column('attribute_4', sa.String),
    sa.Column('attribute_5', sa.String),
    sa.Column('seq_id', sa.BigInteger),
)
