# -*- coding: utf-8 -*-

import balance.balance_steps as steps
import balance.balance_db as db

client_id_1 = steps.ClientSteps.create(params={'NAME': 'Никифор I'})
client_id_2 = steps.ClientSteps.create(params={'NAME': 'Никифор II'})

db.balance().execute('UPDATE t_client SET class_id = :client_id_2 WHERE id = :client_id_1',
                     {'client_id_1': client_id_1, 'client_id_2': client_id_2})
