DO $$
DECLARE api_key text;
DECLARE plan_id text;
DECLARE provider_id text;
BEGIN
INSERT INTO apiteka.api_key (id, secret, restrictions, origin)
    VALUES ('12345678-dead-beef-cafe-babe12345678', 'supersecret', '{"signature": true}', 'manual')
    RETURNING id INTO api_key;

INSERT INTO apiteka.provider (id, abc_slug)
    VALUES ('maps-static-api', 'maps-core-renderer-staticapi')
    RETURNING id INTO provider_id;

INSERT INTO apiteka.plan (id, origin, provider_id, features)
    VALUES ('maps-static-api@freemium', 'manual', provider_id, '{"maxSize": "500,500", "allowLogoDisabling": "False"}')
    RETURNING id INTO plan_id;

INSERT INTO apiteka.assignments (provider_id, plan_id, api_key)
    VALUES (provider_id, plan_id, api_key);
END $$;
