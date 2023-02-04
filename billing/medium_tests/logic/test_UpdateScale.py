# -*- coding: utf-8 -*-

from balance.mapper import Service, ProductUnit
import uuid
from datetime import datetime
from dateutil.relativedelta import relativedelta
from tests.base import MediumTest



class TestScale(MediumTest):

    def test_create_and_update_scale(self):

        ses = self.session
        service = (ses.query(Service)
                   .filter(Service.token.isnot(None))
                   ).first()

        code = str(uuid.uuid4())

        x_unit, y_unit = (ses.query(ProductUnit)
                          .limit(2)
                          ).all()

        # Проверяю создание шкалы
        res = self.xmlrpcserver.CreateScale(
            ses.oper_id,
            dict(service_token=service.token,
                 code=code,
                 type='staircase',
                 comments='Some comments',
                 x_unit_id=x_unit.id,
                 y_unit_id=y_unit.id,
                 ))

        self.assertEqual(res,
                         {'COMMENTS': 'Some comments',
                          'NAMESPACE': str(service.cc),
                          'SCALE_CODE': str(code),
                          'SCALE_TYPE': 'staircase',
                          'POINTS': [],
                          'X_UNIT': x_unit.id,
                          'Y_UNIT': y_unit.id,
                          },
                         "Test creating scale")

        # Проверяю добавление точек в пустую шкалу
        future_dt_1 = (datetime.now().replace(hour=0, minute=0, second=0, microsecond=0) +
                       relativedelta(months=1)
                       ).replace(day=1)

        max_sum = '10000.5'
        points1 = [dict(x='1', y='1'), dict(x='10', y='2'), dict(x='100', y='3')]
        points1 = [dict(p, max_sum=max_sum)
                   for p in points1]

        res = self.xmlrpcserver.UpdateScale(
            ses.oper_id,
            dict(service_token=service.token,
                 code=code,
                 start_dt=future_dt_1,
                 scale_points=points1,
                 ))

        expected_points1 = [dict(x=p['x'], y=p['y'], max_sum=max_sum,
                                 start_dt=str(future_dt_1), end_dt=None)
                            for p in points1]

        self.assertEqual(res,
                         {'COMMENTS': 'Some comments',
                          'NAMESPACE': str(service.cc),
                          'SCALE_CODE': str(code),
                          'SCALE_TYPE': 'staircase',
                          'POINTS': expected_points1,
                          'X_UNIT': x_unit.id,
                          'Y_UNIT': y_unit.id,
                          })

        # Проверяю добавление точек в шкалу, которая уже имеет точки
        future_dt_2 = (future_dt_1 +
                       relativedelta(months=1)
                       ).replace(day=1)

        points2 = [dict(x='1', y='4'), dict(x='10', y='5'), dict(x='100', y='6')]
        points2 = [dict(p, max_sum=max_sum)
                   for p in points2]

        res = self.xmlrpcserver.UpdateScale(
            ses.oper_id,
            dict(service_token=service.token,
                 code=code,
                 start_dt=future_dt_2,
                 scale_points=points2,
                 ))

        expected_points2 = ([dict(p, end_dt=str(future_dt_2))
                             for p in expected_points1
                             ] +
                            [dict(x=p['x'], y=p['y'], max_sum=max_sum,
                                  start_dt=str(future_dt_2), end_dt=None)
                             for p in points2]
                            )

        self.assertEqual(res,
                         {'COMMENTS': 'Some comments',
                          'NAMESPACE': str(service.cc),
                          'SCALE_CODE': str(code),
                          'SCALE_TYPE': 'staircase',
                          'POINTS': expected_points2,
                          'X_UNIT': x_unit.id,
                          'Y_UNIT': y_unit.id,
                          })

        # Проверяю замену существующих точек
        points2v2 = [dict(x='10', y='7'), dict(x='100', y='8'), dict(x='1000', y='9')]
        points2v2 = [dict(p, max_sum=max_sum)
                     for p in points2v2]

        res = self.xmlrpcserver.UpdateScale(
            ses.oper_id,
            dict(service_token=service.token,
                 code=code,
                 start_dt=future_dt_2,
                 scale_points=points2v2,
                 update_existing=1,
                 ))

        expected_points2v2 = ([dict(p, end_dt=str(future_dt_2))
                              for p in expected_points1
                               ] +
                              [dict(x=p['x'], y=p['y'], max_sum=max_sum,
                                    start_dt=str(future_dt_2), end_dt=None)
                               for p in points2v2]
                              )

        self.assertEqual(res,
                         {'COMMENTS': 'Some comments',
                          'NAMESPACE': str(service.cc),
                          'SCALE_CODE': str(code),
                          'SCALE_TYPE': 'staircase',
                          'POINTS': expected_points2v2,
                          'X_UNIT': x_unit.id,
                          'Y_UNIT': y_unit.id,
                          })
