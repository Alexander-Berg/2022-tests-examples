# from datetime import (
#     date,
# )
# from importlib import import_module
#
# import yaml
# from luigi import (
#     date_interval as di
# )
# import pytest
#
# from ..grocery.task import (
#     YQLTask
# )
# from ..grocery.targets import (
#     YTTableTarget,
#     TargetParameter,
# )
# from ..grocery.tools import (
#     PROD,
#     TEST,
# )
#
# dwh_200_agg = import_module(".dwh-200-business", 'grocery')
#
#
# check_sql = """
# $test = (
# SELECT deviceos
#       ,pageid
#       ,engineid
#       ,clientid
#       ,ordertype
#       ,product
#       ,rtbshadow
#       ,devicegroup
#       ,SUM(rur_cost) as rc
#       ,SUM(cost_net) as cn
#       ,SUM(rur_cost_net) as rcn
#       ,SUM(usd_cost_net) as ucn
#       ,SUM(usd_cost) as uc
#       ,SUM(cost) as c
#       ,SUM(eur_cost) as ec
#       ,SUM(try_cost) as tc
#       ,SUM(eur_cost_net) as ecn
#       ,SUM(try_cost_net) as tcn
#       ,SUM(clicks) as clk
#       ,SUM(shows) as shw
# --FROM `//home/balance/test/dwh/business-2018-09`
# FROM {test_input}
# GROUP BY deviceos
#         ,pageid
#         ,engineid
#         ,clientid
#         ,ordertype
#         ,product
#         ,rtbshadow
#         ,devicegroup
# );
#
# $prod = (
# SELECT deviceos
#       ,pageid
#       ,engineid
#       ,clientid
#       ,ordertype
#       ,product
#       ,rtbshadow
#       ,devicegroup
#       ,SUM(rur_cost) as rc
#       ,SUM(cost_net) as cn
#       ,SUM(rur_cost_net) as rcn
#       ,SUM(usd_cost_net) as ucn
#       ,SUM(usd_cost) as uc
#       ,SUM(cost) as c
#       ,SUM(eur_cost) as ec
#       ,SUM(try_cost) as tc
#       ,SUM(eur_cost_net) as ecn
#       ,SUM(try_cost_net) as tcn
#       ,SUM(clicks) as clk
#       ,SUM(shows) as shw
# --FROM `//home/balance/prod/dwh/business-2018-09`
# FROM {prod_input}
# GROUP BY deviceos
#         ,pageid
#         ,engineid
#         ,clientid
#         ,ordertype
#         ,product
#         ,rtbshadow
#         ,devicegroup
# );
#
# insert into {output} with truncate
# SELECT COUNT(*) as num_of_bad
# FROM (
#     SELECT p.deviceos as deviceos
#           ,p.pageid as pageid
#           ,p.engineid as engineid
#           ,p.clientid as clientid
#           ,p.ordertype as ordertype
#           ,p.product as product
#           ,p.rtbshadow as rtbshadow
#           ,p.devicegroup as devicegroup
#           ,ABS((p.rc ?? 0.0) - (t.rc ?? 0.0)) as d_rur_cost
#           ,ABS((p.cn ?? 0.0) - (t.cn ?? 0.0)) as d_cost_net
#           ,ABS((p.rcn ?? 0.0) - (t.rcn ?? 0.0)) as d_rur_cost_net
#           ,ABS((p.ucn ?? 0.0) - (t.ucn ?? 0.0)) as d_usd_cost_net
#           ,ABS((p.uc ?? 0.0) - (t.uc ?? 0.0)) as d_usd_cost
#           ,ABS((p.c ?? 0.0) - (t.c ?? 0.0)) as d_cost
#           ,ABS((p.ec ?? 0.0) - (t.ec ?? 0.0)) as d_eur_cost
#           ,ABS((p.tc ?? 0.0) - (t.tc ?? 0.0)) as d_try_cost
#           ,ABS((p.ecn ?? 0.0) - (t.ecn ?? 0.0)) as d_eur_cost_net
#           ,ABS((p.tcn ?? 0.0) - (t.tcn ?? 0.0)) as d_try_cost_net
#           ,ABS((p.clk ?? 0) - (t.clk ?? 0)) as d_clicks
#           ,ABS((p.shw ?? 0) - (t.shw ?? 0)) as d_shows
#     FROM
#     $prod as p
#     JOIN
#     $test as t
#     ON (
#         p.deviceos = t.deviceos
#     AND p.pageid = t.pageid
#     AND p.engineid = t.engineid
#     AND p.clientid = t.clientid
#     AND p.ordertype = t.ordertype
#     AND p.product = t.product
#     AND p.rtbshadow = t.rtbshadow
#     AND p.devicegroup = t.devicegroup
#     )
# )
# WHERE d_clicks > 1 or d_cost > 3
# """
#
#
# class Compare(YQLTask):
#     yql = check_sql
#     test_input = TargetParameter()
#     prod_input = TargetParameter()
#     output_table = TargetParameter()
#
#     def input(self):
#         return {
#             "test": self.test_input,
#             "prod": self.prod_input,
#         }
#
#     def output(self):
#         return self.output_table
#
#
# class IndevCookedBusiness(grocery.dwh_200_agg.CookedBusiness):
#     yql_path = "./dwh/usr/bin/dwh/dwh-200/cooked_yql.sql"
#
#
# class TestDWH200Aggregation:
#
#     @pytest.mark.long
#     def test_aggregation(self):
#         month = di.Month.parse(
#             date.today().strftime("%Y-%m")
#         ).prev(3)
#
#         dates = month.dates()
#         date_min, date_max = dates[0], dates[-1]
#
#         next_month = month.next()
#         second_day = next_month.dates()[1]
#         first_day = next_month.dates()[0]
#
#         indev_business = IndevCookedBusiness(
#             force=True,
#             month=month,
#             no_mnclose=True,
#             disable=['simple_eventcost'],
#             kwargs={
#                 'begin_date': f"{date_min:%Y-%m-%d}",
#                 'end_date': f"{date_max:%Y-%m-%d}",
#                 'undo_end_date': f"{first_day:%Y-%m-%d}",
#                 'second_day': f"{second_day:%Y-%m-%d}",
#             },
#         )
#
#         # indev_business.run()
#
#         with open(f"./dwh/etc/dwh/conf.{TEST}.yaml") as tf:
#             test_conf = yaml.safe_load(tf)
#
#         with open(f"./dwh/etc/dwh/conf.{PROD}.yaml") as tf:
#             prod_conf = yaml.safe_load(tf)
#
#         test_input = indev_business.output()
#         assert isinstance(test_input, YTTableTarget)
#
#         prod_input = YTTableTarget(
#             f"{prod_conf['YT']['PATH']}/business-{month}"
#         )
#         output = YTTableTarget(
#             f"{test_conf['YT']['PATH']}/tmp/compare_result"
#         )
#
#         c = Compare(
#             test_input=test_input,
#             prod_input=prod_input,
#             output_table=output
#         )
#         c.run()
#
#         records = output.read()
#         assert records[0]['num_of_bad'] == 0
