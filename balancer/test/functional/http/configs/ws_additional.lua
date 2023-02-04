function gen_balancer2(mode)
    if mode == nil or mode ~= "balancer2" then
        return nil
    end

    retval = {}
    retval.by_name_policy = {
            name = "backend1";
            unique_policy = {};
        };
    retval.attempts = attempts;

    retval.hedged_delay = hedged_delay;

    retval.rr = {
            randomize_initial_state = false;
            backend1 = {
                proxy = {
                    host = 'localhost'; port = backend1_port;
                    keepalive_count = keepalive_count;
                    connect_timeout = "1s"; resolve_timeout = "1s";
                    backend_read_timeout = "5s"; backend_write_timeout="5s";
                    client_read_timeout = "5s"; client_write_timeout="5s";
                    allow_connection_upgrade = true;
                }; -- proxy
            }; -- backend1
            backend2 = {
                proxy = {
                    host = 'localhost'; port = backend2_port;
                    keepalive_count = keepalive_count;
                    connect_timeout = "1s"; resolve_timeout = "1s";
                    backend_read_timeout = "5s"; backend_write_timeout="5s";
                    client_read_timeout = "5s"; client_write_timeout="5s";
                    allow_connection_upgrade = true;
                }; -- proxy
            }; -- backend1
        }; -- rr

    return retval
end

function gen_antirobot(mode)
    if mode == nil or mode ~= "antirobot" then
        return nil
    end

    retval = {}
    retval.checker = {
        balancer2 = {
            attempts = 2;
            by_name_policy = {
                name = "antirobot1";
                unique_policy = {};
            };
            rr = {
                randomize_initial_state = false;
                antirobot1 = {
                    proxy = {
                        host = "localhost"; port = antirobot1_port;
                        connect_timeout = "0.3s"; backend_timeout = "10s";
                        resolve_timeout = "0.3s";
                        fail_on_5xx = 0;
                    }; -- proxy
                }; -- antirobot1
            }; -- rr
        }; -- balancer2
    }; -- checker

    retval.module = {
            proxy = {
                host = "localhost"; port = backend1_port;
                connect_timeout = "0.3s"; backend_timeout = "10s";
                resolve_timeout = "0.3s";
                fail_on_5xx = 0;
                allow_connection_upgrade = true;
            }; -- proxy
        }; -- module

    return retval

end

instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    log = log;

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
                log_level = "DEBUG";
                http = {
                    maxlen = maxlen;
                    maxreq = maxreq;
                    maxheaders = maxheaders;
                    keepalive = keepalive;
                    no_keepalive_file = no_keepalive_file;
                    allow_trace = allow_trace;
                    multiple_hosts_enabled = multiple_hosts_enabled;
                    stats_attr = stats_attr;
                    events = {
                        stats = "report";
                    };
                    balancer2 = gen_balancer2(mode);
                    antirobot = gen_antirobot(mode);
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
