import datetime

from source.tests.fixtures import (FakeFabric,
                                   FakeFabricIterator)

PREPARED_FOR_ISSUES_TESTDATA = [
    (FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id="orange13"),
        assignee=FakeFabric(id="orange13"),
        changelog=[
            FakeFabric(
                fields=
                [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                  'to': FakeFabric(id='507')}],
                updatedAt="2019-01-14T13:32:45.783+0000")
        ],
        comments=FakeFabricIterator(
            comment_list=[
                FakeFabric(
                    text="Простой комментарий",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                )
            ]),
    ),
     1),
    (FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id="orange13"),
        assignee=FakeFabric(id="orange13"),
        changelog=[
            FakeFabric(
                fields=
                [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                  'to': FakeFabric(id='507')}],
                updatedAt="2019-01-14T13:32:45.783+0000")
        ],
        comments=FakeFabricIterator(
            comment_list=[
                FakeFabric(
                    text="кто:orange13, привет, ждем тебя для получения и" \
                         " донастройки профиля оборудования.",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                )
            ])
    ),
     2),
    (FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id="orange13"),
        assignee=FakeFabric(id="orange13"),
        changelog=[
            FakeFabric(
                fields=
                [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                  'to': FakeFabric(id='507')}],
                updatedAt="2019-01-14T13:32:45.783+0000")
        ],
        comments=FakeFabricIterator(
            comment_list=[
                FakeFabric(
                    text="кто:orange13, привет, ждем тебя для получения и" \
                         " донастройки профиля оборудования.",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                ),
                FakeFabric(
                    text="кто:orange13, когда сможешь прийти в HelpDesk?",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                )
            ])
    ),
     3),
    (FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id="orange13"),
        assignee=FakeFabric(id="orange13"),
        changelog=[
            FakeFabric(
                fields=
                [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                  'to': FakeFabric(id='507')}],
                updatedAt="2019-01-14T13:32:45.783+0000")
        ],
        comments=FakeFabricIterator(
            comment_list=[
                FakeFabric(
                    text="кто:orange13, привет, ждем тебя для получения и" \
                         " донастройки профиля оборудования.",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                ),
                FakeFabric(
                    text="кто:orange13, когда сможешь прийти в HelpDesk?",
                    updatedAt=datetime.datetime.now().strftime(
                        '%Y-%m-%dT%H:%M:%S')
                )
            ])
    ),
     None),
    (FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id="orange13"),
        assignee=FakeFabric(id="orange13"),
        changelog=[
            FakeFabric(
                fields=
                [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                  'to': FakeFabric(id='507')}],
                updatedAt=datetime.datetime.now().strftime(
                        '%Y-%m-%dT%H:%M:%S'))
        ],
        comments=FakeFabricIterator(
            comment_list=[
                FakeFabric(
                    text="кто:orange13, привет, ждем тебя для получения и" \
                         " донастройки профиля оборудования.",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                ),
                FakeFabric(
                    text="кто:orange13, когда сможешь прийти в HelpDesk?",
                    updatedAt=datetime.datetime.now().strftime(
                        '%Y-%m-%dT%H:%M:%S')
                )
            ])
    ),
     None)
]


WAITING_FOR_PREPARED_TESTDATA = [
    (FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id="orange13"),
        assignee=FakeFabric(id="orange13"),
        prepared=[FakeFabric(id="orange13")],

        changelog=[
            FakeFabric(
                fields=
                [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                  'to': FakeFabric(id='493')}],
                updatedAt="2019-01-14T13:32:45.783+0000")
        ],
        comments=FakeFabricIterator(
            comment_list=[
                FakeFabric(
                    text="Простой комментарий",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                )
            ]),
    ),
     {"expected_hop":1,
      "summonees":["orange13"]}),
    (FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id="orange13"),

        changelog=[
            FakeFabric(
                fields=
                [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                  'to': FakeFabric(id='493')}],
                updatedAt="2019-01-14T13:32:45.783+0000")
        ],
        comments=FakeFabricIterator(
            comment_list=[
                FakeFabric(
                    text="Простой комментарий",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                )
            ]),
    ),
     {"expected_hop":1,
      "summonees":["agrebenyuk"]}),
    (FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id="orange13"),
        assignee=FakeFabric(id="user1"),
        prepared=[FakeFabric(id="user2")],

        changelog=[
            FakeFabric(
                fields=
                [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                  'to': FakeFabric(id='493')}],
                updatedAt="2019-01-14T13:32:45.783+0000")
        ],
        comments=FakeFabricIterator(
            comment_list=[
                FakeFabric(
                    text="кто:orange13, привет, напоминаем, что тебе нужно " \
                          "сдать оборудование в HelpDesk.",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                )
            ]),
    ),
     {"expected_hop":2,
      "summonees":["user1","user2"]}),
    (FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id="orange13"),
        assignee=FakeFabric(id="user1"),
        prepared=[FakeFabric(id="user2")],

        changelog=[
            FakeFabric(
                fields=
                [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                  'to': FakeFabric(id='493')}],
                updatedAt="2019-01-14T13:32:45.783+0000")
        ],
        comments=FakeFabricIterator(
            comment_list=[
                FakeFabric(
                    text="кто:orange13, привет, повторно напоминаем, что тебе нужно " \
                          "сдать оборудование в HelpDesk.",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                )
            ]),
    ),
     {"expected_hop":3,
      "summonees":["user1","user2"]}),
    (FakeFabric(
        key='HDRFS-5',
        createdBy=FakeFabric(id="orange13"),
        assignee=FakeFabric(id="user1"),
        prepared=[FakeFabric(id="user2")],

        changelog=[
            FakeFabric(
                fields=
                [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                  'to': FakeFabric(id='493')}],
                updatedAt="2019-01-14T13:32:45.783+0000")
        ],
        comments=FakeFabricIterator(
            comment_list=[
                FakeFabric(
                    text="2help: этот тикет находится в статусе " \
                          "'Ждем оборудование' более 3 недель, нужно " \
                          "проверить актуальность задачи.",
                    updatedAt="2019-01-14T13:32:45.783+0000"
                )
            ]),
    ),
     {"expected_hop":None,
      "summonees":["user1","user2"]})
]

SCHEDULED_TESTDATA = [
    (FakeFabric(type=FakeFabric(key="newComputer"),
                tags=[]),
     "confirmed"),
    (FakeFabric(type=FakeFabric(key="newComputer"),
                tags=["COMP_NewLaptopYa"],
                components=[
                    FakeFabric(id="52286")
                ]),
     "treated"),
    (FakeFabric(type=FakeFabric(key="newComputer"),
                tags=["COMP_NewLaptopYa"],
                components=[
                    FakeFabric(id="52287")
                ]),
     "confirmed"),
    (FakeFabric(type=FakeFabric(key="OFR"),
                tags=[]),
     "treated"),
]
