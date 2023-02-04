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
  tcp_listen_queue = 128;
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
  addrs = {
    {
      ip = "127.0.0.4";
      port = get_port_var("port");
    };
    {
      ip = "*";
      port = 80;
      disabled = get_int_var("disable_external", 0);
    };
  }; -- addrs
  sd = {
    client_name = "awacs-l7-balancer(namespace-id:balancer)";
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
    stats_storage = {
      ips = {
        "127.0.0.4";
      }; -- ips
      ports = {
        get_port_var("port");
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
            status = 204;
            force_conn_close = false;
          }; -- errordocument
        }; -- http
      }; -- report
    }; -- stats_storage
    http_section = {
      ips = {
        "*";
      }; -- ips
      ports = {
        80;
      }; -- ports
      errorlog = {
        log_level = "ERROR";
        log = get_log_path("error_log", 80, "/place/db/www/logs");
        http = {
          maxlen = 65536;
          maxreq = 65536;
          keepalive = true;
          no_keepalive_file = "./controls/keepalive_disabled";
          events = {
            stats = "report";
          }; -- events
          accesslog = {
            log = get_log_path("access_log", 80, "/place/db/www/logs");
            report = {
              uuid = "http";
              refers = "service_total";
              ranges = get_str_var("default_ranges");
              just_storage = false;
              disable_robotness = true;
              disable_sslness = true;
              events = {
                stats = "report";
              }; -- events
              regexp = {
                ["awacs-balancer-health-check"] = {
                  priority = 5;
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
                  priority = 4;
                  match_fsm = {
                    url = "/ping";
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
                }; -- slbping
                ["compression-w-0-0-1"] = {
                  priority = 3;
                  match_fsm = {
                    host = "compression-w-0-0-1\\.yandex\\.com.tr(:\\d+)?";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  compressor = {
                    enable_compression = true;
                    enable_decompression = false;
                    compression_codecs = "br";
                    errordocument = {
                      status = 404;
                      content = "Unknown host";
                      force_conn_close = false;
                    }; -- errordocument
                  }; -- compressor
                }; -- ["compression-w-0-0-1"]
                with_codecs = {
                  priority = 2;
                  match_fsm = {
                    host = "with-codecs\\.yandex\\.com.tr(:\\d+)?";
                    case_insensitive = true;
                    surround = false;
                  }; -- match_fsm
                  compressor = {
                    enable_compression = true;
                    enable_decompression = false;
                    compression_codecs = "x-deflate,br";
                    errordocument = {
                      status = 404;
                      content = "Unknown host";
                      force_conn_close = false;
                    }; -- errordocument
                  }; -- compressor
                }; -- with_codecs
                default = {
                  priority = 1;
                  compressor = {
                    enable_compression = true;
                    enable_decompression = false;
                    compression_codecs = "gzip,deflate,br,x-gzip,x-deflate";
                    errordocument = {
                      status = 404;
                      content = "Unknown host";
                      force_conn_close = false;
                    }; -- errordocument
                  }; -- compressor
                }; -- default
              }; -- regexp
            }; -- report
          }; -- accesslog
        }; -- http
      }; -- errorlog
    }; -- http_section
  }; -- ipdispatch
}