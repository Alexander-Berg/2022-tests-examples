from uuid import uuid4

import factory
from wiki.pages.models import CloudPage, Page


class CloudPageFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = CloudPage

    @classmethod
    def make_cloud_page(cls, page: Page):
        return cls(
            page=page,
            cloud_src={
                'driveitem': {
                    'drive_id': 'b!R1UocfNXyU6bFmI8Bc5GDLERVSUjmrJKri57nSGRHnMpTM39UhheSJD3jlShBohv"',
                    'item_id': '01CFXCC6GQ3SD3TP75RBHI53TDAILEMBEL',
                },
                'document': {
                    'type': 'pptx',
                    'filename': 'Demo_Sprint_Kholodoque.pptx',
                    'relative_path': '/My_Presetations',
                },
                'embedding': {
                    'domain': 'yandexteam.sharepoint.com',
                    'namespace': 'sites/wiki-dev',
                    'sourcedoc': '{' + str(uuid4()).upper() + '}',
                },
            },
        )
