-- prepare to use tvmtool mock recipe

local RECIPE_CONFIG = 'maps/infra/tvm2lua/tests/tvmtool.mock.conf'

local mock_environment = {
    CONFIG_FILE = 'tvmtool.conf',
    AUTH_FILE = 'tvmtool.authtoken',
    PORT_FILE = 'tvmtool.port',
}

local function read_file(name)
    local f = io.open(name, 'rb')
    local content = f:read('*a')
    f:close()
    return content
end
local function write_file(name, content)
    local f = io.open(name, 'w')
    f:write(content)
    f:close()
end

local arcadia_root = arg[1] -- expect arcadia root path in first argument

-- patch config with recipe port
local port = read_file(mock_environment.PORT_FILE)
local config = read_file(arcadia_root .. '/' .. RECIPE_CONFIG)
config = config:gsub('"port": [0-9]+', '"port": ' .. port)
write_file(mock_environment.CONFIG_FILE, config)

return mock_environment
