import unittest

from typing import List, Tuple, Optional

from faker import Faker

from saas.library.python.saas_alerts.golovan import golovan_alert_api, AlertTemplate
from saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.config import DisasterAlertsConfig
from saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.services import Service, Tags
from saas.tools.devops.saas_disaster_alerts.disaster_alerts.tests.fake import Provider

fake = Faker()
fake.add_provider(Provider)


class TestAlerts(unittest.TestCase):

    def test_services_template_correctness(self) -> None:
        config: DisasterAlertsConfig = fake.get_config()
        services: List[Service] = fake.get_services()
        content: str = ','.join([service.to_jinja_text() for service in services])

        services_content: str = f'<% set services = [{content}] %> << services >>'

        alert_template: AlertTemplate = AlertTemplate(config.alerts_template_key, config.owners, services_content)
        response_data: List[dict] = golovan_alert_api.render_new_json_template(template=alert_template)

        response_services = []
        for data in response_data:
            tags: Optional[Tags] = Tags(prj=data['tags'].get('prj')) if data.get('tags') else None
            geos: Optional[Tuple[str]] = tuple(data['geos']) if data.get('geos') else None

            alert_limits: Optional[Tuple[float, float]] = tuple(data['alert_limits']) \
                if data.get('alert_limits') else None

            response_services.append(
                Service(
                    name=data['name'].split('.')[-1],
                    ctype=data['ctype'],
                    tags=tags,
                    geos=geos,
                    alert_limits=alert_limits,
                    warden_test_flow=data['warden_test_flow'],
                    warden_functionality_slug=data['warden_functionality_slug'],
                )
            )

        actual: frozenset = frozenset(response_services)
        expected: frozenset = frozenset(service for service in services)

        self.assertEqual(expected, actual)
