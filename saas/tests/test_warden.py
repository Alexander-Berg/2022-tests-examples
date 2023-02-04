import unittest
from functools import reduce

import mock

from faker import Faker

from saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.warden import WardenManager

from saas.library.python.common_functions.tests.fake import Provider as CommonProvider
from saas.library.python.warden.tests.fake import Provider as WardenProvider
from saas.tools.devops.saas_disaster_alerts.disaster_alerts.tests.fake import Provider

fake = Faker()
fake.add_provider(CommonProvider)
fake.add_provider(WardenProvider)
fake.add_provider(Provider)


class TestAlerts(unittest.TestCase):
    def setUp(self) -> None:
        patch = mock.patch('saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.warden.WardenManager._initialize')
        patch.start()

    def test_add_functionality(self):
        for test_flow in [False, True]:
            warden_manager: WardenManager = WardenManager(component_name=fake.random_string(10))

            fake_functionality_id = fake.random_string(10)
            functionality = fake.get_functionality(test_flow=test_flow)

            with mock.patch.multiple(
                'saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.warden.warden_api',
                add_functionality=mock.MagicMock(return_value={'functionalityId': fake_functionality_id}),
                update_functionality=mock.MagicMock(return_value={}),
                delete_functionality=mock.MagicMock(return_value={}),
            ):
                warden_manager.create_or_update_functionality(functionality)

            self.assertIsNotNone(warden_manager._functionality_slug_to_info.get(functionality.slug))
            self.assertEqual(len(warden_manager._functionality_slug_to_info), 1)
            self.assertEqual(warden_manager._functionality_slug_to_info[functionality.slug], (fake_functionality_id, 1))

    def test_update_functionality(self):
        for test_flow in [False, True]:
            warden_manager: WardenManager = WardenManager(component_name=fake.random_string(10))

            fake_functionality_id = fake.random_string(10)
            functionality = fake.get_functionality(test_flow=test_flow)

            warden_manager._functionality_slug_to_info = {functionality.slug: (fake_functionality_id, 0)}

            with mock.patch.multiple(
                'saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.warden.warden_api',
                add_functionality=mock.MagicMock(return_value={'functionalityId': fake_functionality_id}),
                update_functionality=mock.MagicMock(return_value={}),
                delete_functionality=mock.MagicMock(return_value={}),
            ):
                warden_manager.create_or_update_functionality(functionality)

            self.assertIsNotNone(warden_manager._functionality_slug_to_info.get(functionality.slug))
            self.assertEqual(len(warden_manager._functionality_slug_to_info), 1)
            self.assertEqual(warden_manager._functionality_slug_to_info[functionality.slug], (fake_functionality_id, 1))

    def test_update_after_add_functionality(self):
        for test_flow in [False, True]:
            warden_manager: WardenManager = WardenManager(component_name=fake.random_string(10))

            fake_functionality_id = fake.random_string(10)
            functionality = fake.get_functionality(test_flow=test_flow)

            with mock.patch.multiple(
                'saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.warden.warden_api',
                add_functionality=mock.MagicMock(return_value={'functionalityId': fake_functionality_id}),
                update_functionality=mock.MagicMock(return_value={}),
                delete_functionality=mock.MagicMock(return_value={}),
            ):
                warden_manager.create_or_update_functionality(functionality)

                if not test_flow:
                    with self.assertRaises(ValueError):
                        warden_manager.create_or_update_functionality(functionality)
                else:
                    warden_manager.create_or_update_functionality(functionality)

            self.assertIsNotNone(warden_manager._functionality_slug_to_info.get(functionality.slug))
            self.assertEqual(len(warden_manager._functionality_slug_to_info), 1)
            self.assertEqual(warden_manager._functionality_slug_to_info[functionality.slug], (fake_functionality_id, 1))

    def test_double_update(self):
        for test_flow in [False, True]:
            warden_manager: WardenManager = WardenManager(component_name=fake.random_string(10))

            fake_functionality_id = fake.random_string(10)
            functionality = fake.get_functionality(test_flow=test_flow)

            warden_manager._functionality_slug_to_info = {functionality.slug: (fake_functionality_id, 0)}

            with mock.patch.multiple(
                'saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.warden.warden_api',
                add_functionality=mock.MagicMock(return_value={'functionalityId': fake_functionality_id}),
                update_functionality=mock.MagicMock(return_value={}),
                delete_functionality=mock.MagicMock(return_value={}),
            ):
                warden_manager.create_or_update_functionality(functionality)

                if not test_flow:
                    with self.assertRaises(ValueError):
                        warden_manager.create_or_update_functionality(functionality)
                else:
                    warden_manager.create_or_update_functionality(functionality)

            self.assertIsNotNone(warden_manager._functionality_slug_to_info.get(functionality.slug))
            self.assertEqual(len(warden_manager._functionality_slug_to_info), 1)
            self.assertEqual(warden_manager._functionality_slug_to_info[functionality.slug], (fake_functionality_id, 1))

    def test_delete_functionalities(self):
        warden_manager: WardenManager = WardenManager(component_name=fake.random_string(10))
        functionalities_cnt = fake.random.randint(15, 20)

        def get_updates_cnt_from_idx(i):
            if i < fake.random.randint(2, 4):
                return 0
            elif i >= functionalities_cnt - fake.random.randint(2, 4):
                return 1
            else:
                return fake.random.randint(0, 1)

        for idx in range(functionalities_cnt):
            id_ = fake.random_string(10)
            slug = fake.random_string(10)
            updates_cnt = get_updates_cnt_from_idx(idx)

            warden_manager._functionality_slug_to_info[slug] = id_, updates_cnt
            warden_manager._functionality_slug_to_visited[slug] = bool(updates_cnt)

        recorded_before = len(warden_manager._functionality_slug_to_info.keys())
        unvisited_cnt = reduce(
            lambda prev, _next: prev + (_next[1] == 0),
            warden_manager._functionality_slug_to_info.values(),
            0
        )

        with mock.patch.multiple(
            'saas.tools.devops.saas_disaster_alerts.disaster_alerts.modules.warden.warden_api',
            delete_functionality=mock.MagicMock(return_value={}),
        ):
            warden_manager.delete_unvisited_functionalities()

        recorded_after = len(warden_manager._functionality_slug_to_info.keys())

        self.assertEqual(recorded_after, recorded_before - unvisited_cnt)
