import traceback
from logging import getLogger

from .platform.utils import process_action
from .platform.bunker import get_calc, diff
from .config import Config
from .utils.bunker import BunkerCalc
from .utils.startrek import TicketCtl
from .utils.yql_crutches import create_yql_client, create_yt_client, run_yql, validate_yql
from .utils.yt import YtTablesDiffer

logger = getLogger(__name__)


def update_ticket(ticket_ctl: TicketCtl, data: dict):
    """
    Обновляет статус тикета и оставляет коммент
    :param ticket_ctl: Proxy для работы  с тикетом
    :param data: содержит коммент
    """
    ticket_ctl.results = data
    ticket_ctl.leave_comment()
    ticket_ctl.change_status()
    if ticket_ctl.is_client_testing_ok():
        ticket_ctl.is_oked(change_status=False)
        ticket_ctl.set_tested_version()


def run_client_testing(calc: BunkerCalc, differ_class=YtTablesDiffer):
    """
    Запускает расчет (только YQL), получает разницу между результатом расчета и эталонными данными,
    пишет в виде коммента в тикет и меняет статус тикета на "В работе" или "Протестировано"
    """
    logger.info(f"Testing for {calc.name} is starting")

    yt_client = create_yt_client(Config.BALANCE_AR, Config.testing_cluster)
    yql_client = create_yql_client(Config.BALANCE_AR)
    ticket_ctl = TicketCtl(calc, None)

    share_url = None
    try:
        logger.info("processing pre-actions in testing - start")

        for action in calc.pre_actions:
            process_action(action, None, yt_client, yql_client, calc.env_type, Config.testing_cluster.name)

        logger.info("processing pre-actions in testing - done.")

        libs = calc.arcadia_libs
        logger.info(f"processing main task in testing, query: {calc.query}, libs: {libs}")

        yql_request = run_yql(calc.query, yql_client, Config.testing_cluster.name, libs)
        share_url = yql_request.share_url
        validate_yql(calc.query, yql_request, retry_count=5)
        differ = differ_class(
            calc.path,
            calc.correct_test_data,
            yt_client,
            yql_client,
            calc.is_need_payments_control_by_invoices,
            calc.is_need_payments_control,
        )
        result = differ.get_yt_tables_diff()
    except Exception:
        formatted_exc = traceback.format_exc()

        result = {'status': 'error', 'message': formatted_exc}

    if share_url:
        result['yql_url'] = share_url

    # если есть опубликованная версия
    # делаем дифф между опубликованной и тек. версиями
    published_version = get_calc(calc.full_name, calc.env_type, "stable")
    logger.info(f"published version: {published_version}")
    if published_version:
        result['prev_version'] = f'{published_version.version} (опубликованная версия)'
        result['diff'] = diff(published_version, calc)
    # если в атрибутах тикета есть пред. версия
    # делаем между ними diff
    else:
        prev_test_version = ticket_ctl.get_tested_version()
        logger.info(f"prev tested version: {prev_test_version}")
        if prev_test_version:
            result['prev_version'] = f'{prev_test_version} (ранее протестированная)'
            prev_calc = get_calc(calc.full_name, calc.env_type, prev_test_version)
            result['diff'] = diff(prev_calc, calc)

    logger.info(f"result: {result}")
    update_ticket(ticket_ctl, result)

    logger.info(f"Testing for {calc.full_name} is completed")
