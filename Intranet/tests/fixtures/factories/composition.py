import factory
import pytest

from watcher.db import (
    Composition,
    CompositionParticipants,
    CompositionToStaff,
    CompositionToStaffExcluded,
    CompositionToRole,
    CompositionToRoleExcluded,
    CompositionToScope,
)
from .base import (
    COMPOSITION_PARTICIPANTS_SEQUENCE,
    COMPOSITION_SEQUENCE,
    COMPOSITION_TO_ROLE_EXCLUDED_SEQUENCE,
    COMPOSITION_TO_ROLE_SEQUENCE,
    COMPOSITION_TO_SCOPE_SEQUENCE,
    COMPOSITION_TO_STAFF_EXCLUDE_SEQUENCE,
    COMPOSITION_TO_STAFF_SEQUENCE,
)


@pytest.fixture(scope='function')
def composition_factory(meta_base, service_factory):
    class CompositionFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Composition

        id = factory.Sequence(lambda n: n + COMPOSITION_SEQUENCE)
        name = factory.Sequence(lambda n: f"composition name {n}")
        slug = factory.Sequence(lambda n: f"composition_slug_{n}")
        service = factory.SubFactory(service_factory)

    return CompositionFactory


@pytest.fixture(scope='function')
def composition_participants_factory(meta_base, composition_factory, staff_factory):
    class CompositionParticipantsFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = CompositionParticipants

        id = factory.Sequence(lambda n: n + COMPOSITION_PARTICIPANTS_SEQUENCE)
        composition = factory.SubFactory(composition_factory)
        staff = factory.SubFactory(staff_factory)

    return CompositionParticipantsFactory


@pytest.fixture(scope='function')
def composition_to_staff_factory(meta_base, composition_factory, staff_factory):
    class CompositionToStaffFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = CompositionToStaff

        id = factory.Sequence(lambda n: n + COMPOSITION_TO_STAFF_SEQUENCE)
        composition = factory.SubFactory(composition_factory)
        staff = factory.SubFactory(staff_factory)

    return CompositionToStaffFactory


@pytest.fixture(scope='function')
def composition_to_staff_excluded_factory(meta_base, composition_factory, staff_factory):
    class CompositionToStaffExcludedFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = CompositionToStaffExcluded

        id = factory.Sequence(lambda n: n + COMPOSITION_TO_STAFF_EXCLUDE_SEQUENCE)
        composition = factory.SubFactory(composition_factory)
        staff = factory.SubFactory(staff_factory)

    return CompositionToStaffExcludedFactory


@pytest.fixture(scope='function')
def composition_to_role_factory(meta_base, composition_factory, role_factory):
    class CompositionToRoleFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = CompositionToRole

        id = factory.Sequence(lambda n: n + COMPOSITION_TO_ROLE_SEQUENCE)
        composition = factory.SubFactory(composition_factory)
        role = factory.SubFactory(role_factory)

    return CompositionToRoleFactory


@pytest.fixture(scope='function')
def composition_to_role_excluded_factory(meta_base, composition_factory, role_factory):
    class CompositionToRoleExcludedFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = CompositionToRoleExcluded

        id = factory.Sequence(lambda n: n + COMPOSITION_TO_ROLE_EXCLUDED_SEQUENCE)
        composition = factory.SubFactory(composition_factory)
        role = factory.SubFactory(role_factory)

    return CompositionToRoleExcludedFactory


@pytest.fixture(scope='function')
def composition_to_scope_factory(meta_base, composition_factory, scope_factory):
    class CompositionToScopeFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = CompositionToScope

        id = factory.Sequence(lambda n: n + COMPOSITION_TO_SCOPE_SEQUENCE)
        composition = factory.SubFactory(composition_factory)
        scope = factory.SubFactory(scope_factory)

    return CompositionToScopeFactory
