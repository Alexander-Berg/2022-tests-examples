import collections
import getpass
import json
import logging
import py
import pytest
import shutil
import os
import uuid
import yatest.common as yc
import yatest.common.network as ycn
from ppadb.client import Client as AdbClient

from yandex_io.pylibs.functional_tests.tus import TUSClient

from .musicapi import MusicApiClient
from .passport import PassportClient
from .quasar_backend import QuasarBackendClient
from .io import YandexIODevice
from .user import UserRepo, UserCache
from .utils import collect_pytest_marks, FakeExecution, common_data_path, ANY_IDLING_ALICE_STATE
from .stage_logger import StageLogger
from .multidevice_controller import MultiDeviceController


try:
    import library.python.pytest.yatest_tools as tools
except ImportError:
    # fallback for pytest script mode
    import yatest_tools as tools

logger = logging.getLogger(__name__)

DEFAULT_VINS_URL = "http://vins-int.voicetech.yandex.net/speechkit/app/quasar/"
DEFAULT_MUSIC_API_URL = "wss://ws-api.music.yandex.net/quasar/websocket"

MODE_TO_MARKS = {
    "regression": ["regression", "station_presmoke", "presmoke"],
    "presmoke": ["presmoke"],
    "station_presmoke": ["station_premsoke"],
}

Config = collections.namedtuple("Config", ('path', 'extra', 'json'))


def pytest_configure(config):
    config.addinivalue_line("markers", "with_yandex_plus: marked test requires Yandex.Plus subscription")
    config.addinivalue_line("markers", "testpalm: testpalm case")
    config.addinivalue_line("markers", "no_plus: marked test requires account without Plus")
    config.addinivalue_line("markers", "experiments: makred test requires experiment")
    config.addinivalue_line("markers", "multidevice: multidevice test mark")
    config.addinivalue_line("markers", "presmoke: presmoke test case")
    config.addinivalue_line("markers", "regression: regression test case")
    config.addinivalue_line("markers", "station_presmoke: presmoke test case for YandexStation")


@pytest.fixture(scope="session")
def tus_client():
    return TUSClient()


@pytest.fixture(scope="session")
def passport_client():
    return PassportClient()


@pytest.fixture(scope="session")
def music_api_client():
    return MusicApiClient()


@pytest.fixture(scope="session")
def user_cache():
    return UserCache()


@pytest.fixture(scope="session")
def user_repo(tus_client, user_cache):
    repo = UserRepo(tus_client, user_cache)
    yield repo
    repo.unlockCachedUsers()


@pytest.fixture(scope="function")
def user(request, user_repo):
    if yc.get_param("session_id") is not None:
        return None
    return create_user(request, user_repo)


def create_user(request, user_repo):
    tags = set()
    marks = collect_pytest_marks(request)
    needs_yandex_plus = any(mark.name == "with_yandex_plus" for mark in marks)
    no_yandex_plus = any(mark.name == "no_plus" for mark in marks)
    if needs_yandex_plus and no_yandex_plus:
        raise RuntimeError("no_plus and with_yandex_plus used in the same test. Thats stupid.")
    if needs_yandex_plus:
        tags.add("plus")
    elif no_yandex_plus:
        tags.add("no_plus")
    user = user_repo.getUser(tags)
    return user


def make_sessionid(yc, user, passport_client, stage_logger):
    sessionid = yc.get_param("session_id")
    if sessionid is not None:
        return sessionid
    LOG_MESSAGE = "Using account {} with tags: {}".format(user["account"]["uid"], user["account"]["tags"])
    logger.info(LOG_MESSAGE)
    stage_logger.log(LOG_MESSAGE)
    stage_logger.end_logging()
    return passport_client.getSessionid(user)


@pytest.fixture(scope="function")
def sessionid(user, passport_client, stage_logs_path):
    return make_sessionid(
        yc, user, passport_client, StageLogger(stage_logs_path, config=None, log_service_events=False)
    )


