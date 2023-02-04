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


function get_geo(name, default_geo)
  default_geo = default_geo or "random"
  return name .. (DC or default_geo);
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


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


function get_str_env_var(name, default)
  rv = os.getenv(name)
  if rv == nil then
    if default == nil then
      error(string.format('Environment variable "%s" is not set.', name))
    else
      return default
    end
  else
    return rv
  end
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
  workers = get_workers();
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", get_port_var("port"), "/place/db/www/logs");
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
  sd = {
    client_name = "awacs-l7-balancer(namespace-id:balancer)";
    host = "sd.yandex.net";
    port = 8080;
    connect_timeout = "50ms";
    request_timeout = "1s";
    cache_dir = "./sd_cache";
  }; -- sd
  addrs = {
    {
      ip = "*";
      port = get_port_var("port");
      disabled = get_int_var("disable_external", 0);
    };
    {
      ip = "*";
      port = 81;
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
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
        get_port_var("port");
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", get_port_var("port"), "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", get_port_var("port"), "/place/db/www/logs");
            report = {
              uuid = "service_total";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              regexp = {
                ["awacs-balancer-health-check"] = {
                  priority = 7;
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
                images_apphost_with_ab = {
                  priority = 6;
                  match_fsm = {
                    URI = "/images-apphost/alice";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "6726893485926116582";
                    report = {
                      uuid = "images_apphost_with_ab";
                      ranges = get_str_var("default_ranges");
                      just_storage = false;
                      disable_robotness = true;
                      disable_sslness = true;
                      events = {
                        stats = "report";
                      }; -- events
                      geobase = {
                        trusted = false;
                        geo_host = "laas.yandex.ru";
                        take_ip_from = "X-Forwarded-For-Y";
                        laas_answer_header = "X-LaaS-Answered";
                        file_switch = "./controls/disable_geobase.switch";
                        geo_path = "/region?response_format=header&version=1&service=balancer";
                        geo = {
                          shared = {
                            uuid = "6796952198230220343";
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
                                      { "laas.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::1022"; };
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
                        headers = {
                          create = {
                            ["X-L7-EXP"] = "true";
                          }; -- create
                          exp_getter = {
                            trusted = false;
                            file_switch = "./controls/expgetter.switch";
                            service_name = "images";
                            service_name_header = "Y-Service";
                            exp_headers = "X-Yandex-ExpConfigVersion(-Pre)?|X-Yandex-ExpBoxes(-Pre)?|X-Yandex-ExpFlags(-Pre)?|X-Yandex-LogstatUID|X-Yandex-ExpSplitParams";
                            uaas = {
                              shared = {
                                uuid = "8455959467899863695";
                                report = {
                                  uuid = "expgetter";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  stats_eater = {
                                    balancer2 = {
                                      by_name_policy = {
                                        name = get_geo("bygeo_", "random");
                                        simple_policy = {};
                                      }; -- by_name_policy
                                      attempts = 1;
                                      rr = {
                                        bygeo_iva = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            sd = {
                                              endpoint_sets = {
                                                {
                                                  cluster_name = "iva";
                                                  endpoint_set_id = "production_uaas_iva";
                                                };
                                              }; -- endpoint_sets
                                              proxy_options = {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = false;
                                              }; -- proxy_options
                                              rr = {};
                                            }; -- sd
                                          }; -- balancer2
                                        }; -- bygeo_iva
                                        bygeo_man = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            sd = {
                                              endpoint_sets = {
                                                {
                                                  cluster_name = "man";
                                                  endpoint_set_id = "production_uaas_man";
                                                };
                                              }; -- endpoint_sets
                                              proxy_options = {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = false;
                                              }; -- proxy_options
                                              rr = {};
                                            }; -- sd
                                          }; -- balancer2
                                        }; -- bygeo_man
                                        bygeo_myt = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            sd = {
                                              endpoint_sets = {
                                                {
                                                  cluster_name = "myt";
                                                  endpoint_set_id = "production_uaas_myt";
                                                };
                                              }; -- endpoint_sets
                                              proxy_options = {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = false;
                                              }; -- proxy_options
                                              rr = {};
                                            }; -- sd
                                          }; -- balancer2
                                        }; -- bygeo_myt
                                        bygeo_sas = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            sd = {
                                              endpoint_sets = {
                                                {
                                                  cluster_name = "sas";
                                                  endpoint_set_id = "production_uaas_sas";
                                                };
                                              }; -- endpoint_sets
                                              proxy_options = {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = false;
                                              }; -- proxy_options
                                              rr = {};
                                            }; -- sd
                                          }; -- balancer2
                                        }; -- bygeo_sas
                                        bygeo_vla = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            sd = {
                                              endpoint_sets = {
                                                {
                                                  cluster_name = "vla";
                                                  endpoint_set_id = "production_uaas_vla";
                                                };
                                              }; -- endpoint_sets
                                              proxy_options = {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = false;
                                              }; -- proxy_options
                                              rr = {};
                                            }; -- sd
                                          }; -- balancer2
                                        }; -- bygeo_vla
                                      }; -- rr
                                      on_error = {
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 1;
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "uaas.search.yandex.net"; 80; 1.000; "2a02:6b8:0:3400::2:48"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "20ms";
                                              backend_timeout = "30ms";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 1;
                                              need_resolve = true;
                                            }))
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- stats_eater
                                }; -- report
                              }; -- shared
                            }; -- uaas
                            headers_hasher = {
                              header_name = "X-Yandex-LogstatUID";
                              surround = false;
                              randomize_empty_match = true;
                              shared = {
                                uuid = "images_apphost_prod";
                              }; -- shared
                            }; -- headers_hasher
                          }; -- exp_getter
                        }; -- headers
                      }; -- geobase
                    }; -- report
                  }; -- shared
                }; -- images_apphost_with_ab
                images_apphost = {
                  priority = 5;
                  match_fsm = {
                    URI = "/images-apphost/.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "6013407083076926241";
                    shared = {
                      uuid = "images_apphost_prod";
                      report = {
                        uuid = "images_apphost";
                        ranges = get_str_var("default_ranges");
                        just_storage = false;
                        disable_robotness = true;
                        disable_sslness = true;
                        events = {
                          stats = "report";
                        }; -- events
                        threshold = {
                          lo_bytes = 1048576;
                          hi_bytes = 5242880;
                          recv_timeout = "50ms";
                          pass_timeout = "10s";
                          on_pass_timeout_failure = {
                            errordocument = {
                              status = 413;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- on_pass_timeout_failure
                          balancer2 = {
                            by_name_from_header_policy = {
                              allow_zero_weights = true;
                              strict = true;
                              hints = {
                                {
                                  hint = "man";
                                  backend = "images_man";
                                };
                                {
                                  hint = "sas";
                                  backend = "images_sas";
                                };
                                {
                                  hint = "vla";
                                  backend = "images_vla";
                                };
                              }; -- hints
                              by_hash_policy = {
                                unique_policy = {};
                              }; -- by_hash_policy
                            }; -- by_name_from_header_policy
                            attempts = 1;
                            attempts_file = "./controls/images.attempts";
                            rr = {
                              weights_file = "./controls/search_l7_balancer_switch.json";
                              images_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "images_apphost_requests_to_vla";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  shared = {
                                    uuid = "images_apphost_vla";
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 2;
                                      attempts_file = "./controls/attempts.count";
                                      rr = {
                                        unpack(gen_proxy_backends({
                                          { "vla1-0147.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3f:0:604:db7:a6ca"; };
                                          { "vla1-0152.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3f:0:604:db7:a705"; };
                                          { "vla1-0221.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:81:0:604:db7:a8cf"; };
                                          { "vla1-0703.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d01"; };
                                          { "vla1-0951.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a3de"; };
                                          { "vla1-1128.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a77d"; };
                                          { "vla1-1161.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab54"; };
                                          { "vla1-1207.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:9a:0:604:db7:aa9b"; };
                                          { "vla1-1239.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:93:0:604:db7:a751"; };
                                          { "vla1-1257.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:81:0:604:db7:a7e7"; };
                                          { "vla1-1278.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:4d:0:604:db7:a440"; };
                                          { "vla1-1294.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:1b:0:604:db7:9b7e"; };
                                          { "vla1-1304.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:4c:0:604:db7:a0ce"; };
                                          { "vla1-1315.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:a325"; };
                                          { "vla1-1330.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:78:0:604:db7:a9c3"; };
                                          { "vla1-1358.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a764"; };
                                          { "vla1-1384.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:44:0:604:db7:9f36"; };
                                          { "vla1-1395.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:84:0:604:db7:abfb"; };
                                          { "vla1-1464.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:48:0:604:db7:a695"; };
                                          { "vla1-1516.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:43:0:604:db7:a110"; };
                                          { "vla1-1518.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9eb8"; };
                                          { "vla1-1598.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:13:0:604:db7:9a64"; };
                                          { "vla1-1710.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a416"; };
                                          { "vla1-1757.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:88:0:604:db7:a771"; };
                                          { "vla1-1815.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:57:0:604:db7:a38f"; };
                                          { "vla1-1911.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a5fd"; };
                                          { "vla1-1930.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:44:0:604:db7:a6cc"; };
                                          { "vla1-1987.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:82:0:604:db7:a872"; };
                                          { "vla1-2016.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:91:0:604:db7:ab5e"; };
                                          { "vla1-2017.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:34:0:604:db7:9d11"; };
                                          { "vla1-2078.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d02"; };
                                          { "vla1-2092.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:17:0:604:db7:a7f5"; };
                                          { "vla1-2121.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:98:0:604:db7:a008"; };
                                          { "vla1-2122.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d07"; };
                                          { "vla1-2143.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:27:0:604:db7:9f6f"; };
                                          { "vla1-2163.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9cfa"; };
                                          { "vla1-2315.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:6e:0:604:db7:a371"; };
                                          { "vla1-2319.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a368"; };
                                          { "vla1-2330.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7d:0:604:db7:a2b0"; };
                                          { "vla1-2332.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a84b"; };
                                          { "vla1-2345.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:ab9e"; };
                                          { "vla1-2351.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a7ea"; };
                                          { "vla1-2358.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a3d8"; };
                                          { "vla1-2453.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:94:0:604:db7:a94f"; };
                                          { "vla1-2472.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:97:0:604:db7:a909"; };
                                          { "vla1-3780.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:9e7d"; };
                                          { "vla1-4111.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a820"; };
                                          { "vla1-4272.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a45a"; };
                                          { "vla1-4375.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:a82d"; };
                                          { "vla1-4428.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8a:0:604:db7:a818"; };
                                          { "vla1-4443.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:9dee"; };
                                          { "vla1-4504.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:90:0:604:db7:a7e1"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "10s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- rr
                                      on_error = {
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 2;
                                          attempts_file = "./controls/attempts.count";
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "sas1-5973.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:100:225:90ff:fee3:9cf2"; };
                                              { "sas1-6349.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:69f:215:b2ff:fea7:7bc4"; };
                                              { "sas1-6351.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:11a:215:b2ff:fea7:8280"; };
                                              { "sas1-6752.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:9008"; };
                                              { "sas1-6893.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:11d:215:b2ff:fea7:9060"; };
                                              { "sas1-6939.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b324"; };
                                              { "sas1-6978.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:123:215:b2ff:fea7:b720"; };
                                              { "sas1-7095.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:124:215:b2ff:fea7:ab98"; };
                                              { "sas1-7098.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:124:215:b2ff:fea7:8ee4"; };
                                              { "sas1-7125.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:907c"; };
                                              { "sas1-7155.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:122:215:b2ff:fea7:8bf0"; };
                                              { "sas1-7156.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aae4"; };
                                              { "sas1-7238.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:121:215:b2ff:fea7:ac10"; };
                                              { "sas1-7272.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:121:215:b2ff:fea7:8300"; };
                                              { "sas1-7286.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:128:215:b2ff:fea7:7aec"; };
                                              { "sas1-7287.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:128:215:b2ff:fea7:7910"; };
                                              { "sas1-7326.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:126:215:b2ff:fea7:ad8c"; };
                                              { "sas1-7330.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:628:215:b2ff:fea7:90d0"; };
                                              { "sas1-7331.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc9c"; };
                                              { "sas1-7459.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8cd0"; };
                                              { "sas1-7494.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:125:215:b2ff:fea7:6590"; };
                                              { "sas1-7498.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:125:215:b2ff:fea7:aaa4"; };
                                              { "sas1-7686.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bf0c"; };
                                              { "sas1-7825.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:7fbc"; };
                                              { "sas1-7843.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:82f0"; };
                                              { "sas1-7854.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a1d:6382"; };
                                              { "sas1-7855.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a1d:5ed0"; };
                                              { "sas1-7857.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a1d:630a"; };
                                              { "sas1-7858.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a1d:5d0c"; };
                                              { "sas1-7878.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12e:215:b2ff:fea7:7db8"; };
                                              { "sas1-7917.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a52:c938"; };
                                              { "sas1-7922.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a51:503a"; };
                                              { "sas1-7923.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a52:ccda"; };
                                              { "sas1-7929.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12b:215:b2ff:fea7:ac24"; };
                                              { "sas1-7946.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:789:0:604:1e9:e532"; };
                                              { "sas1-7948.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:767:0:604:35c6:3276"; };
                                              { "sas1-7949.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:766:0:604:35c6:3412"; };
                                              { "sas1-7950.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a9:0:604:1e9:dcbe"; };
                                              { "sas1-7951.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:769:0:604:35c6:2e80"; };
                                              { "sas1-7953.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:769:0:604:35c6:32e4"; };
                                              { "sas1-7954.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:767:0:604:35c6:311a"; };
                                              { "sas1-7955.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:767:0:604:35c6:34ca"; };
                                              { "sas1-7956.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a9:0:604:1e9:e762"; };
                                              { "sas1-7958.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a9:0:604:1e9:de2e"; };
                                              { "sas1-7959.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:769:0:604:35c6:32c4"; };
                                              { "sas1-7960.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:768:0:604:35c6:304a"; };
                                              { "sas1-7962.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:769:0:604:35c6:2a8c"; };
                                              { "sas1-7963.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:766:0:604:35c6:3580"; };
                                              { "sas1-8187.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:68f:215:b2ff:fea7:8b68"; };
                                              { "sas1-8255.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a6:215:b2ff:fea7:b1d0"; };
                                              { "sas1-8873.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:138:215:b2ff:fea7:ba80"; };
                                              { "sas1-8979.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3f9e"; };
                                              { "sas1-9164.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:143:feaa:14ff:fede:417c"; };
                                              { "sas1-9283.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:143:feaa:14ff:fede:3eb8"; };
                                              { "sas1-9452.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:dc86"; };
                                              { "sas1-9467.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:ddfe"; };
                                              { "sas1-9468.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a8:0:604:1e9:e16a"; };
                                              { "sas1-9778.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a8:0:604:1e9:df96"; };
                                              { "sas2-2081.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:e4c6"; };
                                              { "sas2-5021.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:500:0:604:9088:b8a6"; };
                                              { "sas2-7093.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7c4:0:604:90e4:b418"; };
                                              { "sas2-7097.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7c4:0:604:90e4:b61c"; };
                                              { "sas2-7104.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7c4:0:604:90e4:cb0c"; };
                                              { "sas2-7113.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5bb:0:604:3564:4ddd"; };
                                              { "sas2-7120.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:555:0:604:90e5:4420"; };
                                              { "sas2-7130.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:52d:0:604:90e4:cfaa"; };
                                              { "sas2-7139.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5bb:0:604:35c6:3038"; };
                                              { "sas2-7142.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:539:0:604:354b:5dcd"; };
                                              { "sas2-8199.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:778:0:604:7a18:10d0"; };
                                              { "sas2-8200.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77f:0:604:7a1d:5f88"; };
                                              { "sas2-8201.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:778:0:604:7a51:501c"; };
                                              { "sas2-8202.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5d5:0:604:7a18:f16"; };
                                              { "sas2-8203.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:780:0:604:7a51:554c"; };
                                              { "sas2-8204.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77d:0:604:7a51:515a"; };
                                              { "sas2-8205.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77e:0:604:7a51:5012"; };
                                              { "sas2-8206.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:785:0:604:7a18:e44"; };
                                              { "sas2-8207.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:780:0:604:7a51:5596"; };
                                              { "sas2-8208.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:777:0:604:7a52:cb38"; };
                                              { "sas2-8209.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7bc:0:604:7a1d:5c46"; };
                                              { "sas2-8210.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5d5:0:604:7a1d:5edc"; };
                                              { "sas2-8211.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:780:0:604:7a51:55d8"; };
                                              { "sas2-8212.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:778:0:604:90eb:915e"; };
                                              { "sas2-8213.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77d:0:604:7a51:52a6"; };
                                              { "sas2-8214.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7bc:0:604:7a51:5594"; };
                                              { "sas2-8216.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7bc:0:604:7a51:539e"; };
                                              { "sas2-8911.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d2:0:604:14db:76b0"; };
                                              { "sas2-9039.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b14:0:604:141d:f5f8"; };
                                              { "sas2-9713.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b39:0:604:3564:5191"; };
                                              { "sas2-9714.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b39:0:604:3564:5301"; };
                                              { "sas3-0468.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:dffe"; };
                                              { "sas3-0479.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:dc72"; };
                                              { "sas3-0480.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:e42a"; };
                                              { "sas3-0482.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:e7b6"; };
                                              { "sas3-0497.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:dbbe"; };
                                              { "sas3-0503.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:dc3e"; };
                                              { "sas3-0516.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:e0d6"; };
                                              { "sas3-0524.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:e5da"; };
                                              { "sas3-0536.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:dcfe"; };
                                              { "sas3-0542.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:dede"; };
                                              { "sas3-0546.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a8:0:604:1e9:e142"; };
                                              { "sas3-0558.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:def6"; };
                                              { "sas3-0559.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:e4ee"; };
                                              { "sas3-0561.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:e286"; };
                                              { "sas3-0569.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:e7f6"; };
                                              { "sas3-0572.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:db7e"; };
                                              { "sas3-0580.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:dc3a"; };
                                              { "sas3-0586.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:e456"; };
                                              { "sas3-0593.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:de06"; };
                                              { "sas3-0600.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:de52"; };
                                              { "sas3-0611.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:e0aa"; };
                                              { "sas3-0612.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:dd12"; };
                                              { "sas3-0616.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7aa:0:604:1e9:dcce"; };
                                              { "sas3-1118.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:e35e"; };
                                              { "sas3-2031.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77e:0:604:90cb:d2a"; };
                                              { "sas3-2038.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77e:0:604:7a18:a14"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "15s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                            }))
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- shared
                                }; -- report
                              }; -- images_vla
                              images_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "images_apphost_requests_to_sas";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  shared = {
                                    uuid = "images_apphost_sas";
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 2;
                                      attempts_file = "./controls/attempts.count";
                                      rr = {
                                        unpack(gen_proxy_backends({
                                          { "sas1-5973.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:100:225:90ff:fee3:9cf2"; };
                                          { "sas1-6349.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:69f:215:b2ff:fea7:7bc4"; };
                                          { "sas1-6351.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:11a:215:b2ff:fea7:8280"; };
                                          { "sas1-6752.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:9008"; };
                                          { "sas1-6893.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:11d:215:b2ff:fea7:9060"; };
                                          { "sas1-6939.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b324"; };
                                          { "sas1-6978.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:123:215:b2ff:fea7:b720"; };
                                          { "sas1-7095.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:124:215:b2ff:fea7:ab98"; };
                                          { "sas1-7098.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:124:215:b2ff:fea7:8ee4"; };
                                          { "sas1-7125.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:907c"; };
                                          { "sas1-7155.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:122:215:b2ff:fea7:8bf0"; };
                                          { "sas1-7156.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aae4"; };
                                          { "sas1-7238.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:121:215:b2ff:fea7:ac10"; };
                                          { "sas1-7272.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:121:215:b2ff:fea7:8300"; };
                                          { "sas1-7286.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:128:215:b2ff:fea7:7aec"; };
                                          { "sas1-7287.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:128:215:b2ff:fea7:7910"; };
                                          { "sas1-7326.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:126:215:b2ff:fea7:ad8c"; };
                                          { "sas1-7330.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:628:215:b2ff:fea7:90d0"; };
                                          { "sas1-7331.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc9c"; };
                                          { "sas1-7459.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8cd0"; };
                                          { "sas1-7494.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:125:215:b2ff:fea7:6590"; };
                                          { "sas1-7498.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:125:215:b2ff:fea7:aaa4"; };
                                          { "sas1-7686.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bf0c"; };
                                          { "sas1-7825.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:7fbc"; };
                                          { "sas1-7843.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:82f0"; };
                                          { "sas1-7854.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a1d:6382"; };
                                          { "sas1-7855.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a1d:5ed0"; };
                                          { "sas1-7857.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a1d:630a"; };
                                          { "sas1-7858.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a1d:5d0c"; };
                                          { "sas1-7878.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12e:215:b2ff:fea7:7db8"; };
                                          { "sas1-7917.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a52:c938"; };
                                          { "sas1-7922.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a51:503a"; };
                                          { "sas1-7923.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a6:0:604:7a52:ccda"; };
                                          { "sas1-7929.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:12b:215:b2ff:fea7:ac24"; };
                                          { "sas1-7946.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:789:0:604:1e9:e532"; };
                                          { "sas1-7948.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:767:0:604:35c6:3276"; };
                                          { "sas1-7949.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:766:0:604:35c6:3412"; };
                                          { "sas1-7950.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a9:0:604:1e9:dcbe"; };
                                          { "sas1-7951.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:769:0:604:35c6:2e80"; };
                                          { "sas1-7953.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:769:0:604:35c6:32e4"; };
                                          { "sas1-7954.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:767:0:604:35c6:311a"; };
                                          { "sas1-7955.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:767:0:604:35c6:34ca"; };
                                          { "sas1-7956.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a9:0:604:1e9:e762"; };
                                          { "sas1-7958.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a9:0:604:1e9:de2e"; };
                                          { "sas1-7959.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:769:0:604:35c6:32c4"; };
                                          { "sas1-7960.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:768:0:604:35c6:304a"; };
                                          { "sas1-7962.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:769:0:604:35c6:2a8c"; };
                                          { "sas1-7963.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:766:0:604:35c6:3580"; };
                                          { "sas1-8187.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:68f:215:b2ff:fea7:8b68"; };
                                          { "sas1-8255.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6a6:215:b2ff:fea7:b1d0"; };
                                          { "sas1-8873.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:138:215:b2ff:fea7:ba80"; };
                                          { "sas1-8979.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3f9e"; };
                                          { "sas1-9164.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:143:feaa:14ff:fede:417c"; };
                                          { "sas1-9283.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:143:feaa:14ff:fede:3eb8"; };
                                          { "sas1-9452.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:dc86"; };
                                          { "sas1-9467.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:ddfe"; };
                                          { "sas1-9468.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a8:0:604:1e9:e16a"; };
                                          { "sas1-9778.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a8:0:604:1e9:df96"; };
                                          { "sas2-2081.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:e4c6"; };
                                          { "sas2-5021.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:500:0:604:9088:b8a6"; };
                                          { "sas2-7093.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7c4:0:604:90e4:b418"; };
                                          { "sas2-7097.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7c4:0:604:90e4:b61c"; };
                                          { "sas2-7104.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7c4:0:604:90e4:cb0c"; };
                                          { "sas2-7113.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5bb:0:604:3564:4ddd"; };
                                          { "sas2-7120.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:555:0:604:90e5:4420"; };
                                          { "sas2-7130.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:52d:0:604:90e4:cfaa"; };
                                          { "sas2-7139.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5bb:0:604:35c6:3038"; };
                                          { "sas2-7142.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:539:0:604:354b:5dcd"; };
                                          { "sas2-8199.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:778:0:604:7a18:10d0"; };
                                          { "sas2-8200.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77f:0:604:7a1d:5f88"; };
                                          { "sas2-8201.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:778:0:604:7a51:501c"; };
                                          { "sas2-8202.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5d5:0:604:7a18:f16"; };
                                          { "sas2-8203.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:780:0:604:7a51:554c"; };
                                          { "sas2-8204.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77d:0:604:7a51:515a"; };
                                          { "sas2-8205.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77e:0:604:7a51:5012"; };
                                          { "sas2-8206.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:785:0:604:7a18:e44"; };
                                          { "sas2-8207.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:780:0:604:7a51:5596"; };
                                          { "sas2-8208.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:777:0:604:7a52:cb38"; };
                                          { "sas2-8209.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7bc:0:604:7a1d:5c46"; };
                                          { "sas2-8210.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:5d5:0:604:7a1d:5edc"; };
                                          { "sas2-8211.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:780:0:604:7a51:55d8"; };
                                          { "sas2-8212.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:778:0:604:90eb:915e"; };
                                          { "sas2-8213.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77d:0:604:7a51:52a6"; };
                                          { "sas2-8214.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7bc:0:604:7a51:5594"; };
                                          { "sas2-8216.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7bc:0:604:7a51:539e"; };
                                          { "sas2-8911.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d2:0:604:14db:76b0"; };
                                          { "sas2-9039.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b14:0:604:141d:f5f8"; };
                                          { "sas2-9713.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b39:0:604:3564:5191"; };
                                          { "sas2-9714.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b39:0:604:3564:5301"; };
                                          { "sas3-0468.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:dffe"; };
                                          { "sas3-0479.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:dc72"; };
                                          { "sas3-0480.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:e42a"; };
                                          { "sas3-0482.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:e7b6"; };
                                          { "sas3-0497.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:dbbe"; };
                                          { "sas3-0503.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:dc3e"; };
                                          { "sas3-0516.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:e0d6"; };
                                          { "sas3-0524.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:e5da"; };
                                          { "sas3-0536.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:dcfe"; };
                                          { "sas3-0542.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:dede"; };
                                          { "sas3-0546.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7a8:0:604:1e9:e142"; };
                                          { "sas3-0558.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:def6"; };
                                          { "sas3-0559.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:e4ee"; };
                                          { "sas3-0561.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:e286"; };
                                          { "sas3-0569.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:e7f6"; };
                                          { "sas3-0572.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:db7e"; };
                                          { "sas3-0580.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b16:0:604:1e9:dc3a"; };
                                          { "sas3-0586.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:e456"; };
                                          { "sas3-0593.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:de06"; };
                                          { "sas3-0600.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:de52"; };
                                          { "sas3-0611.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:b13:0:604:1e9:e0aa"; };
                                          { "sas3-0612.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:dd12"; };
                                          { "sas3-0616.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7aa:0:604:1e9:dcce"; };
                                          { "sas3-1118.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:7d3:0:604:1e9:e35e"; };
                                          { "sas3-2031.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77e:0:604:90cb:d2a"; };
                                          { "sas3-2038.search.yandex.net"; 31815; 40.000; "2a02:6b8:c02:77e:0:604:7a18:a14"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "10s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- rr
                                      on_error = {
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 2;
                                          attempts_file = "./controls/attempts.count";
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "man1-1076.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f1f2"; };
                                              { "man1-1150.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f2ca"; };
                                              { "man1-1885.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1d50"; };
                                              { "man1-1957.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:16f0"; };
                                              { "man1-1979.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:550"; };
                                              { "man1-2023.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1640"; };
                                              { "man1-2087.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e320"; };
                                              { "man1-2092.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6098:f652:14ff:fe8c:1e0"; };
                                              { "man1-2106.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:720"; };
                                              { "man1-2873.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e840"; };
                                              { "man1-2943.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dc40"; };
                                              { "man1-3252.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1a10"; };
                                              { "man1-3260.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c700"; };
                                              { "man1-3261.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c6d0"; };
                                              { "man1-3265.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:d4c0"; };
                                              { "man1-3752.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:b9d0"; };
                                              { "man1-3822.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7eaa"; };
                                              { "man1-3904.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7866"; };
                                              { "man1-3959.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe6f:80a2"; };
                                              { "man1-4025.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe6e:ba8c"; };
                                              { "man1-4311.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7dfc"; };
                                              { "man1-6102.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4ed0"; };
                                              { "man1-6227.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6057:f652:14ff:fef5:c8b0"; };
                                              { "man1-6242.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6057:f652:14ff:fef5:cb40"; };
                                              { "man1-6263.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5370"; };
                                              { "man1-6763.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6058:e61d:2dff:fe04:50d0"; };
                                              { "man1-7132.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:89b:0:604:ba55:f742"; };
                                              { "man1-7430.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:899:0:604:2d04:49d0"; };
                                              { "man1-8883.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6064:e61d:2dff:fe03:41c0"; };
                                              { "man2-4942.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:634:0:604:2d6d:4000"; };
                                              { "man2-4948.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63a:0:604:2d6d:2d50"; };
                                              { "man2-4959.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7888"; };
                                              { "man2-4964.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:634:0:604:2d6d:15e0"; };
                                              { "man2-5025.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:73ac"; };
                                              { "man2-5159.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7802"; };
                                              { "man2-5408.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:797c"; };
                                              { "man2-5427.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7ec6"; };
                                              { "man2-5476.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:76a4"; };
                                              { "man2-5522.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:7358"; };
                                              { "man2-5676.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7fcc"; };
                                              { "man2-5681.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:638:0:604:2d6d:2c20"; };
                                              { "man2-5691.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:3fe0"; };
                                              { "man2-5694.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:3900"; };
                                              { "man2-5708.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65a:0:604:2d6d:1120"; };
                                              { "man2-5710.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:3a50"; };
                                              { "man2-5725.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:638:0:604:2d6d:bf0"; };
                                              { "man2-5743.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:3040"; };
                                              { "man2-5747.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:635:0:604:2d6d:740"; };
                                              { "man2-5752.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:1440"; };
                                              { "man2-5934.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:635:0:604:2d6d:3a40"; };
                                              { "man2-5951.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:7306"; };
                                              { "man2-5954.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:3500"; };
                                              { "man2-5967.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63a:0:604:2d6d:3c50"; };
                                              { "man2-6000.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65a:0:604:2d6d:2b90"; };
                                              { "man2-6122.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63f:0:604:14a9:68ea"; };
                                              { "man2-6155.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:633:0:604:14a7:ba50"; };
                                              { "man2-6244.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:830:0:604:1465:cd69"; };
                                              { "man2-6324.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63e:0:604:14a7:ba0b"; };
                                              { "man2-6363.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:657:0:604:1465:cd75"; };
                                              { "man2-6369.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63e:0:604:14a7:b9f2"; };
                                              { "man2-6376.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:648:0:604:1465:cd6a"; };
                                              { "man2-6416.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:641:0:604:14a7:6812"; };
                                              { "man2-6468.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:633:0:604:14a7:ba87"; };
                                              { "man2-6505.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:657:0:604:14a7:67d9"; };
                                              { "man2-6596.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:647:0:604:14a7:bb87"; };
                                              { "man2-6624.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:643:0:604:14a7:bb79"; };
                                              { "man2-6636.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:630:0:604:14a7:b9d3"; };
                                              { "man2-6665.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:649:0:604:14a7:bb33"; };
                                              { "man2-6736.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:647:0:604:14a7:bb94"; };
                                              { "man2-6759.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:653:0:604:14a9:6873"; };
                                              { "man2-6779.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:836:0:604:14a7:6647"; };
                                              { "man2-6831.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:645:0:604:14a9:6944"; };
                                              { "man2-6834.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:644:0:604:14a7:bcd8"; };
                                              { "man2-6859.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:645:0:604:14a9:693f"; };
                                              { "man2-6870.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:643:0:604:14a7:bb0d"; };
                                              { "man2-6886.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:83c:0:604:14a7:bd08"; };
                                              { "man2-6890.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:643:0:604:14a7:ba9f"; };
                                              { "man2-6916.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:642:0:604:14a7:badb"; };
                                              { "man2-6921.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63b:0:604:14a7:ba19"; };
                                              { "man2-7070.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:83d:0:604:14a7:6754"; };
                                              { "man2-7099.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:644:0:604:14a9:69a0"; };
                                              { "man2-7179.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:655:0:604:14a7:ba6b"; };
                                              { "man2-7189.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:651:0:604:14a9:6a51"; };
                                              { "man2-7221.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:647:0:604:14a7:bb81"; };
                                              { "man2-7241.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:651:0:604:14a9:69d8"; };
                                              { "man2-7261.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:655:0:604:14a9:687b"; };
                                              { "man2-7327.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:64b:0:604:14a7:b99d"; };
                                              { "man2-7334.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:83c:0:604:14a7:b98e"; };
                                              { "man2-7342.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:653:0:604:14a9:69a1"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "15s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                            }))
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- shared
                                }; -- report
                              }; -- images_sas
                              images_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "images_apphost_requests_to_man";
                                  ranges = get_str_var("default_ranges");
                                  just_storage = false;
                                  disable_robotness = true;
                                  disable_sslness = true;
                                  events = {
                                    stats = "report";
                                  }; -- events
                                  shared = {
                                    uuid = "images_apphost_man";
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 2;
                                      attempts_file = "./controls/attempts.count";
                                      rr = {
                                        unpack(gen_proxy_backends({
                                          { "man1-1076.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f1f2"; };
                                          { "man1-1150.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f2ca"; };
                                          { "man1-1885.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1d50"; };
                                          { "man1-1957.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:16f0"; };
                                          { "man1-1979.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:550"; };
                                          { "man1-2023.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1640"; };
                                          { "man1-2087.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e320"; };
                                          { "man1-2092.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6098:f652:14ff:fe8c:1e0"; };
                                          { "man1-2106.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:720"; };
                                          { "man1-2873.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e840"; };
                                          { "man1-2943.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dc40"; };
                                          { "man1-3252.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1a10"; };
                                          { "man1-3260.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c700"; };
                                          { "man1-3261.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c6d0"; };
                                          { "man1-3265.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:d4c0"; };
                                          { "man1-3752.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:b9d0"; };
                                          { "man1-3822.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7eaa"; };
                                          { "man1-3904.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7866"; };
                                          { "man1-3959.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe6f:80a2"; };
                                          { "man1-4025.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe6e:ba8c"; };
                                          { "man1-4311.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7dfc"; };
                                          { "man1-6102.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4ed0"; };
                                          { "man1-6227.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6057:f652:14ff:fef5:c8b0"; };
                                          { "man1-6242.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6057:f652:14ff:fef5:cb40"; };
                                          { "man1-6263.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5370"; };
                                          { "man1-6763.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6058:e61d:2dff:fe04:50d0"; };
                                          { "man1-7132.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:89b:0:604:ba55:f742"; };
                                          { "man1-7430.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:899:0:604:2d04:49d0"; };
                                          { "man1-8883.search.yandex.net"; 31815; 40.000; "2a02:6b8:b000:6064:e61d:2dff:fe03:41c0"; };
                                          { "man2-4942.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:634:0:604:2d6d:4000"; };
                                          { "man2-4948.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63a:0:604:2d6d:2d50"; };
                                          { "man2-4959.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7888"; };
                                          { "man2-4964.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:634:0:604:2d6d:15e0"; };
                                          { "man2-5025.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:73ac"; };
                                          { "man2-5159.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7802"; };
                                          { "man2-5408.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:797c"; };
                                          { "man2-5427.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7ec6"; };
                                          { "man2-5476.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:76a4"; };
                                          { "man2-5522.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:7358"; };
                                          { "man2-5676.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:636:0:604:baa1:7fcc"; };
                                          { "man2-5681.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:638:0:604:2d6d:2c20"; };
                                          { "man2-5691.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:3fe0"; };
                                          { "man2-5694.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:3900"; };
                                          { "man2-5708.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65a:0:604:2d6d:1120"; };
                                          { "man2-5710.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:3a50"; };
                                          { "man2-5725.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:638:0:604:2d6d:bf0"; };
                                          { "man2-5743.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:3040"; };
                                          { "man2-5747.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:635:0:604:2d6d:740"; };
                                          { "man2-5752.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65b:0:604:2d6d:1440"; };
                                          { "man2-5934.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:635:0:604:2d6d:3a40"; };
                                          { "man2-5951.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:637:0:604:baa1:7306"; };
                                          { "man2-5954.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:663:0:604:2d6d:3500"; };
                                          { "man2-5967.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63a:0:604:2d6d:3c50"; };
                                          { "man2-6000.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:65a:0:604:2d6d:2b90"; };
                                          { "man2-6122.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63f:0:604:14a9:68ea"; };
                                          { "man2-6155.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:633:0:604:14a7:ba50"; };
                                          { "man2-6244.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:830:0:604:1465:cd69"; };
                                          { "man2-6324.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63e:0:604:14a7:ba0b"; };
                                          { "man2-6363.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:657:0:604:1465:cd75"; };
                                          { "man2-6369.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63e:0:604:14a7:b9f2"; };
                                          { "man2-6376.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:648:0:604:1465:cd6a"; };
                                          { "man2-6416.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:641:0:604:14a7:6812"; };
                                          { "man2-6468.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:633:0:604:14a7:ba87"; };
                                          { "man2-6505.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:657:0:604:14a7:67d9"; };
                                          { "man2-6596.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:647:0:604:14a7:bb87"; };
                                          { "man2-6624.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:643:0:604:14a7:bb79"; };
                                          { "man2-6636.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:630:0:604:14a7:b9d3"; };
                                          { "man2-6665.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:649:0:604:14a7:bb33"; };
                                          { "man2-6736.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:647:0:604:14a7:bb94"; };
                                          { "man2-6759.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:653:0:604:14a9:6873"; };
                                          { "man2-6779.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:836:0:604:14a7:6647"; };
                                          { "man2-6831.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:645:0:604:14a9:6944"; };
                                          { "man2-6834.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:644:0:604:14a7:bcd8"; };
                                          { "man2-6859.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:645:0:604:14a9:693f"; };
                                          { "man2-6870.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:643:0:604:14a7:bb0d"; };
                                          { "man2-6886.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:83c:0:604:14a7:bd08"; };
                                          { "man2-6890.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:643:0:604:14a7:ba9f"; };
                                          { "man2-6916.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:642:0:604:14a7:badb"; };
                                          { "man2-6921.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:63b:0:604:14a7:ba19"; };
                                          { "man2-7070.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:83d:0:604:14a7:6754"; };
                                          { "man2-7099.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:644:0:604:14a9:69a0"; };
                                          { "man2-7179.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:655:0:604:14a7:ba6b"; };
                                          { "man2-7189.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:651:0:604:14a9:6a51"; };
                                          { "man2-7221.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:647:0:604:14a7:bb81"; };
                                          { "man2-7241.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:651:0:604:14a9:69d8"; };
                                          { "man2-7261.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:655:0:604:14a9:687b"; };
                                          { "man2-7327.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:64b:0:604:14a7:b99d"; };
                                          { "man2-7334.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:83c:0:604:14a7:b98e"; };
                                          { "man2-7342.search.yandex.net"; 31815; 40.000; "2a02:6b8:c01:653:0:604:14a9:69a1"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "10s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- rr
                                      on_error = {
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 2;
                                          attempts_file = "./controls/attempts.count";
                                          rr = {
                                            unpack(gen_proxy_backends({
                                              { "vla1-0147.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3f:0:604:db7:a6ca"; };
                                              { "vla1-0152.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3f:0:604:db7:a705"; };
                                              { "vla1-0221.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:81:0:604:db7:a8cf"; };
                                              { "vla1-0703.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d01"; };
                                              { "vla1-0951.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a3de"; };
                                              { "vla1-1128.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a77d"; };
                                              { "vla1-1161.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab54"; };
                                              { "vla1-1207.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:9a:0:604:db7:aa9b"; };
                                              { "vla1-1239.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:93:0:604:db7:a751"; };
                                              { "vla1-1257.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:81:0:604:db7:a7e7"; };
                                              { "vla1-1278.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:4d:0:604:db7:a440"; };
                                              { "vla1-1294.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:1b:0:604:db7:9b7e"; };
                                              { "vla1-1304.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:4c:0:604:db7:a0ce"; };
                                              { "vla1-1315.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:67:0:604:db7:a325"; };
                                              { "vla1-1330.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:78:0:604:db7:a9c3"; };
                                              { "vla1-1358.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a764"; };
                                              { "vla1-1384.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:44:0:604:db7:9f36"; };
                                              { "vla1-1395.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:84:0:604:db7:abfb"; };
                                              { "vla1-1464.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:48:0:604:db7:a695"; };
                                              { "vla1-1516.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:43:0:604:db7:a110"; };
                                              { "vla1-1518.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9eb8"; };
                                              { "vla1-1598.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:13:0:604:db7:9a64"; };
                                              { "vla1-1710.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:71:0:604:db7:a416"; };
                                              { "vla1-1757.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:88:0:604:db7:a771"; };
                                              { "vla1-1815.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:57:0:604:db7:a38f"; };
                                              { "vla1-1911.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:45:0:604:db7:a5fd"; };
                                              { "vla1-1930.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:44:0:604:db7:a6cc"; };
                                              { "vla1-1987.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:82:0:604:db7:a872"; };
                                              { "vla1-2016.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:91:0:604:db7:ab5e"; };
                                              { "vla1-2017.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:34:0:604:db7:9d11"; };
                                              { "vla1-2078.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d02"; };
                                              { "vla1-2092.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:17:0:604:db7:a7f5"; };
                                              { "vla1-2121.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:98:0:604:db7:a008"; };
                                              { "vla1-2122.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d07"; };
                                              { "vla1-2143.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:27:0:604:db7:9f6f"; };
                                              { "vla1-2163.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:53:0:604:db7:9cfa"; };
                                              { "vla1-2315.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:6e:0:604:db7:a371"; };
                                              { "vla1-2319.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a368"; };
                                              { "vla1-2330.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7d:0:604:db7:a2b0"; };
                                              { "vla1-2332.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a84b"; };
                                              { "vla1-2345.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:ab9e"; };
                                              { "vla1-2351.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a7ea"; };
                                              { "vla1-2358.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a3d8"; };
                                              { "vla1-2453.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:94:0:604:db7:a94f"; };
                                              { "vla1-2472.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:97:0:604:db7:a909"; };
                                              { "vla1-3780.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:9e7d"; };
                                              { "vla1-4111.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:87:0:604:db7:a820"; };
                                              { "vla1-4272.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a45a"; };
                                              { "vla1-4375.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:a82d"; };
                                              { "vla1-4428.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:8a:0:604:db7:a818"; };
                                              { "vla1-4443.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:86:0:604:db7:9dee"; };
                                              { "vla1-4504.search.yandex.net"; 31815; 60.000; "2a02:6b8:c0e:90:0:604:db7:a7e1"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "100ms";
                                              backend_timeout = "15s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                            }))
                                          }; -- rr
                                        }; -- balancer2
                                      }; -- on_error
                                    }; -- balancer2
                                  }; -- shared
                                }; -- report
                              }; -- images_man
                              images_devnull = {
                                weight = -1.000;
                                report = {
                                  uuid = "images_apphost_requests_to_devnull";
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
                              }; -- images_devnull
                            }; -- rr
                          }; -- balancer2
                        }; -- threshold
                      }; -- report
                    }; -- shared
                  }; -- shared
                }; -- images_apphost
                images_antiadblocker_checks = {
                  priority = 4;
                  match_and = {
                    {
                      match_fsm = {
                        match = "POST.*";
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                    {
                      match_fsm = {
                        header = {
                          name = "x-aab-http-check";
                          value = ".*";
                        }; -- header
                        case_insensitive = true;
                        surround = false;
                      }; -- match_fsm
                    };
                  }; -- match_and
                  shared = {
                    uuid = "images_antiadblocker_checks";
                    balancer2 = {
                      simple_policy = {};
                      attempts = 1;
                      rr = {
                        weights_file = "./controls/images_antiadblock.txt";
                        images_antiadblock = {
                          weight = -1.000;
                          headers = {
                            create = {
                              ["X-AAB-PartnerToken"] = get_str_env_var("AAB_TOKEN");
                              ["X-Forwarded-Proto"] = "https";
                              ["X-Yandex-Service-L7-Port"] = "81";
                              ["Y-Service"] = "images_antiadblock";
                            }; -- create
                            report = {
                              uuid = "images_antiadblock";
                              ranges = get_str_var("default_ranges");
                              just_storage = false;
                              disable_robotness = true;
                              disable_sslness = true;
                              events = {
                                stats = "report";
                              }; -- events
                              balancer2 = {
                                simple_policy = {};
                                attempts = 2;
                                rr = {
                                  unpack(gen_proxy_backends({
                                    { "cryprox.yandex.net"; 80; 1.000; "2a02:6b8::402"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "60ms";
                                    backend_timeout = "30s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- headers
                        }; -- images_antiadblock
                        images_prod = {
                          weight = 1.000;
                          shared = {
                            uuid = "2205371174650516168";
                          }; -- shared
                        }; -- images_prod
                      }; -- rr
                    }; -- balancer2
                  }; -- shared
                }; -- images_antiadblocker_checks
                images_antiadblocker_posts = {
                  priority = 3;
                  match_fsm = {
                    match = "POST.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "2205371174650516168";
                    shared = {
                      uuid = "images_prod";
                    }; -- shared
                  }; -- shared
                }; -- images_antiadblocker_posts
                images_antiadblocker = {
                  priority = 2;
                  match_or = {
                    {
                      match_and = {
                        {
                          match_fsm = {
                            URI = "/(images|gorsel)/_crpd/.*";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                      }; -- match_and
                    };
                    {
                      match_and = {
                        {
                          match_fsm = {
                            URI = "/(images|gorsel)/.*";
                            case_insensitive = true;
                            surround = false;
                          }; -- match_fsm
                        };
                        {
                          match_not = {
                            match_fsm = {
                              URI = "/(images|gorsel)(/(touch|pad))?/";
                              case_insensitive = true;
                              surround = false;
                            }; -- match_fsm
                          }; -- match_not
                        };
                        {
                          match_or = {
                            {
                              match_fsm = {
                                cookie = "yacob=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "bltsr=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "qgZTpupNMGJBM=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "mcBaGDt=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "BgeeyNoBJuyII=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "orrXTfJaS=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "FgkKdCjPqoMFm=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "EIXtkCTlX=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "JPIqApiY=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "KIykI=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "HgGedof=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "ancQTZw=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "involved=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "instruction=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "engineering=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "telecommunications=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "discussion=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "computer=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "substantial=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "specific=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "engineer=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                            {
                              match_fsm = {
                                cookie = "adequate=1";
                                case_insensitive = true;
                                surround = true;
                              }; -- match_fsm
                            };
                          }; -- match_or
                        };
                      }; -- match_and
                    };
                  }; -- match_or
                  shared = {
                    uuid = "images_antiadblocker_checks";
                  }; -- shared
                }; -- images_antiadblocker
                default = {
                  priority = 1;
                  shared = {
                    uuid = "1185741777505834653";
                    shared = {
                      uuid = "images_prod";
                      report = {
                        uuid = "images";
                        ranges = get_str_var("default_ranges");
                        just_storage = false;
                        disable_robotness = true;
                        disable_sslness = true;
                        events = {
                          stats = "report";
                        }; -- events
                        threshold = {
                          lo_bytes = 1048576;
                          hi_bytes = 5242880;
                          recv_timeout = "50ms";
                          pass_timeout = "10s";
                          on_pass_timeout_failure = {
                            errordocument = {
                              status = 413;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- on_pass_timeout_failure
                          geobase = {
                            trusted = false;
                            geo_host = "laas.yandex.ru";
                            take_ip_from = "X-Forwarded-For-Y";
                            laas_answer_header = "X-LaaS-Answered";
                            file_switch = "./controls/disable_geobase.switch";
                            geo_path = "/region?response_format=header&version=1&service=balancer";
                            geo = {
                              shared = {
                                uuid = "6796952198230220343";
                              }; -- shared
                            }; -- geo
                            headers = {
                              create = {
                                ["X-L7-EXP"] = "true";
                              }; -- create
                              exp_getter = {
                                trusted = false;
                                file_switch = "./controls/expgetter.switch";
                                service_name = "images";
                                service_name_header = "Y-Service";
                                exp_headers = "X-Yandex-ExpConfigVersion(-Pre)?|X-Yandex-ExpBoxes(-Pre)?|X-Yandex-ExpFlags(-Pre)?|X-Yandex-LogstatUID|X-Yandex-ExpSplitParams";
                                uaas = {
                                  shared = {
                                    uuid = "8455959467899863695";
                                  }; -- shared
                                }; -- uaas
                                request_replier = {
                                  sink = {
                                    balancer2 = {
                                      simple_policy = {};
                                      attempts = 2;
                                      rr = {
                                        unpack(gen_proxy_backends({
                                          { "sinkadm.priemka.yandex.ru"; 80; 1.000; "2a02:6b8:0:3400::eeee:20"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "10s";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- rr
                                    }; -- balancer2
                                  }; -- sink
                                  enable_failed_requests_replication = false;
                                  rate = 0.000;
                                  rate_file = "./controls/request_replier_images.ratefile";
                                  headers_hasher = {
                                    header_name = "X-Yandex-LogstatUID";
                                    surround = false;
                                    randomize_empty_match = true;
                                    regexp = {
                                      xml = {
                                        priority = 11;
                                        match_fsm = {
                                          URI = "/images-xml/.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "images_xml";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2271549645238535913";
                                            shared = {
                                              uuid = "images_all";
                                            }; -- shared
                                          }; -- shared
                                        }; -- report
                                      }; -- xml
                                      api = {
                                        priority = 10;
                                        match_fsm = {
                                          URI = "/(images|gorsel)/api/.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "images_api";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2271549645238535913";
                                          }; -- shared
                                        }; -- report
                                      }; -- api
                                      touch_search = {
                                        priority = 9;
                                        match_fsm = {
                                          URI = "/(images|gorsel)/touch/search.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "images_touch_search";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2271549645238535913";
                                          }; -- shared
                                        }; -- report
                                      }; -- touch_search
                                      touch = {
                                        priority = 8;
                                        match_fsm = {
                                          URI = "/(images|gorsel)/touch/.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "images_touch";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2271549645238535913";
                                          }; -- shared
                                        }; -- report
                                      }; -- touch
                                      pad_search = {
                                        priority = 7;
                                        match_fsm = {
                                          URI = "/(images|gorsel)/pad/search.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "images_pad_search";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2271549645238535913";
                                          }; -- shared
                                        }; -- report
                                      }; -- pad_search
                                      pad = {
                                        priority = 6;
                                        match_fsm = {
                                          URI = "/(images|gorsel)/pad/.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "images_pad";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2271549645238535913";
                                          }; -- shared
                                        }; -- report
                                      }; -- pad
                                      smart_search = {
                                        priority = 5;
                                        match_fsm = {
                                          URI = "/(images|gorsel)/smart/search.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "images_smart_search";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2271549645238535913";
                                          }; -- shared
                                        }; -- report
                                      }; -- smart_search
                                      smart = {
                                        priority = 4;
                                        match_fsm = {
                                          URI = "/(images|gorsel)/smart/.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "images_smart";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2271549645238535913";
                                          }; -- shared
                                        }; -- report
                                      }; -- smart
                                      desktop_search = {
                                        priority = 3;
                                        match_fsm = {
                                          URI = "/(images|gorsel)/search.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "images_desktop_search";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2271549645238535913";
                                          }; -- shared
                                        }; -- report
                                      }; -- desktop_search
                                      desktop = {
                                        priority = 2;
                                        match_fsm = {
                                          URI = "/(images|gorsel)/.*";
                                          case_insensitive = true;
                                          surround = false;
                                        }; -- match_fsm
                                        report = {
                                          uuid = "images_desktop";
                                          ranges = get_str_var("default_ranges");
                                          just_storage = false;
                                          disable_robotness = true;
                                          disable_sslness = true;
                                          events = {
                                            stats = "report";
                                          }; -- events
                                          shared = {
                                            uuid = "2271549645238535913";
                                          }; -- shared
                                        }; -- report
                                      }; -- desktop
                                      default = {
                                        priority = 1;
                                        shared = {
                                          uuid = "images_all";
                                          balancer2 = {
                                            by_name_from_header_policy = {
                                              allow_zero_weights = true;
                                              strict = true;
                                              hints = {
                                                {
                                                  hint = "man";
                                                  backend = "images_man";
                                                };
                                                {
                                                  hint = "sas";
                                                  backend = "images_sas";
                                                };
                                                {
                                                  hint = "vla";
                                                  backend = "images_vla";
                                                };
                                              }; -- hints
                                              by_hash_policy = {
                                                unique_policy = {};
                                              }; -- by_hash_policy
                                            }; -- by_name_from_header_policy
                                            attempts = 2;
                                            attempts_file = "./controls/images.attempts";
                                            rr = {
                                              weights_file = "./controls/search_l7_balancer_switch.json";
                                              images_vla = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "images_requests_to_vla";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  shared = {
                                                    uuid = "images_vla";
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 2;
                                                      attempts_file = "./controls/attempts.count";
                                                      rr = {
                                                        unpack(gen_proxy_backends({
                                                          { "vla1-0147.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:3f:0:604:db7:a6ca"; };
                                                          { "vla1-0152.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:3f:0:604:db7:a705"; };
                                                          { "vla1-0221.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:81:0:604:db7:a8cf"; };
                                                          { "vla1-0703.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:53:0:604:db7:9d01"; };
                                                          { "vla1-0951.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:71:0:604:db7:a3de"; };
                                                          { "vla1-1128.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:45:0:604:db7:a77d"; };
                                                          { "vla1-1161.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:8d:0:604:db7:ab54"; };
                                                          { "vla1-1207.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:9a:0:604:db7:aa9b"; };
                                                          { "vla1-1239.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:93:0:604:db7:a751"; };
                                                          { "vla1-1257.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:81:0:604:db7:a7e7"; };
                                                          { "vla1-1278.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:4d:0:604:db7:a440"; };
                                                          { "vla1-1294.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:1b:0:604:db7:9b7e"; };
                                                          { "vla1-1304.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:4c:0:604:db7:a0ce"; };
                                                          { "vla1-1315.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:67:0:604:db7:a325"; };
                                                          { "vla1-1330.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:78:0:604:db7:a9c3"; };
                                                          { "vla1-1358.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:45:0:604:db7:a764"; };
                                                          { "vla1-1384.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:44:0:604:db7:9f36"; };
                                                          { "vla1-1395.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:84:0:604:db7:abfb"; };
                                                          { "vla1-1464.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:48:0:604:db7:a695"; };
                                                          { "vla1-1516.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:43:0:604:db7:a110"; };
                                                          { "vla1-1518.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:3c:0:604:db7:9eb8"; };
                                                          { "vla1-1598.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:13:0:604:db7:9a64"; };
                                                          { "vla1-1710.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:71:0:604:db7:a416"; };
                                                          { "vla1-1757.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:88:0:604:db7:a771"; };
                                                          { "vla1-1815.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:57:0:604:db7:a38f"; };
                                                          { "vla1-1911.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:45:0:604:db7:a5fd"; };
                                                          { "vla1-1930.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:44:0:604:db7:a6cc"; };
                                                          { "vla1-1987.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:82:0:604:db7:a872"; };
                                                          { "vla1-2016.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:91:0:604:db7:ab5e"; };
                                                          { "vla1-2017.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:34:0:604:db7:9d11"; };
                                                          { "vla1-2078.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:53:0:604:db7:9d02"; };
                                                          { "vla1-2092.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:17:0:604:db7:a7f5"; };
                                                          { "vla1-2121.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:98:0:604:db7:a008"; };
                                                          { "vla1-2122.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:53:0:604:db7:9d07"; };
                                                          { "vla1-2143.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:27:0:604:db7:9f6f"; };
                                                          { "vla1-2163.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:53:0:604:db7:9cfa"; };
                                                          { "vla1-2315.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:6e:0:604:db7:a371"; };
                                                          { "vla1-2319.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:7f:0:604:db7:a368"; };
                                                          { "vla1-2330.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:7d:0:604:db7:a2b0"; };
                                                          { "vla1-2332.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:87:0:604:db7:a84b"; };
                                                          { "vla1-2345.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:86:0:604:db7:ab9e"; };
                                                          { "vla1-2351.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:87:0:604:db7:a7ea"; };
                                                          { "vla1-2358.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:7f:0:604:db7:a3d8"; };
                                                          { "vla1-2453.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:94:0:604:db7:a94f"; };
                                                          { "vla1-2472.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:97:0:604:db7:a909"; };
                                                          { "vla1-3780.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:7f:0:604:db7:9e7d"; };
                                                          { "vla1-4111.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:87:0:604:db7:a820"; };
                                                          { "vla1-4375.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:86:0:604:db7:a82d"; };
                                                          { "vla1-4428.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:8a:0:604:db7:a818"; };
                                                          { "vla1-4443.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:86:0:604:db7:9dee"; };
                                                          { "vla1-4504.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:90:0:604:db7:a7e1"; };
                                                          { "vla2-0560.search.yandex.net"; 8080; 495.000; "2a02:6b8:c0e:9c:0:604:db7:aa6e"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "100ms";
                                                          backend_timeout = "30s";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- rr
                                                    }; -- balancer2
                                                  }; -- shared
                                                }; -- report
                                              }; -- images_vla
                                              images_sas = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "images_requests_to_sas";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  shared = {
                                                    uuid = "images_sas";
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 2;
                                                      attempts_file = "./controls/attempts.count";
                                                      rr = {
                                                        unpack(gen_proxy_backends({
                                                          { "sas1-5973.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:100:225:90ff:fee3:9cf2"; };
                                                          { "sas1-6349.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:69f:215:b2ff:fea7:7bc4"; };
                                                          { "sas1-6351.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:11a:215:b2ff:fea7:8280"; };
                                                          { "sas1-6752.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:6a4:215:b2ff:fea7:9008"; };
                                                          { "sas1-6893.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:11d:215:b2ff:fea7:9060"; };
                                                          { "sas1-6939.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:12f:215:b2ff:fea7:b324"; };
                                                          { "sas1-6978.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:123:215:b2ff:fea7:b720"; };
                                                          { "sas1-7095.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:124:215:b2ff:fea7:ab98"; };
                                                          { "sas1-7098.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:124:215:b2ff:fea7:8ee4"; };
                                                          { "sas1-7125.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:907c"; };
                                                          { "sas1-7155.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:122:215:b2ff:fea7:8bf0"; };
                                                          { "sas1-7156.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aae4"; };
                                                          { "sas1-7238.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:121:215:b2ff:fea7:ac10"; };
                                                          { "sas1-7272.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:121:215:b2ff:fea7:8300"; };
                                                          { "sas1-7286.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:128:215:b2ff:fea7:7aec"; };
                                                          { "sas1-7287.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:128:215:b2ff:fea7:7910"; };
                                                          { "sas1-7326.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:126:215:b2ff:fea7:ad8c"; };
                                                          { "sas1-7330.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:628:215:b2ff:fea7:90d0"; };
                                                          { "sas1-7331.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc9c"; };
                                                          { "sas1-7459.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8cd0"; };
                                                          { "sas1-7494.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:125:215:b2ff:fea7:6590"; };
                                                          { "sas1-7498.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:125:215:b2ff:fea7:aaa4"; };
                                                          { "sas1-7686.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bf0c"; };
                                                          { "sas1-7825.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:1a4:215:b2ff:fea7:7fbc"; };
                                                          { "sas1-7843.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:1a4:215:b2ff:fea7:82f0"; };
                                                          { "sas1-7854.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a6:0:604:7a1d:6382"; };
                                                          { "sas1-7855.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a6:0:604:7a1d:5ed0"; };
                                                          { "sas1-7857.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a6:0:604:7a1d:630a"; };
                                                          { "sas1-7858.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a6:0:604:7a1d:5d0c"; };
                                                          { "sas1-7878.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:12e:215:b2ff:fea7:7db8"; };
                                                          { "sas1-7917.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a6:0:604:7a52:c938"; };
                                                          { "sas1-7922.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a6:0:604:7a51:503a"; };
                                                          { "sas1-7923.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a6:0:604:7a52:ccda"; };
                                                          { "sas1-7929.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:12b:215:b2ff:fea7:ac24"; };
                                                          { "sas1-7946.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:789:0:604:1e9:e532"; };
                                                          { "sas1-7948.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:767:0:604:35c6:3276"; };
                                                          { "sas1-7949.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:766:0:604:35c6:3412"; };
                                                          { "sas1-7950.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a9:0:604:1e9:dcbe"; };
                                                          { "sas1-7951.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:769:0:604:35c6:2e80"; };
                                                          { "sas1-7953.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:769:0:604:35c6:32e4"; };
                                                          { "sas1-7954.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:767:0:604:35c6:311a"; };
                                                          { "sas1-7955.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:767:0:604:35c6:34ca"; };
                                                          { "sas1-7956.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a9:0:604:1e9:e762"; };
                                                          { "sas1-7958.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a9:0:604:1e9:de2e"; };
                                                          { "sas1-7959.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:769:0:604:35c6:32c4"; };
                                                          { "sas1-7960.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:768:0:604:35c6:304a"; };
                                                          { "sas1-7962.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:769:0:604:35c6:2a8c"; };
                                                          { "sas1-7963.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:766:0:604:35c6:3580"; };
                                                          { "sas1-8187.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:68f:215:b2ff:fea7:8b68"; };
                                                          { "sas1-8255.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:6a6:215:b2ff:fea7:b1d0"; };
                                                          { "sas1-8873.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:138:215:b2ff:fea7:ba80"; };
                                                          { "sas1-8979.search.yandex.net"; 8080; 313.904; "2a02:6b8:b000:13d:feaa:14ff:fede:3f9e"; };
                                                          { "sas1-9164.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:143:feaa:14ff:fede:417c"; };
                                                          { "sas1-9283.search.yandex.net"; 8080; 338.000; "2a02:6b8:b000:143:feaa:14ff:fede:3eb8"; };
                                                          { "sas1-9452.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:dc86"; };
                                                          { "sas1-9467.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:ddfe"; };
                                                          { "sas1-9468.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a8:0:604:1e9:e16a"; };
                                                          { "sas1-9778.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a8:0:604:1e9:df96"; };
                                                          { "sas2-2081.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:e4c6"; };
                                                          { "sas2-5021.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:500:0:604:9088:b8a6"; };
                                                          { "sas2-7093.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7c4:0:604:90e4:b418"; };
                                                          { "sas2-7097.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7c4:0:604:90e4:b61c"; };
                                                          { "sas2-7104.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7c4:0:604:90e4:cb0c"; };
                                                          { "sas2-7113.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:5bb:0:604:3564:4ddd"; };
                                                          { "sas2-7120.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:555:0:604:90e5:4420"; };
                                                          { "sas2-7130.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:52d:0:604:90e4:cfaa"; };
                                                          { "sas2-7139.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:5bb:0:604:35c6:3038"; };
                                                          { "sas2-7142.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:539:0:604:354b:5dcd"; };
                                                          { "sas2-8199.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:778:0:604:7a18:10d0"; };
                                                          { "sas2-8200.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:77f:0:604:7a1d:5f88"; };
                                                          { "sas2-8201.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:778:0:604:7a51:501c"; };
                                                          { "sas2-8202.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:5d5:0:604:7a18:f16"; };
                                                          { "sas2-8203.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:780:0:604:7a51:554c"; };
                                                          { "sas2-8204.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:77d:0:604:7a51:515a"; };
                                                          { "sas2-8205.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:77e:0:604:7a51:5012"; };
                                                          { "sas2-8206.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:785:0:604:7a18:e44"; };
                                                          { "sas2-8207.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:780:0:604:7a51:5596"; };
                                                          { "sas2-8208.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:777:0:604:7a52:cb38"; };
                                                          { "sas2-8209.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7bc:0:604:7a1d:5c46"; };
                                                          { "sas2-8210.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:5d5:0:604:7a1d:5edc"; };
                                                          { "sas2-8211.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:780:0:604:7a51:55d8"; };
                                                          { "sas2-8212.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:778:0:604:90eb:915e"; };
                                                          { "sas2-8213.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:77d:0:604:7a51:52a6"; };
                                                          { "sas2-8214.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7bc:0:604:7a51:5594"; };
                                                          { "sas2-8216.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7bc:0:604:7a51:539e"; };
                                                          { "sas2-8911.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7d2:0:604:14db:76b0"; };
                                                          { "sas2-9039.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b14:0:604:141d:f5f8"; };
                                                          { "sas2-9713.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b39:0:604:3564:5191"; };
                                                          { "sas2-9714.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b39:0:604:3564:5301"; };
                                                          { "sas3-0468.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7d3:0:604:1e9:dffe"; };
                                                          { "sas3-0479.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b13:0:604:1e9:dc72"; };
                                                          { "sas3-0480.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:e42a"; };
                                                          { "sas3-0482.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7d3:0:604:1e9:e7b6"; };
                                                          { "sas3-0497.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b13:0:604:1e9:dbbe"; };
                                                          { "sas3-0503.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b13:0:604:1e9:dc3e"; };
                                                          { "sas3-0516.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:e0d6"; };
                                                          { "sas3-0524.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:e5da"; };
                                                          { "sas3-0536.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:dcfe"; };
                                                          { "sas3-0542.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:dede"; };
                                                          { "sas3-0546.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7a8:0:604:1e9:e142"; };
                                                          { "sas3-0558.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:def6"; };
                                                          { "sas3-0559.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7d3:0:604:1e9:e4ee"; };
                                                          { "sas3-0561.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7d3:0:604:1e9:e286"; };
                                                          { "sas3-0569.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b13:0:604:1e9:e7f6"; };
                                                          { "sas3-0572.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:db7e"; };
                                                          { "sas3-0580.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b16:0:604:1e9:dc3a"; };
                                                          { "sas3-0586.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b13:0:604:1e9:e456"; };
                                                          { "sas3-0593.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b13:0:604:1e9:de06"; };
                                                          { "sas3-0600.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7d3:0:604:1e9:de52"; };
                                                          { "sas3-0611.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:b13:0:604:1e9:e0aa"; };
                                                          { "sas3-0612.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7d3:0:604:1e9:dd12"; };
                                                          { "sas3-0616.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7aa:0:604:1e9:dcce"; };
                                                          { "sas3-1118.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:7d3:0:604:1e9:e35e"; };
                                                          { "sas3-2031.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:77e:0:604:90cb:d2a"; };
                                                          { "sas3-2038.search.yandex.net"; 8080; 338.000; "2a02:6b8:c02:77e:0:604:7a18:a14"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "100ms";
                                                          backend_timeout = "30s";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- rr
                                                    }; -- balancer2
                                                  }; -- shared
                                                }; -- report
                                              }; -- images_sas
                                              images_man = {
                                                weight = 1.000;
                                                report = {
                                                  uuid = "images_requests_to_man";
                                                  ranges = get_str_var("default_ranges");
                                                  just_storage = false;
                                                  disable_robotness = true;
                                                  disable_sslness = true;
                                                  events = {
                                                    stats = "report";
                                                  }; -- events
                                                  shared = {
                                                    uuid = "images_man";
                                                    balancer2 = {
                                                      unique_policy = {};
                                                      attempts = 2;
                                                      attempts_file = "./controls/attempts.count";
                                                      rr = {
                                                        unpack(gen_proxy_backends({
                                                          { "man1-1076.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f1f2"; };
                                                          { "man1-1150.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f2ca"; };
                                                          { "man1-1885.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1d50"; };
                                                          { "man1-1957.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:16f0"; };
                                                          { "man1-1979.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:550"; };
                                                          { "man1-2023.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1640"; };
                                                          { "man1-2087.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e320"; };
                                                          { "man1-2092.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6098:f652:14ff:fe8c:1e0"; };
                                                          { "man1-2106.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:720"; };
                                                          { "man1-2873.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e840"; };
                                                          { "man1-2943.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dc40"; };
                                                          { "man1-3252.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1a10"; };
                                                          { "man1-3260.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c700"; };
                                                          { "man1-3261.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c6d0"; };
                                                          { "man1-3265.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:d4c0"; };
                                                          { "man1-3752.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:b9d0"; };
                                                          { "man1-3822.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7eaa"; };
                                                          { "man1-3904.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7866"; };
                                                          { "man1-3959.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:603e:92e2:baff:fe6f:80a2"; };
                                                          { "man1-4025.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:603c:92e2:baff:fe6e:ba8c"; };
                                                          { "man1-4311.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7dfc"; };
                                                          { "man1-6102.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4ed0"; };
                                                          { "man1-6227.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6057:f652:14ff:fef5:c8b0"; };
                                                          { "man1-6242.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6057:f652:14ff:fef5:cb40"; };
                                                          { "man1-6263.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5370"; };
                                                          { "man1-6763.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6058:e61d:2dff:fe04:50d0"; };
                                                          { "man1-7132.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:89b:0:604:ba55:f742"; };
                                                          { "man1-7430.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:899:0:604:2d04:49d0"; };
                                                          { "man1-8883.search.yandex.net"; 8080; 352.000; "2a02:6b8:b000:6064:e61d:2dff:fe03:41c0"; };
                                                          { "man2-4942.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:634:0:604:2d6d:4000"; };
                                                          { "man2-4948.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:63a:0:604:2d6d:2d50"; };
                                                          { "man2-4959.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:636:0:604:baa1:7888"; };
                                                          { "man2-4964.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:634:0:604:2d6d:15e0"; };
                                                          { "man2-5025.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:637:0:604:baa1:73ac"; };
                                                          { "man2-5159.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:636:0:604:baa1:7802"; };
                                                          { "man2-5408.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:637:0:604:baa1:797c"; };
                                                          { "man2-5427.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:636:0:604:baa1:7ec6"; };
                                                          { "man2-5476.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:636:0:604:baa1:76a4"; };
                                                          { "man2-5522.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:637:0:604:baa1:7358"; };
                                                          { "man2-5676.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:636:0:604:baa1:7fcc"; };
                                                          { "man2-5681.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:638:0:604:2d6d:2c20"; };
                                                          { "man2-5691.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:65b:0:604:2d6d:3fe0"; };
                                                          { "man2-5694.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:663:0:604:2d6d:3900"; };
                                                          { "man2-5708.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:65a:0:604:2d6d:1120"; };
                                                          { "man2-5710.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:65b:0:604:2d6d:3a50"; };
                                                          { "man2-5725.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:638:0:604:2d6d:bf0"; };
                                                          { "man2-5743.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:663:0:604:2d6d:3040"; };
                                                          { "man2-5747.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:635:0:604:2d6d:740"; };
                                                          { "man2-5752.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:65b:0:604:2d6d:1440"; };
                                                          { "man2-5934.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:635:0:604:2d6d:3a40"; };
                                                          { "man2-5951.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:637:0:604:baa1:7306"; };
                                                          { "man2-5954.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:663:0:604:2d6d:3500"; };
                                                          { "man2-5967.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:63a:0:604:2d6d:3c50"; };
                                                          { "man2-6000.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:65a:0:604:2d6d:2b90"; };
                                                          { "man2-6122.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:63f:0:604:14a9:68ea"; };
                                                          { "man2-6155.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:633:0:604:14a7:ba50"; };
                                                          { "man2-6244.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:830:0:604:1465:cd69"; };
                                                          { "man2-6324.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:63e:0:604:14a7:ba0b"; };
                                                          { "man2-6363.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:657:0:604:1465:cd75"; };
                                                          { "man2-6369.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:63e:0:604:14a7:b9f2"; };
                                                          { "man2-6376.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:648:0:604:1465:cd6a"; };
                                                          { "man2-6416.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:641:0:604:14a7:6812"; };
                                                          { "man2-6468.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:633:0:604:14a7:ba87"; };
                                                          { "man2-6505.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:657:0:604:14a7:67d9"; };
                                                          { "man2-6596.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:647:0:604:14a7:bb87"; };
                                                          { "man2-6624.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:643:0:604:14a7:bb79"; };
                                                          { "man2-6636.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:630:0:604:14a7:b9d3"; };
                                                          { "man2-6665.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:649:0:604:14a7:bb33"; };
                                                          { "man2-6736.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:647:0:604:14a7:bb94"; };
                                                          { "man2-6759.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:653:0:604:14a9:6873"; };
                                                          { "man2-6779.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:836:0:604:14a7:6647"; };
                                                          { "man2-6831.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:645:0:604:14a9:6944"; };
                                                          { "man2-6834.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:644:0:604:14a7:bcd8"; };
                                                          { "man2-6859.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:645:0:604:14a9:693f"; };
                                                          { "man2-6870.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:643:0:604:14a7:bb0d"; };
                                                          { "man2-6886.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:83c:0:604:14a7:bd08"; };
                                                          { "man2-6890.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:643:0:604:14a7:ba9f"; };
                                                          { "man2-6916.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:642:0:604:14a7:badb"; };
                                                          { "man2-6921.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:63b:0:604:14a7:ba19"; };
                                                          { "man2-7070.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:83d:0:604:14a7:6754"; };
                                                          { "man2-7099.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:644:0:604:14a9:69a0"; };
                                                          { "man2-7179.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:655:0:604:14a7:ba6b"; };
                                                          { "man2-7189.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:651:0:604:14a9:6a51"; };
                                                          { "man2-7221.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:647:0:604:14a7:bb81"; };
                                                          { "man2-7241.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:651:0:604:14a9:69d8"; };
                                                          { "man2-7261.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:655:0:604:14a9:687b"; };
                                                          { "man2-7327.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:64b:0:604:14a7:b99d"; };
                                                          { "man2-7334.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:83c:0:604:14a7:b98e"; };
                                                          { "man2-7342.search.yandex.net"; 8080; 352.000; "2a02:6b8:c01:653:0:604:14a9:69a1"; };
                                                        }, {
                                                          resolve_timeout = "10ms";
                                                          connect_timeout = "100ms";
                                                          backend_timeout = "30s";
                                                          fail_on_5xx = true;
                                                          http_backend = true;
                                                          buffering = false;
                                                          keepalive_count = 0;
                                                          need_resolve = true;
                                                        }))
                                                      }; -- rr
                                                    }; -- balancer2
                                                  }; -- shared
                                                }; -- report
                                              }; -- images_man
                                              images_pumpkin = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "images_requests_to_pumpkin";
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
                                                      unpack(gen_proxy_backends({
                                                        { "lite01h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b02:922b:34ff:fecf:236e"; };
                                                        { "lite01i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6100:92e2:baff:fe56:e910"; };
                                                        { "lite02h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:172:922b:34ff:fecf:2c28"; };
                                                        { "lite02i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:f880"; };
                                                        { "lite03h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b04:922b:34ff:fecf:2b68"; };
                                                        { "lite03i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6105:e61d:2dff:fe00:8cf0"; };
                                                        { "lite04h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b0f:922b:34ff:fecf:2aee"; };
                                                        { "lite04i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6104:e61d:2dff:fe01:fb60"; };
                                                        { "lite05h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b06:922b:34ff:fecf:28b0"; };
                                                        { "lite05i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6107:f652:14ff:fef5:d4d0"; };
                                                        { "lite06h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b10:922b:34ff:fecf:3b22"; };
                                                        { "lite06i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6109:f652:14ff:fef5:d5c0"; };
                                                        { "lite07h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b10:922b:34ff:fecf:3aca"; };
                                                        { "lite08h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b02:922b:34ff:fecf:27dd"; };
                                                        { "lite08i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6101:e61d:2dff:fe03:36c0"; };
                                                        { "lite09h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:172:922b:34ff:fecf:3c32"; };
                                                        { "lite09i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6106:f652:14ff:fe74:3930"; };
                                                        { "lite10h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b06:922b:34ff:fecf:2bd0"; };
                                                        { "lite10i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6103:f652:14ff:fef5:c840"; };
                                                        { "lite11h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b09:922b:34ff:fecc:7a66"; };
                                                        { "lite11i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:610b:f652:14ff:fe74:3c00"; };
                                                        { "lite12h.search.yandex.net"; 80; 1130.000; "2a02:6b8:b000:b11:922b:34ff:fecf:3338"; };
                                                        { "lite12i.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:6108:e61d:2dff:fe03:3740"; };
                                                        { "man1-3946.search.yandex.net"; 80; 1293.000; "2a02:6b8:b000:610f:92e2:baff:fea1:7862"; };
                                                        { "vla1-4617.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:70:0:614:db7:a43c"; };
                                                        { "vla1-4637.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:9e:0:614:db7:a15b"; };
                                                        { "vla1-4658.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:84:0:614:db7:a02a"; };
                                                        { "vla1-4659.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:83:0:614:db7:a024"; };
                                                        { "vla1-4665.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:93:0:614:db7:a854"; };
                                                        { "vla1-4668.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:50:0:614:db7:a476"; };
                                                        { "vla1-4670.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:4d:0:614:db7:a46e"; };
                                                        { "vla1-4672.search.yandex.net"; 80; 2042.000; "2a02:6b8:c0e:4c:0:614:db7:a474"; };
                                                      }, {
                                                        resolve_timeout = "10ms";
                                                        connect_timeout = "100ms";
                                                        backend_timeout = "10s";
                                                        fail_on_5xx = true;
                                                        http_backend = true;
                                                        buffering = false;
                                                        keepalive_count = 0;
                                                        need_resolve = true;
                                                      }))
                                                    }; -- rr
                                                  }; -- balancer2
                                                }; -- report
                                              }; -- images_pumpkin
                                              images_devnull = {
                                                weight = -1.000;
                                                report = {
                                                  uuid = "images_requests_to_devnull";
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
                                              }; -- images_devnull
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- shared
                                      }; -- default
                                    }; -- regexp
                                  }; -- headers_hasher
                                }; -- request_replier
                              }; -- exp_getter
                            }; -- headers
                          }; -- geobase
                        }; -- threshold
                      }; -- report
                    }; -- shared
                  }; -- shared
                }; -- default
              }; -- regexp
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
    http_no_cryprox_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        81;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 81, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 81, "/place/db/www/logs");
            report = {
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              regexp = {
                ["awacs-balancer-health-check"] = {
                  priority = 4;
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
                images_apphost_with_ab = {
                  priority = 3;
                  match_fsm = {
                    URI = "/images-apphost/alice";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "6726893485926116582";
                  }; -- shared
                }; -- images_apphost_with_ab
                images_apphost = {
                  priority = 2;
                  match_fsm = {
                    URI = "/images-apphost/.*";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  shared = {
                    uuid = "6013407083076926241";
                  }; -- shared
                }; -- images_apphost
                default = {
                  priority = 1;
                  shared = {
                    uuid = "1185741777505834653";
                  }; -- shared
                }; -- default
              }; -- regexp
              refers = "service_total";
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_no_cryprox_section
  }; -- ipdispatch
}