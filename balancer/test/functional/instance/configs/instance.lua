enable_reuse_port = enable_reuse_port and enable_reuse_port == "true"

function get_port(flag, port)
    if flag then
        return 1
    else
        return port
    end
end

instance = {
    thread_mode = thread_mode;
    set_no_file = set_no_file;

    addrs = {
        { ip = "localhost"; port = port; disabled = 0; }; --[[ SLB:  ]]
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = get_port(bad_admin_port, admin_port); disabled = 0; }; --[[ SLB:  ]]
    };
    unistat = {
        addrs = {
            { ip = "localhost"; port = get_port(bad_stats_port, stats_port); };
        };
    };

    events = {
        stats = "report";
    };

    log = instance_log;
    workers = workers;
    maxconn = maxconn;
    sosndbuf = sosndbuf;
    buffer = buffer;

    enable_reuse_port = enable_reuse_port;
    affinity_mask = affinity_mask;
    dns_timeout = dns_timeout;

    tcp_keep_idle = tcp_keep_idle;
    tcp_keep_intvl = tcp_keep_intvl;
    tcp_keep_cnt = tcp_keep_cnt;

    private_address = private_address;
    worker_start_delay = worker_start_delay or '0s';

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
                errordocument = {
                    status = 200;
                    content = "OK";
                };
            }; -- http
        }; -- test
    }; -- ipdispatch
}; -- instance
