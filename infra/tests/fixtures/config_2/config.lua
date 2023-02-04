default_ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


default_ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms"


function gen_proxy_backends(backends, proxy_options)
  local result = {}

  for index, backend in pairs(backends) do
    local proxy = {
      host = backend[1] or backend['host'];
      port = backend[2] or backend['port'];
      cached_ip = backend[4] or backend['cached_ip'];
    };

    if proxy_options ~= nil then
      for optname, optvalue in pairs(proxy_options) do
        proxy[optname] = optvalue
      end
    end

    result[index] = {
      weight = backend[3] or backend['weight'];
      proxy = proxy;
    };
  end

  if next(result) == nil then
    error("backends list is empty")
  end

  return result
end


function check_int(value, var_name)
    return tonumber(value) or error("Could not cast variable \"" .. var_name .. "\" to a number.'")
end

function get_int_var(name, default)
  value = _G[name]
  return value and check_int(value) or default
end


function get_ip_by_iproute(addr_family)
  if disable_external then
    if addr_family == "v4" then
      return "127.1.1.1"
    elseif addr_family == "v6" then
      return "127.2.2.2"
    else
      error("invalid parameter")
    end
  end

  local ipcmd
  if addr_family == "v4" then
    ipcmd = "ip route get 77.88.8.8 2>/dev/null| awk '/src/ {print $NF}'"
  elseif addr_family == "v6" then
    ipcmd = "ip route get 2a00:1450:4010:c05::65 2>/dev/null | grep -oE '2a02[:0-9a-f]+' | tail -1"
  else
    error("invalid parameter")
  end
  local handler = io.popen(ipcmd)
  local ip = handler:read("*l")
  handler:close()
  if ip == nil or ip == "" or ip == "proto" then
    return "127.0.0.2"
  end
  return ip
end


function get_log_path(name, port, default_log_dir)
  default_log_dir = default_log_dir or "/place/db/www/logs"
  rv = (log_dir or default_log_dir) .. "/current-" .. name .. "-balancer";
  if port ~= nil then
    rv = rv .. "-" .. port;
  end
  return rv
end


function get_port_var(name, offset, default)
  value = get_int_var(name, default)
  if value == nil then
    error("Neither port variable \"" .. name .. "\" nor default port is specified.")
  end
  if value < 0 or value > 65535 then
    error("Variable \"" .. name .. "\" is not a valid port: " .. value)
  end
  if offset ~= nil then
    value = value + offset
  end
  return value
end


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


function get_str_var(name, default)
  return _G[name] or default
end


