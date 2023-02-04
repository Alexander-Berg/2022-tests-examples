from uuid import uuid4
import factory
from datetime import timedelta

from django.conf import settings
from django.contrib.auth.models import Permission
from django.utils import timezone

from intranet.vconf.src.call.constants import STREAM_LANGUAGES


class EventFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = 'call.Event'

    id = 123456
    name = 'test_event'
    description = 'some text'
    start_time = timezone.now() + timedelta(minutes=5)
    end_time = factory.LazyAttribute(lambda o: o.start_time + timedelta(hours=1))
    secret = factory.LazyFunction(lambda: str(uuid4()))


class ConferenceCallFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = 'call.ConferenceCall'

    conf_cms_id = factory.Sequence(lambda n: '123-555-%04d' % n)
    call_cms_id = factory.Sequence(lambda n: '345-333-%04d' % n)
    meeting_id = factory.Sequence(lambda n: '12345%04d' % n)
    name = factory.LazyAttribute(lambda o: '%s_conf_name' % o.meeting_id)
    start_time = timezone.now()
    duration = timedelta(minutes=35)
    stop_time = factory.LazyAttribute(lambda o: o.start_time + o.duration)
    stream = False
    record = False
    chat = False
    chat_invite_hash = ''
    author_login = settings.AUTH_TEST_USER
    state = 'active'
    cms_node = factory.Sequence(lambda n: '%04d' % n)
    priority = 0
    master_call = None
    lang = STREAM_LANGUAGES.ru


class ParticipantFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = 'call.Participant'

    cms_id = factory.Sequence(lambda n: '123-555-%04d' % n)
    state = 'active'
    obj_id = factory.Sequence(lambda n: 'participant%d' % n)
    obj_type = 'person'
    number = '5566'
    camera = True
    microphone = True
    camera_active = True
    microphone_active = True
    method = 'cisco'


class CallTemplateFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = 'call.CallTemplate'

    name = factory.Sequence(lambda n: 'temp%02d' % n)
    duration = timedelta(minutes=35)
    stream = False
    record = False
    owners = [settings.AUTH_TEST_USER]


class ParticipantTemplateFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = 'call.ParticipantTemplate'

    obj_id = factory.Sequence(lambda n: 'participant%d' % n)
    obj_type = 'person'
    method = 'cisco'


class NodeFactory(factory.django.DjangoModelFactory):

    class Meta:
        model = 'call.Node'

    id = factory.Sequence(lambda n: n)
    last_update = timezone.now()
    load_limit = 100
    enabled = True


class UserFactory(factory.django.DjangoModelFactory):

    class Meta:
        model = settings.AUTH_USER_MODEL


class UserSettingsFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = 'call.UserSettings'

    login = settings.AUTH_TEST_USER
    method = 'cisco'


def create_user(**kwargs):
    kwargs['extra'] = {
        'uid': '123',
        'is_external': kwargs.pop('is_external', False),
        'is_ip_external': kwargs.pop('is_ip_external', False),
        'affiliation': 'yandex',
        'tz': 'Europe/Moscow',
        'lang': 'ru',
        'secret': kwargs.pop('secret', None),
        'user_ticket': '123',
    }
    user = UserFactory(**kwargs)
    return user


def create_user_with_perm(perm_codename, **kwargs):
    perm, _created = Permission.objects.get_or_create(codename=perm_codename)
    user = create_user(**kwargs)
    user.user_permissions.add(perm)
    return user


def create_admin(**kwargs):
    return create_user_with_perm('admin_perm', **kwargs)
