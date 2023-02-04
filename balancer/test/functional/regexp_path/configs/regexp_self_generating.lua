function generate_regexp_path(count_value)
    local count = tonumber(count_value)
    local width = 0
    if count <= 0 then
        error("count must be positive number")
    else
        local current = count
        while current > 1 do
            width = width + 1
            current = current / 10
        end
    end

    local format = string.format("/%%0%dd.*", width)

    retval = {}
    for i = 0, count - 1 do
        retval[string.format("r_%d", i)] = {
            pattern = string.format(format, i);
            priority = 1;
            case_insensitive = true;

            errordocument = {
                status = 200;
                content = string.format("%d", i);
            };
        };
    end

    retval.default = {
        errordocument = {
            status = 200;
            content = "default";
        };
    };

    return retval;
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    }; -- admin_addrs

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                regexp_path = generate_regexp_path(count);
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
