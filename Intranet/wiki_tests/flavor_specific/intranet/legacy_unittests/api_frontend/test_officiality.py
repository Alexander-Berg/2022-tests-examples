import ujson as json

from wiki.intranet import models as intranet_models
from wiki.pages import models
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class APIOfficialityTest(BaseApiTestCase):
    def setUp(self):
        super(APIOfficialityTest, self).setUp()
        self.setUsers()
        self.setGroups()
        self.setPages()

        self.page = models.Page.objects.get(supertag='homepage')
        self.group = intranet_models.Group.objects.all()[0]
        self.user = self.page.get_authors().first()
        self.client.login(self.user.username)
        self.url = '/_api/frontend/%s/.officiality' % self.page.tag

    def test_officiality_response_data_not_official(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.content)
        assert 'data' in response_data
        self.assertEqual(response_data['data'], {})

    def test_officiality_response_data_official(self):
        self.page.is_official = True
        self.page.save()

        officiality = models.Officiality.objects.create(page=self.page)
        officiality.responsible_persons.add(self.user)
        officiality.responsible_groups.add(self.group)
        officiality.save()

        with self.assertNumQueries(10):
            response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.content)

        self.assertIn('data', response_data)
        data = response_data['data']

        self.assertIn('responsible_persons', data)
        self.assertIn('responsible_groups', data)

        persons = data['responsible_persons']
        groups = data['responsible_groups']

        self.assertEqual(len(persons), 1)
        self.assertEqual(self.user.username, persons[0]['login'])
        self.assertEqual(len(groups), 1)
        self.assertEqual(self.group.id, groups[0]['id'])

    def test_corrupted_officiality(self):
        self.page.is_official = True
        self.page.save()

        response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        response_data = json.loads(response.content)

        assert 'data' in response_data
        self.assertEqual(response_data['data'], {})
