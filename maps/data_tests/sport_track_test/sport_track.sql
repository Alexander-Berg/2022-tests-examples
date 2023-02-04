INSERT INTO {sport_track_out} (ft_id, ft_type_id, ft_type_name, name, tags, zmin, zmax, geom) VALUES

(0, 2901, 'sport-track-ski-green', json('{"ru_RU_LOCAL":"Трасса А"}'), ['road', 'path', 'piste'],
 15, 21, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 0, 2 2, 8 2)', 4326), 3395)),

(1, 2902, 'sport-track-ski-blue',  json('{"ru_RU_LOCAL":"Трасса Б"}'), ['road', 'path', 'piste'],
 15, 21, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 2, 2 2)', 4326), 3395)),

(1, 2902, 'sport-track-ski-blue',  json('{"ru_RU_LOCAL":"Трасса Б"}'), ['road', 'path', 'piste'],
 15, 21, Spatial::Transform(Spatial::GeomFromText('LINESTRING(2 4, 2 8)', 4326), 3395));
