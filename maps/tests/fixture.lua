local fixture = {
    mock_ngx = {
        request_headers = {},
        request_uri_args = {},

        init_request = function(self, headers_table, uri_args_table)
            self.request_headers = {}
            self.request_uri_args = {}
            for header, value in pairs(headers_table) do
                self:set_request_header(header, value)
            end

            for uri_arg, value in pairs(uri_args_table) do
                self.request_uri_args[uri_arg] = value
            end
        end,

        set_request_header = function(self, header, value)
            self.request_headers[header] = value
        end,

        clear_header = function(self, header)
            self.request_headers[header] = nil
        end
    },
}

ngx = {
    req = {
        get_headers = function()
            return fixture.mock_ngx.request_headers
        end,

        set_header = function(name, value)
            fixture.mock_ngx:set_request_header(name, value)
        end,

        clear_header = function(name)
            fixture.mock_ngx:clear_header(name)
        end,

        get_uri_args = function()
            return fixture.mock_ngx.request_uri_args
        end,

        set_uri_args = function(args)
            fixture.mock_ngx.request_uri_args = args
        end
    },

    crc32_short = function(str)
        return 0
    end
}

-- mock tvm2-nginx
package.loaded['tvm2-nginx'] = {
    check_user_ticket = function()
        return 0
    end,

    attach_service_ticket = function(alias)
        if alias == 'host-name.which.is.also.a.tvmtool.alias' then
            ngx.req.set_header('Fake-Service-Ticket', 'fake service ticket')
        end
    end,
}

return fixture
