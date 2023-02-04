function get_headers()
    if headers then
        return {
            Xxx = '999';
            Zzz = '000';
        };
    elseif bad_header_name ~= nil then
        return {
            [bad_header_name] = bad_header_value;
        };
    else
        return None;
    end
end

instance = {
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; disabled = 0; }; --[[ SLB:  ]]
    };

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
    };

    thread_mode = thread_mode; set_no_file = false;

    _sock_inbufsize = socket_buffer;
    _sock_outbufsize = socket_buffer;

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
            errorlog = {
                log = errorlog;
                http = {
                    maxlen = 65535; maxreq = 65535;
                    accesslog = {
                        log = accesslog;
                        report = {
                            uuid = 'total';
                            errordocument = {
                                status = status;
                                content = content;
                                base64 = c_base64;
                                file = c_file;
                                force_conn_close = force_conn_close;
                                remain_headers = remain_headers;
                                headers = get_headers()
                            }; -- errordocument
                        };

                    }; -- accesslog
                }; -- http

            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
