"""
Генерация тестовых данных для регрессионных тестов
"""

import os
import shutil
import sys
import signal
import datetime

from agency_rewards.rewards.mapper import Run
from agency_rewards.rewards.utils.argument_parsers import parse_generate_mode
from agency_rewards.rewards.utils.interrupt_handler import getInterruptHandler
from agency_rewards.rewards.config import Config
from agency_rewards.rewards.application import Application
from agency_rewards.rewards.scheme import acts, paid_periods
from agency_rewards.rewards.utils.yql_crutches import (
    create_yql_client,
    create_yt_client,
)
from agency_rewards.rewards.butils.session import Session
from agency_rewards.rewards.utils.check_active_runs import active_run_exists
from agency_rewards.rewards.utils.const import ARRunType

from ..generators.m import belarus, kazakhstan, domains, market  # noqa
from ..generators.q import base, base_direct, base_light, prof, prof_direct  # noqa
from ..generators.hy import base_hy, base_light_hy, prof_hy  # noqa
from ..generators.tasks import balance34491  # noqa
from ..generators.tasks import balance33913  # noqa
from ..generators.tasks import balance34278  # noqa
from ..generators.tasks import balance33711  # noqa
from ..generators.tasks import balance34499  # noqa
from ..generators.tasks import balance34675  # noqa
from ..generators.tasks import balance35071  # noqa
from ..generators.tasks import balance36935  # noqa
from ..generators.tasks import balance36694  # noqa
from ..generators.tasks import balance39025  # noqa
from ..generators.tasks import balance39089  # noqa
from ..generators.tasks import balance39264  # noqa
from ..generators.tasks import balance39628  # noqa
from ..generators.tasks import statkeyar11  # noqa
from ..generators.utils import platform, cashback  # noqa
from ..generators import test_rewards_errors_detection  # noqa
from ..generators import test_report_differences  # noqa

from billing.agency_rewards.tests_platform.common import TestBase
from billing.agency_rewards.tests_platform.common.scheme import (
    v_opt_2015_acts_last_month,
    v_opt_2015_acts_2_month_ago,
    agency_stats,
    rewards,
    rewards_history,
    payments,
    v_ar_acts_hy,
    v_ar_acts_q_ext,
    domains_stats,
    fin_docs,
    regtest_data,
)


def setup_models(session: Session, run: Run):
    """
    Очищение таблиц, генерация данных для расчетов
    """
    try:
        for model in (
            v_opt_2015_acts_last_month,
            v_opt_2015_acts_2_month_ago,
            acts,
            v_ar_acts_hy,
            rewards,
            rewards_history,
            payments,
            paid_periods,
            agency_stats,
            domains_stats,
            v_ar_acts_q_ext,
            fin_docs,
            regtest_data,
        ):
            print('>>>> setup, model: {}'.format(model))
            model.drop(session.connection(), checkfirst=True)
            model.create(session.connection(), checkfirst=True)

            if model in (rewards,):
                for column in ('turnover_to_charge', 'turnover_to_pay', 'reward_to_charge', 'reward_to_pay'):
                    session.execute(f'alter table {model.fullname} modify {column} number')
        clients = {}
        for cluster in Config.clusters:
            if cluster not in clients:
                clients[cluster] = {}
            clients[cluster]["yt"] = create_yt_client(Config.BALANCE_AR, cluster=cluster)
            clients[cluster]["yql"] = create_yql_client(Config.BALANCE_AR)

        for cls in TestBase.__subclasses__():
            # cls.setup_fixtures(session)
            setup = getattr(cls, "setup_fixtures", None)
            if callable(setup) and setup is not TestBase.setup_fixtures:
                print('>>>> setup, fixtures: {}'.format(cls))
                setup(session)

            setup_ext = getattr(cls, "setup_fixtures_ext", None)
            if callable(setup_ext) and setup_ext is not TestBase.setup_fixtures_ext:
                for cluster in Config.clusters:
                    print(f'>>>> setup, fixtures: {cls} [ext, cluster={cluster}]')
                    setup_ext(session, clients[cluster]["yt"], clients[cluster]["yql"])

            cls.pickle_data(session)
    except Exception:
        with session.begin():
            run.finish_dt = datetime.datetime.now()
        raise


def clear_tmp_emails():
    """
    Очищаем папку со сброшенными во время предыдущей регрессии письмами
    """
    if os.path.exists(Config.regression_email_path):
        shutil.rmtree(Config.regression_email_path)


def main():
    app = Application()
    env_type = app.get_current_env_type()

    if env_type != 'dev':
        print('Only for development env, exit! (current env: {})'.format(env_type))
        sys.exit(2)

    opt = parse_generate_mode()
    session = app.new_session(database_id='meta')
    if opt.check_active_run and (
        active_run_exists(session, [ARRunType.Regression.value])
        or active_run_exists(session, [ARRunType.Prod.value, ARRunType.Cashback.value])
    ):
        print('>>>> There already is an active run.')
        sys.exit(1)
    clear_tmp_emails()
    insert_dt = opt.insert_dt - datetime.timedelta(seconds=1)
    print(f"regression insert_dt: {insert_dt}; calc insert dt: {opt.insert_dt}")
    run = Run(opt.run_dt, insert_dt, insert_dt, type=ARRunType.Regression.value, args=str(vars(opt)))
    with session.begin():
        session.add(run)
    keyboardInterruptHandler = getInterruptHandler(session, run)
    signal.signal(signal.SIGINT, keyboardInterruptHandler)
    setup_models(session, run)
    print('>>>> setup, done.')