def _reset_account_music_info(config, authCode, passport_client, music_api_client):
    xTokenClientId = config.json["authd"]["xTokenClientId"]
    xTokenClientSecret = config.json["authd"]["xTokenClientSecret"]

    xToken = passport_client.getXToken(authCode, xTokenClientId, xTokenClientSecret)

    authTokenClientId = config.json["authd"]["authTokenClientId"]
    authTokenClientSecret = config.json["authd"]["authTokenClientSecret"]

    oAuthToken = passport_client.getOAuthToken(xToken, authTokenClientId, authTokenClientSecret)
    music_api_client.debugResetAccountMusicInfo(oAuthToken)


def pull_logs_from_device():
    logger.info("Pulling logs")
    if os.system('adb pull /data/quasar/daemons_logs ' + yc.test_output_path()) != 0:
        logger.error("Failed to pull logs")


@pytest.fixture(scope="function")
def testpalm_cases(request):
    return list(
        map(lambda mark: mark.args[0], filter(lambda mark: mark.name == "testpalm", collect_pytest_marks(request)))
    )


@pytest.fixture(scope="function")
def experiments(request):
    return list(
        map(lambda mark: mark.args[0], filter(lambda mark: mark.name == "experiments", collect_pytest_marks(request)))
    )


@pytest.fixture(scope="function")
def test_name(request):
    return request.node.name


def run_sample_app(cwd, args, stdout_filepath, stderr_filepath):
    b = yc.binary_path("yandex_io/sample_app/linux/sample_app")
    sample_app = yc.execute(
        [b, *args],
        wait=False,
        shell=True,
        cwd=str(cwd),
        stdout=stdout_filepath,
        stderr=stderr_filepath,
    )
    return sample_app


def tear_down(is_remote_test, use_tus, init_config, yio, backend_client, stage_logger):
    if is_remote_test:
        pull_logs_from_device()
        if init_config is not None:
            backend_client.setAccountConfig(init_config)
        with yio.get_service_connector("aliced") as aliced:
            state = aliced.wait_for_message().alice_state.state

            if state not in ANY_IDLING_ALICE_STATE:
                # Attempt to stop any ongoing alice's activity
                yio.press_alice_button(target_state=ANY_IDLING_ALICE_STATE)

            yio.start_conversation()
            yio.say_to_mic(common_data_path("enough.wav"))
            aliced.wait_for_message(
                lambda m: m.HasField('alice_state') and m.alice_state.state in ANY_IDLING_ALICE_STATE
            )
    if use_tus and yc.get_param("unregister", False):
        backend_client.unregister()
    yio.stop()
    stage_logger.end_logging()


def default_sample_app_args(config_path, auth_code, config_extra, telemetry_path):
    return [
        "--config",
        config_path,
        "--authcode",
        auth_code,
        "--config_pattern",
        "WORKDIR:" + config_extra["WORKDIR"],
        "--config_pattern",
        "SOURCE_ROOT:" + config_extra["SOURCE_ROOT"],
        "--telemetry_file",
        telemetry_path,
        "--disable-key-input",
    ]


