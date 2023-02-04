instance = {
    set_no_file = false;
    log = log;
    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };
    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port; }
        };
    };

    ipdispatch = {
        default = {
            errorlog = {
                log = errorlog;
                http = {
                    maxreq = 64 * 1024; maxlen = 64 * 1024;
                    accesslog = {
                        log = accesslog;
                        cryprox = {
                            partner_token = partner_token;
                            secrets_file = secrets_file;
                            disable_file = disable_file;
                            use_cryprox_matcher = {
                                match_fsm = {
                                    cookie = "cryprox=1";
                                    case_insensitive = true;
                                    surround = true;
                                }; -- match_fsm
                            }; -- use_cryprox_matcher
                            cryprox_backend = {
                                report = {
                                    uuid = "cryprox_backend";
                                    proxy = {
                                        host = "127.0.0.1";
                                        port = cryprox_backend_port;
                                        connect_timeout = "10s";
                                        resolve_timeout = "10s";
                                        backend_timeout = cryprox_backend_timeout or "10s";
                                    }; -- proxy
                                }; -- report
                            }; -- cryprox_backend
                            service_backend = {
                                report = {
                                    uuid = "service_backend";
                                    proxy = {
                                        host = "127.0.0.1";
                                        port = backend_port;
                                        connect_timeout = "10s";
                                        resolve_timeout = "10s";
                                        backend_timeout = service_backend_timeout or "10s";
                                    }; -- proxy
                                }; -- report
                            }; -- service_backend
                        }; -- cryprox
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- default
    }; -- ipdispatch
}; -- instance
