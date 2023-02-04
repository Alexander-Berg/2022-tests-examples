import itertools

from telethon.tl.types import ChannelParticipantAdmin


class attrdict(dict):
    def __getattr__(self, item):
        return self[item]


class FakeResponse:
    def __init__(self, json_data):
        self.json_data = json_data

    async def json(self):
        return self.json_data


class MockTelethonEntityMeta(type):
    def __getitem__(cls, x):
        return getattr(cls, x)


class MockTelethonEntity(metaclass=MockTelethonEntityMeta):
    current_id = 1000

    def __init__(self, entity_id=None, entity_type=None, username=None, admins=None, participants=None,
                 access_hash='', title='', client=None):
        if entity_id is None:
            entity_id = MockTelethonEntity.current_id
            MockTelethonEntity.current_id += 1
        self.id = entity_id
        self.username = username
        self._ = entity_type
        self.access_hash = access_hash
        self.title = title

        self.admins = []
        if admins is not None:
            for username in admins:
                if isinstance(username, MockTelethonParticipant):
                    self.admins.append(username)
                else:
                    self.admins.append(MockTelethonParticipant(
                        username=username,
                        first_name='mocked_first_name',
                        participant_type=ChannelParticipantAdmin(None, None, None, None, None),
                        client=client,
                    ))

        self.participants = []
        if participants is not None:
            for username in participants:
                if isinstance(username, MockTelethonParticipant):
                    self.participants.append(username)
                else:
                    self.participants.append(
                        MockTelethonParticipant(
                            username=username,
                            first_name=username[::-1],
                            last_name='mocked_last_name',
                            client=client,
                        )
                    )
        if client:
            client.entities[entity_id] = self

    def to_dict(self):
        return {key: value for key, value in self.__dict__.items() if not key.startswith('__')}

    @property
    def participants_count(self):
        return len(self.participants)


class MockTelethonParticipant(MockTelethonEntity):
    def __init__(
            self, username='', first_name='', last_name='',
            telegram_id=None, bot=False, participant_type=None, client=None
    ):
        self.id = telegram_id if telegram_id is not None else MockTelethonEntity.current_id
        MockTelethonEntity.current_id += 1
        self.username = username
        self.first_name = first_name
        self.last_name = last_name
        self.bot = bot
        self.participant = participant_type
        if client:
            client.entities[self.id] = self


class MockTelethonDialog(MockTelethonEntity):
    def __init__(self, dialog_id=0, title='', is_user=False, group_type='Channel', access_hash='', deactivated=False,
                 entity=None, admins=None, participants=None, client=None):

        super(MockTelethonDialog, self).__init__(
            dialog_id, admins=admins, participants=participants, client=client, title=title
        )

        self.is_user = is_user
        if entity is None:
            entity = MockTelethonEntity(
                entity_type=group_type,
                admins=admins,
                participants=participants,
                access_hash=access_hash,
                title=title,
                client=client,
            )
        self.entity = entity
        self.dialog = attrdict({
            'pts': self.entity.participants_count,
            'deactivated': deactivated
        })

    def __getitem__(self, x):
        return getattr(self, x)

    def __getattr__(self, item):
        return self.dialog[item]


class MockTelethonSession:
    def save(self):
        pass


class MetaTelethonClient(type):
    client = None

    def __call__(cls, *args, **kwargs):
        if cls.client is None:
            cls.client = super(MetaTelethonClient, cls).__call__(*args, **kwargs)
        return cls.client


class MockTelethonClient(metaclass=MetaTelethonClient):
    def __init__(self, admins=None, participants=None):
        self.session = MockTelethonSession()
        self.entities = {}

        self.me = MockTelethonEntity(100000000000000, username='test_bot')
        self.dialogs = [MockTelethonDialog(admins=admins, participants=participants, client=self)]

    async def __aenter__(self):
        pass

    async def __aexit__(self, exc_type, exc, tb):
        pass

    async def __call__(self, *args, **kwargs):
        pass

    async def get_me(self, *args, **kwargs):
        return self.me

    async def iter_dialogs(self, *args, **kwargs):
        for dialog in self.dialogs:
            yield dialog

    async def get_entity(self, entity_id, q=None):
        if isinstance(entity_id, str):
            return MockTelethonEntity(username=entity_id, client=self)
        elif isinstance(entity_id, int):
            return self.entities[entity_id]

    async def iter_participants(self, entity, filter=None, search=None, **kwargs):
        if filter is None:
            users = itertools.chain(entity.admins, entity.participants)
        else:
            users = entity.admins
        for user in users:
            if search is None or search.lower() == user.username.lower():
                yield user

    async def get_participants(self, entity, filter=None, search=None, **kwargs):
        if filter is None:
            users = entity.admins + entity.participants
        else:
            users = entity.admins
        if search is None:
            return users
        else:
            return [user for user in users if user.username.lower() == search.lower()]

    async def send_message(self, *args, **kwargs):
        pass


class MockAioTgChat:
    kick_error = None
    unban_error = None
    chat_info = {}
    users_info = {}

    def __init__(self, bot, chat_id, *args, **kwargs):
        self.bot = bot
        self.chat_id = chat_id

    async def get_chat(self):
        return self.chat_info

    async def kick_chat_member(self, user_id):
        if self.kick_error:
            raise self.kick_error(response=FakeResponse({'description': 'not enough rights'}))
        else:
            return {'result': 'ok'}

    async def get_chat_member(self, user_id):
        return self.users_info.get(user_id)

    async def unban_chat_member(self, user_id):
        if self.unban_error:
            raise self.unban_error(response=None)
        else:
            return {'result': 'ok'}
