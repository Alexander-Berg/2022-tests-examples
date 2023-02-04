from datetime import datetime

from analytics.plotter_lib.plotter import Plotter
from analytics.plotter_lib.utils import DATE_FORMAT


def get_yt_proxy(yt):
    return 'localhost:{}'.format(yt.yt_proxy_port)


def get_yql_proxy(yql_api):
    return 'localhost:{}'.format(yql_api.port)


def init_plotter(yt_proxy, yql_proxy):
    return Plotter(
        datetime.strptime('2019-10-20', DATE_FORMAT),
        datetime.strptime('2019-10-23', DATE_FORMAT),
        token='dummy_token',
        yt_proxy=yt_proxy,
        yql_proxy=yql_proxy,
        plot_classes=[],
    )


class Test_Plotter():
    def test_plotter_inited(self, yql_api, yt):
        plotter = init_plotter(get_yt_proxy(yt), get_yql_proxy(yql_api))
        plotter.build_flow_graph()
        plotter.get_optimized_flow_graph()
        assert True
