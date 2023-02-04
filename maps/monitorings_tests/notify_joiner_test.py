import yaml

from maps.infra.sedem.cli.lib.monitorings.notify_joiner import collect_notifications, NotificationJoiner


def test_serviceduty_notifications_with_abc_schedules(test_vcr, service_factory):
    service = service_factory('fake_srv')
    with test_vcr.use_cassette('duty_notifications_with_abc_schedules.yaml'):
        notifications = collect_notifications(
            service, [{'type': 'service_duty', 'abc_service': 'maps-core-ratelimiter'}]
        )

        assert yaml.dump(notifications) == yaml.dump([
            NotificationJoiner.Notification({
                'type': 'startrek',
                'components': ['maps-infra'],
                'queue': 'GEOMONITORINGS',
                'status': ['CRIT']
            }),
            NotificationJoiner.Notification({
                'type': 'telegram',
                'login': ['maps-infra-chat'],
                'status': ['CRIT']
            }),
            NotificationJoiner.Notification({
                'type': 'slack',
                'chats': [
                    {
                        'name': 'infra-monitorings',
                        'link': 'https://yndx-maps-platform.slack.com/archives/C01T07CTGHE',
                    },
                ],
                'status': ['CRIT'],
            }),
            NotificationJoiner.Notification({
                'type': 'phone',
                'login': '@svc_maps-duty-infra:primary',
                'delay': 540,
                'status': ['CRIT']
            }),
            NotificationJoiner.Notification({
                'type': 'phone',
                'login': '@svc_maps-duty-infra:secondary',
                'delay': 780,
                'status': ['CRIT']
            }),
            NotificationJoiner.Notification({
                'type': 'phone_escalation',
                'logins': ['khrolenko'],
                'delay': 1020,
                'call_tries': 2,
                'on_success_next_call_delay': 600,
                'repeat': 3
            }),
        ])


def test_serviceduty_notifications_with_abc_calendars(test_vcr, service_factory):
    service = service_factory('fake_srv')
    # Abc with single duty calendar and no duty schedule
    with test_vcr.use_cassette('duty_notifications_with_abc_calendars.yaml'):
        notifications = collect_notifications(
            service, [{'type': 'service_duty', 'abc_service': 'maps-core-mobile'}]
        )

        assert yaml.dump(notifications) == yaml.dump([
            NotificationJoiner.Notification({
                'type': 'startrek',
                'components': ['maps-mapkit'],
                'queue': 'GEOMONITORINGS',
                'status': ['CRIT']
            }),
            NotificationJoiner.Notification({
                'type': 'telegram',
                'login': ['mapkit-chat'],
                'status': ['CRIT']
            }),
            NotificationJoiner.Notification({
                'type': 'phone',
                'calendar_id': 59422,
                'delay': 540,
                'status': ['CRIT']
            }),
            NotificationJoiner.Notification({
                'type': 'phone_escalation',
                'logins': ['imseleznev', 'vereschagin'],
                'delay': 780,
                'call_tries': 2,
                'on_success_next_call_delay': 600,
                'repeat': 3
            }),
        ])
