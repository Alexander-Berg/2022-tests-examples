import decimal
import json
import random
from functools import reduce
from datetime import datetime, timedelta
from typing import Dict

import pytz

from billing.apikeys.apikeys.tarifficator import Tariffication


def mskdate(*args, **kwargs):
    return pytz.utc.normalize(pytz.timezone('Europe/Moscow').localize(datetime(*args, **kwargs)))


class BaseTariffTest:

    def _run_test_case(self, tariff, start_dt, end_dt, statistic, payments, checkpoints, watchpoints=None,
                       skip_rate=0.0):
        watchpoints = watchpoints if watchpoints else {}
        tariffication = Tariffication(tariff["tarifficator_config"], FakeStatisticGetterProxy(statistic))
        state = {}
        current_date = start_dt
        passed_checkpoints = 0
        important_dates = [start_dt, end_dt] + list(statistic.keys()) + list(payments.keys()) + \
                          list(checkpoints.keys()) + list(watchpoints.keys())

        while end_dt >= current_date:
            if current_date in important_dates or skip_rate <= random.random():
                points, state = self._test_flow(state, current_date, tariffication, payments, checkpoints, watchpoints)
                passed_checkpoints += points
            current_date += timedelta(hours=1)

        assert passed_checkpoints == len(checkpoints), "Some checkpoints were not checked! Total: {}, passed: {}" \
                                                       "".format(len(checkpoints), passed_checkpoints)

    def _test_flow(self, state, current_date, tariffication, payments, checkpoints, watchpoints):
        WP_DEBUG = current_date in watchpoints

        self._print_debug(WP_DEBUG, "---", "---", watchpoints[current_date] if WP_DEBUG else '---')
        self._print_debug(WP_DEBUG, "Initial", current_date, state)

        if current_date in payments:
            for product, amount in list(payments[current_date].items()):
                state.setdefault("products", {})
                state["products"].setdefault(product, {})
                state["products"][product]["credited"] = \
                    str(decimal.Decimal(state["products"][product].get("credited", 0)) + decimal.Decimal(amount))
            self._print_debug(WP_DEBUG, "Payed", current_date, state)

        new_state = tariffication.execute(state, current_date)

        self._print_debug(WP_DEBUG, "Executed", current_date, new_state)

        passed_checkpoints = 0
        if current_date in checkpoints:
            for state_path, checkpoint_value in list(checkpoints[current_date].items()):
                state_value = self._get_state_value(new_state, state_path)
                assert state_value == checkpoint_value, "On {dt} it's expected {cpv} on path '{path}' but {stv} is " \
                                                        "found in {state}".format(
                                                            dt=current_date, cpv=checkpoint_value, stv=state_value,
                                                            path=state_path, state=new_state)
            passed_checkpoints += 1

        return passed_checkpoints, new_state

    @staticmethod
    def _get_state_value(state, path):
        path_map = path.split("__")
        return reduce(lambda d, p: d.get(p), path_map, state)

    @staticmethod
    def _print_debug(debug, msg, dt, state):
        if debug:
            print("{msg:10}\t{dt} ==> {state}".format(
                msg=msg, dt=dt.__repr__() if isinstance(dt, datetime) else dt, state=state))


class JSONEncoder(json.JSONEncoder):

    def default(self, o):
        if isinstance(o, (decimal.Decimal, datetime)):
            return str(o)
        return super().default(o)


class FakeStatisticGetterProxy:

    empty_stat = {
        "voice_unit": 0,
        "tts_unit": 0,
        "ner_unit": 0,
        "total": 0,
        "hits": 0
    }

    def __init__(self, statistic_array):
        self.statistic_array: Dict[datetime, dict] = statistic_array

    def __call__(self, state, date_from, date_to=None):
        assert isinstance(date_from, datetime)
        date_to = date_to or state['now']
        stat = self.empty_stat.copy()
        for statistic_date, statistic_item in list(self.statistic_array.items()):
            if date_from <= statistic_date < date_to:
                for unit, value in list(statistic_item.items()):
                    stat[unit] += value
        return stat
