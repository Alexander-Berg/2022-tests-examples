import datetime

import pytest
from hamcrest import assert_that, equal_to, none, not_none, empty, is_
from sqlalchemy_continuum import Operation, version_class

from yb_darkspirit import scheme

FIRM_INN = "123"
DEFAULT_STRING = "string"
DEFAULT_INT = 1
DEFAULT_DATETIME = datetime.datetime.utcnow().replace(microsecond=0)


def _make_firm(inn=FIRM_INN):
    return scheme.Firm(inn=inn, title=DEFAULT_STRING, kpp=DEFAULT_STRING, ogrn=DEFAULT_STRING,
                       agent=False, sono_initial=DEFAULT_INT, sono_destination=DEFAULT_INT, hidden=False)


def _make_signer(first_name=DEFAULT_STRING, middle_name=DEFAULT_STRING, last_name=DEFAULT_STRING,
                 document=DEFAULT_STRING, main=True, hidden=False, certificate_filepath=DEFAULT_STRING,
                 certificate_expiration_date=DEFAULT_DATETIME, firm=None):
    return scheme.Signer(
        first_name=first_name, middle_name=middle_name, last_name=last_name, document=document,
        main=main, hidden=hidden, certificate_filepath=certificate_filepath,
        certificate_expiration_date=certificate_expiration_date, firm=firm,
    )


def test_continuum_create_versioning(session):
    with session.begin():
        firm = _make_firm('1111111')
        non_relation_props = dict(
            first_name=DEFAULT_STRING, middle_name=DEFAULT_STRING, last_name=DEFAULT_STRING,
            document=DEFAULT_STRING, main=True, hidden=False, certificate_filepath=DEFAULT_STRING,
            certificate_expiration_date=DEFAULT_DATETIME)
        signer = _make_signer(firm=firm, **non_relation_props)
        session.add(signer)

    changes = {key: [None, val] for key, val in non_relation_props.items()}
    changes['firm_inn'] = [None, firm.inn]
    changes['id'] = [None, signer.id]

    assert_that(signer.versions.count(), equal_to(1))
    assert_that(signer.versions[0].operation_type == Operation.INSERT)
    assert_that(signer.versions[0].changeset, equal_to(changes), 'Changeset is wrong')


def test_continuum_update_versioning(session):
    initial_fname, initial_lname, initial_firm = 'first_name1', 'last_name1', _make_firm('1111111')
    new_fname, new_lname, new_firm = 'first_name2', 'last_name2', _make_firm('2222222')

    with session.begin():
        signer = _make_signer(first_name=initial_fname, last_name=initial_lname, firm=initial_firm)
        session.add(signer)

    with session.begin():
        signer.first_name = new_fname
        signer.last_name = new_lname
        signer.firm = new_firm
        session.add(signer)

    changes = {'first_name': [initial_fname, new_fname], 'last_name': [initial_lname, new_lname],
               'firm_inn': [initial_firm.inn, new_firm.inn]}

    assert_that(signer.versions.count(), equal_to(2), 'New version does not appear')
    assert_that(signer.versions[1].operation_type == Operation.UPDATE)
    assert_that(signer.versions[1].changeset, equal_to(changes), 'Changeset is wrong')


def test_continuum_delete_versioning(session):
    SignerVersion = version_class(scheme.Signer)
    with session.begin():
        signer = _make_signer(firm=_make_firm('11111'))
        session.add(signer)
    signer_id = signer.id

    with session.begin():
        session.delete(signer)

    versions = session.query(SignerVersion).filter_by(id=signer_id).all()
    assert_that(len(versions), equal_to(2))
    assert_that(versions[1].operation_type, equal_to(Operation.DELETE))


