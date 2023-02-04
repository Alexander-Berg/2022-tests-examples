import datetime

from source.queue_auto.queue_auto import UserSideRobot
from source.tests.fixtures import *

DISK_FINDER_MAIN_DATA = [
    ({"_check_inv_by_sn":
          {'owner': None,'temp_user': None,'finded': True, 'type':"USR-ACCESSORIES"},
      "_fetch_data_from_cmdb":
          [{'instance_number': '102539700',
            'attached_disk_devices_collection': 'NA7SR7MC',
            'username':'orange13'}]
      },
     {'summary': 'Мониторинг внешних накопителей (жесткие диски)',
      'description': '\n**Замечено подозрительное использование жесткого диска:**\n#|\n|| Инвентарный номер ноутбука | Владелец ноутбука | На кого проведён диск | Серийный номер диска ||\n|| 102539700 | orange13@ |None@ | NA7SR7MC||\n|#',
      'queue': 'HDRFS', 'channel': 'ST API', 'owner': None, 'temp_user': None,
      'finded': True, 'type': 'USR-ACCESSORIES',
      'instance_number': '102539700', 'notebook_owner': 'orange13',
      'sn': 'NA7SR7MC'}),
    ({"_check_inv_by_sn":
          {'owner': None,'temp_user': None,'finded': False},
      "_fetch_data_from_cmdb":
          [{'instance_number': '102539700',
            'attached_disk_devices_collection': 'NA7SR7MC'}]
      },
     {}),
    ({"_check_inv_by_sn":
          {'owner': "orange13",'temp_user': None,'finded': True},
      "_fetch_data_from_cmdb":
          [{'instance_number': '102539700',
            'attached_disk_devices_collection': 'NA7SR7MC'}]
      },
     {}),
]

DISK_FINDER_FIND_SN = [
    ({"bot_data":
          {"res": 2, "msg": "not found", "os": []},
      },
     {
         'owner': None,
         'temp_user': None,
         'finded': False,
         'type': None
     }
     ),
    ({"bot_data":
          {"res": 1, "os": [
              {"EMPLOYEE_OWNED": "zhenyazhe", "EMPLOYEE_TEMPORARY_USE": None}]},
      },
     {
         'owner': "zhenyazhe",
         'temp_user': None,
         'finded': True,
         'type': None
     }
     ),
    ({"bot_data":
          {"res": 1, "os": [
              {"EMPLOYEE_OWNED": None, "EMPLOYEE_TEMPORARY_USE": None}]},
      },
     {
         'owner': None,
         'temp_user': None,
         'finded': True,
         'type': None
     }
     ),
]

