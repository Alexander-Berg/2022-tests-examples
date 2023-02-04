SELECT count(*) = 4 FROM {road} WHERE zmin=15

UNION ALL

SELECT Spatial::Equals(geom, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 3)', 4326), 3395))
FROM {road}
WHERE class='1' AND oneway='0' AND zmin=15

UNION ALL

SELECT Spatial::Equals(geom, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 3, 0 6)', 4326), 3395))
FROM {road}
WHERE class='1' AND oneway='1' AND zmin=15

UNION ALL

SELECT count(*) = 3 FROM {road} WHERE zmax=10

UNION ALL

-- one-way and two-way merged into one
SELECT count(*) = 1 FROM {road} WHERE class='1' AND zmax=10

-- toll and free roads are not merged
UNION ALL
SELECT count(*) = 1 FROM {road} WHERE class='2' AND toll = '1' AND zmax=10
UNION ALL
SELECT count(*) = 1 FROM {road} WHERE class='2' AND toll = '0' AND zmax=10
