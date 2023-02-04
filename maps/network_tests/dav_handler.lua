local HTTP_NO_CONTENT = 204 -- old versions of lua don't have this constant
local TEST_DIR = '/tmp/test/dav'
local TEST_PARENT_DIR = '/tmp/test'
local GARBAGE_COLLECT_TIMEOUT = 60

local function deleteSilent(premature, filename)
    os.remove(filename)
end

local function delete(filename)
    local result = os.remove(filename)
    if result == nil then
        ngx.exit(ngx.HTTP_NOT_FOUND)
    else
        ngx.exit(HTTP_NO_CONTENT)
    end
end

local function put(filename)
    local data = ngx.req.get_body_data()
    local file = io.open(filename, 'r')
    local successResponse
    if file == nil then
        successResponse = ngx.HTTP_CREATED
        os.execute('mkdir -p ' .. TEST_DIR)
    else
        successResponse = HTTP_NO_CONTENT
        file:close()
    end

    file = io.open(filename, 'w')
    if file == nil then
        ngx.exit(ngx.HTTP_INTERNAL_SERVER_ERROR)
    else
        if data ~= nil then
            file:write(data)
        end
        file:close()
        ngx.timer.at(GARBAGE_COLLECT_TIMEOUT, deleteSilent, filename)
        ngx.exit(successResponse)
    end
end

local function get(filename)
    local file = io.open(filename, 'r')
    if file == nil then
        ngx.exit(ngx.HTTP_NOT_FOUND)
    else
        local content = file:read "*all"
        file:close()
        ngx.print(content)
        ngx.exit(ngx.HTTP_OK)
    end
end

local method = ngx.req.get_method()
local filename = TEST_DIR .. '/' .. ngx.var[1]
if method == 'DELETE' then
    delete(filename)
elseif method == 'PUT' then
    put(filename)
elseif method == 'GET' then
    get(filename)
end

