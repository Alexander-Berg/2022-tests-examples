function make_headers()
    local result = {
        proxy = {
            host = "localhost"; port = backend_port;
            backend_timeout = "5s";
            connect_timeout = "1s";
            resolve_timeout = "1s";
        }; -- proxy
    };
    if enable_delete then
        result["delete"] = delete_regexp;
    end;
    if enable_create then
        result["create"] = {
            ["X-UID"] = "yandex.ru";
        };
    end;
    if enable_create_custom then
        result["create"] = {
            [header] = value;
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
    if enable_create_from_file then
        result["create_from_file"] = {
            [header] = filename;
        };
    end;
    if enable_create_weak then
        result["create_weak"] = {
            ["X-UID"] = "yandex.ru";
        };
    end;
    if enable_create_func_weak then
        result["create_func_weak"] = {
            [header] = func;
        };
    end;
    if enable_create_from_file_weak then
        result["create_from_file_weak"] = {
            [header] = filename;
        };
    end;
    if enable_append then
        result["append"] = {
            ["X-UID"] = "yandex.com";
        };
    end;
    if enable_append_weak then
        result["append_weak"] = {
            ["X-UID"] = "yandex.eu";
        };
    end;
    if enable_append_func then
        result["append_func"] = {
            [header] = func;
        };
    end;
    if enable_append_func_weak then
        result["append_func_weak"] = {
            [header] = func;
        };
    end;
    if enable_copy then
        result["copy"] = {
            [copy_src_header] = copy_dst_header;
        };
    end;
    if enable_copy_weak then
        result["copy_weak"] = {
            [copy_src_header] = copy_dst_header;
        };
    end;
    if rules_file then
        result["rules_file"] = rules_file;
    end;

    result["delimiter"] = delimiter

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

    p0f_enabled = p0f_enabled;

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
                headers = make_headers();
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
