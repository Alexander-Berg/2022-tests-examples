import logging

import alembic.config
import alembic.command
import flask
import sqlalchemy as sa
from flask_sqlalchemy import SQLAlchemy

from lib.utils.flask_masterslave import MasterSlaveConnectionPool

from unittest import TestCase


class FlaskTest(TestCase):
    def setUp(self):
        super(FlaskTest, self).setUp()

        logging.captureWarnings(True)
        self.app = flask.Flask(__name__)
        self.app.config['DEBUG'] = True
        self.app.config['TESTING'] = True
        self.app.config['SQLALCHEMY_RECORD_QUERIES'] = True
        self.app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
        self.app.config['SQLALCHEMY_DATABASE_URI'] = "postgresql://postgres@localhost/qnotifier?connect_timeout=10&port=5432&password=qwerty"

        self.app_context = self.app.app_context()
        self.app_context.push()

        db = self.app.database = SQLAlchemy(self.app)
        self.app.database_pool = MasterSlaveConnectionPool(self.app)
        cfg = alembic.config.Config('migrations/alembic.ini')
        with db.engine.begin() as connection:
            cfg.attributes['connection'] = connection
            cfg.set_main_option('script_location', 'migrations')
            alembic.command.upgrade(cfg, 'head')

        metadata = sa.MetaData()
        self.tags_table = sa.Table(
            'tags', metadata,
            sa.Column('tag_id', sa.Integer, primary_key=True, autoincrement=True),
            sa.Column('name', sa.VARCHAR(length=256))
        )

        self.subscriptions_table = sa.Table(
            'subscriptions', metadata,
            sa.Column('subscription_id', sa.Integer, primary_key=True, autoincrement=True),
            sa.Column('rule', sa.Text, nullable=False),
            sa.Column('is_group', sa.SmallInteger, nullable=False),
            sa.Column('name', sa.VARCHAR(length=256), nullable=False),
            sa.Column('positive_clauses', sa.SmallInteger, nullable=False),
            sa.Column('negative_clauses', sa.SmallInteger, nullable=False),
        )

        self.subscriptions_tags_table = sa.Table(
            'subscriptions_tags', metadata,
            sa.Column('subscription_id', sa.Integer,
                      sa.ForeignKey('subscriptions.subscription_id'), primary_key=True),
            sa.Column('tag_id', sa.Integer, sa.ForeignKey('tags.tag_id'), primary_key=True),
            sa.Column('invert', sa.SmallInteger, primary_key=True)
        )

        self.users_table = sa.Table(
            'users', metadata,
            sa.Column('login', sa.VARCHAR(length=256), nullable=False, primary_key=True),
            sa.Column('email', sa.VARCHAR(length=256), nullable=True),
        )

    def tearDown(self):
        self.app.database.session.rollback()
        self.app_context.pop()
        super(FlaskTest, self).tearDown()