TRANCIEVERS_FINDER_TESTDATA = [
    ({"switch_info":
          {"res": 1, "os": [
              {"loc_room_type": "COMCENTER", "loc_segment1": "RU",
               "loc_segment2": "MOW", "loc_segment3": "AVRORA",
               "loc_segment4": "CC-9.1", "loc_segment5": "1",
               "loc_segment6": "36"}]},
      "trancievers_infos": {"res": 1, "os": [
              {"loc_room_type": "COMCENTER", "loc_segment1": "RU",
               "loc_segment2": "MOW", "loc_segment3": "AVRORA",
               "loc_segment4": "CC-9.1", "loc_segment5": "1",
               "loc_segment6": "36"}]}
      },
     {}
     ),
    ({"switch_info":
          {"res": 1, "os": [
              {"loc_room_type": "COMCENTER", "loc_segment1": "RU",
               "loc_segment2": "MOW", "loc_segment3": "AVRORA",
               "loc_segment4": "CC-9.1", "loc_segment5": "1",
               "loc_segment6": "36"}]},
      "trancievers_infos": {"res": 1, "os": [
              {"loc_room_type": "COMCENTER", "loc_segment1": "RU",
               "loc_segment2": "MOW", "loc_segment3": "AVRORA",
               "loc_segment4": "CC-9.1", "loc_segment5": "1",
               "loc_segment6": "37"}]}
      },
     {'summary': 'Мониторинг трансиверов',
      'description': "\n**Замечены неправильно проведённые трансиверы:**\n#|\n|| Описание проблемы | Информация | Серийный номер | Switch ||\n\n|| tranciever and swith in diff locs | {'loc_room_type': 'COMCENTER', 'loc_segment1': 'RU', 'loc_segment2': 'MOW', 'loc_segment3': 'AVRORA', 'loc_segment4': 'CC-9.1', 'loc_segment5': '1', 'loc_segment6': '36'} | DCB2208B4GT | admiral-u1.yndx.net ||tranciever and swith in diff locs | {'loc_room_type': 'COMCENTER', 'loc_segment1': 'RU', 'loc_segment2': 'MOW', 'loc_segment3': 'AVRORA', 'loc_segment4': 'CC-9.1', 'loc_segment5': '1', 'loc_segment6': '36'} | FCW2032A155 | avex-101a1.yndx.net ||tranciever and swith in diff locs | {'loc_room_type': 'COMCENTER', 'loc_segment1': 'RU', 'loc_segment2': 'MOW', 'loc_segment3': 'AVRORA', 'loc_segment4': 'CC-9.1', 'loc_segment5': '1', 'loc_segment6': '36'} | FDO2211E16R | admiral-u1.yndx.net ||tranciever and swith in diff locs | {'loc_room_type': 'COMCENTER', 'loc_segment1': 'RU', 'loc_segment2': 'MOW', 'loc_segment3': 'AVRORA', 'loc_segment4': 'CC-9.1', 'loc_segment5': '1', 'loc_segment6': '36'} | FDO2211E16R | admiral-u1.yndx.net ||tranciever and swith in diff locs | {'loc_room_type': 'COMCENTER', 'loc_segment1': 'RU', 'loc_segment2': 'MOW', 'loc_segment3': 'AVRORA', 'loc_segment4': 'CC-9.1', 'loc_segment5': '1', 'loc_segment6': '36'} | PX40F7Z | avex-101a1.yndx.net ||tranciever and swith in diff locs | {'loc_room_type': 'COMCENTER', 'loc_segment1': 'RU', 'loc_segment2': 'MOW', 'loc_segment3': 'AVRORA', 'loc_segment4': 'CC-9.1', 'loc_segment5': '1', 'loc_segment6': '36'} | SB3A690031 | avex-101a1.yndx.net ||tranciever and swith in diff locs | {'loc_room_type': 'COMCENTER', 'loc_segment1': 'RU', 'loc_segment2': 'MOW', 'loc_segment3': 'AVRORA', 'loc_segment4': 'CC-9.1', 'loc_segment5': '1', 'loc_segment6': '36'} | W18083104235 | admiral-u1.yndx.net ||\n|#",
      'queue': 'HDRFS'}
     )
]

CMDB_NOTEBOOKS_STATUSES_TESTDATA = [
    ([{'instance_number': '100000000', 'creation_date': '2013-03-31 18:48:54'},
      {'instance_number': '100000001', 'creation_date': '2013-03-31 18:50:47'},
      {'instance_number': '100000002', 'creation_date': '2013-03-31 18:52:04'},
      {'instance_number': '100000003', 'creation_date': '2013-03-31 18:53:21'},
      {'instance_number': '100000004', 'creation_date': '2013-03-31 18:55:08'},
      {'instance_number': '100000049', 'creation_date': '2013-04-08 19:09:26'},
      {'instance_number': '100000050', 'creation_date': '2013-04-08 19:09:28'},
      {'instance_number': '100000051', 'creation_date': '2013-04-08 19:09:27'},
      {'instance_number': '100000055', 'creation_date': '2013-04-08 19:09:28'},
      {'instance_number': '100000057',
       'creation_date': '2013-04-08 19:09:28'}],
     [{'instance_number': '100000000', 'exp_date': '2016-03-30 18:48:54',
       'eq_status': 'non_actual'},
      {'instance_number': '100000001', 'exp_date': '2016-03-30 18:50:47',
       'eq_status': 'non_actual'},
      {'instance_number': '100000002', 'exp_date': '2016-03-30 18:52:04',
       'eq_status': 'non_actual'},
      {'instance_number': '100000003', 'exp_date': '2016-03-30 18:53:21',
       'eq_status': 'non_actual'},
      {'instance_number': '100000004', 'exp_date': '2016-03-30 18:55:08',
       'eq_status': 'non_actual'},
      {'instance_number': '100000049', 'exp_date': '2016-04-07 19:09:26',
       'eq_status': 'non_actual'},
      {'instance_number': '100000050', 'exp_date': '2016-04-07 19:09:28',
       'eq_status': 'non_actual'},
      {'instance_number': '100000051', 'exp_date': '2016-04-07 19:09:27',
       'eq_status': 'non_actual'},
      {'instance_number': '100000055', 'exp_date': '2016-04-07 19:09:28',
       'eq_status': 'non_actual'},
      {'instance_number': '100000057', 'exp_date': '2016-04-07 19:09:28',
       'eq_status': 'non_actual'}])
]

