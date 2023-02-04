SELECT count(*) = 2 FROM {road} WHERE zmin=12

UNION ALL

SELECT
  Spatial::Equals(geom, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 0, 0 6)', 4326), 3395)) OR
  Spatial::Equals(geom, Spatial::Transform(Spatial::GeomFromText('LINESTRING(1 0, 1 6)', 4326), 3395))
FROM {road} WHERE zmin=12
