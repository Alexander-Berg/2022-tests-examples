import pretend
import pytest

from plan.roles.models import Role
from plan.services.checkers import ServicePermissionChecker, service_checker
from common import factories


@pytest.fixture
def data(db, owner_role):
    other = factories.ServiceFactory(name='other')
    meta = factories.ServiceFactory(name='meta')
    service = factories.ServiceFactory(parent=meta, name='service')

    fixture = pretend.stub(
        other=other,
        metaservice=meta,
        service=service,
        owner_role=owner_role,
    )

    for s in (service, meta, other):
        m = factories.ServiceMemberFactory(
            service=s,
            role=owner_role,
        )
        # Responsible?

        attr_name = '{}_head'.format(s.name)
        m.staff.login = attr_name
        m.staff.save()

        setattr(fixture, attr_name, m)
        s.owner = m.staff
        s.save()

    create_roles = {
        'deputy': Role.DEPUTY_OWNER,
        'slave': 'slave',
        'cripple': 'cripple',
    }
    for slug, role_code in create_roles.items():
        r = factories.RoleFactory(name=role_code.capitalize(), code=role_code)
        setattr(fixture, 'role_{}'.format(slug), r)

        for s in (service, meta, other):
            m = factories.ServiceMemberFactory(
                service=s,
                role=r,
            )

            attr_name = '{service}_{quality}_{slug}'.format(
                service=s.name,
                quality='regular',
                slug=slug,
            )

            m.staff.login = attr_name
            m.staff.save()

            setattr(fixture, attr_name, m)

    return fixture


def assert_bound_unbound(checker_name, service, sender, obj, expected):
    unbound_checker = getattr(service_checker, checker_name)
    assert unbound_checker(obj, sender) == expected, (
        'unbound checker {} failed: '.format(checker_name))

    bound_checker = getattr(ServicePermissionChecker(service, sender),
                            checker_name)
    assert bound_checker(obj, sender) == expected, (
        'bound checker {} failed'.format(checker_name))


def assert_can_approve_member(sender, member, expected):
    return assert_bound_unbound(
        checker_name='can_approve_member',
        service=member.service,
        sender=sender,
        obj=member,
        expected=expected,
    )