@pytest.fixture(scope="function")
def device(
    backend_client,
    config,
    cwd,
    sessionid,
    passport_client,
    music_api_client,
    request,
    telemetry_file_path,
    is_remote_test,
    stage_logs_path,
    testpalm_cases,
    test_name,
    deviceid,
    use_tus,
    user,
):
    host = yc.get_param("target_host", "localhost")
    if not is_remote_test:
        # Clear current user so that we're starting with default configs:
        backend_client.dropUser()
        if yc.get_param("reset_account_music_info", False):
            _reset_account_music_info(
                config, get_authcode(sessionid, passport_client, config), passport_client, music_api_client
            )

        normalized_nodeid = normalize_nodeid(request.node.nodeid)
        stdout_filepath = yc.get_unique_file_path(yc.output_path(), f"{normalized_nodeid}.sample_app.out")
        stderr_filepath = yc.get_unique_file_path(yc.output_path(), f"{normalized_nodeid}.sample_app.err")
        args = default_sample_app_args(
            str(config.path), get_authcode(sessionid, passport_client, config), config.extra, str(telemetry_file_path)
        )
        args.extend(["--use-glagol", "--neighbor", "{}:{}".format(deviceid, config.json["glagold"]["externalPort"])])
        exec = run_sample_app(cwd, args, stdout_filepath, stderr_filepath)
    else:
        exec = FakeExecution()
        cwd = "REMOTE: {}".format(host)

    stage_logger = StageLogger(stage_logs_path, config.json, device_id="sample_app")
    stage_logger.log(f"Running test: {test_name},  with DeviceId: {deviceid}")
    if testpalm_cases:
        stage_logger.log("Used testpalm cases:\n" + "\n".join(testpalm_cases))

    yio = YandexIODevice(deviceid, exec, config.json, cwd, stage_logger, user)
    yio.start_test(test_name)
    if use_tus and is_remote_test:
        yio.authenticate(get_authcode(sessionid, passport_client, config))
    init_config = backend_client.getAccountConfig()

    yield yio
    yio.end_test(test_name)

    tear_down(is_remote_test, use_tus, init_config, yio, backend_client, stage_logger)


def normalize_nodeid(nodeid):
    d = nodeid.replace("[", ".").replace("]", "")
    return ".".join(tools.split_node_id(d))


@pytest.fixture(scope="function")
def cwd(tmpdir):
    ramDrivePath = yc.ram_drive_path()
    if ramDrivePath is None:
        logger.info(f"ram drive is none, use {tmpdir}")
        return tmpdir

    tmpTestDir = py.path.local(ramDrivePath).join(tmpdir.basename)
    tmpTestDir.mkdir()
    return tmpTestDir


@pytest.fixture(scope="function")
def dumpPath(cwd):
    path = cwd.join("audio_dump")
    path.mkdir()
    yield path
    shutil.move(str(path), yc.test_output_path())


@pytest.fixture(scope="function")
def telemetry_file_path(cwd, is_remote_test):
    path = cwd.join("telemetry.txt")
    yield path
    # remote tests do not create this file
    if not is_remote_test:
        shutil.move(str(path), yc.test_output_path())


@pytest.fixture(scope='function')
def stage_logs_path(cwd):
    path = cwd.join("stages.log")
    yield path
    shutil.move(str(path), yc.test_output_path())


def make_backend_client(config, sessionid, deviceid):
    backend_url = config.json["common"]["backendUrl"]
    platform = config.json["common"]["deviceType"]
    auth_cookies = {"Session_id": sessionid}
    return QuasarBackendClient(backend_url, auth_cookies, deviceid, platform)


@pytest.fixture(scope="function")
def backend_client(config, sessionid, deviceid):
    client = make_backend_client(config, sessionid, deviceid)
    yield client


# This one is a session-scoped fixture so that we're not reusing any of the previously used ports to avoid clashing with sockets lingering in time_wait state
# We should not have more than a few hundred tests overall (at least not until the current ipc is rewritten), which puts us at a couple thousands of ports per run
@pytest.fixture(scope="session")
def pm():
    yield ycn.PortManager()


