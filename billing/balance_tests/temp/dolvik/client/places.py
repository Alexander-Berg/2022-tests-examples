# -*- coding: utf-8 -*-

import balance.balance_steps as steps

client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

page_ids = [3010, 10003, 4009]
# создаем площадки
place_id, _ = steps.DistributionSteps.create_distr_place(client_id, tag_id, page_ids)

for i in range(50):
    steps.DistributionSteps.create_distr_place(client_id, tag_id, page_ids)