CMDB_NOTEBOOKS_OS_TESTDATA = [
    ([{'instance_number': '100000000', 'segment1': 'MD103RU/A',
       'segment2': 'APPLE - MACBOOK PRO 15'},
      {'instance_number': '100000001', 'segment1': 'MD103RU/A',
       'segment2': 'APPLE - MACBOOK PRO 15'},
      {'instance_number': '100000002', 'segment1': 'MD103RU/A',
       'segment2': 'APPLE - MACBOOK PRO 15'},
      {'instance_number': '100000003', 'segment1': 'MD103RU/A',
       'segment2': 'APPLE - MACBOOK PRO 15'},
      {'instance_number': '100000004', 'segment1': 'MD103RU/A',
       'segment2': 'APPLE - MACBOOK PRO 15'},
      {'instance_number': '100000049', 'segment1': 'MD232C18GH1RS/A',
       'segment2': 'APPLE - MACBOOK AIR 13'},
      {'instance_number': '100000050', 'segment1': 'MD232C18GH1RS/A',
       'segment2': 'APPLE - MACBOOK AIR 13'},
      {'instance_number': '100000051', 'segment1': 'MD232C18GH1RS/A',
       'segment2': 'APPLE - MACBOOK AIR 13'},
      {'instance_number': '100000055', 'segment1': 'MD232C18GH1RS/A',
       'segment2': 'APPLE - MACBOOK AIR 13'},
      {'instance_number': '100000057', 'segment1': 'MD232C18GH1RS/A',
       'segment2': 'APPLE - MACBOOK AIR 13'},
      {'instance_number': '1000000412', 'segment1': 'MD232C18GH1RS/A',
       'segment2': 'DELL - LATTITUDE 13'}
      ],
     [{'instance_number': '100000000', 'os': 'macOS'},
      {'instance_number': '100000001', 'os': 'macOS'},
      {'instance_number': '100000002', 'os': 'macOS'},
      {'instance_number': '100000003', 'os': 'macOS'},
      {'instance_number': '100000004', 'os': 'macOS'},
      {'instance_number': '100000049', 'os': 'macOS'},
      {'instance_number': '100000050', 'os': 'macOS'},
      {'instance_number': '100000051', 'os': 'macOS'},
      {'instance_number': '100000055', 'os': 'macOS'},
      {'instance_number': '100000057', 'os': 'macOS'},
      {'instance_number': '1000000412', 'os': 'Windows'}])
]

UNITED_DASHBOARD_BACKLOG_TESTDATA = [
    ([FakeFabric(
        fixVersions = FakeFabricIterator(
            fix_versions = [
                FakeFabric(name='MSK Morozov'),
                FakeFabric(name='MSK Avrora')
            ]
        ),
        abcService = FakeFabricIterator(
            fix_versions = [
                FakeFabric(id=3459),
                FakeFabric(id=1200)
            ]
        )),
         FakeFabric(
             fixVersions=FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(name='Birylevo'),
                     FakeFabric(name='MSK Avrora')
                 ]
             ),
             abcService=FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(id=1234),
                     FakeFabric(id=1200)
                 ]
             )),
         FakeFabric(
             fixVersions=FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(name='Kitezhgrad'),
                     FakeFabric(name='MSK Avrora')
                 ]
             ),
             abcService=FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(id=432),
                     FakeFabric(id=1200)
                 ]
             )),
         FakeFabric(
             fixVersions=FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(name='MSK Morozov'),
                     FakeFabric(name='MSK Avrora')
                 ]
             ),
             abcService=FakeFabricIterator(
                 fix_versions=[]
             ))
     ],
     [{'backlog_3459': 1, 'backlog_empty': 1, 'location': 'msk_morozov'},
      {'backlog_1234': 1, 'location': 'birylevo'},
      {'backlog_432': 1, 'location': 'kitezhgrad'}]
     ),
]

robot = UserSideRobot()
text_1_hop = robot.config["first"]["text"]
text_2_hop = robot.config["second"]["text"]
text_3_hop = robot.config["third"]["text"]

