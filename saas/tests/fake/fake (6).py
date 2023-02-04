# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six
import json
import datetime

from faker import Faker
from faker.providers import BaseProvider

from library.python import resource

from saas.library.python.common_functions import DC_LOCATIONS, random_hexadecimal_string
from saas.library.python.gencfg.tests.fake.fake import Provider as GencfgProvider
from saas.library.python.nanny_rest.resource import SandboxFile, StaticFile
from saas.library.python.nanny_rest.tickets_integration import SimpleTicketIntegrationRule

fake = Faker()
fake.add_provider(GencfgProvider)


class Provider(BaseProvider):
    # noinspection PyMethodMayBeStatic
    def snapshot_id(self):
        return random_hexadecimal_string(40)

    # noinspection PyMethodMayBeStatic
    def nanny_service_name(self, delimiter=None):
        if delimiter is None:
            delimiter = '_' if fake.pybool() else '-'
        word_number = fake.random.randint(1, 5)
        return delimiter.join(fake.words(word_number, unique=True))

    # noinspection PyMethodMayBeStatic
    def instance_allocation_type(self):
        return 'EXTENDED_GENCFG_GROUPS' if fake.pybool() else 'YP_POD_IDS'

    # noinspection PyMethodMayBeStatic
    def _gencfg_instances(self, geo=None, base_name='', prefix_geo=False):
        geo_names = tuple(DC_LOCATIONS)

        if not base_name:
            base_name_words = fake.random.randint(1, 3)
            base_name = '_'.join(fake.words(base_name_words))
        base_name = base_name.upper()

        if isinstance(geo, six.string_types):
            geo = geo.upper().split(',')
        elif isinstance(geo, (list, tuple)):
            geo = [g.upper() for g in geo]
        else:
            geo = fake.random_elements(geo_names, unique=True)

        gencfg_group_names = [fake.gencfg_group_name(geo=g, base_name=base_name, prefix_geo=prefix_geo) for g in geo]
        gencfg_tags = [fake.gencfg_tag_name(nanny_format=b) for b in [True]*len(gencfg_group_names)]

        result = []
        for i, g in enumerate(gencfg_group_names):
            result.append({
                "name": g,
                "release": gencfg_tags[i],
                "limits": {"io_policy": "normal", "cpu_policy": "normal"},
                "cpu_set_policy": "SERVICE",
                "tags": []
            })

        return result

    def _attrs_meta(self, parent):
        created_before = datetime.datetime.now()

        if isinstance(parent, dict):
            created_after = datetime.datetime.utcfromtimestamp(parent['change_info']['ctime']/1000)
            parent = parent['_id']
        else:
            created_after = created_before - datetime.timedelta(days=30)
            if not parent:
                parent = self.snapshot_id()

        ctime = fake.unix_time(end_datetime=created_before, start_datetime=created_after) * 1000 + fake.random_int(min=0, max=999)

        return {
            'parent': parent,
            'ctime': ctime
        }

    # noinspection PyMethodMayBeStatic
    def runtime_attributes_content(self, instance_allocation=None):
        instance_allocation = instance_allocation if instance_allocation else 'YP_POD_IDS'
        base_runtime_attrs_content = json.loads(resource.find('saas/library/nanny/fake/runtime_attrs_content.json'))
        base_runtime_attrs_content['instances']['chosen_type'] = instance_allocation
        base_runtime_attrs_content['instances']['extended_gencfg_groups']['groups'] = self._gencfg_instances()
        return base_runtime_attrs_content

    def runtime_attributes(self, parent=None, instance_allocation=None):
        instance_allocation = instance_allocation if instance_allocation is not None else self.instance_allocation_type()
        meta = self._attrs_meta(parent)
        return {
            "content": self.runtime_attributes_content(instance_allocation=instance_allocation),
            "parent_id": meta['parent'],
            "_id": self.snapshot_id(),
            "change_info": {
                "comment": fake.sentence(),
                "ctime": meta['ctime'],
                "author": fake.word()
            },
            "meta_info": {
                "startrek_tickets": [],
                "is_disposable": False,
                "infra_notifications": {},
                "scheduling_config": {"scheduling_priority": "NONE"},
                "conf_id": "saas_production_searchproxy_market-1575304628575",
                "annotations": {
                    "startable": "true", "deploy_engine": "ISS_MULTI"
                }
            }
        }

    # noinspection PyMethodMayBeStatic
    def info_attributes_content(self):
        return json.loads(resource.find('saas/library/nanny/fake/info_attrs_content.json'))

    def info_attributes(self, parent=None):
        meta = self._attrs_meta(parent)
        return {
            "content": self.info_attributes_content(),
            "parent_id": meta['parent'],
            "_id": self.snapshot_id(),
            "change_info": {
                "comment": fake.sentence(),
                "ctime": meta['ctime'],
                "author": fake.word()
            }
        }

    # noinspection PyMethodMayBeStatic
    def auth_attributes_content(self):
        return {
            "observers": {"logins": [], "groups": []},
            "owners": {
                "logins": [
                    "anikella",
                    "saku",
                    "i024",
                    "salmin"
                ],
                "groups": ["29985"]
            },
            "conf_managers": {
                "logins": [],
                "groups": ["26994", "98775"]
            },
            "ops_managers": {
                "logins": [],
                "groups": ["26994", "98775"]
            }
        }

    def auth_attributes(self, parent=None):
        meta = self._attrs_meta(parent)
        return {
            "content": self.auth_attributes_content(),
            "_id": self.snapshot_id(),
            "change_info": {
                "comment": fake.sentence(),
                "ctime": meta['ctime'],
                "author": fake.word()
            }
        }

    # noinspection PyMethodMayBeStatic
    def _local_path(self, local_path=None):
        if local_path is not None:
            return local_path
        else:
            local_path_words = fake.random.randint(1, 7)
            local_path = '_'.join(fake.words(local_path_words))
            return local_path

    def _sandbox_name(self, value=None):
        return self._local_path(value).upper()

    def static_file_resource(self, local_path=None, content=None):
        local_path = self._local_path(local_path)
        content = content if content is not None else fake.text()
        return StaticFile(local_path, content)

    def sandbox_file_resource(self, local_path=None, task_type=None, resource_type=None):
        local_path = self._local_path(local_path)
        task_type = self._sandbox_name(task_type)
        resource_type = self._sandbox_name(resource_type)
        return SandboxFile(local_path,
                           task_type, self.random_number(digits=9),
                           resource_type, self.random_number(digits=10))

    def simple_tickets_integration_rule(self, task_type=None, resource_type=None):
        return SimpleTicketIntegrationRule(
            self._sandbox_name(task_type),
            self._sandbox_name(resource_type),
            fake.sentence(),
        )
