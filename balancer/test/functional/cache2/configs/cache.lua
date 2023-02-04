if cache_ttl == nil then cache_ttl = '15s'; end

instance = {
    thread_mode = thread_mode; set_no_file = false;
    workers = 5;
    storage_gc_required = true;
    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
        hide_legacy_signals = true;
    };

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                admin = {};
            }; -- http
        }; -- admin
        test = {
            ip = "localhost";
            port = port;
            http = {
                maxreq = 64 * 1024; maxlen = 64 * 1024;
                accesslog = {
                                        log = accesslog;
                cache2 = {
                    cache_ttl = cache_ttl;
                    ignore_cache_control = ignore_cache_control;
                    proxy = {
                        host = "localhost"; port = backend1_port;
                        connect_timeout = "5s"; backend_timeout = "5s";
                        resolve_timeout = "1s";
                        fail_on_5xx = 0;
                    }; -- proxy
                }; -- cache2
                };
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
