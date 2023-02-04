function make_cookies()
    local result = {
        proxy = {
            host = "localhost"; port = backend_port;
            backend_timeout = "5s";
            connect_timeout = "1s"; resolve_timeout = "1s";
        }; -- proxy
    };
    if delete_regexp then
        result["delete"] = delete_regexp;
    end;
    if enable_create then
        result["create"] = {
            ["Foo"] = "bar";
        };
    end;
    if create_func then
        result["create_func"] = {
            ["func"] = create_func;
        };
    end;
    if enable_create_multiple then
        result["create"] = {
            ["Foo"] = "bar";
            ["boo"] = "moo";
        };
    end;
    if enable_create_weak then
        result["create_weak"] = {
            ["Foo"] = "bar";
        };
    end;
    if create_func_weak then
        result["create_func_weak"] = {
            ["func"] = create_func_weak;
        };
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
                cookies = make_cookies();
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
