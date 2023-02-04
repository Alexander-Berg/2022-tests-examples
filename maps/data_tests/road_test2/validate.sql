SELECT count(*) = 1 FROM {road} WHERE zmax=10

UNION ALL

SELECT count(*) = 5 FROM {road} WHERE zmin=15

UNION ALL

SELECT Spatial::Equals(geom, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 0, 0 1)', 4326), 3395))
FROM {road} WHERE f_zlev=0 AND t_zlev=0 AND zmin=15

UNION ALL

SELECT Spatial::Equals(geom, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395))
FROM {road} WHERE f_zlev=0 AND t_zlev=1 AND zmin=15

UNION ALL

SELECT Spatial::Equals(geom, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 2, 0 4)', 4326), 3395))
FROM {road} WHERE f_zlev=1 AND t_zlev=1 AND zmin=15

UNION ALL

SELECT Spatial::Equals(geom, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 4, 0 5)', 4326), 3395))
FROM {road} WHERE f_zlev=1 AND t_zlev=2 AND zmin=15

UNION ALL

SELECT Spatial::Equals(geom, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 5, 0 6)', 4326), 3395))
FROM {road} WHERE f_zlev=2 AND t_zlev=2 AND zmin=15
