import contextlib
import json
import os
from pathlib import Path
import random
import re
import socket
import subprocess
import tempfile
import time
import urllib.request
import copy
import sys
import collections.abc

import yatest.common
import yatest.common.network

import ydb

from captcha.server.test import util


START_TIMEOUT = 80
START_POLL_DELAY = 0.1


def update(d, u):
    for k, v in u.items():
        if isinstance(v, collections.abc.Mapping):
            d[k] = update(d.get(k, {}), v)
        else:
            d[k] = v
    return d


class Subprocess:
    def __init__(self, path, args=[], mute=False):
        if mute:
            redir = subprocess.DEVNULL
        else:
            redir = None

        self.__process = subprocess.Popen(
            [path] + args,
            stdout=redir,
            stderr=redir,
        )

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        self.terminate()
        self.__process.__exit__(exc_type, exc, tb)

    def terminate(self):
        self.__process.terminate()
        self.__process.wait()


class NetworkSubprocess(Subprocess):
    def __init__(self, path, port, args=[], **kwargs):
        super().__init__(path, args, **kwargs)
        self.host = f"localhost:{port}"
        self.port = port


class NoRedirection(urllib.request.HTTPErrorProcessor):
    def http_response(self, request, response):
        return response

    https_response = http_response


class Captcha:
    def __init__(self, process, config):
        self.process = process
        self.config = copy.deepcopy(config)
        self.port = config["HttpServer"]["Port"]
        self.host = f"localhost:{self.port}"
        self.url_opener = urllib.request.build_opener(NoRedirection)

    @classmethod
    @contextlib.contextmanager
    def make(cls, options, mute=False):
        bin_path = yatest.common.build_path("captcha/server/captcha-server")

        with tempfile.NamedTemporaryFile("w") as config_file:
            base_config_path = yatest.common.source_path(
                "captcha/server/test/data/config.json",
            )

            with open(base_config_path, 'r') as inp:
                config = json.load(inp)
            update(config, options)

            config["SessionCache"]["Enabled"] = True
            for cache_bucket in config["SessionCache"]["Buckets"]:
                cache_bucket["MaxSessions"] = min(cache_bucket["MaxSessions"], 50)

            json.dump(config, config_file, indent=4)
            config_file.flush()

            if not mute:
                print(json.dumps(config, indent=4), file=sys.stderr)

            if mute:
                redir = subprocess.DEVNULL
            else:
                redir = None

            with subprocess.Popen(
                [
                    bin_path,
                    "-c", config_file.name,
                ],
                stdout=redir,
                stderr=redir,
            ) as process:
                try:
                    ok = False

                    for _ in range(round(START_TIMEOUT / START_POLL_DELAY)):
                        time.sleep(START_POLL_DELAY)

                        assert process.poll() is None

                        if util.service_available(options["HttpServer"]["Port"]):
                            ok = True
                            break

                    assert ok

                    yield cls(process, config)
                finally:
                    process.terminate()

    def is_alive(self):
        return self.process.poll() is None

    def finished_ok(self):
        return self.process.poll() == 0

    def send_request(self, request):
        if not request.startswith("http://") and not request.startswith("https://"):
            request = f"http://{self.host}{request}"

        request = re.sub('^https:', 'http:', request)

        r = urllib.request.Request(request)
        return self.url_opener.open(r)


