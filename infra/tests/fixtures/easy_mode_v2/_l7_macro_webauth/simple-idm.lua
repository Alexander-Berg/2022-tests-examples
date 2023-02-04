default_ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2"


default_ranges = "1ms,4ms,7ms,11ms,17ms,26ms,39ms,58ms,87ms,131ms,197ms,296ms,444ms,666ms,1000ms,1500ms,2250ms,3375ms,5062ms,7593ms,11390ms,17085ms,30000ms,60000ms,150000ms"


function _call_func_providers(overridable_func_names)
  for _, func_name in pairs(overridable_func_names) do
    local func_provider_path = _G[func_name .. "_provider"]
    if func_provider_path ~= nil then
      local env = {}
      setmetatable(env, {__index = _G})
      local provider, err = loadfile(func_provider_path, nil, env)
      if provider == nil then
        error(string.format('Failed to import provider "%s": %s', func_provider_path, err))
      end
      ok, rv = pcall(provider)
      if ok then
        if type(rv) ~= 'function' then
          error(string.format('Provider "%s" must return a function, not %s.', func_provider_path, type(rv)))
        end
        _G["do_" .. func_name] = rv
      else
        error(string.format('Provider "%s" failed: %s', func_provider_path, rv))
      end
    end
  end
end


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


function get_ca_cert_path(name, default_ca_cert_dir)
  default_ca_cert_dir = default_ca_cert_dir or "/dev/shm/balancer/priv"
  return (ca_cert_dir or default_ca_cert_dir) .. "/" .. name;
end


function check_int(value, var_name)
    return tonumber(value) or error("Could not cast variable \"" .. var_name .. "\" to a number.'")
end

function get_int_var(name, default)
  value = _G[name]
  return value and check_int(value) or default
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


function get_private_cert_path(name, default_private_cert_dir)
  default_private_cert_dir = default_private_cert_dir or "/dev/shm/balancer/priv"
  return (private_cert_dir or default_private_cert_dir) .. "/" .. name;
end


function get_public_cert_path(name, default_public_cert_dir)
  default_public_cert_dir = default_public_cert_dir or "/dev/shm/balancer"
  return (public_cert_dir or default_public_cert_dir) .. "/" .. name;
end


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


function get_str_var(name, default)
  return _G[name] or default
end


function do_get_workers()
  -- actual get_workers() implementation, can be overridden
  value = _G["workers"]
  if value == nil then
    error('Variable "workers" is not specified.')
  end
  int_value = tonumber(value)
  if int_value == nil then
    error('Could not cast variable "workers" to a number.')
  end
  return int_value
end


function get_workers()
  value = do_get_workers()
  if type(value) ~= 'number' then
    error(string.format('Provided get_workers() implementation must return a number, not %s.', type(value)))
  end
  if value < 0 or value % 1 ~= 0 then
    error(string.format('Provided get_workers() implementation must return a non-negative integer, not %s', value))
  end
  return value
end


_call_func_providers({
  "get_workers";
})