USER_SIDE_AUTOMATIC_TESTDATA = [
    ({"author": "orange13",
      "changelog": [
          FakeFabric(
              fields=
              [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                'to': FakeFabric(id='400')}],
              updatedAt="2019-01-14T13:32:45.783+0000")
      ],
      "comments": FakeFabricIterator(
          comment_list=[
              FakeFabric(
                  text="Простой комментарий",
                  updatedAt="2019-01-14T13:32:45.783+0000"
              )
          ]),
      "transitions": FakeFabricIterator(
          transition_list={"onTheSideOfUser": FakeFabric(
              transition="onTheSideOfUser"
          )}
      ),
      "fixVersions": FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(id='60938'),
                     FakeFabric(id='100000')
                 ]
             )
      },
     {'comment': {'summonees': 'orange13',
                  'text': text_1_hop.format(username='orange13')},
      'transition': {'onTheSideOfUser':True}
      }
     ),

    ({"author": "orange13",
      "changelog": [
          FakeFabric(
              fields=
              [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                'to': FakeFabric(id='400')}],
              updatedAt="2019-01-14T13:32:45.783+0000")
      ],
      "comments": FakeFabricIterator(
          comment_list=[
              FakeFabric(
                  text="Простой комментарий",
                  updatedAt="2019-01-14T13:32:45.783+0000"
              ),
              FakeFabric(
                  text=text_1_hop.format(username='orange13'),
                  updatedAt="2019-01-17T13:32:45.783+0000"
              ),
          ]),
      "transitions": FakeFabricIterator(
          transition_list={"onTheSideOfUser": FakeFabric(
              transition="onTheSideOfUser"
          )}
      ),
      "fixVersions": FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(id='60938'),
                     FakeFabric(id='100000')
                 ]
             )
      },
     {'comment': {'summonees': 'orange13',
                  'text': text_2_hop.format(username='orange13')},
      'transition': {'onTheSideOfUser':True}
      }),

    ({"author": "orange13",
      "changelog": [
          FakeFabric(
              fields=
              [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                'to': FakeFabric(id='400')}],
              updatedAt="2019-01-14T13:32:45.783+0000")
      ],
      "comments": FakeFabricIterator(
          comment_list=[
              FakeFabric(
                  text="Простой комментарий",
                  updatedAt="2019-01-14T13:32:45.783+0000"
              ),
              FakeFabric(
                  text=text_1_hop.format(username='orange13'),
                  updatedAt="{}T13:32:45.783+0000".format(
                      datetime.datetime.now().strftime('%Y-%m-%d'))
              ),
          ]),
      "fixVersions": FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(id='60938'),
                     FakeFabric(id='100000')
                 ]
             )
      },
     {'comment': {}}),

    ({"author": "orange13",
      "changelog": [
          FakeFabric(
              fields=
              [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                'to': FakeFabric(id='400')}],
              updatedAt="2019-01-14T13:32:45.783+0000")
      ],
      "comments": FakeFabricIterator(
          comment_list=[
              FakeFabric(
                  text="Простой комментарий",
                  updatedAt="2019-01-14T13:32:45.783+0000"
              ),
              FakeFabric(
                  text=text_2_hop.format(username='orange13'),
                  updatedAt="2019-01-17T13:32:45.783+0000"
              ),
          ]),
      "transitions": FakeFabricIterator(
          transition_list={"onTheSideOfUser": FakeFabric(transition="onTheSideOfUser")}
      ),
      "fixVersions": FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(id='60938'),
                     FakeFabric(id='100000')
                 ]
             )
      },
     {'comment': {'text': text_1_hop.format(username='orange13'),
                  'summonees': ['orange13', 'agrebenyuk']},
      'transition': {'onTheSideOfUser':True}
      }),

    ({"author": "orange13",
      "changelog": [
          FakeFabric(
              fields=
              [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                'to': FakeFabric(id='400')}],
              updatedAt="2019-01-14T13:32:45.783+0000")
      ],
      "comments": FakeFabricIterator(
          comment_list=[
              FakeFabric(
                  text="Простой комментарий",
                  updatedAt="2019-01-14T13:32:45.783+0000"
              ),
              FakeFabric(
                  text=text_2_hop.format(username='orange13'),
                  updatedAt="2019-01-17T13:32:45.783+0000"
              ),
          ]),
      "transitions": FakeFabricIterator(
          transition_list={"resolved": FakeFabric(transition="resolved")}
      ),
      "fixVersions": FakeFabricIterator(
                 fix_versions=[
                 ]
             )
      },
     {'comment': {'text': text_3_hop.format(username='orange13')},
      'transition': {'resolved':True}
      }),

    ({"author": "orange13",
      "changelog": [
          FakeFabric(
              fields=
              [{'field': FakeFabric(id='status'), 'from': FakeFabric(id='6'),
                'to': FakeFabric(id='400')}],
              updatedAt="2019-01-14T13:32:45.783+0000")
      ],
      "comments": FakeFabricIterator(
          comment_list=[
              FakeFabric(
                  text="Простой комментарий",
                  updatedAt="2019-01-14T13:32:45.783+0000"
              ),
              FakeFabric(
                  text=text_2_hop.format(username='orange13'),
                  updatedAt="{}T13:32:45.783+0000".format(
                      (datetime.datetime.now() + datetime.timedelta(days=6))
                          .strftime('%Y-%m-%d'))
              ),
          ]),
      "fixVersions": FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(id='60938'),
                     FakeFabric(id='100000')
                 ]
             )
      },
     {'comment':{}}),

]