def prepare_config(cwd, pm, file_name, dump_path, experiments):
    cfg_extra = {
        "SOURCE_ROOT": yc.source_path(),
        "WORKDIR": str(cwd),
        "MUSIC_API_URL": yc.get_param("music-api-url", DEFAULT_MUSIC_API_URL),
        "VINS_URL": yc.get_param("vins-url", DEFAULT_VINS_URL),
    }
    base_cfg_path = yc.binary_path("yandex_io/functional_tests/data_common/quasar.cfg")
    with open(base_cfg_path, 'r') as c:
        base_cfg = c.read()

    for pattern, value in cfg_extra.items():
        base_cfg = base_cfg.replace(f"${{{pattern}}}", value)

    cfg = json.loads(base_cfg)

    for _, service_config in cfg.items():
        if 'port' in service_config:
            service_config['port'] = pm.get_port()
        if 'httpPort' in service_config:
            service_config['httpPort'] = pm.get_port()

    cfg["glagold"]["externalPort"] = pm.get_port()
    cfg["audiod"]["dumpPath"] = str(dump_path) + "/"
    cfg["audiod"]["audioDevice"]["port"] = pm.get_port()
    cfg["glagold"]["externalPort"] = pm.get_port()
    cfg["aliced"]["speechkitDumpPath"] = str(dump_path) + "/"

    if experiments is not None:
        for experiment in experiments:
            cfg["aliced"]["experiments"].append(experiment)

    cfg_path = cwd.join(file_name)
    with open(cfg_path, "w+") as f:
        json.dump(cfg, f)
    return Config(cfg_path, cfg_extra, cfg)


@pytest.fixture(scope="function")
def config(cwd, pm, deviceid, dumpPath, is_remote_test, experiments):
    if is_remote_test:
        cfg_path = yc.get_param("config_path")
        if cfg_path is None:
            cfg_path = yc.source_path("yandex_io/functional_tests/quasar.cfg")
            if not os.path.isfile(cfg_path):
                raise Exception(
                    f"There is no quasar.cfg by default path: {cfg_path}. Use --test-param config_path to provide config for tests"
                )
        logger.debug(f"Get device quasar.cfg from: {cfg_path}")
        with open(cfg_path, "r") as f:
            cfg = f.read()
        config = Config(cfg_path, {}, json.loads(cfg))
        yield config
    else:
        config = prepare_config(cwd, pm, "quasar.cfg", dumpPath, experiments)

        deviceid_path = config.json["common"]["deviceIdFileName"]
        with open(deviceid_path, "w+") as f:
            f.write(deviceid)

        yield config

    if not is_remote_test:
        shutil.move(str(config.path), yc.test_output_path())


def get_authcode(sessionid, passport_client, config):
    authdConfig = config.json["authd"]
    xTokenClientId = authdConfig["xTokenClientId"]
    xTokenClientSecret = authdConfig["xTokenClientSecret"]
    return passport_client.getAccountManagerCode(sessionid, xTokenClientId, xTokenClientSecret)


@pytest.fixture(scope="function")
def authtoken(sessionid, passport_client, config):
    authdConfig = config.json["authd"]
    xTokenClientId = authdConfig["xTokenClientId"]
    xTokenClientSecret = authdConfig["xTokenClientSecret"]
    authCode = passport_client.getAccountManagerCode(sessionid, xTokenClientId, xTokenClientSecret)
    xToken = passport_client.getXToken(authCode, xTokenClientId, xTokenClientSecret)
    return passport_client.getOAuthToken(xToken, authdConfig["authTokenClientId"], authdConfig["authTokenClientSecret"])


@pytest.fixture(scope="session")
def adb_device(is_remote_test):
    if not is_remote_test:
        return None
    client = AdbClient()
    devices = client.devices()
    if not devices:
        logger.error('No adb device found')
        return None
    return devices[0]


@pytest.fixture(scope="function")
def deviceid(is_remote_test, adb_device):
    if not is_remote_test:
        user = getpass.getuser()
        rnd = uuid.uuid4().hex[:20]
        text = f"{user}_{rnd}_ft"
        if len(text) >= 48:
            text = text[-48:]
        return text
    else:
        deviceid_path = yc.source_path("yandex_io/functional_tests/device_id")
        if os.path.isfile(deviceid_path):
            with open(deviceid_path, "r+") as deviceid_file:
                return deviceid_file.readline().strip()
        else:
            return adb_device.serial


