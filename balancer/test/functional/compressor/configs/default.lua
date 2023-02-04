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
                        compressor = {
                            enable_compression = enable_compression;
                            enable_decompression = enable_decompression;
                            compression_codecs = compression_codecs;
                            report = {
                                uuid = "service_total";
                                proxy = {
                                    host = "localhost";
                                    port = backend_port;
                                    connect_timeout = "10s";
                                    resolve_timeout = "10s";
                                    backend_timeout = "10s";
                                }; -- proxy
                            }; -- compressor
                        }; -- report
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- default
    }; -- ipdispatch
}; -- instance