class CaptchaTestSuite:
    options = {}
    captcha = None

    @classmethod
    def setup_class(cls):
        cls.__stack = contextlib.ExitStack().__enter__()

        try:
            if socket.gethostname().startswith("distbuild-"):
                default_mute = "0"
            else:
                default_mute = "1"

            cls.mute = yatest.common.get_param("mute", default_mute) == "1"

            lsan_blacklist_file = cls.enter(tempfile.NamedTemporaryFile("w"))
            lsan_blacklist_file.write("leak:kchashdb.h\n")
            lsan_blacklist_file.flush()

            lsan_options = f"suppressions={lsan_blacklist_file.name}"
            if os.environ["LSAN_OPTIONS"]:
                os.environ["LSAN_OPTIONS"] += "," + lsan_options
            else:
                os.environ["LSAN_OPTIONS"] = lsan_options

            cls.__port_manager = cls.enter(
                yatest.common.network.PortManager(),
            )
            port = cls.__port_manager.get_port()
            options = copy.deepcopy(cls.options)

            runtime_dir = Path(cls.enter(tempfile.TemporaryDirectory())).absolute()

            cls.ydb_endpoint = os.getenv('YDB_ENDPOINT')
            cls.ydb_database = os.getenv('YDB_DATABASE')
            assert cls.ydb_endpoint and cls.ydb_database

            update(options, {
                "HttpServer": {
                    "Port": port,
                },
                "ServiceUrl": f"localhost:{port}",
                "Items": {
                    "Database": {
                        "Endpoint": cls.ydb_endpoint,
                        "Name": cls.ydb_database,
                        "SecurityTokenFile": "",
                    },
                },
                "Locations": [
                    {
                        "Name": "all",
                        "Id": "Z",
                        "Database": {
                            "Endpoint": cls.ydb_endpoint,
                            "Name": cls.ydb_database,
                            "SecurityTokenFile": "",
                            "MaxActiveYdbSessions": 150
                        },
                        "SessionsTable": "sessions"
                    }
                ],
                "ServiceLogPath": os.path.join(runtime_dir, "captcha-service.log"),
                "ChecksLogPath": os.path.join(runtime_dir, "captcha-checks.log"),
                "GenerateLogPath": os.path.join(runtime_dir, "captcha-generate.log"),
                "AccessLogPath": os.path.join(runtime_dir, "captcha-access.log"),
                "DisplayInternalErrorDetails": True,
            })

            cls.prepare_ydb()

            cls.captcha = cls.enter(Captcha.make(
                options,
                mute=cls.mute,
            ))

            if hasattr(cls, "setup_subclass"):
                cls.setup_subclass()
        except:
            cls.__stack.__exit__(None, None, None)
            raise

    @classmethod
    def prepare_ydb(cls):
        driver = ydb.Driver(ydb.DriverConfig(cls.ydb_endpoint, cls.ydb_database))
        driver.wait()
        cls.ydb_session = ydb.retry_operation_sync(lambda: driver.table_client.session().create())
        assert cls.ydb_session.transaction().execute('select 1 as cnt;', commit_tx=True)[0].rows[0].cnt == 1

        cls.ydb_session.execute_scheme("""
            CREATE TABLE versions (
                version String,
                type String,
                timestamp Int32,
                PRIMARY KEY (version, type)
            );
        """)
        cls.ydb_session.execute_scheme("""
            CREATE TABLE items (
                version String,
                type String,
                id String,
                data String,
                metadata String,
                PRIMARY KEY (version, type, id)
            );
        """)
        make_sharded_session_table_bin_path = yatest.common.build_path("captcha/tools/make_sharded_session_table/make_sharded_session_table")
        kikimr_uploader_bin_path = yatest.common.build_path("captcha/tools/kikimr_uploader/kikimr_uploader")
        with tempfile.NamedTemporaryFile("w") as token_file:
            processes = []
            processes.append(subprocess.Popen([
                make_sharded_session_table_bin_path,
                "--endpoint", cls.ydb_endpoint,
                "--database", cls.ydb_database,
                "--table", "sessions",
            ],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            ))
            kikimr_uploader_args = [
                kikimr_uploader_bin_path,
                "--endpoint", cls.ydb_endpoint,
                "--database", cls.ydb_database,
                "--table-items", "items",
                "--table-versions", "versions",
                "--transaction-limit", "500",
                "--auth-token-file", token_file.name,
            ]
            for type in ("ocr", "sound_ru"):
                args = kikimr_uploader_args + [
                    "legacy_tarball",
                    "--type", type,
                    "--version", f"version_{type}",
                    f"{type}.tar.gz"
                ]
                if type.startswith("sound_"):
                    args += [
                        "--content-type", "audio/mpeg; charset=utf-8",
                        '--bare-answer'
                    ]
                processes.append(subprocess.Popen(args,
                                                  stdout=subprocess.PIPE,
                                                  stderr=subprocess.PIPE,
                                                  ))

            for type in ("txt_v0", "txt_v0_en"):
                # чтобы создать такой файл,  нужно зайти в интерфейс YT,
                # кликнуть кнопку "Download", выбрать "YSON", Range = 5 строк, Format = Text
                args = kikimr_uploader_args + [
                    "yt-table",
                    "--type", type,
                    "--version", f"version_{type}",
                    "--proxy", "test",
                    "--path", f"{type}.yson",
                ]
                processes.append(subprocess.Popen(args,
                                                  stdout=subprocess.PIPE,
                                                  stderr=subprocess.PIPE,
                                                  ))

            for p in processes:
                (output, err) = p.communicate()
                status = p.wait()
                assert status == 0, err

    @classmethod
    def get_session(cls, token):
        rows = cls.ydb_session.transaction().execute(f"SELECT * FROM sessions WHERE token='{token}'", commit_tx=True)[0].rows
        if len(rows) == 0:
            return None
        assert len(rows) == 1
        return rows[0]

    @classmethod
    def teardown_class(cls):
        cls.__stack.__exit__(None, None, None)
        assert cls.captcha.finished_ok()

    def setup_method(self, method):
        test_key = f"{type(self).__name__}.{method.__name__}"
        random.seed(hash(test_key))

    def teardown_method(self):
        assert self.captcha.is_alive()

    @classmethod
    def enter(cls, ctxmgr):
        return cls.__stack.enter_context(ctxmgr)

    @classmethod
    def send_request(cls, *args, **kwargs):
        return cls.captcha.send_request(*args, **kwargs)
