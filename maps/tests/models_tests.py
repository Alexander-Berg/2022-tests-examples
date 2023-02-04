from nose.tools import eq_

from maps.factory.pylibs.test_tools.utils import save_objects
from maps.factory.pylibs.models import service


def test_child_tasks_deletion(init_environment, session):
    parent = service.Task()
    child = service.Task()
    child.parent = parent
    save_objects(session, parent, child)

    session.delete(parent)
    eq_(session.query(service.Task).count(), 0)


def test_parent_tasks_deletion(init_environment, session):
    parent = service.Task()
    child = service.Task()
    child.parent = parent
    save_objects(session, parent, child)

    session.delete(child)
    eq_(session.query(service.Task).count(), 0)
