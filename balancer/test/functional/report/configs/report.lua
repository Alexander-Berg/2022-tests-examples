if ranges == nil then ranges = "10s"; end
if backend_timeout == nil then backend_timeout = "5s"; end
if keepalive == nil then keepalive = 1; end
instance = {
    thread_mode = thread_mode; set_no_file = false;

    addrs = {
        { ip = "localhost"; port = port; };
    }; -- addrs

    admin_addrs = {
         { ip = "localhost"; port = admin_port; };
    };

    unistat = {
        addrs = {
            { ip = "localhost"; port = stats_port };
        };
        hide_legacy_signals = true;
    };

    ipdispatch = {
        test = {
            ip = "localhost";
            port = port;
            errorlog = {
                log = errorlog;
                log_level = "DEBUG";
                http = {
                    maxlen = 65536; maxreq = 65536;
                    keepalive = keepalive;
                    accesslog = {
                        log = accesslog;
                        regexp = {
                            led = {
                                match_fsm = {
                                    path = "/led/"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "led";
                                    labels = {
                                        a = "b";
                                        c = "d";
                                    };
                                    ranges = legacy_ranges;
                                    input_size_ranges = input_size_ranges;
                                    output_size_ranges = output_size_ranges;
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                }; -- report
                            }; -- regexp/led
                            zeppelin = {
                                match_fsm = {
                                    path = "/zeppelin/"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "zeppelin";
                                    ranges = legacy_ranges;
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                }; -- report
                            }; -- regexp/zeppelin
                            refers = {
                                match_fsm = {
                                    path = "/refers/"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "refers";
                                    ranges = legacy_ranges;
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    errordocument = {
                                        status = 404;
                                    };
                                }; -- report
                            }; -- regexp/refers
                            storage = {
                                match_fsm = {
                                    path = "/storage/"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "storage";
                                    refers = "refers";
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    errordocument = {
                                        status = 200;
                                        content = "storage";
                                    };
                                };
                            };
                            multirefers = {
                                match_fsm = {
                                    path = "/multirefers/"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "multirefers";
                                    refers = "refers,default";
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    errordocument = {
                                        status = 200;
                                        content = "multirefers";
                                    };
                                };
                            }; -- regexp/multirefers
                            selfrefer = {
                                match_fsm = {
                                    path = "/selfrefer/"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "selfrefer";
                                    refers = "selfrefer";
                                    ranges = legacy_ranges;
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    errordocument = {
                                        status = 200;
                                        content = "selfrefer";
                                    };
                                };
                            }; -- regexp/selfrefer
                            transitivity = {
                                match_fsm = {
                                    path = "/transitivity/"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "transitivity";
                                    ranges = legacy_ranges;
                                    refers = "transitivity_first";
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    errordocument = {
                                        status = 200;
                                        content = "transitivity";
                                    };
                                };
                            };
                            transitivity_first = {
                                match_fsm = {
                                    path = "/transitivity-first/"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "transitivity_first";
                                    ranges = legacy_ranges;
                                    refers = "transitivity_second";
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    errordocument = {
                                        status = 200;
                                        content = "transitivity_first";
                                    };
                                };
                            }; -- regexp/transitivity-first
                            transitivity_second = {
                                match_fsm = {
                                    path = "/transitivity_second/"; surround = false; case_insensitive = true;
                                };
                                report = {
                                    uuid = "transitivity_second";
                                    ranges = legacy_ranges;
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    errordocument = {
                                        status = 200;
                                        content = "transitivity-second";
                                    };
                                };
                            }; -- regexp/transitivity-second
                            default = {
                                report = {
                                    uuid = "default";
                                    ranges = legacy_ranges;
                                    events = {
                                        stats = "report";
                                    }; -- events
                                    proxy = {
                                        host = "localhost"; port = backend_port;
                                        connect_timeout = "0.3s"; backend_timeout = backend_timeout;
                                        resolve_timeout = "1s";
                                        fail_on_5xx = 0;
                                    }; -- proxy
                                }; -- report
                            }; -- regexp/default
                        }; -- regexp
                    }; -- accesslog
                }; -- http
            }; -- errorlog
        }; -- test
    }; -- ipdispatch
}; -- instance
