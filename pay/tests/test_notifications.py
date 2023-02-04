import pytest
from juggler_sdk import NotificationOptions
from paysys.sre.tools.monitorings.lib.notifications import \
    Notifications, \
    DEFAULT_ESCALATION_USERS, DEFAULT_CALENDAR_ID, DEFAULT_TELEGRAM_GROUP


class TestNotifications:
    def setup(self):
        self.n = Notifications()

    def test_init(self):
        iron_woman = Notifications.notifications(
            [
                NotificationOptions(
                    'phone_escalation',
                    {
                        'logins': DEFAULT_ESCALATION_USERS,
                        'delay': 60
                    }
                )
            ]
        )
        assert self.n.iron_woman == iron_woman

        sms = Notifications.notifications(
            [
                NotificationOptions(
                    'on_status_change',
                    {
                        "method": ["sms"],
                        "status": ["CRIT", "OK"],
                        "delay": 120,
                        "calendar_id": DEFAULT_CALENDAR_ID
                    }
                )
            ]
        )
        assert self.n.sms == sms

        telegram = Notifications.notifications([
            NotificationOptions(
                'on_status_change',
                {
                    "method": ["telegram"],
                    "status": ["CRIT", "OK"],
                    "delay": 120,
                    "login": DEFAULT_TELEGRAM_GROUP,
                }
            )
        ])
        assert self.n.telegram == telegram

    def test_setters(self):
        self.n.set_calendar('123')
        assert self.n.sms['notifications'][0].template_kwargs['calendar_id'] == '123'
        from paysys.sre.tools.monitorings.lib.notifications import DEFAULT_CALENDAR_ID as id
        assert id != '123'

        self.n.set_telegram('testgroup')
        assert self.n.telegram['notifications'][0].template_kwargs['login'] == ['testgroup']
        from paysys.sre.tools.monitorings.lib.notifications import DEFAULT_TELEGRAM_GROUP as group
        assert group != ['testgroup']

        self.n.set_iron_woman(['a', 'b'])
        assert self.n.iron_woman['notifications'][0].template_kwargs['logins'] == ['a', 'b']
        from paysys.sre.tools.monitorings.lib.notifications import DEFAULT_ESCALATION_USERS as logins
        assert logins != ['a', 'b']

    def test_setter_startrek_simple(self):
        self.n.set_startrek(queue="TEST")
        self.n.startrek["notifications"][0].template_kwargs.pop("on_dashboard", True)
        assert self.n.startrek["notifications"][0].to_dict() == NotificationOptions(
            "startrek",
            {
                "queue": "TEST",
                "priority": "normal",
                "status": "CRIT",
                "followers": [],
            }
        ).to_dict()

    def test_setter_startrek_simple_priority(self):
        self.n.set_startrek(queue="TEST", priority="critical")
        self.n.startrek["notifications"][0].template_kwargs.pop("on_dashboard", True)
        assert self.n.startrek["notifications"][0].to_dict() == NotificationOptions(
            "startrek",
            {
                "queue": "TEST",
                "priority": "critical",
                "status": "CRIT",
                "followers": [],
            }
        ).to_dict()

    def test_setter_startrek_simple_followers(self):
        self.n.set_startrek(queue="TEST", followers=["foo", "bar"])
        self.n.startrek["notifications"][0].template_kwargs.pop("on_dashboard", True)
        assert self.n.startrek["notifications"][0].to_dict() == NotificationOptions(
            "startrek",
            {
                "queue": "TEST",
                "status": "CRIT",
                "priority": "normal",
                "followers": ["foo", "bar"],
            }
        ).to_dict()

    def test_setter_startrek_simple_components(self):
        self.n.set_startrek(queue="TEST", components=["foo", "bar"])
        self.n.startrek["notifications"][0].template_kwargs.pop("on_dashboard", True)
        assert self.n.startrek["notifications"][0].to_dict() == NotificationOptions(
            "startrek",
            {
                "queue": "TEST",
                "status": "CRIT",
                "priority": "normal",
                "components": ["foo", "bar"],
                "followers": [],
            }
        ).to_dict()

    def test_setter_startrek_simple_issue(self):
        self.n.set_startrek(queue="TEST", issue="TEST-123")
        self.n.startrek["notifications"][0].template_kwargs.pop("on_dashboard", True)
        assert self.n.startrek["notifications"][0].to_dict() == NotificationOptions(
            "startrek",
            {
                "queue": "TEST",
                "status": "CRIT",
                "priority": "normal",
                "issue": "TEST-123",
                "followers": [],
            }
        ).to_dict()

    def test_setter_startrek_simple_delay(self):
        self.n.set_startrek(queue="TEST", delay=123)
        self.n.startrek["notifications"][0].template_kwargs.pop("on_dashboard", True)
        assert self.n.startrek["notifications"][0].to_dict() == NotificationOptions(
            "startrek",
            {
                "queue": "TEST",
                "status": "CRIT",
                "priority": "normal",
                "delay": 123,
                "followers": [],
            }
        ).to_dict()

    def test_setter_startrek_wrong_priority(self):
        with pytest.raises(Exception):
            self.n.set_startrek(queue="TEST", priority="foo")

    def test_setter_startrek_wrong_issue_and_components(self):
        with pytest.raises(Exception):
            self.n.set_startrek(queue="TEST", issue="TEST-123", components=["test"])

    def test_setter_startrek_wrong_delay(self):
        with pytest.raises(TypeError):
            self.n.set_startrek(queue="TEST", delay="123")

    def test_deepcopy(self):
        n1 = Notifications().set_calendar('123').set_telegram(['xxx']).set_iron_woman(['a', 'b'])
        n2 = Notifications().set_calendar('321').set_telegram(['yyy']).set_iron_woman(['b', 'a'])

        assert n1.sms['notifications'][0].template_kwargs['calendar_id'] !=\
               n2.sms['notifications'][0].template_kwargs['calendar_id']

        assert n1.telegram['notifications'][0].template_kwargs['login'] !=\
               n2.telegram['notifications'][0].template_kwargs['login']

        assert n1.iron_woman['notifications'][0].template_kwargs['logins'] !=\
               n2.iron_woman['notifications'][0].template_kwargs['logins']
