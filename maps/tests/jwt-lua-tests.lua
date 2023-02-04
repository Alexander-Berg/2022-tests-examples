local source_root = os.getenv('ARCADIA_SOURCE_ROOT')
assert(source_root ~= nil)

local test = require "testing"
local describe, it = test.describe, test.it

local jwt = require "libjwt_lua_cppmodule"

-- see if the file exists
function file_exists(file)
  local f = io.open(file, "rb")
  if f then f:close() end
  return f ~= nil
end

-- get all lines from a file, returns an empty
-- list/table if the file does not exist
function lines_from(file)
  assert(file_exists(file))
  local lines = nil
  for line in io.lines(file) do
    if lines ~= nil then
        lines = lines .. '\n' .. line
    else
        lines = line
    end
  end
  return lines
end

-- initialize library with constant keys
function init_jwt_lib(pub_key, priv_key)
    local key_id = 'keyId'

    issuers_to_public_keys = {
        ["issuerName"] = {[key_id] = pub_key}
    }
    jwt.init_from_constants(
        'issuerName',
        'verifierName',
        priv_key,
        key_id,
        issuers_to_public_keys
    )
end

-- ensure func will fail if payload has unknown fields or don't have required ones
function ensure_bad_payload_fails(payload, func, required_field_name, unknown_field_name)
    local payload_without_required_field = copy_table(payload)
    payload_without_required_field[required_field_name] = nil
    local status, err = pcall(function() func(payload_without_required_field, { "verifierName" }) end)
    assert(status == false)

    local payload_with_unknown_field = copy_table(payload)
    payload_with_unknown_field[unknown_field_name] = "this is bad payload"
    local status, err = pcall(function() func(payload_with_unknown_field, { "verifierName" }) end)
    assert(status == false)
end

function copy_table(t)
    local t2 = {}
    for k,v in pairs(t) do
        local v2 = v
        if type(v) == 'table' then
            v2 = copy_table(v)
        end
        t2[k] = v
    end
    return t2
end


local public_key = lines_from(source_root .. '/maps/b2bgeo/libs/jwt-lua/tests/jwtES256.key.pub')
local private_key = lines_from(source_root .. '/maps/b2bgeo/libs/jwt-lua/tests/jwtES256.key')

local public_key_second = lines_from(source_root .. '/maps/b2bgeo/libs/jwt-lua/tests/jwtES256_second.key.pub')
local private_key_second = lines_from(source_root .. '/maps/b2bgeo/libs/jwt-lua/tests/jwtES256_second.key')

local test_keycloak_user = {
    ["uid"] = "smith@example.com",
    ["login"] = "login"
}

describe('jwt_lua', function()
    it('test_jwt_use_uninitialized', function()
        assert(jwt.is_initialized() == false)

        local status, err = pcall(function() jwt.create_keycloak_jwt(test_keycloak_user, { "verifierName" }) end)
        assert(status == false)

        local token = "eyJhbGciOiJFUzI1NiIsImtpZCI6ImtleUlkIiwidHlwIjoiSldUIn0.eyJhdWQiOlsidmVyaWZpZXJOYW1lIl0sImV4cCI6MTY1MzQ2NzExMywiaXNzIjoiaXNzdWVyTmFtZSIsIm5iZiI6MTY1MzQ2NjIxMywic3ViIjoidXNlcl91aWQiLCJzdWJqZWN0Ijp7ImNvbXBhbnlfaWRzIjpbImNvbXBhbnlfaWRfMSIsImNvbXBhbnlfaWRfMiJdLCJpZCI6InVzZXJfaWQiLCJpc19zdXBlciI6dHJ1ZSwicm9sZSI6ImFkbWluIiwidHlwZSI6InVzZXIifX0.HJBcp7wtAvdNnccED5mp3TTZ4Yf77nboHPZvqTtgXy4fa5KHrE0CfJUO23L-zLGF8xKFbWGx7JG34_C8XYtVTA"
        local status, err = pcall(function() jwt.parse_keycloak_jwt(token, "issuerName") end)
        assert(status == false)
    end)

    it('test_jwt_initialize_from_env_variables', function()
        assert(jwt.is_initialized() == false)

        issuers_to_public_keys = {
            ["issuerName"] = "PUBLIC_KEY_MAP"
        }
        jwt.init_from_env_variables(
            'issuerName',
            'verifierName',
            'PRIVATE_KEY',
            'KEY_ID',
            issuers_to_public_keys
        )

        assert(jwt.is_initialized() == true)
    end)

    it('test_jwt_initialize_from_env_variables_failure', function()
        issuers_to_public_keys = {
            ["issuerName"] = "UNEXISTENT_VAR_NAME"
        }
        local status, err = pcall(function()
            jwt.init_from_env_variables(
                'issuerName',
                'verifierName',
                'UNEXISTENT_VAR_NAME',
                'UNEXISTENT_VAR_NAME',
                issuers_to_public_keys
            )
        end)
        assert(status == false)
    end)

    it('test_jwt_verify', function()
        init_jwt_lib(public_key, private_key)
        local token = jwt.create_keycloak_jwt(test_keycloak_user, { "verifierName", "otherVerifierName" })

        local status, err = pcall(function() jwt.verify_jwt(token, "issuerName") end)
        assert(status == true)

        local status, err = pcall(function() jwt.verify_jwt(token, "wrongIssuerName") end)
        assert(status == false)
    end)

    it('test_jwt_create_and_check_for_keycloak_user', function()
        init_jwt_lib(public_key, private_key)

        local token = jwt.create_keycloak_jwt(test_keycloak_user, { "verifierName", "otherVerifierName" })
        local parsed_payload = jwt.parse_keycloak_jwt(token, "issuerName")

        assert(parsed_payload["uid"] == test_keycloak_user["uid"])
        assert(parsed_payload["login"] == test_keycloak_user["login"])
    end)

    it('test_jwt_fail_bad_keycloak_user', function()
        init_jwt_lib(public_key, private_key)

        ensure_bad_payload_fails(
            test_keycloak_user,
            jwt.create_company_jwt,
            "uid",
            "keycloakDescription"
        )
    end)

    it('test_jwt_fail_wrong_issuer', function()
        init_jwt_lib(public_key, private_key)
        local token = jwt.create_keycloak_jwt(test_keycloak_user, { "verifierName" })

        local status, err = pcall(function() jwt.parse_keycloak_jwt(token, "wrongIssuerName") end)
        assert(status == false)
    end)

    it('test_jwt_fail_wrong_audience', function()
        init_jwt_lib(public_key, private_key)
        local token = jwt.create_keycloak_jwt(test_keycloak_user, { "wrongVerifierName1", "wrongVerifierName2" })

        local status, err = pcall(function() jwt.parse_keycloak_jwt(token, "issuerName") end)
        assert(status == false)
    end)

    it('test_jwt_fail_wrong_key', function()
        init_jwt_lib(public_key, private_key)
        local wrong_key_token = jwt.create_keycloak_jwt(test_keycloak_user, { "verifierName" })

        init_jwt_lib(public_key_second, private_key_second)
        local status, err = pcall(function() jwt.parse_keycloak_jwt(wrong_key_token, "issuerName") end)
        assert(status == false)

        local token = jwt.create_keycloak_jwt(test_keycloak_user, { "verifierName" })
        local parsed_payload = jwt.parse_keycloak_jwt(token, "issuerName")
        assert(parsed_payload["id"] == test_keycloak_user["id"])
    end)
end)
