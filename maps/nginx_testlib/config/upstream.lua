-- Performs IPC communications through request and response files:
-- writes incoming data into the request_file and reads outgoing data
-- from the response_file.


local request_file = ngx.var.request_file
local response_file = ngx.var.response_file

ngx.req.read_body()
local request_body = ngx.req.get_body_data()
if request_body == nil then
    request_body = ""
end

local headers = ngx.req.get_headers()
local headers_count = 0
for header, value in pairs(headers) do
    headers_count = headers_count + 1
    ngx.log(ngx.INFO, "> Header: " .. header .. ": " .. value)
end
ngx.log(ngx.INFO, "> Body: " .. request_body)

local requests = io.open(request_file, "a")
requests:write(string.format("%s\n%s\n%s\n%d\n%s",
                             headers["host"] or "",
                             ngx.req.get_method(),
                             ngx.var.request_uri,
                             request_body:len(),
                             request_body))

requests:write(string.format("%s\n", headers_count))
for header, value in pairs(headers) do
    requests:write(string.format("%s\n%s\n", header, value))
end

requests:close()

-- Read the response
local responses = io.open(response_file)
    or error(string.format('Bad test upstream configuration: %s', response_file))
ngx.status = responses:read("*number")
local response_body_size = responses:read("*number")
responses:read("*line") -- Skip newline
local response_body = responses:read(response_body_size)
if response_body ~= nil then
    ngx.print(response_body)
    ngx.log(ngx.INFO, "< Body: " .. response_body)
end

-- Read other responses
local other_responses = responses:read("*all")
responses:close()

-- Remove the used response by writing other responses only
os.remove(response_file)
responses = io.open(response_file, "w")
responses:write(other_responses)
responses:close()
