instance = {
    thread_mode = thread_mode; set_no_file = false;

    events = {
      stats = "report";
    }; -- events

    addrs = {
        { ip = "localhost"; port = port; };
    };
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    };

    workers = workers;

    http = {
        maxreq = 64 * 1024; maxlen = 64 * 1024;
        ipdispatch = {
            admin = {
                ip = "localhost"; port = admin_port;
                admin = {};
            }; -- ipdispatch/admin
            default = {

                regexp = {
                    darkthrone = {
                        match_fsm = {
                            path = "/darkthrone.*";
                        };
                        shared = {
                            uuid = darkthrone_uuid;
                            report = {
                                uuid = darkthrone_uuid;
                                events = {
                                  stats = "report";
                                }; -- events

                                errordocument = {
                                    status = 200;
                                    content = "darkthrone";
                                };
                            }
                        };
                    }; -- regexp/darkthrone
                    immortal = {
                        match_fsm = {
                            path = "/immortal.*";
                        };
                        shared = {
                            uuid = immortal_uuid;
                            report = {
                                uuid = immortal_uuid;
                                events = {
                                  stats = "report";
                                }; -- events

                                errordocument = {
                                    status = 200;
                                    content = "immortal";
                                };
                            }
                        };
                    }; -- regexp/immortal
                    default = {
                        shared = {
                            uuid = default_uuid;
                        };
                    }; -- regexp/default
                }; -- regexp
            }; -- ipdispatch/defaul
        }; -- ipdispatch
    }; -- http
}; -- instance
