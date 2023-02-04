
function get_match()
    path_val = '/(CamelCase|StAiRwAy)'

    if second_matcher then return {
        path = path_val;
        host = "yandex[.]ru";
        case_insensitive = case_insensitive;
    } else return {
        path = path_val;
        case_insensitive = case_insensitive;
    } end
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
                regexp = {
                    match = {
                        match_fsm = get_match();
                        errordocument = {
                            status = 200;
                            content = 'match';
                        }; -- errordocument
                    }; -- match
                    default = {
                        errordocument = {
                            status = 200;
                            content = 'default';
                        }; -- errordocument
                    }; -- default
                }; -- regexp
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
