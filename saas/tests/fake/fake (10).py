from typing import Tuple, List, Optional

from faker import Faker
from faker.providers import BaseProvider

from saas.library.python.common_functions.tests.fake import Provider as CommonProvider
from saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.config import DisasterAlertsConfig
from saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.services import Service, Tags


fake = Faker()
fake.add_provider(CommonProvider)


class Provider(BaseProvider):
    COMMON_GEOS = ['SAS', 'MAN', 'VLA']

    @staticmethod
    def get_config() -> DisasterAlertsConfig:
        return DisasterAlertsConfig(owners=[fake.random_string(20)], jinja_templates_root_dir=fake.random_string(20))

    def get_service(self) -> Service:
        tags: Tags = Tags(prj=fake.random_string(8))
        limits: Tuple[float, float] = (fake.random_int(min=0, max=100) % 100, fake.random_int(min=0, max=100) % 100)

        geos_cnt: int = fake.random_int(min=0, max=len(self.COMMON_GEOS))
        chosen_geos: List[str] = self.COMMON_GEOS[:geos_cnt]
        geos: Optional[Tuple[str]] = tuple(chosen_geos) if chosen_geos else None

        service: Service = Service(
            name=fake.random_string(16),
            ctype=fake.random_string(8),
            tags=tags,
            geos=geos,
            alert_limits=limits,

            warden_test_flow=True,
            warden_functionality_slug=fake.random_string(10),
        )
        return service

    def get_services(self) -> List[Service]:
        services: List[Service] = []
        for _ in range(fake.random_int(min=5, max=10)):
            service: Service = self.get_service()
            services.append(service)

        return services