def test_continuum_revert_versioning(session):
    initial_fname = 'initial'
    changed_fname = 'changed'
    SignerVersion = version_class(scheme.Signer)

    with session.begin():
        signer = _make_signer(first_name=initial_fname, firm=_make_firm('11111'))
        session.add(signer)
    signer_id = signer.id

    with session.begin():
        signer.first_name = changed_fname
        session.add(signer)

    with session.begin():
        session.delete(signer)
    assert_that(session.query(scheme.Signer).get(signer_id), none())

    versions = session.query(SignerVersion).filter_by(id=signer_id).all()
    assert_that(len(versions), equal_to(3))

    with session.begin():
        versions[1].revert()

    reverted_signer = session.query(scheme.Signer).get(signer_id)
    assert_that(reverted_signer, not_none())
    assert_that(reverted_signer.first_name, equal_to(changed_fname))

    with session.begin():
        versions[0].revert()

    reverted_signer = session.query(scheme.Signer).get(signer_id)
    assert_that(reverted_signer, not_none())
    assert_that(reverted_signer.first_name, equal_to(initial_fname))


@pytest.mark.parametrize('cls', [
    scheme.OFD, scheme.Firm, scheme.Signer, scheme.Address, scheme.WhiteSpirit,
    scheme.Registration, scheme.CashRegisterProcess
])
def test_versioned_schema(session, cls):
    continuum_columns = ('transaction_id', 'end_transaction_id', 'operation_type')
    version_cls = version_class(cls)

    # cols is a dict of column_name: column_type. For example {'title': 'VARCHAR(64)'})
    cols = {c.key: str(c.type) for c in list(cls.__table__.columns)}
    version_cols = {c.key: str(c.type) for c in list(version_cls.__table__.columns) if c.key not in continuum_columns}
    assert_that(cols, equal_to(version_cols))


def test_set_param(session):
    param = 'test_param'
    param_value = 'test'
    instance = scheme.Task(worker_id='test_worker', task_name='test_task', state='test_state', init_uuid='bla')
    with session.begin():
        session.add(instance)
        instance.set_param(param, param_value)
    session.expire(instance)

    assert_that({param: param_value}, equal_to(instance.params))


def test_get_params(session):
    param = 'test_param'
    param_value = 'test'
    instance = scheme.Task(worker_id='test_worker', task_name='test_task', state='test_state', init_uuid='bla')
    with session.begin():
        session.add(instance)
        instance.params = {param: param_value}
    session.expire(instance)
    assert_that(param_value, equal_to(instance.get_param(param)))


def test_get_last_finished_tasks_info_for_uuid(session):
    init_uuid = 'blabla'
    params = {'test_param': 'test'}
    worker_id = 'test_worker'
    task_name = 'test_task'
    state = scheme.TaskState.FINISHED.value
    instance = scheme.Task(worker_id=worker_id, task_name=task_name, state=state, init_uuid=init_uuid)

    with session.begin():
        session.add(instance)

    with session.begin():
        instance.params = params

    with session.begin():
        session.delete(instance)

    with session.begin():
        tasks = scheme.Task.get_last_finished_tasks_info_for_uuid(session, init_uuid)

    expected_task_info = scheme.TaskInfo(task_name=task_name, worker_id=worker_id, state=state, params=params)

    assert_that(expected_task_info, equal_to(tasks[0]))


def test_get_last_tasks_info_for_uuid_doesnt_fetch_other(session):
    instance = scheme.Task(
        worker_id='test_worker', task_name='test_task', state=scheme.TaskState.FINISHED.value, init_uuid='not_blabla'
    )

    with session.begin():
        session.add(instance)

    with session.begin():
        session.delete(instance)

    with session.begin():
        tasks = scheme.Task.get_last_finished_tasks_info_for_uuid(session, 'blabla')

    assert_that(tasks, is_(empty()))


def test_get_last_tasks_info_for_uuid_doesnt_fetch_not_finished(session):
    init_uuid = 'blabla'
    instance = scheme.Task(worker_id='test_worker', task_name='test_task', state='test_state', init_uuid=init_uuid)

    with session.begin():
        session.add(instance)

    with session.begin():
        session.delete(instance)

    with session.begin():
        tasks = scheme.Task.get_last_finished_tasks_info_for_uuid(session, init_uuid)

    assert_that(tasks, is_(empty()))
