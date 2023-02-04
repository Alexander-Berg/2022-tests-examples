from django.test import TestCase

from staff.lib.testing import StaffFactory
from staff.emails.controllers import EmailRedirectionController
from staff.emails.models import EmailRedirection

from .factories import EmailRedirectionFactory


class RedirectionTestCase(TestCase):
    'Тесты перенаправлений почты'

    def setUp(self):
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

    def test_get_xml(self):
        ctrl = EmailRedirectionController()
        xml = ctrl.get_xml()
        self.assertEqual(
            xml.decode('utf-8'),
            (
                "<?xml version='1.0' encoding='UTF-8'?>\n"
                "<redirections>"
                "    <redirection>"
                "        <from>user0</from>"
                "        <to>"
                "            <login>user1</login>"
                "            <login>user3</login>"
                "        </to>"
                "    </redirection>"
                "    <redirection>"
                "        <from>user2</from>"
                "        <to>"
                "            <login>user3</login>"
                "        </to>"
                "    </redirection>"
                "</redirections>"
            ).replace('  ', ''),
        )

    def test_create_redirection_forward(self):
        just_dismissed = StaffFactory(login='loser')
        forward_correspondence_to = StaffFactory(login='winner')

        [
            EmailRedirectionFactory(
                from_person=self.persons[0],
                to_person=just_dismissed),
            EmailRedirectionFactory(
                from_person=self.persons[1],
                to_person=just_dismissed),
            EmailRedirectionFactory(
                from_person=self.persons[2],
                to_person=just_dismissed),
        ]

        query1 = EmailRedirection.objects.filter(
            to_person=forward_correspondence_to)

        query2 = EmailRedirection.objects.filter(
            to_person=just_dismissed)

        self.assertEqual(query1.count(), 0)
        self.assertEqual(query2.count(), 3)

        ctrl = EmailRedirectionController()
        ctrl.create_redirection(just_dismissed, forward_correspondence_to)

        self.assertEqual(query1.count(), 4)
        self.assertEqual(query2.count(), 0)

    def test_create_redirection_forward_twice(self):
        just_dismissed = StaffFactory(login='loser')
        forward_correspondence_to = StaffFactory(login='winner')

        [
            EmailRedirectionFactory(
                from_person=self.persons[0],
                to_person=just_dismissed),
            EmailRedirectionFactory(
                from_person=self.persons[1],
                to_person=just_dismissed),
            EmailRedirectionFactory(
                from_person=self.persons[2],
                to_person=just_dismissed),
        ]

        query1 = EmailRedirection.objects.filter(
            to_person=forward_correspondence_to)

        query2 = EmailRedirection.objects.filter(
            to_person=just_dismissed)

        self.assertEqual(query1.count(), 0)
        self.assertEqual(query2.count(), 3)

        ctrl = EmailRedirectionController()
        ctrl.create_redirection(just_dismissed, forward_correspondence_to)
        ctrl.create_redirection(just_dismissed, forward_correspondence_to)

        self.assertEqual(query1.count(), 4)
        self.assertEqual(query2.count(), 0)

    def test_create_redirection_no_forward(self):
        just_dismissed = StaffFactory(login='loser')
        forward_correspondence_to = None

        [
            EmailRedirectionFactory(
                from_person=self.persons[0],
                to_person=just_dismissed),
            EmailRedirectionFactory(
                from_person=self.persons[1],
                to_person=just_dismissed),
            EmailRedirectionFactory(
                from_person=self.persons[2],
                to_person=just_dismissed),
        ]

        query1 = EmailRedirection.objects.filter(
            to_person=forward_correspondence_to)

        self.assertEqual(query1.count(), 0)

        query2 = EmailRedirection.objects.filter(
            to_person=just_dismissed)

        self.assertEqual(query1.count(), 0)
        self.assertEqual(query2.count(), 3)

        ctrl = EmailRedirectionController()
        ctrl.create_redirection(just_dismissed, forward_correspondence_to)

        self.assertEqual(query1.count(), 0)
        self.assertEqual(query2.count(), 0)

    def test_notify_restored(self):
        just_restored = StaffFactory(login='lucky')

        EmailRedirectionFactory(from_person=just_restored, to_person=self.persons[2])
        ctrl = EmailRedirectionController()

        query = EmailRedirection.objects.filter(from_person=just_restored)

        self.assertEqual(query.count(), 1)
        ctrl.notify_restored(just_restored)
        self.assertEqual(query.count(), 0)

    def test_get_for_reciever(self):
        ctrl = EmailRedirectionController()
        redirections = ctrl.get_for_reciever(self.persons[3])
        dismissed_people = redirections

        self.assertTrue(self.persons[2] in dismissed_people)
        self.assertTrue(self.persons[0] in dismissed_people)

    def test_drop_redirections(self):
        ctrl = EmailRedirectionController()

        query = EmailRedirection.objects.all()

        self.assertEqual(query.count(), 3)
        ctrl.drop_redirections(self.persons[2], self.persons[3])
        self.assertEqual(query.count(), 2)