instance = {
  workers = 0;
  buffer = 65536;
  maxconn = 1000;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 15010, "/place/db/www/logs/");
  admin_addrs = {
    {
      port = 15010;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15010;
    };
  }; -- admin_addrs
  unistat = {
    addrs = {
      {
        ip = "*";
        port = get_port_var("port", 2);
        disabled = get_int_var("disable_external", 0);
      };
    }; -- addrs
  }; -- unistat
  sd = {
    client_name = "unknown-awacs-l7-balancer";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = "./sd_cache";
    allow_empty_endpoint_sets = true;
  }; -- sd
  addrs = {
    {
      port = 15010;
      ip = "127.0.0.4";
    };
    {
      ip = "127.0.0.44";
      port = get_port_var("http_port", 0, 80);
    };
    {
      ip = "127.0.0.44";
      port = get_port_var("https_port");
    };
    {
      port = 15010;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15010;
      ip = get_ip_by_iproute("v6");
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        15010;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        admin = {
          disable_xml_stats = true;
        }; -- admin
      }; -- http
    }; -- admin
    stats_storage = {
      ips = {
        "127.0.0.4";
      }; -- ips
      ports = {
        15010;
      }; -- ports
      report = {
        uuid = "service_total";
        ranges = get_str_var("default_ranges");
        just_storage = true;
        disable_robotness = true;
        disable_sslness = true;
        events = {
          stats = "report";
        }; -- events
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          errordocument = {
            status = 200;
            force_conn_close = false;
          }; -- errordocument
        }; -- http
      }; -- report
    }; -- stats_storage
    remote_ips_80 = {
      ips = {
        "127.0.0.44";
      }; -- ips
      ports = {
        get_port_var("http_port", 0, 80);
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", get_port_var("http_port", 0, 80), "/place/db/www/logs/");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          allow_client_hints_restore = true;
          client_hints_ua_header = "X-Yandex-UA";
          client_hints_ua_proto_header = "X-Yandex-UA-Proto";
          disable_client_hints_restore_file = "./disable_client_hints_restore.switch";
          accesslog = {
            log = get_log_path("access_log", get_port_var("http_port", 0, 80), "/place/db/www/logs/");
            report = {
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              threshold = {
                lo_bytes = 524288;
                hi_bytes = 1048576;
                recv_timeout = "1s";
                pass_timeout = "10s";
                headers = {
                  create_func = {
                    ["X-Real-IP"] = "realip";
                  }; -- create_func
                  create_func_weak = {
                    ["X-Forwarded-For"] = "realip";
                    ["X-Req-Id"] = "reqid";
                    ["X-Scheme"] = "scheme";
                    ["X-Source-Port"] = "realport";
                  }; -- create_func_weak
                  redirects = {
                    actions = {
                      {
                        src = "//mir.trains.yandex.ru/*";
                        forward = {
                          dst = "http://yastatic.net/s3/travel/other-projects/mir/robots.txt";
                          legacy_rstrip = true;
                          dst_rewrites = {
                            {
                              regexp = "[.]xml$";
                              rewrite = "";
                            };
                          }; -- dst_rewrites
                          errordocument = {
                            status = 503;
                            force_conn_close = false;
                            content = "Service unavailable";
                          }; -- errordocument
                        }; -- forward
                      };
                    }; -- actions
                    shared = {
                      uuid = "7923657007242447949";
                      regexp = {
                        ping = {
                          priority = 3;
                          match_fsm = {
                            URI = "/ping";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          stats_eater = {
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/slb_check.weights";
                                to_upstream = {
                                  weight = 1.000;
                                  active_check_reply = {
                                    default_weight = 33;
                                    use_header = true;
                                    use_body = true;
                                    use_dynamic_weight = false;
                                    weight_file = "./controls/l3_dynamic_weight";
                                    zero_weight_at_shutdown = true;
                                  }; -- active_check_reply
                                }; -- to_upstream
                                switch_off = {
                                  weight = -1.000;
                                  errordocument = {
                                    status = 503;
                                    force_conn_close = false;
                                  }; -- errordocument
                                }; -- switch_off
                              }; -- rr
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- ping
                        gobabygo = {
                          priority = 2;
                          match_fsm = {
                            URI = "/gobabygo(/.*)?";
                            case_insensitive = false;
                            surround = false;
                          }; -- match_fsm
                          rewrite = {
                            actions = {
                              {
                                split = "url";
                                global = false;
                                literal = false;
                                rewrite = "/%1";
                                case_insensitive = false;
                                regexp = "/gobabygo/?(.*)";
                              };
                            }; -- actions
                            headers = {
                              create_func = {
                                ["X-Real-IP"] = "realip";
                              }; -- create_func
                              create_func_weak = {
                                ["X-Forwarded-For"] = "realip";
                                ["X-Req-Id"] = "reqid";
                                ["X-Scheme"] = "scheme";
                                ["X-Source-Port"] = "realport";
                              }; -- create_func_weak
                              flags_getter = {
                                service_name = "my-test-service";
                                flags_path = "flags-path";
                                flags_host = "flags-host";
                                file_switch = "./disable-flags.txt";
                                flags = {
                                  proxy = {
                                    host = "localhost";
                                    port = 8080;
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "10s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                    watch_client_close = true;
                                  }; -- proxy
                                }; -- flags
                                balancer2 = {
                                  unique_policy = {};
                                  attempts = 3;
                                  rr = {
                                    {
                                      weight = 100.000;
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 3;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "20s";
                                            feedback_time = "10s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.050;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "google.com"; 90; 2.000; "2a00:1450:4010:c0d::8b"; };
                                            { "ya.ru"; 80; 1.000; "2a02:6b8::3"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "999ms";
                                            backend_timeout = "5s";
                                            fail_on_5xx = false;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 3;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                        on_error = {
                                          errordocument = {
                                            status = 504;
                                            force_conn_close = false;
                                          }; -- errordocument
                                        }; -- on_error
                                      }; -- balancer2
                                    };
                                  }; -- rr
                                }; -- balancer2
                              }; -- flags_getter
                            }; -- headers
                          }; -- rewrite
                        }; -- gobabygo
                        pdb = {
                          priority = 1;
                          match_fsm = {
                            host = "pdb\\.test-cplb\\.yandex\\.ru";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                          headers = {
                            create_func = {
                              ["X-Real-IP"] = "realip";
                            }; -- create_func
                            create_func_weak = {
                              ["X-Forwarded-For-Y"] = "realip";
                              ["X-Req-Id"] = "reqid";
                              ["X-Scheme"] = "scheme";
                              ["X-Source-Port"] = "realport";
                            }; -- create_func_weak
                            create = {
                              ["X-Forwarded-Proto"] = "https";
                            }; -- create
                            geobase = {
                              trusted = false;
                              geo_host = "laas.yandex.ru";
                              take_ip_from = "X-Forwarded-For-Y";
                              laas_answer_header = "X-LaaS-Answered";
                              file_switch = "./controls/disable_geobase.switch";
                              geo_path = "/region?response_format=header&version=1&service=balancer";
                              geo = {
                                shared = {
                                  uuid = "7684621431733358954";
                                }; -- shared
                              }; -- geo
                              regexp = {
                                pdb_backend_test = {
                                  priority = 2;
                                  match_fsm = {
                                    URI = "/(api|picture)(/.*)?";
                                    case_insensitive = true;
                                    surround = false;
                                  }; -- match_fsm
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
                                    rr = {
                                      {
                                        weight = 100.000;
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          weighted2 = {
                                            slow_reply_time = "1s";
                                            correction_params = {
                                              max_weight = 5.000;
                                              min_weight = 0.050;
                                              history_time = "20s";
                                              feedback_time = "10s";
                                              plus_diff_per_sec = 0.050;
                                              minus_diff_per_sec = 0.050;
                                            }; -- correction_params
                                            unpack(gen_proxy_backends({
                                              { "pdb_backend_test.yandex.ru"; 80; 1.000; "2a02:6b8::4"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "999ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = false;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 3;
                                              need_resolve = true;
                                              watch_client_close = true;
                                            }))
                                          }; -- weighted2
                                          on_error = {
                                            errordocument = {
                                              status = 504;
                                              force_conn_close = false;
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      };
                                    }; -- rr
                                  }; -- balancer2
                                }; -- pdb_backend_test
                                default = {
                                  priority = 1;
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 3;
                                    rr = {
                                      {
                                        weight = 100.000;
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 3;
                                          weighted2 = {
                                            slow_reply_time = "1s";
                                            correction_params = {
                                              max_weight = 5.000;
                                              min_weight = 0.050;
                                              history_time = "20s";
                                              feedback_time = "10s";
                                              plus_diff_per_sec = 0.050;
                                              minus_diff_per_sec = 0.050;
                                            }; -- correction_params
                                            unpack(gen_proxy_backends({
                                              { "pdb_nodejs_test.yandex.ru"; 80; 1.000; "2a02:6b8::5"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "999ms";
                                              backend_timeout = "5s";
                                              fail_on_5xx = false;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 3;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                          on_error = {
                                            errordocument = {
                                              status = 504;
                                              force_conn_close = false;
                                            }; -- errordocument
                                          }; -- on_error
                                        }; -- balancer2
                                      };
                                    }; -- rr
                                  }; -- balancer2
                                }; -- default
                              }; -- regexp
                            }; -- geobase
                          }; -- headers
                        }; -- pdb
                      }; -- regexp
                    }; -- shared
                  }; -- redirects
                }; -- headers
              }; -- threshold
              refers = "service_total";
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- remote_ips_80
    local_ips_443 = {
      ips = {
        "127.0.0.44";
      }; -- ips
      ports = {
        get_port_var("https_port");
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = "/place/db/www/logs/current-error_log-balancer-443";
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          contexts = {
            default = {
              priority = 1;
              timeout = "100800s";
              ciphers = get_str_var("default_ciphers");
              log = "/place/db/www/logs/current-ssl_sni-balancer-443";
              priv = "/dev/shm/balancer/priv/awacs.yandex-team.ru.pem";
              cert = "/dev/shm/balancer/allCAs-awacs.yandex-team.ru.pem";
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = "/dev/shm/balancer/priv/1st.awacs.yandex-team.ru.key";
                };
                {
                  priority = 2;
                  keyfile = "/dev/shm/balancer/priv/2nd.awacs.yandex-team.ru.key";
                };
                {
                  priority = 1;
                  keyfile = "/dev/shm/balancer/priv/3rd.awacs.yandex-team.ru.key";
                };
              }; -- ticket_keys_list
            }; -- default
          }; -- contexts
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = "/place/db/www/logs/current-access_log-balancer-443";
              threshold = {
                lo_bytes = 524288;
                hi_bytes = 1048576;
                recv_timeout = "1s";
                pass_timeout = "10s";
                report = {
                  ranges = get_str_var("default_ranges");
                  just_storage = false;
                  disable_robotness = true;
                  disable_sslness = true;
                  events = {
                    stats = "report";
                  }; -- events
                  headers = {
                    create_func = {
                      ["X-Real-IP"] = "realip";
                    }; -- create_func
                    create_func_weak = {
                      ["X-Forwarded-For"] = "realip";
                      ["X-Req-Id"] = "reqid";
                      ["X-Scheme"] = "scheme";
                      ["X-Source-Port"] = "realport";
                    }; -- create_func_weak
                    hasher = {
                      mode = "subnet";
                      take_ip_from = "X-Real-IP";
                      h100 = {
                        cutter = {
                          bytes = 512;
                          timeout = "0.1s";
                          antirobot = {
                            cut_request = true;
                            no_cut_request_file = "./controls/no_cut_request_file";
                            file_switch = "./controls/disable_antirobot_module";
                            cut_request_bytes = 512;
                            checker = {
                              report = {
                                uuid = "antirobot";
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                stats_eater = {
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 2;
                                    hashing = {
                                      unpack(gen_proxy_backends({
                                        { "antirobot.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::121"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "30ms";
                                        backend_timeout = "100ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- hashing
                                  }; -- balancer2
                                }; -- stats_eater
                              }; -- report
                            }; -- checker
                            module = {
                              shared = {
                                uuid = "7923657007242447949";
                              }; -- shared
                            }; -- module
                          }; -- antirobot
                        }; -- cutter
                      }; -- h100
                    }; -- hasher
                  }; -- headers
                  refers = "service_total";
                }; -- report
              }; -- threshold
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- local_ips_443
    local_ips_15010 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15010;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 15010, "/place/db/www/logs/");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 15010, "/place/db/www/logs/");
            report = {
              ranges = get_str_var("default_ranges");
              input_size_ranges = "100,300,5000";
              output_size_ranges = "3000,6000,10240";
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              threshold = {
                lo_bytes = 524288;
                hi_bytes = 1048576;
                recv_timeout = "1s";
                pass_timeout = "10s";
                headers = {
                  create_func = {
                    ["X-Real-IP"] = "realip";
                  }; -- create_func
                  create_func_weak = {
                    ["X-Forwarded-For"] = "realip";
                    ["X-Req-Id"] = "reqid";
                    ["X-Scheme"] = "scheme";
                    ["X-Source-Port"] = "realport";
                  }; -- create_func_weak
                  geobase = {
                    trusted = false;
                    geo_host = "laas.yandex.ru";
                    take_ip_from = "X-Forwarded-For-Y";
                    laas_answer_header = "X-LaaS-Answered";
                    file_switch = "./controls/disable_geobase.switch";
                    geo_path = "/region?response_format=header&version=1&service=balancer";
                    geo = {
                      shared = {
                        uuid = "7684621431733358954";
                        report = {
                          uuid = "geobasemodule";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          stats_eater = {
                            balancer2 = {
                              simple_policy = {};
                              attempts = 2;
                              rr = {
                                unpack(gen_proxy_backends({
                                  { "laas.yandex.ru"; 80; 1.000; "2a02:6b8::91"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "15ms";
                                  backend_timeout = "20ms";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 10;
                                  need_resolve = true;
                                }))
                              }; -- rr
                            }; -- balancer2
                          }; -- stats_eater
                        }; -- report
                      }; -- shared
                    }; -- geo
                    hasher = {
                      mode = "subnet";
                      take_ip_from = "X-Real-IP";
                      h100 = {
                        cutter = {
                          bytes = 512;
                          timeout = "0.1s";
                          antirobot = {
                            cut_request = true;
                            no_cut_request_file = "./controls/no_cut_request_file";
                            file_switch = "./controls/disable_antirobot_module";
                            cut_request_bytes = 512;
                            checker = {
                              report = {
                                ranges = get_str_var("default_ranges");
                                just_storage = false;
                                disable_robotness = true;
                                disable_sslness = true;
                                events = {
                                  stats = "report";
                                }; -- events
                                stats_eater = {
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 2;
                                    hashing = {
                                      unpack(gen_proxy_backends({
                                        { "antirobot.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::121"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "30ms";
                                        backend_timeout = "100ms";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 1;
                                        need_resolve = true;
                                      }))
                                    }; -- hashing
                                  }; -- balancer2
                                }; -- stats_eater
                                refers = "antirobot";
                              }; -- report
                            }; -- checker
                            module = {
                              shared = {
                                uuid = "7923657007242447949";
                              }; -- shared
                            }; -- module
                          }; -- antirobot
                        }; -- cutter
                      }; -- h100
                    }; -- hasher
                  }; -- geobase
                }; -- headers
              }; -- threshold
              refers = "service_total";
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- local_ips_15010
  }; -- ipdispatch
}