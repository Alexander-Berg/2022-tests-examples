function gen_config_check()
    if mode == 'extended' then
        return {};
    end
    return nil
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "0100::10"; port = port; };
    }; -- addrs

    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; };
        };
    };

    config_check = gen_config_check();
    ignore_bind_errors_file = ignore_bind_errors_file;

    ipdispatch = {
        test = {
            ip = "0100::10";
            port = real_port or port;
            http = {
                errordocument = {
                    status = 200;
                    content = "OK";
                }; -- errordocument
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
