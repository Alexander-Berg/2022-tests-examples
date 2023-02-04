# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import balance.balance_db as db

# https://st.yandex-team.ru/BALANCE-8395

if __name__ == '__main__':
    login = 'isupov'

    result = db.balance().execute('select passport_id from t_passport where login = :login', {'login': login})
    passport_id = result[1]['passport_id']

    template = "exec BO.sp_set_user({passport_id}, '{login}', '{login}', p_role_id=>0)"
    dml = template.format(**{'passport_id': passport_id, 'login': login})

    db.balance().execute(dml)

    pass
