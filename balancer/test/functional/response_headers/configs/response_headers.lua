enable_delete = enable_delete and enable_delete == "true"
enable_create = enable_create and enable_create == "true"
enable_create_multiple = enable_create_multiple and enable_create_multiple == "true"
enable_create_func = enable_create_func and enable_create_func == "true"
enable_create_weak = enable_create_weak and enable_create_weak == "true"
enable_create_func_weak = enable_create_func_weak and enable_create_func_weak == "true"

function make_headers()
    local result = {
        proxy = {
            host = "localhost"; port = backend_port;
            backend_timeout = "5s";
            connect_timeout = "1s"; resolve_timeout = "1s";
        }; -- proxy
    };
    if enable_delete then
        result["delete"] = delete_regexp;
    end;
    if enable_create then
        result["create"] = {
            ["Host"] = "yandex.ru";
        };
    end;
    if enable_create_multiple then
        result["create"] = {
            ["Host"] = "yandex.ru";
            ["Port"] = "8765";
        };
    end;
    if enable_create_func then
        result["create_func"] = {
            [header] = func;
        };
    end;
    if enable_create_weak then
        result["create_weak"] = {
            ["Host"] = "yandex.ru";
        };
    end;
    if enable_create_func_weak then
        result["create_func_weak"] = {
            [header] = func;
        };
    end;
    if enable_append then
        result["append"] = {
            ["X-UID"] = "yandex.com";
        };
    end;
    if enable_append_weak then
        result["append_weak"] = {
            ["X-UID"] = "yandex.com";
        };
    end;
    if rules_file then
        result["rules_file"] = rules_file;
    end;
    return result
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
                multiple_hosts_enabled = multiple_hosts_enabled;
                response_headers = make_headers();
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