instance = {
  buffer = 65536;
  maxconn = 5000;
  tcp_fastopen = 0;
  pinger_required = true;
  tcp_listen_queue = 128;
  workers = get_workers();
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  shutdown_close_using_bpf = true;
  events = {
    stats = "report";
  }; -- events
  state_directory = "/dev/shm/balancer-state";
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", get_port_var("port"), "/place/db/www/logs");
  pinger_log = get_log_path("pinger_log", get_port_var("port"), "/place/db/www/logs/");
  config_check = {
    quorums_file = "./controls/backend_check_quorums";
  }; -- config_check
  dynamic_balancing_log = get_log_path("dynamic_balancing_log", get_port_var("port"), "/place/db/www/logs/");
  admin_addrs = {
    {
      ip = "127.0.0.1";
      port = get_port_var("port");
    };
    {
      ip = "::1";
      port = get_port_var("port");
    };
  }; -- admin_addrs
  cpu_limiter = {
    active_check_subnet_default = true;
    disable_file = "./controls/cpu_limiter_disabled";
    active_check_subnet_file = "./controls/active_check_subnets_list";
  }; -- cpu_limiter
  unistat = {
    addrs = {
      {
        ip = "*";
        port = get_port_var("port", 2);
        disabled = get_int_var("disable_external", 0);
      };
    }; -- addrs
    hide_legacy_signals = true;
  }; -- unistat
  addrs = {
    {
      ip = "*";
      port = 11111;
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = 11112;
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
  sd = {
    client_name = "awacs-l7-balancer(easy-mode-v2.test.yandex.net:balancer)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = get_str_var("sd_cache_dir", "./sd_cache");
  }; -- sd
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        get_port_var("port");
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
    http_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        11111;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 11111, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 11111, "/place/db/www/logs");
            report = {
              uuid = "service_total";
              ranges = get_str_var("default_ranges");
              input_size_ranges = "32,64,128,256,512,1024,4096,8192,16384,131072,524288,1048576,2097152";
              output_size_ranges = "512,1024,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608";
              matcher_map = {
                molly = {
                  match_fsm = {
                    cgi = ".*everybodybecoolthisis=(crasher|molly).*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                }; -- molly
              }; -- matcher_map
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              cookie_policy = {
                uuid = "service_total";
                default_yandex_policies = "stable";
                regexp = {
                  ["awacs-balancer-health-check"] = {
                    priority = 3;
                    match_fsm = {
                      URI = "/awacs-balancer-health-check";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    errordocument = {
                      status = 200;
                      force_conn_close = false;
                    }; -- errordocument
                  }; -- ["awacs-balancer-health-check"]
                  slbping = {
                    priority = 2;
                    match_fsm = {
                      url = "/ping";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    shared = {
                      uuid = "7812409944542501370";
                      stats_eater = {
                        balancer2 = {
                          unique_policy = {};
                          attempts = 1;
                          rr = {
                            weights_file = "./controls/slb_check.weights";
                            to_upstream = {
                              weight = 1.000;
                              active_check_reply = {
                                default_weight = 1;
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
                    }; -- shared
                  }; -- slbping
                  default = {
                    priority = 1;
                    headers = {
                      create = {
                        Location = "1";
                      }; -- create
                      rewrite = {
                        actions = {
                          {
                            regexp = ".*";
                            global = false;
                            literal = false;
                            case_insensitive = false;
                            header_name = "Location";
                            rewrite = "https://%{host}%{url}";
                          };
                        }; -- actions
                        regexp = {
                          unsafe_methods = {
                            priority = 2;
                            match_fsm = {
                              match = "(DELETE|PATCH|POST|PUT).*";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                            errordocument = {
                              status = 307;
                              force_conn_close = false;
                              remain_headers = "Location";
                            }; -- errordocument
                          }; -- unsafe_methods
                          default = {
                            priority = 1;
                            errordocument = {
                              status = 302;
                              force_conn_close = false;
                              remain_headers = "Location";
                            }; -- errordocument
                          }; -- default
                        }; -- regexp
                      }; -- rewrite
                    }; -- headers
                  }; -- default
                }; -- regexp
              }; -- cookie_policy
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
    https_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        11112;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 11112, "/place/db/www/logs");
        ssl_sni = {
          force_ssl = true;
          events = {
            stats = "report";
            reload_ocsp_response = "reload_ocsp";
            reload_ticket_keys = "reload_ticket";
          }; -- events
          ja3_enabled = true;
          contexts = {
            ["ec.yandex.net_ec"] = {
              priority = 2;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 11112, "/place/db/www/logs");
              priv = get_private_cert_path("ec.yandex.net_ec.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-ec.yandex.net_ec.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(ec\\.yandex\\.net)";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.ec.yandex.net_ec.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.ec.yandex.net_ec.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.ec.yandex.net_ec.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["ec.yandex.net_ec"]
            default = {
              priority = 1;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 11112, "/place/db/www/logs");
              priv = get_private_cert_path("ec.yandex.net_ec.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-ec.yandex.net_ec.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.ec.yandex.net_ec.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.ec.yandex.net_ec.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.ec.yandex.net_ec.key", "/dev/shm/balancer/priv");
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
              log = get_log_path("access_log", 11112, "/place/db/www/logs");
              report = {
                ranges = get_str_var("default_ranges");
                input_size_ranges = "32,64,128,256,512,1024,4096,8192,16384,131072,524288,1048576,2097152";
                output_size_ranges = "512,1024,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608";
                matcher_map = {
                  molly = {
                    match_fsm = {
                      cgi = ".*everybodybecoolthisis=(crasher|molly).*";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                  }; -- molly
                }; -- matcher_map
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                cookie_policy = {
                  uuid = "service_total";
                  default_yandex_policies = "stable";
                  regexp = {
                    ["awacs-balancer-health-check"] = {
                      priority = 3;
                      match_fsm = {
                        URI = "/awacs-balancer-health-check";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      errordocument = {
                        status = 200;
                        force_conn_close = false;
                      }; -- errordocument
                    }; -- ["awacs-balancer-health-check"]
                    slbping = {
                      priority = 2;
                      match_fsm = {
                        url = "/ping";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                      shared = {
                        uuid = "7812409944542501370";
                      }; -- shared
                    }; -- slbping
                    default = {
                      priority = 1;
                      headers = {
                        create_weak = {
                          ["X-Yandex-HTTPS"] = "yes";
                        }; -- create_weak
                        webauth = {
                          auth_path = "/auth_request?idm_role=/webauth-awacs/awacs/easy-mode-v2.test.yandex.net/user/";
                          checker = {
                            headers = {
                              create = {
                                Host = "webauth.yandex-team.ru";
                              }; -- create
                              append_func = {
                                ["X-Forwarded-For"] = "realip";
                                ["X-Forwarded-For-Y"] = "realip";
                              }; -- append_func
                              log_headers = {
                                response_name_re = "Webauth-Denial-Reasons";
                                balancer2 = {
                                  retry_policy = {
                                    unique_policy = {};
                                  }; -- retry_policy
                                  attempts = 2;
                                  connection_attempts = 2;
                                  rr = {
                                    unpack(gen_proxy_backends({
                                      { "webauth.yandex-team.ru"; 443; 1.000; "2a02:6b8:0:3400:0:71d:0:3a1"; };
                                    }, {
                                      resolve_timeout = "10ms";
                                      connect_timeout = "100ms";
                                      backend_timeout = "5s";
                                      fail_on_5xx = true;
                                      http_backend = true;
                                      buffering = false;
                                      keepalive_count = 0;
                                      need_resolve = true;
                                      https_settings = {
                                        ciphers = "kEECDH+AESGCM+AES128:kEECDH+AES128:kEECDH+AESGCM+AES256:kRSA+AESGCM+AES128:kRSA+AES128:RC4-SHA:DES-CBC3-SHA:!aNULL:!eNULL:!MD5:!EXPORT:!LOW:!SEED:!CAMELLIA:!IDEA:!PSK:!SRP:!SSLv2";
                                        ca_file = get_ca_cert_path("allCAs.pem", "./");
                                        sni_on = true;
                                        verify_depth = 3;
                                        sni_host = "webauth.yandex-team.ru";
                                      }; -- https_settings
                                    }))
                                  }; -- rr
                                  attempts_rate_limiter = {
                                    limit = 1.200;
                                    coeff = 0.990;
                                    switch_default = true;
                                  }; -- attempts_rate_limiter
                                }; -- balancer2
                              }; -- log_headers
                            }; -- headers
                          }; -- checker
                          on_forbidden = {
                            errordocument = {
                              status = 403;
                              force_conn_close = false;
                              content = "You are not authorized to access this resource.";
                            }; -- errordocument
                          }; -- on_forbidden
                          unauthorized_set_cookie = "webauth_csrf_token={csrf_token}; Path=/";
                          unauthorized_redirect = "https://passport.yandex-team.ru/auth?retpath={retpath}";
                          on_error = {
                            errordocument = {
                              status = 504;
                              force_conn_close = false;
                              content = "Failed to authenticate user.";
                            }; -- errordocument
                          }; -- on_error
                          headers = {
                            create_func = {
                              ["X-Forwarded-For-Y"] = "realip";
                            }; -- create_func
                            regexp = {
                              default = {
                                priority = 1;
                                regexp_host = {
                                  ["ec.yandex.net"] = {
                                    priority = 2;
                                    pattern = "((ec\\.yandex\\.net))(:11112)?";
                                    case_insensitive = true;
                                    regexp = {
                                      flat = {
                                        priority = 1;
                                        match_fsm = {
                                          path = ".*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "flat";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 3;
                                            sd = {
                                              endpoint_sets = {
                                                {
                                                  cluster_name = "sas";
                                                  endpoint_set_id = "httpbin";
                                                };
                                                {
                                                  cluster_name = "man";
                                                  endpoint_set_id = "httpbin";
                                                };
                                                {
                                                  cluster_name = "vla";
                                                  endpoint_set_id = "httpbin";
                                                };
                                              }; -- endpoint_sets
                                              proxy_options = {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "20ms";
                                                backend_timeout = "50ms";
                                                fail_on_5xx = false;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 0;
                                                need_resolve = false;
                                                switched_backend_timeout = "31536000s";
                                              }; -- proxy_options
                                              dynamic = {
                                                max_pessimized_share = 0.200;
                                                min_pessimization_coeff = 0.100;
                                                weight_increase_step = 0.100;
                                                history_interval = "10s";
                                                backends_name = "httpbin-man#httpbin-sas#httpbin-vla";
                                              }; -- dynamic
                                            }; -- sd
                                            attempts_rate_limiter = {
                                              limit = 0.200;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- flat
                                    }; -- regexp
                                  }; -- ["ec.yandex.net"]
                                  domain_not_found = {
                                    priority = 1;
                                    pattern = ".*";
                                    case_insensitive = true;
                                    errordocument = {
                                      status = 404;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- domain_not_found
                                }; -- regexp_host
                              }; -- default
                            }; -- regexp
                          }; -- headers
                        }; -- webauth
                      }; -- headers
                    }; -- default
                  }; -- regexp
                }; -- cookie_policy
                refers = "service_total";
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- ssl_sni
      }; -- errorlog
    }; -- https_section
  }; -- ipdispatch
}