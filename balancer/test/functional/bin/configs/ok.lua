function gen_config_check()
    if mode == 'extended' then
        return {};
    end
    return nil
end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = real_port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    config_check = gen_config_check();

    ipdispatch = {
        test = {
            ip = "localhost";
            port = port;
            http = {
                errordocument = {
                    status = 200;
                    content = "OK";
                }; -- errordocument
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
