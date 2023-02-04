import sqlalchemy as sa
import pytest

from agency_rewards.rewards.utils.const import ARRunType
from agency_rewards.rewards.application import Application
from agency_rewards.rewards.scheme import runs


def set_finish_dt():
    """
    Проставить finish_dt для записи с типом regression в bo.t_ar_run
    """
    app = Application()
    new_session = app.new_session(database_id='meta')
    run_id = new_session.execute(
        sa.select([runs.c.id])
        .where(sa.and_(runs.c.type == ARRunType.Regression.value, runs.c.finish_dt.is_(None)))
        .order_by(runs.c.insert_dt.desc())
    ).fetchone()
    if run_id:
        new_session.execute(f'UPDATE bo.T_AR_RUN SET finish_dt = SYSDATE where id = {run_id[0]}')


def pytest_sessionfinish(session, exitstatus):
    """
    После того, как отработают все тесты, проставить finish_dt в bo.t_ar_run
    """
    set_finish_dt()


@pytest.hookimpl(tryfirst=True)
def pytest_keyboard_interrupt(excinfo):
    """
    При прерывании тестов проставить finish_dt в bo.t_ar_run
    """
    set_finish_dt()