UNITED_DASHBOARD_SLA_TESTDATA = [
    ([FakeFabric(
        key="HDRFS-1",
        fixVersions=FakeFabricIterator(
            fix_versions=[
                FakeFabric(name='MSK Morozov'),
                FakeFabric(name='MSK Avrora')
            ]
        ),
        abcService=FakeFabricIterator(
            fix_versions=[
                FakeFabric(id=3459),
                FakeFabric(id=1200)
            ]
        )),
         FakeFabric(
             key="HDRFS-2",
             fixVersions=FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(name='Birylevo'),
                     FakeFabric(name='MSK Avrora')
                 ]
             ),
             abcService=FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(id=1234),
                     FakeFabric(id=1200)
                 ]
             )),
         FakeFabric(
             key="HDRFS-3",
             fixVersions=FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(name='Kitezhgrad'),
                     FakeFabric(name='MSK Avrora')
                 ]
             ),
             abcService=FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(id=432),
                     FakeFabric(id=1200)
                 ]
             )),
         FakeFabric(
             key="HDRFS-4",
             fixVersions=FakeFabricIterator(
                 fix_versions=[
                     FakeFabric(name='MSK Morozov'),
                     FakeFabric(name='MSK Avrora')
                 ]
             ),
             abcService=FakeFabricIterator(
                 fix_versions=[]
             ))
     ],
     {"reaction_time": [
         {'sla_spent_3459': 100, 'sla_spent_0': 200, 'sla_count_3459': 1,
          'sla_count_0': 2, 'sla_spent_empty': 100, 'sla_count_empty': 1,
          'location': 'msk_morozov'},
         {'sla_spent_1234': 100, 'sla_spent_0': 100, 'sla_count_1234': 1,
          'sla_count_0': 1, 'location': 'birylevo'},
         {'sla_spent_432': 100, 'sla_spent_0': 100, 'sla_count_432': 1,
          'sla_count_0': 1, 'location': 'kitezhgrad'}],
      "reaction_count": [{'sla_ok_3459': 1, 'sla_ok_0': 2, 'sla_ok_empty': 1,
                          'location': 'msk_morozov'},
                         {'sla_ok_1234': 1, 'sla_ok_0': 1,
                          'location': 'birylevo'},
                         {'sla_ok_432': 1, 'sla_ok_0': 1,
                          'location': 'kitezhgrad'}],
      "solve_time": [
          {'sla_spent_3459': 300, 'sla_spent_0': 600, 'sla_count_3459': 1,
           'sla_count_0': 2, 'sla_spent_empty': 300, 'sla_count_empty': 1,
           'location': 'msk_morozov'},
          {'sla_spent_1234': 300, 'sla_spent_0': 300, 'sla_count_1234': 1,
           'sla_count_0': 1, 'location': 'birylevo'},
          {'sla_spent_432': 300, 'sla_spent_0': 300, 'sla_count_432': 1,
           'sla_count_0': 1, 'location': 'kitezhgrad'}],
      "solve_count": [{'sla_ok_3459': 1, 'sla_ok_0': 2, 'sla_ok_empty': 1,
                       'location': 'msk_morozov'},
                      {'sla_ok_1234': 1, 'sla_ok_0': 1,
                       'location': 'birylevo'},
                      {'sla_ok_432': 1, 'sla_ok_0': 1,
                       'location': 'kitezhgrad'}]}
    ),
]

DISMISSAL_MIRACLE_FOLDER = [
    (  
        {
            "comments": FakeFabricIterator(
                comment_list=[
                    FakeFabric(
                        text="",
                    )
                ]
            ),
            "fixVersions": FakeFabricIterator(
                 fix_versions=[
                     FakeFabric()
                 ]
             ),
            "description": "https://staff.yandex-team.ru/efim",
            "transitions": FakeFabricIterator(
                transition_list={"resolved": FakeFabric(
                    transition="resolved"
                )}
            )
        },
        
        {
            "comments": "Личной папки %%\\\\miracle.yandex.ru\\Users\\efim%% не существует.",
            "fixversions": "53054",
            
        }
     ),
]

PRINT_MONITORING = [
    (  
        {
            "currentOffice": FakeFabricIterator(
                 currunt_office=[
                     FakeFabric()
                 ]
             ),
            "fixVersions": "53054",
            "description": "prn-123.yandex-team.ru"
        },
        
        {
            "fixversions": "53054",
        }
     ),
]