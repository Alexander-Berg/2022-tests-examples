tvm2lua = require 'libticket_parser2_lua'

local ok = tvm2lua.initialize_via_tvmtool('')
assert(not ok)

local valid_service_ticket = '3:serv:CBAQ__________9_IgUIZBDIAQ:EVOhMP4A7r540XogWRADiGfI8AFmkY_A4jw_auN'..
        'GrKbpqe9e-vWQRXJvtZgDp1VNNqCpinTB1P_PSKcpugY_BuWes9sJXHHX9OEH7bPL3PmFGD-K'..
        'U04c91M0G7EfUuf4_We52jUqq4m9quNPxaHihsvBwCRIz8CdY8Ta_Ptyfs4'

-- init mock with pre-configured tvm-ids and tickets
local ok = tvm2lua.initialize_via_mock(
    100,  -- self tvm-id
    { superduper = 200 },  -- target tvm-id
    { [200] = valid_service_ticket }
)
assert(ok)

-- invalid target alias
local ticket = tvm2lua.fetch_service_ticket('invalid-dest');
assert(ticket == '')

local ticket = tvm2lua.fetch_service_ticket('');
assert(ticket == '')

-- success issue ticket
local got_ticket = tvm2lua.fetch_service_ticket('superduper');
assert(got_ticket == valid_service_ticket)


-- ticket validation success
local ok = tvm2lua.initialize_via_mock(200, {}, {})  -- set self tvm-id to 200
assert(ok)

local unpacked_ticket = tvm2lua.parse_service_ticket(valid_service_ticket);
assert(unpacked_ticket.status == tvm2lua.STATUS_OK)
assert(unpacked_ticket.source_id == '100')


-- invalid destination
local ok = tvm2lua.initialize_via_mock(100500, {}, {})
assert(ok)

local unpacked_ticket = tvm2lua.parse_service_ticket(valid_service_ticket);
assert(unpacked_ticket.status == tvm2lua.STATUS_INVALID_DST, 'Invalid status: ' .. unpacked_ticket.status)
assert(not unpacked_ticket.source_id)

-- invalid ticket
local unpacked_ticket = tvm2lua.parse_service_ticket('bullshit');
assert(unpacked_ticket.status == tvm2lua.STATUS_MALFORMED, 'Invalid status: ' .. unpacked_ticket.status)
assert(not unpacked_ticket.source_id)

local unpacked_ticket = tvm2lua.parse_service_ticket('');
assert(unpacked_ticket.status == tvm2lua.STATUS_MALFORMED)
assert(not unpacked_ticket.source_id)

-- user tickets tests

local valid_user_ticket = '3:user:CA0Q__________9_GiIKAwiZAQoDCM0CEJkBGgNoYnoaBmtla2VrZSDShdjMBCgB:'..
        'CS9uv0c-c6-2dd4xuifJsEuneoDeCHQx7myp5mW7Xnb_yyubWx_9P8TnhSaHh_ByUcLlwcRRBEmRcO817nNN'..
        'dzfHldh-CHlrXFAW8QizJWnNExYK2ksRL9U0onzGv4UkfwvSNWmkbzLcv2O3_XXzchHl36sjjoRZNgmbsu4jvaI'

local uid = tvm2lua.parse_user_ticket_uid(valid_user_ticket);
assert(uid == '153', 'Got invalid uid:' .. (uid or 'nil'))

-- invalid user ticket
local uid = tvm2lua.parse_user_ticket_uid('3:user:garbage');
assert(uid == nil or uid == '')

-- service ticket in place of user
local uid = tvm2lua.parse_user_ticket_uid(valid_service_ticket);
assert(uid == nil or uid == '')
