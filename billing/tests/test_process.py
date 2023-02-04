# from xml.etree import ElementTree as et
#
# import pandas as pd
# import sqlalchemy
#
# from ..grocery.processing.process import (
#     Process,
#     DUMP_TEMPLATE
# )
#
#
# def alchemy_connect(filename, dbid):
#     xml = et.parse(f'/etc/yandex/balance-common/db-conn-{filename}.cfg.xml')
#     node = xml.find(f".[@id='{dbid}']") or xml.find(f"./DbBackend[@id='{dbid}']")
#     user = node.findtext("User")
#     pass_ = node.findtext("Pass")
#     host = node.findtext("Host")
#     return sqlalchemy.create_engine(f'oracle+cx_oracle://{user}:{pass_}@{host}')
#
#
# class TestProcess:
#
#     def setUp(self):
#         # print(os.path.abspath(os.path.curdir))
#         # self.process = Process(os.path.abspath('./dwh-200.yaml'))
#         self.process = Process('./grocery/dwh-200.yaml')
#         self.common = {
#             'balance_ro': alchemy_connect("balance", "balance_ro"),
#             'meta': alchemy_connect('meta', 'meta'),
#             'begin_dt': '2017-11-01',
#             'end_dt': '2017-11-02',
#         }
#
#     # def test_connection(self):
#         # print(type(self.common['meta']))
#         # assert type(self.common['meta']) == int
#
#     # def test_extract_market(self):
#     #     # reward = pd.read_json("/home/ecialo/dwh-200-7-reward-extract_reward.json")
#     #     # reward = pd.read_json("../dwh-200-7-reward-extract_reward.json", orient='table')
#     #     reward = pd.read_pickle("../dwh-200-07-reward-extract_reward.pcl")
#     #     stat = pd.read_excel('./stat-2017-11.xls')
#     #     env = {
#     #         'reward': reward,
#     #         'stat': stat
#     #     }
#     #     self.process.run_stage({**env, **self.common}, stagename='extract_market')
#
#     def test_process(self):
#         rows = pd.read_json("./sampled.json")
#         pages = pd.read_json("./pages-2017-11.json")
#         stat = pd.read_excel('./stat-2017-11.xls')
#         self.process.run_process(rows=rows, stat=stat, pages=pages, **self.common, sampled=True)
#
#     # def test_wrong_types(self):
#     #     rows = pd.read_json("./sampled.json")
#     #     pages = pd.read_json("./pages-2017-11.json")
#     #     stat = pd.read_excel('./stat-2017-11.xls')
#     #     rows['shows'] = "ololo"
#     #     try:
#     #         self.process.run_process(rows=rows, stat=stat, pages=pages, **self.common)
#     #     except AssertionError:
#     #         pass
#     #     else:
#     #         assert False, "no exception raised"
