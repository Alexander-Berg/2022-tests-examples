import hashlib
import logging
import typing as tp
import uuid
from dataclasses import dataclass, asdict

from flask import Flask

from maps.infra.quotateka.cli.tests.mocks.flask_app import FlaskApp

logger = logging.getLogger(__name__)


@dataclass
class FolderInfo:
    id: str
    version: int = 0
    folderType: str = 'COMMON_DEFAULT_FOR_SERVICE'
    displayName: str = 'default'
    description: str = ''
    deleted: bool = False


def uuid_from_string(s: str) -> str:
    m = hashlib.md5()
    m.update(s.encode('utf-8'))
    return str(uuid.UUID(m.hexdigest(), version=4))


class Abcd(FlaskApp):

    def __init__(self, listen_port: int) -> None:
        super(Abcd, self).__init__(listen_port)

    def create_app(self) -> Flask:
        app = Flask('abcd')

        @app.route('/ping')
        def ping() -> str:
            return 'ok'

        @app.route('/api/v1/services/<int:abc_id>/folders')
        def folders(abc_id) -> tp.Dict[str, tp.Any]:
            items = [
                asdict(FolderInfo(id=uuid_from_string(str(abc_id))))
            ]
            return {
                # nextPageToken: None,
                'items': items
            }

        return app
