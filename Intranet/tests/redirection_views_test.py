from django.test import TestCase
from django.test.client import RequestFactory

from mock import patch, Mock

from staff.lib.testing import StaffFactory

from .factories import EmailRedirectionFactory


class RedirectionViewsTestCase(TestCase):

    factory = RequestFactory()

    @patch('staff.lib.decorators._check_service_id', Mock(side_effect=lambda *a, **b: True))
    def test_redirections_list_xml(self):
        self.persons = [
            StaffFactory(login='user0'),
            StaffFactory(login='user1'),
            StaffFactory(login='user2'),
            StaffFactory(login='user3'),
        ]

        self.redirections = [
            EmailRedirectionFactory(
                from_person=self.persons[0],
                to_person=self.persons[1]),
            EmailRedirectionFactory(
                from_person=self.persons[2],
                to_person=self.persons[3]),
            EmailRedirectionFactory(
                from_person=self.persons[0],
                to_person=self.persons[3]),
        ]

        response = self.client.get('/api/emails/redirections/')

        self.assertEqual(response['Content-Type'], 'text/xml')
        self.assertContains(
            response,
            (
                "<?xml version='1.0' encoding='UTF-8'?>\n"
                "      <redirections>"
                "          <redirection>"
                "              <from>user0</from>"
                "              <to>"
                "                  <login>user1</login>"
                "                  <login>user3</login>"
                "              </to>"
                "          </redirection>"
                "          <redirection>"
                "              <from>user2</from>"
                "              <to>"
                "                  <login>user3</login>"
                "              </to>"
                "          </redirection>"
                "      </redirections>"
            ).replace('  ', ''),
        )
