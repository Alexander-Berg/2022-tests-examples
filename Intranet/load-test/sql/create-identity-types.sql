insert into identity_type(id, is_group) values
    ('test_type_id', false),
    ('test_type_id2', true)
ON CONFLICT DO NOTHING;

