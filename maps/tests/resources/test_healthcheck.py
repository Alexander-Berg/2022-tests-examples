import logging
from http import HTTPStatus

from flask_sqlalchemy import _SQLAlchemyState  # noqa

from maps.b2bgeo.reference_book.lib.app import extract_resources
from maps.b2bgeo.reference_book.lib.models import db
from maps.b2bgeo.reference_book.lib.run import create_app


def test_responses_ok_if_healthy(test_client):
    resp = test_client.get('/api/v1/healthcheck')

    assert resp.status_code == HTTPStatus.OK


def test_responses_with_error_if_problems_with_db():
    app = create_app()

    app.config.from_object('maps.b2bgeo.reference_book.lib.config.unit_test')
    logging.disable()

    DB_BASE_URI = 'postgresql+psycopg2://nouser:nopasswd@localhost:1/nodb?target_session_attrs=read-write'
    app.config['SQLALCHEMY_DATABASE_URI'] = DB_BASE_URI
    app.config['SQLALCHEMY_BINDS'] = {'slave': DB_BASE_URI}
    app.config['DB_CONFIG'] = {
        'host': 'localhost',
        'port': 1,
        'user': 'nouser',
        'password': 'nopassword',
        'dbname': 'nodb',
    }

    extract_resources('maps/reference_book/lib/migrations/')

    with app.app_context():
        db.init_app(app)

    with app.test_client() as test_client:
        resp = test_client.get('/api/v1/healthcheck')

    assert resp.status_code == HTTPStatus.SERVICE_UNAVAILABLE
