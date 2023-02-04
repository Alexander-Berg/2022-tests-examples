import logging
import typing as tp
from dataclasses import dataclass

from flask import Flask, request

from maps.infra.quotateka.cli.tests.mocks.flask_app import FlaskApp

logger = logging.getLogger(__name__)


@dataclass
class Member:
    uid: int
    scope_slug: str


@dataclass
class ServiceInfo:
    abc_id: int
    abc_slug: str
    members: tp.List[Member]


class Abc(FlaskApp):
    QUOTATEKA_ABC_SLUG = 'maps-core-quotateka'
    QUOTATEKA_ABC_ID = 7
    ADMINS_ROLE_SCOPE = 'administration'

    def __init__(self, listen_port: int) -> None:
        super().__init__(listen_port)
        self.services_by_slug = {}

    def add_service(self, abc_slug: str, abc_id: int) -> None:
        self.services_by_slug.setdefault(
            abc_slug,
            ServiceInfo(abc_slug=abc_slug, abc_id=abc_id, members=[]))

    def add_member(self, abc_slug: str, abc_id: int, uid: int, scope_slug: str) -> None:
        self.add_service(abc_slug, abc_id)
        self.services_by_slug[abc_slug].members.append(Member(uid=uid, scope_slug=scope_slug))

    def services(self, slug: tp.Optional[str] = None) -> tp.List[ServiceInfo]:
        if slug:
            if slug in self.services_by_slug:
                return [self.services_by_slug[slug]]
            return []
        return list(self.services_by_slug.values())

    def count_members(self,
                      uids: tp.Set[int],
                      abc_slugs: tp.Set[str],
                      scope_slugs: tp.Set[str]) -> int:
        count = 0
        for abc_slug, service in self.services_by_slug.items():
            for member in service.members:
                if abc_slugs and abc_slug not in abc_slugs:
                    continue
                if scope_slugs and member.scope_slug not in scope_slugs:
                    continue
                if uids and member.uid not in uids:
                    continue
                count += 1
        return count

    def create_app(self) -> Flask:
        app = Flask('abc')

        @app.route('/ping')
        def ping() -> str:
            return 'ok'

        @app.route('/api/v4/services/members/')
        def members() -> tp.Dict[str, tp.Any]:
            abc_slugs = set()
            scope_slugs = set()
            uids = set()

            if person_uids := request.args.get('person__uid__in', None):
                uids.union(set(int(uid) for uid in person_uids.split(',')))
            if person_uid := request.args.get('person__uid', None):
                uids.add(int(person_uid))
            if role_scopes := request.args.get('role__scope__slug__in', None):
                scope_slugs.union(set(role_scopes.split(',')))
            if role_scope := request.args.get('role__scope__slug', None):
                scope_slugs.add(role_scope)
            if service_slugs := request.args.get('service__slug__in', None):
                abc_slugs.union(set(service_slugs.split(',')))
            if service_slug := request.args.get('service__slug', None):
                abc_slugs.add(service_slug)

            results = [
                {'id': i}
                for i in range(self.count_members(
                    uids=uids,
                    abc_slugs=abc_slugs,
                    scope_slugs=scope_slugs,
                ))
            ]

            return {
                'next': None,
                'previous': None,
                'results': results
            }

        @app.route('/api/v4/services/')
        def services() -> tp.Dict[str, tp.Any]:
            service_slug = request.args.get('slug', None)
            results = self.services(slug=service_slug)

            return {
                'next': None,
                'previous': None,
                'results': [{'id': service.abc_id, 'slug': service.abc_slug} for service in results]
            }

        return app
