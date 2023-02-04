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
                allow_trace = true;
                regexp = {
                    get = {
                        match_method = { methods = {'get'}; };
                        errordocument = {
                            status = 200;
                            content = 'get';
                        }; -- errordocument
                    }; -- match
                    post = {
                        match_method = { methods = {'post'}; };
                        errordocument = {
                            status = 200;
                            content = 'post';
                        }; -- errordocument
                    }; -- match
                    head = {
                        match_method = { methods = {'head'}; };
                        errordocument = {
                            status = 200;
                            content = 'head';
                        }; -- errordocument
                    }; -- match
                    options = {
                        match_method = { methods = {'options'}; };
                        errordocument = {
                            status = 200;
                            content = 'options';
                        }; -- errordocument
                    }; -- match
                    put = {
                        match_method = { methods = {'put'}; };
                        errordocument = {
                            status = 200;
                            content = 'put';
                        }; -- errordocument
                    }; -- match
                    patch = {
                        match_method = { methods = {'patch'}; };
                        errordocument = {
                            status = 200;
                            content = 'patch';
                        }; -- errordocument
                    }; -- match
                    delete = {
                        match_method = { methods = {'delete'}; };
                        errordocument = {
                            status = 200;
                            content = 'delete';
                        }; -- errordocument
                    }; -- match
                    connect = {
                        match_method = { methods = {'connect'}; };
                        errordocument = {
                            status = 200;
                            content = 'connect';
                        }; -- errordocument
                    }; -- match
                    trace = {
                        match_method = { methods = {'trace'}; };
                        errordocument = {
                            status = 200;
                            content = 'trace';
                        }; -- errordocument
                    }; -- match
                    default = {
                        errordocument = {
                            status = 200;
                            content = 'default';
                        }; -- errordocument
                    }; -- default
                }; -- regexp
            }; -- test
        }; -- http
    }; -- ipdispatch
}; -- instance
