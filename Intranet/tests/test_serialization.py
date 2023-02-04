import os
from unittest import TestCase
from unittest.mock import patch
from django.http import JsonResponse

from intranet.magiclinks.src.links.utils.serializer import MagicLinksSerializer
from intranet.magiclinks.src.links.dto import BinaryResource


class SerializationTestCase(TestCase):

    def test_serialization_success(self):
        path = os.path.abspath(__file__)
        with patch.object(BinaryResource, 'path_to_icon') as path_to_icon:
            path_to_icon.return_value = 'intranet/magiclinks/tests/resources/test_serialization/test_favicon.ico'
            resource = BinaryResource('test_favicon.ico', path)

        result = {
            'data':
                {
                    'src': resource,
                    'text': 'ABC',
                    'type': 'image',
                }
        }
        response = JsonResponse(**result,
                                encoder=MagicLinksSerializer)
        data = b'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAA\\n'
        self.assertIn(data, response.content)
