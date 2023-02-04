# -*- coding: utf-8 -*-
from datacloud.score_api.server import app


def main():
    _app = app.ydb_main()
    my_app = _app.ydb_main()
    host = '::1'
    port = '8008'
    my_app.run(debug=True, host=host, port=port, passthrough_errors=True)


if __name__ == '__main__':
    main()