@pytest.fixture(scope="function")
def another_user(request, user_repo):
    if yc.get_param("session_id") is not None:
        return None
    return create_user(request, user_repo)


# fixtures for second user in tests
@pytest.fixture(scope="function")
def another_sessionid(another_user, passport_client, stage_logs_path):
    return make_sessionid(
        yc, another_user, passport_client, StageLogger(stage_logs_path, config=None, log_service_events=False)
    )


@pytest.fixture(scope="function")
def another_backend_client(config, sessionid, deviceid):
    client = make_backend_client(config, sessionid, deviceid)
    yield client


@pytest.fixture(scope="function")
def another_auth_code(another_sessionid, passport_client, config, another_backend_client):
    another_backend_client.dropUser()
    authCode = passport_client.getAccountManagerCode(
        another_sessionid, config.json["authd"]["xTokenClientId"], config.json["authd"]["xTokenClientSecret"]
    )
    yield authCode


@pytest.fixture(scope="session")
def is_remote_test():
    yield yc.get_param("remote_test") is not None and yc.get_param("remote_test") == "true"


@pytest.fixture(scope='session')
def use_tus():
    yield yc.get_param("use_tus") if yc.get_param("use_tus") is not None else False


@pytest.fixture(scope="session")
def testing_mode():
    return yc.get_param("testing_mode")


@pytest.fixture(autouse=True, scope="function")
def skip_by_testing_mode(request, testing_mode):
    marks = collect_pytest_marks(request)
    if testing_mode is not None and not any(map(lambda mark: mark.name in MODE_TO_MARKS[testing_mode], marks)):
        pytest.skip("Test does not support testing mode: {}".format(testing_mode))


@pytest.fixture(scope='function')
def device_count(request):
    return next((mark.args[0] for mark in collect_pytest_marks(request) if mark.name == "multidevice"), 1)


@pytest.fixture(scope='function')
def devices(
    device_count,
    cwd,
    pm,
    deviceid,
    backend_client,
    passport_client,
    sessionid,
    stage_logs_path,
    dumpPath,
    request,
    telemetry_file_path,
    experiments,
    is_remote_test,
):

    if is_remote_test:
        pytest.skip("Remote tests does not support multidevice mode")

    backend_client.dropUser()

    def get_auth_token(config):
        return passport_client.getAccountManagerCode(
            sessionid, config["authd"]["xTokenClientId"], config["authd"]["xTokenClientSecret"]
        )

    configs = [prepare_config(cwd, pm, "quasar_{}.cfg".format(i), dumpPath, experiments) for i in range(device_count)]
    device_ids = [deviceid + str(i) for i in range(device_count)]
    devices = []
    for i in range(device_count):
        config, device_id = configs[i], device_ids[i]
        auth_code = get_auth_token(config.json)
        args = default_sample_app_args(str(config.path), auth_code, config.extra, str(telemetry_file_path))
        args.extend(["--device-id", device_id, "--use-glagol"])
        for j in range(device_count):
            if i == j:
                continue
            args.extend(["--neighbor", "{}:{}".format(device_ids[j], configs[j].json["glagold"]["externalPort"])])
        normalized_nodeid = normalize_nodeid(request.node.nodeid)
        stdout_filepath = yc.get_unique_file_path(yc.output_path(), f"{normalized_nodeid}.sample_app_{i}.out")
        stderr_filepath = yc.get_unique_file_path(yc.output_path(), f"{normalized_nodeid}.sample_app_{i}.err")
        devices.append(
            YandexIODevice(
                device_id,
                run_sample_app(cwd, args, stdout_filepath, stderr_filepath),
                config.json,
                cwd,
                StageLogger(stage_logs_path, config.json, device_id="sample_app_{}".format(i)),
            )
        )
    controller = MultiDeviceController(devices, StageLogger(stage_logs_path, config=None, log_service_events=False))
    yield controller
    controller.stop_all()
