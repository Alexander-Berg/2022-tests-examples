SELECT count(*) = 1 FROM {road} WHERE zmax=10

UNION ALL

SELECT count(*) = 3 FROM {road} WHERE zmin=15

UNION ALL

SELECT class='1' FROM {road} WHERE zmax=10
