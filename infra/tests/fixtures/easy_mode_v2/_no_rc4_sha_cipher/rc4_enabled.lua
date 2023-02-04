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
  unistat = {
    addrs = {
      {
        ip = "*";
        port = get_port_var("port", 2);
        disabled = get_int_var("disable_external", 0);
      };
    }; -- addrs
  }; -- unistat
  cpu_limiter = {
    active_check_subnet_default = true;
    disable_file = "./controls/cpu_limiter_disabled";
    active_check_subnet_file = "./controls/active_check_subnets_list";
  }; -- cpu_limiter
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
                      uuid = "1683180570280210434";
                      stats_eater = {
                        balancer2 = {
                          unique_policy = {};
                          attempts = 1;
                          rr = {
                            weights_file = "./controls/slb_check.weights";
                            to_upstream = {
                              weight = 1.000;
                              errordocument = {
                                status = 200;
                                force_conn_close = false;
                              }; -- errordocument
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
                    regexp = {
                      default = {
                        priority = 1;
                        regexp_host = {
                          ["by_dc.easy-mode.yandex.net"] = {
                            priority = 2;
                            pattern = "(by_dc\\.easy-mode\\.yandex\\.net)(:11111)?";
                            case_insensitive = true;
                            regexp = {
                              by_dc = {
                                priority = 1;
                                match_fsm = {
                                  path = ".*";
                                  case_insensitive = true;
                                  surround = false;
                                }; -- match_fsm
                                report = {
                                  uuid = "by_dc";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  balancer2 = {
                                    unique_policy = {};
                                    attempts = 2;
                                    rr = {
                                      weights_file = "./controls/traffic_control.weights";
                                      by_dc_man = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_by_dc_to_man";
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
                                                  cluster_name = "man";
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
                                              }; -- proxy_options
                                              dynamic = {
                                                max_pessimized_share = 0.200;
                                                min_pessimization_coeff = 0.100;
                                                weight_increase_step = 0.100;
                                                history_interval = "10s";
                                                backends_name = "httpbin-man";
                                              }; -- dynamic
                                            }; -- sd
                                            attempts_rate_limiter = {
                                              limit = 0.200;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- by_dc_man
                                      by_dc_sas = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_by_dc_to_sas";
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
                                              }; -- proxy_options
                                              dynamic = {
                                                max_pessimized_share = 0.200;
                                                min_pessimization_coeff = 0.100;
                                                weight_increase_step = 0.100;
                                                history_interval = "10s";
                                                backends_name = "httpbin-sas";
                                              }; -- dynamic
                                            }; -- sd
                                            attempts_rate_limiter = {
                                              limit = 0.200;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- by_dc_sas
                                      by_dc_vla = {
                                        weight = 1.000;
                                        report = {
                                          uuid = "requests_by_dc_to_vla";
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
                                              }; -- proxy_options
                                              dynamic = {
                                                max_pessimized_share = 0.200;
                                                min_pessimization_coeff = 0.100;
                                                weight_increase_step = 0.100;
                                                history_interval = "10s";
                                                backends_name = "httpbin-vla";
                                              }; -- dynamic
                                            }; -- sd
                                            attempts_rate_limiter = {
                                              limit = 0.200;
                                              coeff = 0.990;
                                              switch_default = true;
                                            }; -- attempts_rate_limiter
                                          }; -- balancer2
                                        }; -- report
                                      }; -- by_dc_vla
                                      by_dc_devnull = {
                                        weight = -1.000;
                                        report = {
                                          uuid = "requests_by_dc_to_devnull";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          errordocument = {
                                            status = 204;
                                            force_conn_close = false;
                                          }; -- errordocument
                                        }; -- report
                                      }; -- by_dc_devnull
                                    }; -- rr
                                  }; -- balancer2
                                }; -- report
                              }; -- by_dc
                            }; -- regexp
                          }; -- ["by_dc.easy-mode.yandex.net"]
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
            ["flat.easy-mode.yandex.net"] = {
              priority = 2;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 11112, "/place/db/www/logs");
              priv = get_private_cert_path("flat.easy-mode.yandex.net.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-flat.easy-mode.yandex.net.pem", "/dev/shm/balancer");
              servername = {
                surround = false;
                case_insensitive = false;
                servername_regexp = "(flat\\.easy-mode\\.yandex\\.net)";
              }; -- servername
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.flat.easy-mode.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.flat.easy-mode.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.flat.easy-mode.yandex.net.key", "/dev/shm/balancer/priv");
                };
              }; -- ticket_keys_list
            }; -- ["flat.easy-mode.yandex.net"]
            default = {
              priority = 1;
              timeout = "100800s";
              disable_sslv3 = true;
              ciphers = get_str_var("default_ciphers");
              log = get_log_path("ssl_sni", 11112, "/place/db/www/logs");
              priv = get_private_cert_path("flat.easy-mode.yandex.net.pem", "/dev/shm/balancer/priv");
              cert = get_public_cert_path("allCAs-flat.easy-mode.yandex.net.pem", "/dev/shm/balancer");
              ticket_keys_list = {
                {
                  priority = 3;
                  keyfile = get_private_cert_path("1st.flat.easy-mode.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 2;
                  keyfile = get_private_cert_path("2nd.flat.easy-mode.yandex.net.key", "/dev/shm/balancer/priv");
                };
                {
                  priority = 1;
                  keyfile = get_private_cert_path("3rd.flat.easy-mode.yandex.net.key", "/dev/shm/balancer/priv");
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
                        uuid = "1683180570280210434";
                      }; -- shared
                    }; -- slbping
                    default = {
                      priority = 1;
                      regexp = {
                        default = {
                          priority = 1;
                          regexp_host = {
                            ["flat.easy-mode.yandex.net"] = {
                              priority = 2;
                              pattern = "(flat\\.easy-mode\\.yandex\\.net)(:11112)?";
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
                            }; -- ["flat.easy-mode.yandex.net"]
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