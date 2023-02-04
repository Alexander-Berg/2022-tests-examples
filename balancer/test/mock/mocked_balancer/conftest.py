from balancer.test.mock.mocked_balancer.mocked_balancer import BalancerWithMockedConfig
from balancer.test.mock.mocked_balancer.subheavy_ctx import SubheavyContext
from balancer.test.mock.mocked_balancer.term_ctx import TermContext
from balancer.test.util.config import BaseBalancerConfig
import pytest

subnet = '127.70.0.0/19'


@pytest.fixture
def balancer_static_fixture(request):
    yield BalancerWithMockedConfig(subnet)


@pytest.fixture
def balancer_fixture(request):
    balancer = BalancerWithMockedConfig(subnet)
    yield balancer
    balancer.stop()


@pytest.fixture
def subheavy_ctx():
    balancer = BalancerWithMockedConfig(subnet)
    ctx = SubheavyContext(balancer)
    yield ctx
    ctx.stopall()
    balancer.stop()


@pytest.fixture
def term_ctx():
    balancer = BalancerWithMockedConfig(subnet)
    ctx = TermContext(balancer)
    yield ctx
    ctx.stopall()
    balancer.stop()


class BaseJsonConfig(BaseBalancerConfig):
    def __init__(self, path):
        super(BaseJsonConfig, self).__init__()
        self.__path = path

    def get_path(self):
        return self.__path


@pytest.fixture
def balancer_parsed_config():
    conf = BaseJsonConfig(BalancerWithMockedConfig(subnet)._config_file_path)
    return conf.as_json()
