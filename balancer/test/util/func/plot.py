# -*- coding: utf-8 -*-
import os
import json
import allure
from collections import namedtuple
from balancer.test.util import settings
from balancer.test.util.func import html


StepColor = namedtuple('StepColor', ['step_color', 'gap_color'])
AreaColor = namedtuple('AreaColor', ['color', 'opacity'])


STEP_COLORS = [
    StepColor('rgba(255, 255, 255, 1.0)', 'rgba(211, 211, 211, 0.1'),
    StepColor('rgba(68, 170, 213, 0.01)', 'rgba(68, 170, 213, 0.1)'),
]


def get_res(name):
    return settings.get_resource(
        py_path=os.path.join(os.path.dirname(os.path.abspath(__file__)), name),
        ya_path='func/' + name,
    )


HIGHCHARTS = get_res('highcharts.js')
HIGHCHARTS_MORE = get_res('highcharts-more.js')
EXPORTING = get_res('exporting.js')


class ZIndex(object):
    LINE = 5
    AREA_RANGE = 2
    ERROR = 1
    GAP = 0
    STEP = 0


class AreaColor(object):
    OK = AreaColor('green', 0.2)
    FAIL = AreaColor('red', 0.4)


class Plot(object):
    def __init__(self):
        super(Plot, self).__init__()
        self.__lines = dict()
        self.__area_range = dict()
        self.__gaps = list()
        self.__steps = list()
        self.name = None

    def add_line(self, line):
        key = line.name, line.interval
        if key not in self.__lines:
            self.__lines[key] = line

    def add_ok_area(self, bot, top, name=None):
        self.__add_area(bot, top, AreaColor.OK, name)

    def add_fail_area(self, bot, top, name=None):
        self.__add_area(bot, top, AreaColor.FAIL, name)

    def __add_area(self, bot, top, color, name=None):
        key = bot.name, bot.interval, top.name, top.interval
        if key not in self.__area_range:
            if name is None:
                name = '{} -- {}'.format(bot.name, top.name)
            self.__area_range[key] = (name, bot, top, color)

    def add_gap(self, interval):
        self.__gaps.append(interval)

    def add_step(self, step):
        self.__steps.append(step)

    def render(self):
        result = list()
        for line in self.__lines.values():
            result.append({
                'name': line.name,
                'data': line.plot_points,
                'zIndex': ZIndex.LINE
            })
        for name, bot, top, color in self.__area_range.values():
            x_values = [p[0] for p in (top - bot).plot_points]
            data = [(x, bot.call_fuzzy(x), top.call_fuzzy(x)) for x in x_values]
            result.append({
                'name': name,
                'data': data,
                'type': 'arearange',
                'lineWidth': 0,
                'linkedTo': ':previous',
                'fillOpacity': color.opacity,
                'zIndex': ZIndex.AREA_RANGE,
                'color': color.color,
            })
        return result

    def render_bands(self):
        result = list()
        for gap in self.__gaps:
            result.append({
                'color': 'rgba(220, 220, 220, 0.5)',
                'from': gap.start,
                'to': gap.fin,
                'zIndex': ZIndex.GAP,
            })
        color_id = 0
        for step in self.__steps:
            color = STEP_COLORS[color_id]
            result.extend([
                {
                    'color': color.gap_color,
                    'from': step.start,
                    'to': step.start + step.setup,
                    'zIndex': ZIndex.STEP,
                },
                {
                    'color': color.step_color,
                    'from': step.start + step.setup,
                    'to': step.fin - step.teardown,
                    'label': {
                        'text': step.name,
                    },
                    'zIndex': ZIndex.STEP,
                },
                {
                    'color': color.gap_color,
                    'from': step.fin - step.teardown,
                    'to': step.fin,
                    'zIndex': ZIndex.STEP,
                }
            ])
            color_id = (color_id + 1) % 2
        return result

    def attach(self):
        allure.attach(
            self.name,
            html.HTML % (
                HIGHCHARTS,
                HIGHCHARTS_MORE,
                EXPORTING,
                json.dumps(self.render_bands()),
                json.dumps(self.render())
            ),
            allure.attach_type.HTML
        )
