SELECT sort_order=5 FROM {road} WHERE class='10_pedestrian' AND detail=''

UNION ALL

SELECT sort_order=9 FROM {road} WHERE class='10_pedestrian' AND detail='7_pedestrian'

UNION ALL

SELECT sort_order=9 FROM {road} WHERE class='7'

UNION ALL

SELECT sort_order=9 FROM {road} WHERE class='7_pedestrian'

UNION ALL

SELECT sort_order=20 FROM {road} WHERE class='1_link'

UNION ALL

SELECT sort_order=21 FROM {road} WHERE class='1' AND `struct`='none'

UNION ALL

SELECT sort_order=21.25 FROM {road} WHERE class='1' AND `struct`='bridge'

UNION ALL

SELECT sort_order=20.75 FROM {road} WHERE class='1' AND `struct`='tunnel'

UNION ALL

SELECT sort_order=22 FROM {road} WHERE class='10_bike'

UNION ALL

SELECT sort_order=24 FROM {road} WHERE class='ferry'
