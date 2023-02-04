import sys
from io import StringIO

from contextlib2 import ExitStack
from flask import current_app
from hamcrest.core.assert_that import assert_that
from hamcrest.core.core.isequal import equal_to
from unittest.mock import patch

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.common.compat import session_compat
from intranet.yandex_directory.src.yandex_directory.common.components import component_registry
from intranet.yandex_directory.src.yandex_directory.meta.commands.import_registrars import Command

SAMPLE_DATA = '''
{"id":3292088,"new":1,"admin_id":245350767,"password":"oioiausfuisdyfiudasyfa","name":"InfoboxTest","validate_domain_callback":"https://yandex.pa.infobox.ru/domains/payed","delete_domain_callback":"https://yandex.pa.infobox.ru/domains/deleted","added_domain_callback":"https://yandex.pa.infobox.ru/domains/registration_started","verified_domain_callback":"https://yandex.pa.infobox.ru/domains/added", "plength": 10, "iv": "asdasdasd"}
{"id":737508,"new":0,"admin_id":6022964,"password":"askhkahiufyaiusfyusaydtf","name":"yml-shop.ru","validate_domain_callback":"","delete_domain_callback":"","added_domain_callback":"","verified_domain_callback":"", "plength": 0, "iv": ""}
'''

DOMAIN_DATA = '''
{"pdd_version":"old","admin_id":123,"domain":"test.ru","token":"43c1df85fd52d1d1f9904c06e7f08c933ca165290964416d4aa452bd"}
{"pdd_version":"new","admin_id":456,"domain":"test.ru","token":"6c27d8d2deb3d1294a767d1d1ae996c2cce11af03ea077d668545f47"}
'''

class TestImportRegistrarsCommandCase(TestCase):
    create_organization = False

    def test_dry_run(self):
        with ExitStack() as exit_stack:
            meta_session = exit_stack.enter_context(
                session_compat(self.meta_connection),
            )

            exit_stack.enter_context(patch.object(
                target=component_registry(),
                attribute='meta_session',
                new=meta_session,
            ))

            exit_stack.enter_context(patch.object(
                target=sys,
                attribute='stdin',
                new=StringIO(SAMPLE_DATA),
            ))

            command = Command()
            command(current_app, dry_run=True, domain_tokens=False)

            assert_that(command.registrar_repository.count(), equal_to(0))

    def test_not_dry_run(self):
        with ExitStack() as exit_stack:
            meta_session = exit_stack.enter_context(
                session_compat(self.meta_connection),
            )

            exit_stack.enter_context(patch.object(
                target=component_registry(),
                attribute='meta_session',
                new=meta_session,
            ))

            exit_stack.enter_context(patch.object(
                target=sys,
                attribute='stdin',
                new=StringIO(SAMPLE_DATA),
            ))

            command = Command()
            command(current_app, dry_run=False, domain_tokens=False)

            assert_that(command.registrar_repository.count(), equal_to(2))


class TestImportDomainTokensCommandCase(TestCase):
    create_organization = False

    def test_not_dry_run(self):
        with ExitStack() as exit_stack:
            meta_session = exit_stack.enter_context(
                session_compat(self.meta_connection),
            )

            exit_stack.enter_context(patch.object(
                target=component_registry(),
                attribute='meta_session',
                new=meta_session,
            ))

            exit_stack.enter_context(patch.object(
                target=sys,
                attribute='stdin',
                new=StringIO(DOMAIN_DATA),
            ))

            command = Command()
            command(current_app, dry_run=False, domain_tokens=True)

            assert_that(command.domain_token_repository.count(), equal_to(2))
