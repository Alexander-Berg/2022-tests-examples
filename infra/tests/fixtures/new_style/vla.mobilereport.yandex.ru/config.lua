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
  maxconn = 5000;
  buffer = 1048576;
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
  log = get_log_path("childs_log", 15860, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 15860;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15860;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 80;
      ip = "2a02:6b8:0:3400::1:2";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "213.180.205.2";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15860;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15860;
      ip = get_ip_by_iproute("v6");
    };
    {
      port = 15860;
      ip = "127.0.0.44";
    };
  }; -- addrs
  ipdispatch = {
    admin = {
      ips = {
        "127.0.0.1";
        "::1";
      }; -- ips
      ports = {
        15860;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        admin = {};
      }; -- http
    }; -- admin
    http_section_80 = {
      ips = {
        "2a02:6b8:0:3400::1:2";
        "213.180.205.2";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "5509186961134843054";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15860, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 15860, "/place/db/www/logs");
              report = {
                uuid = "service_total";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                headers = {
                  create_func = {
                    ["X-Req-Id"] = "reqid";
                    ["X-Source-Port-Y"] = "realport";
                    ["X-Start-Time"] = "starttime";
                    ["X-Yandex-RandomUID"] = "yuid";
                  }; -- create_func
                  create_func_weak = {
                    ["X-Forwarded-For"] = "realip";
                    ["X-Forwarded-For-Y"] = "realip";
                  }; -- create_func_weak
                  response_headers = {
                    create_weak = {
                      ["X-Content-Type-Options"] = "nosniff";
                      ["X-XSS-Protection"] = "1; mode=block";
                    }; -- create_weak
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
                      ext_slbping = {
                        priority = 4;
                        match_fsm = {
                          url = "/admin/info";
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
                      }; -- ext_slbping
                      ext_captcha = {
                        priority = 3;
                        match_fsm = {
                          URI = "/x?(show|check)?captcha.*";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        report = {
                          uuid = "captchasearch";
                          ranges = get_str_var("default_ranges");
                          just_storage = false;
                          disable_robotness = true;
                          disable_sslness = true;
                          events = {
                            stats = "report";
                          }; -- events
                          h100 = {
                            cutter = {
                              bytes = 512;
                              timeout = "0.1s";
                              antirobot = {
                                cut_request = true;
                                no_cut_request_file = "./controls/no_cut_request_file";
                                file_switch = "./controls/do.not.use.it";
                                cut_request_bytes = 512;
                                checker = {
                                  balancer2 = {
                                    by_name_policy = {
                                      name = get_geo("antirobot_", "random");
                                      simple_policy = {};
                                    }; -- by_name_policy
                                    attempts = 1;
                                    rr = {
                                      antirobot_man = {
                                        weight = 1.000;
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 2;
                                          weighted2 = {
                                            slow_reply_time = "1s";
                                            correction_params = {
                                              max_weight = 5.000;
                                              min_weight = 0.050;
                                              history_time = "100s";
                                              feedback_time = "300s";
                                              plus_diff_per_sec = 0.050;
                                              minus_diff_per_sec = 0.100;
                                            }; -- correction_params
                                            unpack(gen_proxy_backends({
                                              { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                              { "man1-0313.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                              { "man1-0401.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f06"; };
                                              { "man1-0510.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                              { "man1-0619.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                              { "man1-0673.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602e:92e2:baff:fe6e:b630"; };
                                              { "man1-0679.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6003:92e2:baff:fe74:7bbe"; };
                                              { "man1-0694.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                              { "man1-0805.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602c:92e2:baff:fe6e:bd34"; };
                                              { "man1-0877.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f1ea"; };
                                              { "man1-3249.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6000:e61d:2dff:fe6d:bb30"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "30ms";
                                              backend_timeout = "10s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                      }; -- antirobot_man
                                      antirobot_sas = {
                                        weight = 1.000;
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 2;
                                          weighted2 = {
                                            slow_reply_time = "1s";
                                            correction_params = {
                                              max_weight = 5.000;
                                              min_weight = 0.050;
                                              history_time = "100s";
                                              feedback_time = "300s";
                                              plus_diff_per_sec = 0.050;
                                              minus_diff_per_sec = 0.100;
                                            }; -- correction_params
                                            unpack(gen_proxy_backends({
                                              { "sas1-0670.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:154:225:90ff:fe83:1800"; };
                                              { "sas1-0980.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:153:225:90ff:fe83:aa2"; };
                                              { "sas1-2218.search.yandex.net"; 13512; 343.000; "2a02:6b8:b000:66a:225:90ff:fe94:1792"; };
                                              { "sas2-4686.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:665:225:90ff:fe92:894a"; };
                                              { "sas2-8870.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:5cc:0:604:9092:8666"; };
                                              { "sas2-8992.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:792:0:604:9094:2fb4"; };
                                              { "sas2-8993.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:781:0:604:9094:1530"; };
                                              { "sas2-9021.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b11:0:604:90c2:a40a"; };
                                              { "sas2-9033.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:783:0:604:9094:138e"; };
                                              { "sas2-9036.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:781:0:604:9094:1748"; };
                                              { "sas2-9189.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bb94"; };
                                              { "sas2-9190.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:3436:7284"; };
                                              { "sas2-9191.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2ce6"; };
                                              { "sas2-9192.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:3fe4"; };
                                              { "sas2-9193.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2436"; };
                                              { "sas2-9194.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:35f8"; };
                                              { "sas2-9195.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bdea"; };
                                              { "sas2-9196.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:badc"; };
                                              { "sas2-9197.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bf90"; };
                                              { "sas2-9198.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:e87e"; };
                                              { "sas2-9199.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:e962"; };
                                              { "sas2-9200.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:ea60"; };
                                              { "sas2-9201.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:354b:5cef"; };
                                              { "sas2-9202.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bef0"; };
                                              { "sas2-9203.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dd16"; };
                                              { "sas2-9204.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:b96e"; };
                                              { "sas2-9205.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:354b:5b07"; };
                                              { "sas2-9206.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:bc22"; };
                                              { "sas2-9207.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2730"; };
                                              { "sas2-9208.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:ea16"; };
                                              { "sas2-9209.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2238"; };
                                              { "sas2-9210.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:d9e4"; };
                                              { "sas2-9211.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dd5c"; };
                                              { "sas2-9212.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2f8c"; };
                                              { "sas2-9213.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bf66"; };
                                              { "sas2-9214.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34c8:caf6"; };
                                              { "sas2-9215.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808e:7a1f"; };
                                              { "sas2-9216.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:354b:5b3f"; };
                                              { "sas2-9217.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:b90a"; };
                                              { "sas2-9218.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2df2"; };
                                              { "sas2-9219.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808e:7cce"; };
                                              { "sas2-9220.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2dca"; };
                                              { "sas2-9221.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:808e:7c06"; };
                                              { "sas2-9222.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:c018"; };
                                              { "sas2-9223.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:3c3e"; };
                                              { "sas2-9224.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:d98c"; };
                                              { "sas2-9225.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2b10"; };
                                              { "sas2-9226.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:c006"; };
                                              { "sas2-9227.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34bb:baf8"; };
                                              { "sas2-9228.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:325e"; };
                                              { "sas2-9229.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:b93e"; };
                                              { "sas2-9230.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2454"; };
                                              { "sas2-9231.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bb86"; };
                                              { "sas2-9232.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2cfc"; };
                                              { "sas2-9233.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34bb:bb4e"; };
                                              { "sas2-9234.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:20ca"; };
                                              { "sas2-9235.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:e562"; };
                                              { "sas2-9236.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:31e2"; };
                                              { "sas2-9237.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bf4c"; };
                                              { "sas2-9238.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:d902"; };
                                              { "sas2-9239.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dc34"; };
                                              { "sas2-9240.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:d7a8"; };
                                              { "sas2-9241.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:c0ce"; };
                                              { "sas2-9242.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:ea3a"; };
                                              { "sas2-9243.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:c016"; };
                                              { "sas2-9244.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:bf48"; };
                                              { "sas2-9245.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:8089:7456"; };
                                              { "sas2-9246.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:e4ea"; };
                                              { "sas2-9247.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:e964"; };
                                              { "sas2-9411.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b34:0:604:9083:1856"; };
                                              { "sas2-9412.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b34:0:604:9083:17e4"; };
                                              { "sas2-9528.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:726:0:604:90c1:d270"; };
                                              { "sas2-9530.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:530:0:604:34cf:2336"; };
                                              { "sas2-9532.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:7d8:0:604:90c6:2eac"; };
                                              { "sas2-9533.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:7d8:0:604:90c6:2c94"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "30ms";
                                              backend_timeout = "10s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                      }; -- antirobot_sas
                                      antirobot_vla = {
                                        weight = 1.000;
                                        balancer2 = {
                                          unique_policy = {};
                                          attempts = 2;
                                          weighted2 = {
                                            slow_reply_time = "1s";
                                            correction_params = {
                                              max_weight = 5.000;
                                              min_weight = 0.050;
                                              history_time = "100s";
                                              feedback_time = "300s";
                                              plus_diff_per_sec = 0.050;
                                              minus_diff_per_sec = 0.100;
                                            }; -- correction_params
                                            unpack(gen_proxy_backends({
                                              { "vla1-1343.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:44:0:604:db7:a0b2"; };
                                              { "vla1-1797.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:45:0:604:db7:a64b"; };
                                              { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                              { "vla1-3568.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:a1:0:604:db7:a2db"; };
                                              { "vla1-3679.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:87:0:604:db7:ab81"; };
                                              { "vla1-3709.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:72:0:604:db7:a71b"; };
                                              { "vla1-3710.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a836"; };
                                              { "vla1-3716.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:72:0:604:db7:a5c6"; };
                                              { "vla1-3863.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:aa08"; };
                                              { "vla1-3965.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:87:0:604:db7:aba1"; };
                                              { "vla1-4006.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a92c"; };
                                              { "vla1-4025.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:7f:0:604:db7:a3a5"; };
                                              { "vla1-4041.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8c:0:604:db7:abf2"; };
                                              { "vla1-4114.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:aab9"; };
                                              { "vla1-4117.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8a:0:604:db7:a817"; };
                                              { "vla1-4119.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8a:0:604:db7:a978"; };
                                              { "vla1-4130.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:a8d6"; };
                                              { "vla1-4153.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9b:0:604:db7:aa91"; };
                                              { "vla1-4167.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9c:0:604:db7:a8e5"; };
                                              { "vla1-4168.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:97:0:604:db7:a7a3"; };
                                              { "vla1-4177.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9b:0:604:db7:aa6b"; };
                                              { "vla1-4183.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:7c:0:604:db7:9df2"; };
                                              { "vla1-4192.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8c:0:604:db7:ab53"; };
                                              { "vla1-4200.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:a82b"; };
                                              { "vla1-4321.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a842"; };
                                              { "vla1-4344.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:ab5b"; };
                                              { "vla1-4354.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:aa94"; };
                                              { "vla1-4406.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:95:0:604:db7:a9f9"; };
                                              { "vla1-4472.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9c:0:604:db7:aa71"; };
                                              { "vla1-4553.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:77:0:604:d8f:eb26"; };
                                              { "vla1-4554.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:77:0:604:d8f:eb76"; };
                                            }, {
                                              resolve_timeout = "10ms";
                                              connect_timeout = "30ms";
                                              backend_timeout = "10s";
                                              fail_on_5xx = true;
                                              http_backend = true;
                                              buffering = false;
                                              keepalive_count = 0;
                                              need_resolve = true;
                                            }))
                                          }; -- weighted2
                                        }; -- balancer2
                                      }; -- antirobot_vla
                                    }; -- rr
                                  }; -- balancer2
                                }; -- checker
                                module = {
                                  errordocument = {
                                    status = 403;
                                    force_conn_close = false;
                                  }; -- errordocument
                                }; -- module
                              }; -- antirobot
                            }; -- cutter
                          }; -- h100
                        }; -- report
                      }; -- ext_captcha
                      ext_exp_testing = {
                        priority = 2;
                        match_fsm = {
                          cgi = "(exp-testing=da|exp_confs=testing)";
                          case_insensitive = true;
                          surround = true;
                        }; -- match_fsm
                        headers = {
                          create = {
                            ["X-L7-EXP-Testing"] = "true";
                          }; -- create
                          shared = {
                            uuid = "5161154789216279916";
                            exp_getter = {
                              trusted = true;
                              file_switch = "./controls/expgetter.switch";
                              uaas = {
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
                                        bygeo_man = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "man1-0551-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:3372:10e:b563:0:43d1"; };
                                                { "man1-3722-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:37e8:10e:b563:0:43d1"; };
                                                { "man1-4352-a48-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:3cda:10e:b563:0:43d1"; };
                                                { "man1-4648-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:168e:10e:b563:0:43d1"; };
                                                { "man1-5661-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:deb:10e:b563:0:43d1"; };
                                                { "man1-6670-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:172:10e:b563:0:43d1"; };
                                                { "man1-7202-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:a16:10e:b563:0:43d1"; };
                                                { "man1-8284-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:315:10e:b563:0:43d1"; };
                                                { "man1-8314-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:301:10e:b563:0:43d1"; };
                                                { "man2-0395-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4415:10e:b563:0:43d1"; };
                                                { "man2-0510-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4d8c:10e:b563:0:43d1"; };
                                                { "man2-0584-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4105:10e:b563:0:43d1"; };
                                                { "man2-0971-af4-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:22a3:10e:b563:0:43d1"; };
                                                { "man2-1463-c9d-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c09:26a7:10e:b563:0:43d1"; };
                                                { "man2-1680-ca9-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:2617:10e:b563:0:43d1"; };
                                                { "man2-3519-d99-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:4b02:10e:b563:0:43d1"; };
                                                { "man2-3535-57b-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:5989:10e:b563:0:43d1"; };
                                                { "man2-4159-92f-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:5720:10e:b563:0:43d1"; };
                                                { "man2-4167-a09-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0b:571a:10e:b563:0:43d1"; };
                                                { "man2-4667-250-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:1aa7:10e:b563:0:43d1"; };
                                                { "man2-4689-8c8-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:1b8c:10e:b563:0:43d1"; };
                                                { "man2-4806-07c-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c13:2786:10e:b563:0:43d1"; };
                                                { "man2-6550-5da-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0a:2704:10e:b563:0:43d1"; };
                                                { "man2-6586-c86-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0a:2987:10e:b563:0:43d1"; };
                                                { "man2-6943-60c-man-uaas-17361.gencfg-c.yandex.net"; 17361; 200.000; "2a02:6b8:c0a:2584:10e:b563:0:43d1"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- bygeo_man
                                        bygeo_sas = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "sas1-0322-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1092:10e:b566:0:43f7"; };
                                                { "sas1-0370-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:a03:10e:b566:0:43f7"; };
                                                { "sas1-0375-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:929:10e:b566:0:43f7"; };
                                                { "sas1-0730-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1405:10e:b566:0:43f7"; };
                                                { "sas1-1127-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:3515:10e:b566:0:43f7"; };
                                                { "sas1-1693-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:4810:10e:b566:0:43f7"; };
                                                { "sas1-1786-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:8e2f:10e:b566:0:43f7"; };
                                                { "sas1-2165-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:37a0:10e:b566:0:43f7"; };
                                                { "sas1-2335-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1d93:10e:b566:0:43f7"; };
                                                { "sas1-2491-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:162b:10e:b566:0:43f7"; };
                                                { "sas1-2511-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1804:10e:b566:0:43f7"; };
                                                { "sas1-2535-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:18a8:10e:b566:0:43f7"; };
                                                { "sas1-2607-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1603:10e:b566:0:43f7"; };
                                                { "sas1-2659-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1724:10e:b566:0:43f7"; };
                                                { "sas1-2769-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:1812:10e:b566:0:43f7"; };
                                                { "sas1-2802-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:168d:10e:b566:0:43f7"; };
                                                { "sas1-4343-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:3487:10e:b566:0:43f7"; };
                                                { "sas1-4612-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:24af:10e:b566:0:43f7"; };
                                                { "sas1-4621-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:211a:10e:b566:0:43f7"; };
                                                { "sas1-4814-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:219f:10e:b566:0:43f7"; };
                                                { "sas1-4898-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:218a:10e:b566:0:43f7"; };
                                                { "sas1-4903-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:2a4:10e:b566:0:43f7"; };
                                                { "sas1-4906-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:930e:10e:b566:0:43f7"; };
                                                { "sas1-5003-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:4881:10e:b566:0:43f7"; };
                                                { "sas1-5414-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:2106:10e:b566:0:43f7"; };
                                                { "sas1-5538-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:369f:10e:b566:0:43f7"; };
                                                { "sas1-6006-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6a29:10e:b566:0:43f7"; };
                                                { "sas1-7522-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:43af:10e:b566:0:43f7"; };
                                                { "sas1-9397-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:711e:10e:b566:0:43f7"; };
                                                { "sas1-9493-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:7115:10e:b566:0:43f7"; };
                                                { "sas2-0148-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c11:213:10e:b566:0:43f7"; };
                                                { "sas2-0528-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c11:e9a:10e:b566:0:43f7"; };
                                                { "sas2-1143-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:b621:10e:b566:0:43f7"; };
                                                { "sas2-3214-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:ed1c:10e:b566:0:43f7"; };
                                                { "sas2-4113-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:7584:10e:b566:0:43f7"; };
                                                { "sas2-4687-f96-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:692c:10e:b566:0:43f7"; };
                                                { "sas2-6078-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c14:439d:10e:b566:0:43f7"; };
                                                { "sas2-6514-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c16:31d:10e:b566:0:43f7"; };
                                                { "sas2-8852-7e7-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c16:1d9c:10e:b566:0:43f7"; };
                                                { "slovo012-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:72a2:10e:b566:0:43f7"; };
                                                { "slovo045-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6d8a:10e:b566:0:43f7"; };
                                                { "slovo055-5be-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6422:10e:b566:0:43f7"; };
                                                { "slovo080-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6b14:10e:b566:0:43f7"; };
                                                { "slovo103-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6a8f:10e:b566:0:43f7"; };
                                                { "slovo126-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6b18:10e:b566:0:43f7"; };
                                                { "slovo143-sas-uaas-17399.gencfg-c.yandex.net"; 17399; 200.000; "2a02:6b8:c08:6a9c:10e:b566:0:43f7"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
                                          }; -- balancer2
                                        }; -- bygeo_sas
                                        bygeo_vla = {
                                          weight = 1.000;
                                          balancer2 = {
                                            unique_policy = {};
                                            attempts = 1;
                                            connection_attempts = 5;
                                            rr = {
                                              unpack(gen_proxy_backends({
                                                { "vla1-0141-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1f82:10e:b569:0:37d2"; };
                                                { "vla1-0299-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4c09:10e:b569:0:37d2"; };
                                                { "vla1-0487-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1a02:10e:b569:0:37d2"; };
                                                { "vla1-0606-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1391:10e:b569:0:37d2"; };
                                                { "vla1-0660-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:918:10e:b569:0:37d2"; };
                                                { "vla1-0724-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1e08:10e:b569:0:37d2"; };
                                                { "vla1-0732-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:29a2:10e:b569:0:37d2"; };
                                                { "vla1-0969-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:218c:10e:b569:0:37d2"; };
                                                { "vla1-1523-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:261b:10e:b569:0:37d2"; };
                                                { "vla1-1538-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:2987:10e:b569:0:37d2"; };
                                                { "vla1-1560-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3492:10e:b569:0:37d2"; };
                                                { "vla1-1600-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:379d:10e:b569:0:37d2"; };
                                                { "vla1-1674-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3499:10e:b569:0:37d2"; };
                                                { "vla1-1776-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4084:10e:b569:0:37d2"; };
                                                { "vla1-1844-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:2b9b:10e:b569:0:37d2"; };
                                                { "vla1-2051-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:261a:10e:b569:0:37d2"; };
                                                { "vla1-2083-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:2a0d:10e:b569:0:37d2"; };
                                                { "vla1-2192-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:228e:10e:b569:0:37d2"; };
                                                { "vla1-2439-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a93:10e:b569:0:37d2"; };
                                                { "vla1-2467-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a01:10e:b569:0:37d2"; };
                                                { "vla1-2474-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a87:10e:b569:0:37d2"; };
                                                { "vla1-2482-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3c9a:10e:b569:0:37d2"; };
                                                { "vla1-2526-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4a98:10e:b569:0:37d2"; };
                                                { "vla1-3220-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:1912:10e:b569:0:37d2"; };
                                                { "vla1-3454-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:c90:10e:b569:0:37d2"; };
                                                { "vla1-3715-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:5084:10e:b569:0:37d2"; };
                                                { "vla1-3819-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0f:1d89:10e:b569:0:37d2"; };
                                                { "vla1-3876-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4302:10e:b569:0:37d2"; };
                                                { "vla1-4007-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4817:10e:b569:0:37d2"; };
                                                { "vla1-4362-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:439d:10e:b569:0:37d2"; };
                                                { "vla1-4408-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:431a:10e:b569:0:37d2"; };
                                                { "vla1-4580-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:3b96:10e:b569:0:37d2"; };
                                                { "vla1-5539-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0f:1e10:10e:b569:0:37d2"; };
                                                { "vla2-1001-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:498:10e:b569:0:37d2"; };
                                                { "vla2-1003-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:49a:10e:b569:0:37d2"; };
                                                { "vla2-1008-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:49d:10e:b569:0:37d2"; };
                                                { "vla2-1015-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1ba1:10e:b569:0:37d2"; };
                                                { "vla2-1017-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1b83:10e:b569:0:37d2"; };
                                                { "vla2-1019-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c17:48a:10e:b569:0:37d2"; };
                                                { "vla2-1067-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1ba2:10e:b569:0:37d2"; };
                                                { "vla2-1071-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:1b9f:10e:b569:0:37d2"; };
                                                { "vla2-5623-b04-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c0d:4f81:10e:b569:0:37d2"; };
                                                { "vla2-5945-62c-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c18:620:10e:b569:0:37d2"; };
                                                { "vla2-5963-9a4-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c18:612:10e:b569:0:37d2"; };
                                                { "vla2-7970-d06-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c15:398d:10e:b569:0:37d2"; };
                                                { "vla2-7992-190-vla-uaas-14290.gencfg-c.yandex.net"; 14290; 200.000; "2a02:6b8:c18:1422:10e:b569:0:37d2"; };
                                              }, {
                                                resolve_timeout = "10ms";
                                                connect_timeout = "5ms";
                                                backend_timeout = "10ms";
                                                fail_on_5xx = true;
                                                http_backend = true;
                                                buffering = false;
                                                keepalive_count = 1;
                                                need_resolve = true;
                                              }))
                                            }; -- rr
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
                              }; -- uaas
                              shared = {
                                uuid = "int_upstreams";
                              }; -- shared
                            }; -- exp_getter
                          }; -- shared
                        }; -- headers
                      }; -- ext_exp_testing
                      default = {
                        priority = 1;
                        headers = {
                          create = {
                            ["X-L7-EXP"] = "true";
                          }; -- create
                          shared = {
                            uuid = "5161154789216279916";
                          }; -- shared
                        }; -- headers
                      }; -- default
                    }; -- regexp
                  }; -- response_headers
                }; -- headers
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_15860 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15860;
      }; -- ports
      shared = {
        uuid = "5509186961134843054";
      }; -- shared
    }; -- http_section_15860
    fake_section = {
      ips = {
        "127.0.0.44";
      }; -- ips
      ports = {
        15860;
      }; -- ports
      http = {
        maxlen = 65536;
        maxreq = 65536;
        keepalive = true;
        no_keepalive_file = "./controls/keepalive_disabled";
        events = {
          stats = "report";
        }; -- events
        shared = {
          uuid = "int_upstreams";
          regexp = {
            int_searchapp = {
              priority = 16;
              match_fsm = {
                URI = "/(((m)?search/)?searchapp)(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "searchapp";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/searchapp.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control.weights";
                        searchapp_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_searchapp_to_vla";
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
                              weighted2 = {
                                slow_reply_time = "1s";
                                correction_params = {
                                  max_weight = 5.000;
                                  min_weight = 0.050;
                                  history_time = "100s";
                                  feedback_time = "300s";
                                  plus_diff_per_sec = 0.050;
                                  minus_diff_per_sec = 0.100;
                                }; -- correction_params
                                unpack(gen_proxy_backends({
                                  { "vla1-0014.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:9f:0:604:5cf5:bd7f"; };
                                  { "vla1-0040.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:4f:0:604:5cf4:8eff"; };
                                  { "vla1-0068.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:4f:0:604:5cf5:b1c0"; };
                                  { "vla1-0116.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:1:0:604:db6:1a1a"; };
                                  { "vla1-0222.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:9e:0:604:db7:a83b"; };
                                  { "vla1-0336.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:78:0:604:db7:a752"; };
                                  { "vla1-0344.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:78:0:604:db7:a7ad"; };
                                  { "vla1-0436.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:26:0:604:db7:9cb6"; };
                                  { "vla1-0562.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:67:0:604:db7:a31d"; };
                                  { "vla1-0593.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:44:0:604:db7:9f5b"; };
                                  { "vla1-0721.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:53:0:604:db7:9d1d"; };
                                  { "vla1-0737.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:13:0:604:db7:999e"; };
                                  { "vla1-0789.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:13:0:604:db7:9b11"; };
                                  { "vla1-0848.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                  { "vla1-0968.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:27:0:604:db7:9c9c"; };
                                  { "vla1-0985.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:81:0:604:db7:a8c3"; };
                                  { "vla1-1143.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:91:0:604:db7:aab8"; };
                                  { "vla1-1159.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:82:0:604:db7:a7d2"; };
                                  { "vla1-1467.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:57:0:604:db7:a5ec"; };
                                  { "vla1-1538.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:53:0:604:db7:9d66"; };
                                  { "vla1-1753.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:88:0:604:db7:a916"; };
                                  { "vla1-2038.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:34:0:604:db7:9d82"; };
                                  { "vla1-2154.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:54:0:604:db7:a593"; };
                                  { "vla1-2342.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:86:0:604:db7:aab5"; };
                                  { "vla1-2349.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:86:0:604:db7:ab9b"; };
                                  { "vla1-2439.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:95:0:604:db7:a9ce"; };
                                  { "vla1-2454.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:97:0:604:db7:a904"; };
                                  { "vla1-3713.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:87:0:604:db7:aba8"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "60s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                            }; -- balancer2
                          }; -- report
                        }; -- searchapp_vla
                        searchapp_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_searchapp_to_sas";
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
                              weighted2 = {
                                slow_reply_time = "1s";
                                correction_params = {
                                  max_weight = 5.000;
                                  min_weight = 0.050;
                                  history_time = "100s";
                                  feedback_time = "300s";
                                  plus_diff_per_sec = 0.050;
                                  minus_diff_per_sec = 0.100;
                                }; -- correction_params
                                unpack(gen_proxy_backends({
                                  { "sas1-0613.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:144:225:90ff:fe82:ffc6"; };
                                  { "sas1-0623.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:15f:225:90ff:fe4f:f6e6"; };
                                  { "sas1-0907.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:15d:225:90ff:fe83:1892"; };
                                  { "sas1-1297.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:163:225:90ff:fe92:4a1a"; };
                                  { "sas1-1302.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:163:225:90ff:fe94:2ac8"; };
                                  { "sas1-1673.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:615:922b:34ff:fecf:3fc2"; };
                                  { "sas1-1764.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:61a:922b:34ff:fecf:22ac"; };
                                  { "sas1-2253.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:624:922b:34ff:fecf:4176"; };
                                  { "sas1-2349.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:627:922b:34ff:fecf:3094"; };
                                  { "sas1-2522.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:60b:225:90ff:fe83:1aca"; };
                                  { "sas1-2694.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:608:225:90ff:fe83:17d0"; };
                                  { "sas1-2801.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:60c:225:90ff:fe83:129c"; };
                                  { "sas1-4184.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:63a:96de:80ff:fe81:ad2"; };
                                  { "sas1-4518.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:639:96de:80ff:fe81:102e"; };
                                  { "sas1-4781.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:63b:96de:80ff:fe81:100a"; };
                                  { "sas1-5281.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:644:96de:80ff:fe81:984"; };
                                  { "sas1-5283.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:644:96de:80ff:fe81:1600"; };
                                  { "sas1-5678.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:6a8:76d4:35ff:fe62:eb84"; };
                                  { "sas1-5713.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:660:76d4:35ff:fe62:ea36"; };
                                  { "sas1-5733.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:662:76d4:35ff:fec4:22de"; };
                                  { "sas1-5757.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:1a2:76d4:35ff:fe62:eb00"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "60s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                            }; -- balancer2
                          }; -- report
                        }; -- searchapp_sas
                        searchapp_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_searchapp_to_man";
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
                              weighted2 = {
                                slow_reply_time = "1s";
                                correction_params = {
                                  max_weight = 5.000;
                                  min_weight = 0.050;
                                  history_time = "100s";
                                  feedback_time = "300s";
                                  plus_diff_per_sec = 0.050;
                                  minus_diff_per_sec = 0.100;
                                }; -- correction_params
                                unpack(gen_proxy_backends({
                                  { "man1-1348.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:cab0"; };
                                  { "man1-1381.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e8d0"; };
                                  { "man1-1759.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6027:f652:14ff:fe8b:bf40"; };
                                  { "man1-1918.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:be60"; };
                                  { "man1-1930.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bdc0"; };
                                  { "man1-2009.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2a30"; };
                                  { "man1-2153.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:601e:f652:14ff:fe55:4300"; };
                                  { "man1-2468.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6021:f652:14ff:fe55:3480"; };
                                  { "man1-2531.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:ff0"; };
                                  { "man1-3021.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6028:f652:14ff:fe55:3fc0"; };
                                  { "man1-3712.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7dfe"; };
                                  { "man1-3825.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:603a:92e2:baff:fe6f:81da"; };
                                  { "man1-4352.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7fba"; };
                                  { "man1-4354.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6041:92e2:baff:fe74:761e"; };
                                  { "man1-4410.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d98"; };
                                  { "man1-4487.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6041:92e2:baff:fe6e:bd86"; };
                                  { "man1-4649.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:607f:e61d:2dff:fe6d:e6f0"; };
                                  { "man1-5985.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8830"; };
                                  { "man1-6011.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8220"; };
                                  { "man1-6066.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6056:e61d:2dff:fe00:9570"; };
                                  { "man1-6119.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4660"; };
                                  { "man1-6264.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5360"; };
                                  { "man1-6449.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6063:e61d:2dff:fe04:1f00"; };
                                  { "man1-7025.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:4c50"; };
                                  { "man1-7069.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:605e:e61d:2dff:fe6d:3860"; };
                                  { "man1-7153.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6066:f652:14ff:fef5:c7d0"; };
                                  { "man1-8115.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6509:215:b2ff:fea9:6912"; };
                                }, {
                                  resolve_timeout = "10ms";
                                  connect_timeout = "100ms";
                                  backend_timeout = "60s";
                                  fail_on_5xx = true;
                                  http_backend = true;
                                  buffering = false;
                                  keepalive_count = 0;
                                  need_resolve = true;
                                }))
                              }; -- weighted2
                            }; -- balancer2
                          }; -- report
                        }; -- searchapp_man
                        searchapp_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- searchapp_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- int_searchapp
            int_mobsearch_config = {
              priority = 15;
              match_fsm = {
                URI = "/mobilesearch/config/(searchapp|loggercfg)";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "mobsearch_config";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  balancer2 = {
                    unique_policy = {};
                    attempts = 3;
                    attempts_file = "./controls/mobsearch_config.attempts";
                    rr = {
                      weights_file = "./controls/traffic_control.weights";
                      searchapp_vla = {
                        weight = 1.000;
                        report = {
                          uuid = "requests_mobsearch_config_to_vla";
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
                            weighted2 = {
                              slow_reply_time = "1s";
                              correction_params = {
                                max_weight = 5.000;
                                min_weight = 0.050;
                                history_time = "100s";
                                feedback_time = "300s";
                                plus_diff_per_sec = 0.050;
                                minus_diff_per_sec = 0.100;
                              }; -- correction_params
                              unpack(gen_proxy_backends({
                                { "vla1-0014.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:9f:0:604:5cf5:bd7f"; };
                                { "vla1-0040.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:4f:0:604:5cf4:8eff"; };
                                { "vla1-0068.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:4f:0:604:5cf5:b1c0"; };
                                { "vla1-0116.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:1:0:604:db6:1a1a"; };
                                { "vla1-0222.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:9e:0:604:db7:a83b"; };
                                { "vla1-0336.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:78:0:604:db7:a752"; };
                                { "vla1-0344.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:78:0:604:db7:a7ad"; };
                                { "vla1-0436.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:26:0:604:db7:9cb6"; };
                                { "vla1-0562.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:67:0:604:db7:a31d"; };
                                { "vla1-0593.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:44:0:604:db7:9f5b"; };
                                { "vla1-0721.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:53:0:604:db7:9d1d"; };
                                { "vla1-0737.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:13:0:604:db7:999e"; };
                                { "vla1-0789.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:13:0:604:db7:9b11"; };
                                { "vla1-0848.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                { "vla1-0968.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:27:0:604:db7:9c9c"; };
                                { "vla1-0985.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:81:0:604:db7:a8c3"; };
                                { "vla1-1143.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:91:0:604:db7:aab8"; };
                                { "vla1-1159.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:82:0:604:db7:a7d2"; };
                                { "vla1-1467.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:57:0:604:db7:a5ec"; };
                                { "vla1-1538.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:53:0:604:db7:9d66"; };
                                { "vla1-1753.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:88:0:604:db7:a916"; };
                                { "vla1-2038.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:34:0:604:db7:9d82"; };
                                { "vla1-2154.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:54:0:604:db7:a593"; };
                                { "vla1-2342.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:86:0:604:db7:aab5"; };
                                { "vla1-2349.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:86:0:604:db7:ab9b"; };
                                { "vla1-2439.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:95:0:604:db7:a9ce"; };
                                { "vla1-2454.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:97:0:604:db7:a904"; };
                                { "vla1-3713.search.yandex.net"; 7340; 48.000; "2a02:6b8:c0e:87:0:604:db7:aba8"; };
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
                            }; -- weighted2
                          }; -- balancer2
                        }; -- report
                      }; -- searchapp_vla
                      searchapp_sas = {
                        weight = 1.000;
                        report = {
                          uuid = "requests_mobsearch_config_to_sas";
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
                            weighted2 = {
                              slow_reply_time = "1s";
                              correction_params = {
                                max_weight = 5.000;
                                min_weight = 0.050;
                                history_time = "100s";
                                feedback_time = "300s";
                                plus_diff_per_sec = 0.050;
                                minus_diff_per_sec = 0.100;
                              }; -- correction_params
                              unpack(gen_proxy_backends({
                                { "sas1-0613.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:144:225:90ff:fe82:ffc6"; };
                                { "sas1-0623.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:15f:225:90ff:fe4f:f6e6"; };
                                { "sas1-0907.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:15d:225:90ff:fe83:1892"; };
                                { "sas1-1297.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:163:225:90ff:fe92:4a1a"; };
                                { "sas1-1302.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:163:225:90ff:fe94:2ac8"; };
                                { "sas1-1673.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:615:922b:34ff:fecf:3fc2"; };
                                { "sas1-1764.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:61a:922b:34ff:fecf:22ac"; };
                                { "sas1-2253.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:624:922b:34ff:fecf:4176"; };
                                { "sas1-2349.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:627:922b:34ff:fecf:3094"; };
                                { "sas1-2522.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:60b:225:90ff:fe83:1aca"; };
                                { "sas1-2694.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:608:225:90ff:fe83:17d0"; };
                                { "sas1-2801.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:60c:225:90ff:fe83:129c"; };
                                { "sas1-4184.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:63a:96de:80ff:fe81:ad2"; };
                                { "sas1-4518.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:639:96de:80ff:fe81:102e"; };
                                { "sas1-4781.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:63b:96de:80ff:fe81:100a"; };
                                { "sas1-5281.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:644:96de:80ff:fe81:984"; };
                                { "sas1-5283.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:644:96de:80ff:fe81:1600"; };
                                { "sas1-5678.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:6a8:76d4:35ff:fe62:eb84"; };
                                { "sas1-5713.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:660:76d4:35ff:fe62:ea36"; };
                                { "sas1-5733.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:662:76d4:35ff:fec4:22de"; };
                                { "sas1-5757.search.yandex.net"; 7340; 63.000; "2a02:6b8:b000:1a2:76d4:35ff:fe62:eb00"; };
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
                            }; -- weighted2
                          }; -- balancer2
                        }; -- report
                      }; -- searchapp_sas
                      searchapp_man = {
                        weight = 1.000;
                        report = {
                          uuid = "requests_mobsearch_config_to_man";
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
                            weighted2 = {
                              slow_reply_time = "1s";
                              correction_params = {
                                max_weight = 5.000;
                                min_weight = 0.050;
                                history_time = "100s";
                                feedback_time = "300s";
                                plus_diff_per_sec = 0.050;
                                minus_diff_per_sec = 0.100;
                              }; -- correction_params
                              unpack(gen_proxy_backends({
                                { "man1-1348.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:cab0"; };
                                { "man1-1381.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e8d0"; };
                                { "man1-1759.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6027:f652:14ff:fe8b:bf40"; };
                                { "man1-1918.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:be60"; };
                                { "man1-1930.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bdc0"; };
                                { "man1-2009.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2a30"; };
                                { "man1-2153.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:601e:f652:14ff:fe55:4300"; };
                                { "man1-2468.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6021:f652:14ff:fe55:3480"; };
                                { "man1-2531.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:ff0"; };
                                { "man1-3021.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6028:f652:14ff:fe55:3fc0"; };
                                { "man1-3712.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7dfe"; };
                                { "man1-3825.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:603a:92e2:baff:fe6f:81da"; };
                                { "man1-4352.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7fba"; };
                                { "man1-4354.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6041:92e2:baff:fe74:761e"; };
                                { "man1-4410.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d98"; };
                                { "man1-4487.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6041:92e2:baff:fe6e:bd86"; };
                                { "man1-4649.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:607f:e61d:2dff:fe6d:e6f0"; };
                                { "man1-5985.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8830"; };
                                { "man1-6011.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8220"; };
                                { "man1-6066.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6056:e61d:2dff:fe00:9570"; };
                                { "man1-6119.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4660"; };
                                { "man1-6264.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5360"; };
                                { "man1-6449.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6063:e61d:2dff:fe04:1f00"; };
                                { "man1-7025.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:4c50"; };
                                { "man1-7069.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:605e:e61d:2dff:fe6d:3860"; };
                                { "man1-7153.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6066:f652:14ff:fef5:c7d0"; };
                                { "man1-8115.search.yandex.net"; 7340; 50.000; "2a02:6b8:b000:6509:215:b2ff:fea9:6912"; };
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
                            }; -- weighted2
                          }; -- balancer2
                        }; -- report
                      }; -- searchapp_man
                      searchapp_devnull = {
                        weight = -1.000;
                        errordocument = {
                          status = 204;
                          force_conn_close = false;
                        }; -- errordocument
                      }; -- searchapp_devnull
                    }; -- rr
                  }; -- balancer2
                }; -- threshold
              }; -- report
            }; -- int_mobsearch_config
            ["int_atomsearch-avia"] = {
              priority = 14;
              match_fsm = {
                URI = "/((m)?search/)?atomsearch/avia(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "atomsearch-avia";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/atomsearch-avia.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control_localgeo.weights";
                        webatom_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-avia_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-avia_to_first";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "4221074385702540641";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "vla1-0025.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:9f:0:604:5cf4:93c9"; };
                                            { "vla1-0070.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:4f:0:604:5cf4:8a79"; };
                                            { "vla1-0240.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:81:0:604:db7:a845"; };
                                            { "vla1-0264.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:81:0:604:db7:a92b"; };
                                            { "vla1-0686.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:17:0:604:db7:9920"; };
                                            { "vla1-1307.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:78:0:604:db7:ab2b"; };
                                            { "vla1-1317.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:48:0:604:db7:a499"; };
                                            { "vla1-1478.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:78:0:604:db7:a948"; };
                                            { "vla1-1880.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:13:0:604:db7:9b9c"; };
                                            { "vla1-2455.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:94:0:604:db7:a9ca"; };
                                            { "vla1-3079.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:74:0:604:db7:9a3f"; };
                                            { "vla1-4439.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:90:0:604:db7:ab6f"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "250ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-avia_to_second";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "4173093891116737557";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "vla1-0025.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:9f:0:604:5cf4:93c9"; };
                                            { "vla1-0070.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:4f:0:604:5cf4:8a79"; };
                                            { "vla1-0240.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:81:0:604:db7:a845"; };
                                            { "vla1-0264.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:81:0:604:db7:a92b"; };
                                            { "vla1-0686.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:17:0:604:db7:9920"; };
                                            { "vla1-1307.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:78:0:604:db7:ab2b"; };
                                            { "vla1-1317.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:48:0:604:db7:a499"; };
                                            { "vla1-1478.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:78:0:604:db7:a948"; };
                                            { "vla1-1880.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:13:0:604:db7:9b9c"; };
                                            { "vla1-2455.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:94:0:604:db7:a9ca"; };
                                            { "vla1-3079.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:74:0:604:db7:9a3f"; };
                                            { "vla1-4439.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:90:0:604:db7:ab6f"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "250ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_vla
                        webatom_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-avia_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-avia_to_first";
                                    shared = {
                                      uuid = "1984262545489650880";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "sas1-0178.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:14e:225:90ff:fe83:44e"; };
                                            { "sas1-0227.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:150:225:90ff:fe4f:f6d4"; };
                                            { "sas1-0242.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:151:225:90ff:fe83:992"; };
                                            { "sas1-0249.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:151:225:90ff:fe83:b24"; };
                                            { "sas1-0252.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:151:225:90ff:fe83:a7a"; };
                                            { "sas1-0258.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:150:225:90ff:fe83:b52"; };
                                            { "sas1-0291.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15b:225:90ff:fe83:1396"; };
                                            { "sas1-0310.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15a:225:90ff:fe83:9d2"; };
                                            { "sas1-0418.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15d:225:90ff:fe4f:f704"; };
                                            { "sas1-0458.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:158:225:90ff:fe83:366"; };
                                            { "sas1-0463.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:158:225:90ff:fe83:5b8"; };
                                            { "sas1-0464.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:14c:225:90ff:fe83:a68"; };
                                            { "sas1-0536.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:152:225:90ff:fe83:202"; };
                                            { "sas1-0543.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:152:225:90ff:fe83:236"; };
                                            { "sas1-0546.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:152:225:90ff:fe83:20a"; };
                                            { "sas1-0575.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:145:225:90ff:fe83:a66"; };
                                            { "sas1-0598.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15e:225:90ff:fe4f:c6ba"; };
                                            { "sas1-0991.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:14c:225:90ff:fe83:1af4"; };
                                            { "sas1-1456.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:615:922b:34ff:fecf:3cde"; };
                                            { "sas1-1523.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:65b:922b:34ff:fecf:3a6c"; };
                                            { "sas1-4820.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:619:96de:80ff:fe81:146c"; };
                                            { "sas1-5350.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:648:96de:80ff:fe81:1650"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "250ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-avia_to_second";
                                    shared = {
                                      uuid = "6644467148694829300";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "sas1-0178.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:14e:225:90ff:fe83:44e"; };
                                            { "sas1-0227.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:150:225:90ff:fe4f:f6d4"; };
                                            { "sas1-0242.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:151:225:90ff:fe83:992"; };
                                            { "sas1-0249.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:151:225:90ff:fe83:b24"; };
                                            { "sas1-0252.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:151:225:90ff:fe83:a7a"; };
                                            { "sas1-0258.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:150:225:90ff:fe83:b52"; };
                                            { "sas1-0291.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15b:225:90ff:fe83:1396"; };
                                            { "sas1-0310.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15a:225:90ff:fe83:9d2"; };
                                            { "sas1-0418.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15d:225:90ff:fe4f:f704"; };
                                            { "sas1-0458.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:158:225:90ff:fe83:366"; };
                                            { "sas1-0463.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:158:225:90ff:fe83:5b8"; };
                                            { "sas1-0464.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:14c:225:90ff:fe83:a68"; };
                                            { "sas1-0536.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:152:225:90ff:fe83:202"; };
                                            { "sas1-0543.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:152:225:90ff:fe83:236"; };
                                            { "sas1-0546.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:152:225:90ff:fe83:20a"; };
                                            { "sas1-0575.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:145:225:90ff:fe83:a66"; };
                                            { "sas1-0598.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15e:225:90ff:fe4f:c6ba"; };
                                            { "sas1-0991.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:14c:225:90ff:fe83:1af4"; };
                                            { "sas1-1456.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:615:922b:34ff:fecf:3cde"; };
                                            { "sas1-1523.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:65b:922b:34ff:fecf:3a6c"; };
                                            { "sas1-4820.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:619:96de:80ff:fe81:146c"; };
                                            { "sas1-5350.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:648:96de:80ff:fe81:1650"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "250ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_sas
                        webatom_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-avia_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-avia_to_first";
                                    shared = {
                                      uuid = "9091641199288565262";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-0068.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe74:785e"; };
                                            { "man1-0069.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7cea"; };
                                            { "man1-0070.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7d30"; };
                                            { "man1-0087.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6033:92e2:baff:fe74:7964"; };
                                            { "man1-0116.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe6f:7f0a"; };
                                            { "man1-0131.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6035:92e2:baff:fe6f:7dc4"; };
                                            { "man1-0196.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6030:92e2:baff:fe6f:81a4"; };
                                            { "man1-0270.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6034:92e2:baff:fe74:77d0"; };
                                            { "man1-0271.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7ce0"; };
                                            { "man1-0327.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602b:92e2:baff:fe74:7c7e"; };
                                            { "man1-0528.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e1e"; };
                                            { "man1-0638.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7c6a"; };
                                            { "man1-0670.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6033:92e2:baff:fe6f:7d96"; };
                                            { "man1-0731.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7800"; };
                                            { "man1-0788.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe75:4812"; };
                                            { "man1-1405.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6013:e61d:2dff:fe04:4620"; };
                                            { "man1-2050.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2150"; };
                                            { "man1-2875.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:f1e0"; };
                                            { "man1-8139.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fe20"; };
                                            { "man1-8181.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fb50"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "250ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-avia_to_second";
                                    shared = {
                                      uuid = "2074967253000753574";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-0068.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe74:785e"; };
                                            { "man1-0069.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7cea"; };
                                            { "man1-0070.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7d30"; };
                                            { "man1-0087.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6033:92e2:baff:fe74:7964"; };
                                            { "man1-0116.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe6f:7f0a"; };
                                            { "man1-0131.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6035:92e2:baff:fe6f:7dc4"; };
                                            { "man1-0196.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6030:92e2:baff:fe6f:81a4"; };
                                            { "man1-0270.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6034:92e2:baff:fe74:77d0"; };
                                            { "man1-0271.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7ce0"; };
                                            { "man1-0327.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602b:92e2:baff:fe74:7c7e"; };
                                            { "man1-0528.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e1e"; };
                                            { "man1-0638.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7c6a"; };
                                            { "man1-0670.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6033:92e2:baff:fe6f:7d96"; };
                                            { "man1-0731.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7800"; };
                                            { "man1-0788.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe75:4812"; };
                                            { "man1-1405.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6013:e61d:2dff:fe04:4620"; };
                                            { "man1-2050.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2150"; };
                                            { "man1-2875.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:f1e0"; };
                                            { "man1-8139.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fe20"; };
                                            { "man1-8181.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fb50"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "250ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_man
                        webatom_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- webatom_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- ["int_atomsearch-avia"]
            ["int_atomsearch-distr_portal"] = {
              priority = 13;
              match_fsm = {
                URI = "/((m)?search/)?atomsearch/distr_portal(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "atomsearch-distr_portal";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/atomsearch-distr_portal.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control_localgeo.weights";
                        webatom_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-distr_portal_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-distr_portal_to_first";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "4221074385702540641";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-distr_portal_to_second";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "4173093891116737557";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_vla
                        webatom_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-distr_portal_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-distr_portal_to_first";
                                    shared = {
                                      uuid = "1984262545489650880";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-distr_portal_to_second";
                                    shared = {
                                      uuid = "6644467148694829300";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_sas
                        webatom_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-distr_portal_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-distr_portal_to_first";
                                    shared = {
                                      uuid = "9091641199288565262";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-distr_portal_to_second";
                                    shared = {
                                      uuid = "2074967253000753574";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_man
                        webatom_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- webatom_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- ["int_atomsearch-distr_portal"]
            ["int_atomsearch-distr_serp"] = {
              priority = 12;
              match_fsm = {
                URI = "/((m)?search/)?atomsearch/distr_serp(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "atomsearch-distr_serp";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/atomsearch-distr_serp.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control_localgeo.weights";
                        webatom_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-distr_serp_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-distr_serp_to_first";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "1792109151814187044";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "vla1-0025.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:9f:0:604:5cf4:93c9"; };
                                            { "vla1-0070.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:4f:0:604:5cf4:8a79"; };
                                            { "vla1-0240.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:81:0:604:db7:a845"; };
                                            { "vla1-0264.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:81:0:604:db7:a92b"; };
                                            { "vla1-0686.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:17:0:604:db7:9920"; };
                                            { "vla1-1307.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:78:0:604:db7:ab2b"; };
                                            { "vla1-1317.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:48:0:604:db7:a499"; };
                                            { "vla1-1478.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:78:0:604:db7:a948"; };
                                            { "vla1-1880.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:13:0:604:db7:9b9c"; };
                                            { "vla1-2455.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:94:0:604:db7:a9ca"; };
                                            { "vla1-3079.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:74:0:604:db7:9a3f"; };
                                            { "vla1-4439.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:90:0:604:db7:ab6f"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "350ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-distr_serp_to_second";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "7876323894189774598";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "vla1-0025.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:9f:0:604:5cf4:93c9"; };
                                            { "vla1-0070.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:4f:0:604:5cf4:8a79"; };
                                            { "vla1-0240.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:81:0:604:db7:a845"; };
                                            { "vla1-0264.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:81:0:604:db7:a92b"; };
                                            { "vla1-0686.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:17:0:604:db7:9920"; };
                                            { "vla1-1307.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:78:0:604:db7:ab2b"; };
                                            { "vla1-1317.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:48:0:604:db7:a499"; };
                                            { "vla1-1478.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:78:0:604:db7:a948"; };
                                            { "vla1-1880.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:13:0:604:db7:9b9c"; };
                                            { "vla1-2455.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:94:0:604:db7:a9ca"; };
                                            { "vla1-3079.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:74:0:604:db7:9a3f"; };
                                            { "vla1-4439.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:90:0:604:db7:ab6f"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "350ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_vla
                        webatom_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-distr_serp_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-distr_serp_to_first";
                                    shared = {
                                      uuid = "6076148119755480861";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "sas1-0178.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:14e:225:90ff:fe83:44e"; };
                                            { "sas1-0227.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:150:225:90ff:fe4f:f6d4"; };
                                            { "sas1-0242.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:151:225:90ff:fe83:992"; };
                                            { "sas1-0249.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:151:225:90ff:fe83:b24"; };
                                            { "sas1-0252.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:151:225:90ff:fe83:a7a"; };
                                            { "sas1-0258.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:150:225:90ff:fe83:b52"; };
                                            { "sas1-0291.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15b:225:90ff:fe83:1396"; };
                                            { "sas1-0310.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15a:225:90ff:fe83:9d2"; };
                                            { "sas1-0418.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15d:225:90ff:fe4f:f704"; };
                                            { "sas1-0458.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:158:225:90ff:fe83:366"; };
                                            { "sas1-0463.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:158:225:90ff:fe83:5b8"; };
                                            { "sas1-0464.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:14c:225:90ff:fe83:a68"; };
                                            { "sas1-0536.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:152:225:90ff:fe83:202"; };
                                            { "sas1-0543.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:152:225:90ff:fe83:236"; };
                                            { "sas1-0546.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:152:225:90ff:fe83:20a"; };
                                            { "sas1-0575.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:145:225:90ff:fe83:a66"; };
                                            { "sas1-0598.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15e:225:90ff:fe4f:c6ba"; };
                                            { "sas1-0991.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:14c:225:90ff:fe83:1af4"; };
                                            { "sas1-1456.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:615:922b:34ff:fecf:3cde"; };
                                            { "sas1-1523.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:65b:922b:34ff:fecf:3a6c"; };
                                            { "sas1-4820.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:619:96de:80ff:fe81:146c"; };
                                            { "sas1-5350.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:648:96de:80ff:fe81:1650"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "350ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-distr_serp_to_second";
                                    shared = {
                                      uuid = "4926133661797886201";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "sas1-0178.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:14e:225:90ff:fe83:44e"; };
                                            { "sas1-0227.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:150:225:90ff:fe4f:f6d4"; };
                                            { "sas1-0242.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:151:225:90ff:fe83:992"; };
                                            { "sas1-0249.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:151:225:90ff:fe83:b24"; };
                                            { "sas1-0252.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:151:225:90ff:fe83:a7a"; };
                                            { "sas1-0258.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:150:225:90ff:fe83:b52"; };
                                            { "sas1-0291.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15b:225:90ff:fe83:1396"; };
                                            { "sas1-0310.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15a:225:90ff:fe83:9d2"; };
                                            { "sas1-0418.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15d:225:90ff:fe4f:f704"; };
                                            { "sas1-0458.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:158:225:90ff:fe83:366"; };
                                            { "sas1-0463.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:158:225:90ff:fe83:5b8"; };
                                            { "sas1-0464.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:14c:225:90ff:fe83:a68"; };
                                            { "sas1-0536.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:152:225:90ff:fe83:202"; };
                                            { "sas1-0543.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:152:225:90ff:fe83:236"; };
                                            { "sas1-0546.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:152:225:90ff:fe83:20a"; };
                                            { "sas1-0575.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:145:225:90ff:fe83:a66"; };
                                            { "sas1-0598.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15e:225:90ff:fe4f:c6ba"; };
                                            { "sas1-0991.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:14c:225:90ff:fe83:1af4"; };
                                            { "sas1-1456.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:615:922b:34ff:fecf:3cde"; };
                                            { "sas1-1523.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:65b:922b:34ff:fecf:3a6c"; };
                                            { "sas1-4820.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:619:96de:80ff:fe81:146c"; };
                                            { "sas1-5350.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:648:96de:80ff:fe81:1650"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "350ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_sas
                        webatom_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-distr_serp_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-distr_serp_to_first";
                                    shared = {
                                      uuid = "7650522399916865959";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-0068.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe74:785e"; };
                                            { "man1-0069.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7cea"; };
                                            { "man1-0070.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7d30"; };
                                            { "man1-0087.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6033:92e2:baff:fe74:7964"; };
                                            { "man1-0116.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe6f:7f0a"; };
                                            { "man1-0131.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6035:92e2:baff:fe6f:7dc4"; };
                                            { "man1-0196.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6030:92e2:baff:fe6f:81a4"; };
                                            { "man1-0270.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6034:92e2:baff:fe74:77d0"; };
                                            { "man1-0271.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7ce0"; };
                                            { "man1-0327.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602b:92e2:baff:fe74:7c7e"; };
                                            { "man1-0528.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e1e"; };
                                            { "man1-0638.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7c6a"; };
                                            { "man1-0670.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6033:92e2:baff:fe6f:7d96"; };
                                            { "man1-0731.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7800"; };
                                            { "man1-0788.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe75:4812"; };
                                            { "man1-1405.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6013:e61d:2dff:fe04:4620"; };
                                            { "man1-2050.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2150"; };
                                            { "man1-2875.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:f1e0"; };
                                            { "man1-8139.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fe20"; };
                                            { "man1-8181.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fb50"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "350ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-distr_serp_to_second";
                                    shared = {
                                      uuid = "939338917446997809";
                                      balancer2 = {
                                        unique_policy = {};
                                        attempts = 2;
                                        weighted2 = {
                                          slow_reply_time = "1s";
                                          correction_params = {
                                            max_weight = 5.000;
                                            min_weight = 0.050;
                                            history_time = "100s";
                                            feedback_time = "300s";
                                            plus_diff_per_sec = 0.050;
                                            minus_diff_per_sec = 0.100;
                                          }; -- correction_params
                                          unpack(gen_proxy_backends({
                                            { "man1-0068.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe74:785e"; };
                                            { "man1-0069.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7cea"; };
                                            { "man1-0070.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7d30"; };
                                            { "man1-0087.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6033:92e2:baff:fe74:7964"; };
                                            { "man1-0116.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe6f:7f0a"; };
                                            { "man1-0131.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6035:92e2:baff:fe6f:7dc4"; };
                                            { "man1-0196.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6030:92e2:baff:fe6f:81a4"; };
                                            { "man1-0270.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6034:92e2:baff:fe74:77d0"; };
                                            { "man1-0271.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7ce0"; };
                                            { "man1-0327.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602b:92e2:baff:fe74:7c7e"; };
                                            { "man1-0528.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e1e"; };
                                            { "man1-0638.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7c6a"; };
                                            { "man1-0670.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6033:92e2:baff:fe6f:7d96"; };
                                            { "man1-0731.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7800"; };
                                            { "man1-0788.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe75:4812"; };
                                            { "man1-1405.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6013:e61d:2dff:fe04:4620"; };
                                            { "man1-2050.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2150"; };
                                            { "man1-2875.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:f1e0"; };
                                            { "man1-8139.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fe20"; };
                                            { "man1-8181.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fb50"; };
                                          }, {
                                            resolve_timeout = "10ms";
                                            connect_timeout = "100ms";
                                            backend_timeout = "350ms";
                                            fail_on_5xx = true;
                                            http_backend = true;
                                            buffering = false;
                                            keepalive_count = 0;
                                            need_resolve = true;
                                          }))
                                        }; -- weighted2
                                      }; -- balancer2
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_man
                        webatom_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- webatom_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- ["int_atomsearch-distr_serp"]
            ["int_atomsearch-images"] = {
              priority = 11;
              match_fsm = {
                URI = "/((m)?search/)?atomsearch/images(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "atomsearch-images";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/atomsearch-images.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control_localgeo.weights";
                        webatom_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-images_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-images_to_first";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "1792109151814187044";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-images_to_second";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "7876323894189774598";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_vla
                        webatom_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-images_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-images_to_first";
                                    shared = {
                                      uuid = "6076148119755480861";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-images_to_second";
                                    shared = {
                                      uuid = "4926133661797886201";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_sas
                        webatom_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-images_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-images_to_first";
                                    shared = {
                                      uuid = "7650522399916865959";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-images_to_second";
                                    shared = {
                                      uuid = "939338917446997809";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_man
                        webatom_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- webatom_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- ["int_atomsearch-images"]
            ["int_atomsearch-yanews"] = {
              priority = 10;
              match_fsm = {
                URI = "/((m)?search/)?atomsearch/yanews(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "atomsearch-yanews";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/atomsearch-yanews.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control_localgeo.weights";
                        webatom_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-yanews_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-yanews_to_first";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "1792109151814187044";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-yanews_to_second";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "7876323894189774598";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_vla
                        webatom_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-yanews_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-yanews_to_first";
                                    shared = {
                                      uuid = "6076148119755480861";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-yanews_to_second";
                                    shared = {
                                      uuid = "4926133661797886201";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_sas
                        webatom_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-yanews_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-yanews_to_first";
                                    shared = {
                                      uuid = "7650522399916865959";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-yanews_to_second";
                                    shared = {
                                      uuid = "939338917446997809";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_man
                        webatom_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- webatom_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- ["int_atomsearch-yanews"]
            ["int_atomsearch-video"] = {
              priority = 9;
              match_fsm = {
                URI = "/((m)?search/)?atomsearch/video(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "atomsearch-video";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/atomsearch-video.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control_localgeo.weights";
                        webatom_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-video_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-video_to_first";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "1792109151814187044";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-video_to_second";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "7876323894189774598";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_vla
                        webatom_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-video_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-video_to_first";
                                    shared = {
                                      uuid = "6076148119755480861";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-video_to_second";
                                    shared = {
                                      uuid = "4926133661797886201";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_sas
                        webatom_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-video_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-video_to_first";
                                    shared = {
                                      uuid = "7650522399916865959";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-video_to_second";
                                    shared = {
                                      uuid = "939338917446997809";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_man
                        webatom_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- webatom_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- ["int_atomsearch-video"]
            ["int_atomsearch-promolib"] = {
              priority = 8;
              match_fsm = {
                URI = "/((m)?search/)?atomsearch/promolib(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "atomsearch-promolib";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/atomsearch-promolib.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control_localgeo.weights";
                        webatom_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-promolib_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-promolib_to_first";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "1792109151814187044";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-promolib_to_second";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "7876323894189774598";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_vla
                        webatom_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-promolib_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-promolib_to_first";
                                    shared = {
                                      uuid = "6076148119755480861";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-promolib_to_second";
                                    shared = {
                                      uuid = "4926133661797886201";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_sas
                        webatom_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-promolib_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-promolib_to_first";
                                    shared = {
                                      uuid = "7650522399916865959";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-promolib_to_second";
                                    shared = {
                                      uuid = "939338917446997809";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_man
                        webatom_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- webatom_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- ["int_atomsearch-promolib"]
            ["int_atomsearch-viewconfig"] = {
              priority = 7;
              match_fsm = {
                URI = "/((m)?search/)?atomsearch/viewconfig(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "atomsearch-viewconfig";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/atomsearch-viewconfig.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control_localgeo.weights";
                        webatom_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-viewconfig_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-viewconfig_to_first";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "1792109151814187044";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch-viewconfig_to_second";
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    shared = {
                                      uuid = "7876323894189774598";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_vla
                        webatom_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-viewconfig_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-viewconfig_to_first";
                                    shared = {
                                      uuid = "6076148119755480861";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-viewconfig_to_second";
                                    shared = {
                                      uuid = "4926133661797886201";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_sas
                        webatom_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch-viewconfig_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-viewconfig_to_first";
                                    shared = {
                                      uuid = "7650522399916865959";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    ranges = get_str_var("default_ranges");
                                    just_storage = false;
                                    disable_robotness = true;
                                    disable_sslness = true;
                                    events = {
                                      stats = "report";
                                    }; -- events
                                    refers = "requests_atomsearch-viewconfig_to_second";
                                    shared = {
                                      uuid = "939338917446997809";
                                    }; -- shared
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_man
                        webatom_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- webatom_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- ["int_atomsearch-viewconfig"]
            int_atomsearch = {
              priority = 6;
              match_fsm = {
                URI = "/((m)?search/)?atomsearch(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "atomsearch";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/atomsearch.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control_localgeo.weights";
                        webatom_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch_to_first";
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
                                      weighted2 = {
                                        slow_reply_time = "1s";
                                        correction_params = {
                                          max_weight = 5.000;
                                          min_weight = 0.050;
                                          history_time = "100s";
                                          feedback_time = "300s";
                                          plus_diff_per_sec = 0.050;
                                          minus_diff_per_sec = 0.100;
                                        }; -- correction_params
                                        unpack(gen_proxy_backends({
                                          { "vla1-0025.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:9f:0:604:5cf4:93c9"; };
                                          { "vla1-0070.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:4f:0:604:5cf4:8a79"; };
                                          { "vla1-0240.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:81:0:604:db7:a845"; };
                                          { "vla1-0264.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:81:0:604:db7:a92b"; };
                                          { "vla1-0686.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:17:0:604:db7:9920"; };
                                          { "vla1-1307.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:78:0:604:db7:ab2b"; };
                                          { "vla1-1317.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:48:0:604:db7:a499"; };
                                          { "vla1-1478.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:78:0:604:db7:a948"; };
                                          { "vla1-1880.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:13:0:604:db7:9b9c"; };
                                          { "vla1-2455.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:94:0:604:db7:a9ca"; };
                                          { "vla1-3079.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:74:0:604:db7:9a3f"; };
                                          { "vla1-4439.search.yandex.net"; 8348; 140.000; "2a02:6b8:c0e:90:0:604:db7:ab6f"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "500ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
                                    uuid = "requests_atomsearch_to_second";
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
                                      weighted2 = {
                                        slow_reply_time = "1s";
                                        correction_params = {
                                          max_weight = 5.000;
                                          min_weight = 0.050;
                                          history_time = "100s";
                                          feedback_time = "300s";
                                          plus_diff_per_sec = 0.050;
                                          minus_diff_per_sec = 0.100;
                                        }; -- correction_params
                                        unpack(gen_proxy_backends({
                                          { "vla1-0025.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:9f:0:604:5cf4:93c9"; };
                                          { "vla1-0070.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:4f:0:604:5cf4:8a79"; };
                                          { "vla1-0240.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:81:0:604:db7:a845"; };
                                          { "vla1-0264.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:81:0:604:db7:a92b"; };
                                          { "vla1-0686.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:17:0:604:db7:9920"; };
                                          { "vla1-1307.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:78:0:604:db7:ab2b"; };
                                          { "vla1-1317.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:48:0:604:db7:a499"; };
                                          { "vla1-1478.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:78:0:604:db7:a948"; };
                                          { "vla1-1880.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:13:0:604:db7:9b9c"; };
                                          { "vla1-2455.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:94:0:604:db7:a9ca"; };
                                          { "vla1-3079.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:74:0:604:db7:9a3f"; };
                                          { "vla1-4439.search.yandex.net"; 8448; 140.000; "2a02:6b8:c0e:90:0:604:db7:ab6f"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "500ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_vla
                        webatom_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
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
                                      weighted2 = {
                                        slow_reply_time = "1s";
                                        correction_params = {
                                          max_weight = 5.000;
                                          min_weight = 0.050;
                                          history_time = "100s";
                                          feedback_time = "300s";
                                          plus_diff_per_sec = 0.050;
                                          minus_diff_per_sec = 0.100;
                                        }; -- correction_params
                                        unpack(gen_proxy_backends({
                                          { "sas1-0178.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:14e:225:90ff:fe83:44e"; };
                                          { "sas1-0227.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:150:225:90ff:fe4f:f6d4"; };
                                          { "sas1-0242.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:151:225:90ff:fe83:992"; };
                                          { "sas1-0249.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:151:225:90ff:fe83:b24"; };
                                          { "sas1-0252.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:151:225:90ff:fe83:a7a"; };
                                          { "sas1-0258.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:150:225:90ff:fe83:b52"; };
                                          { "sas1-0291.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15b:225:90ff:fe83:1396"; };
                                          { "sas1-0310.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15a:225:90ff:fe83:9d2"; };
                                          { "sas1-0418.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15d:225:90ff:fe4f:f704"; };
                                          { "sas1-0458.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:158:225:90ff:fe83:366"; };
                                          { "sas1-0463.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:158:225:90ff:fe83:5b8"; };
                                          { "sas1-0464.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:14c:225:90ff:fe83:a68"; };
                                          { "sas1-0536.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:152:225:90ff:fe83:202"; };
                                          { "sas1-0543.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:152:225:90ff:fe83:236"; };
                                          { "sas1-0546.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:152:225:90ff:fe83:20a"; };
                                          { "sas1-0575.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:145:225:90ff:fe83:a66"; };
                                          { "sas1-0598.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:15e:225:90ff:fe4f:c6ba"; };
                                          { "sas1-0991.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:14c:225:90ff:fe83:1af4"; };
                                          { "sas1-1456.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:615:922b:34ff:fecf:3cde"; };
                                          { "sas1-1523.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:65b:922b:34ff:fecf:3a6c"; };
                                          { "sas1-4820.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:619:96de:80ff:fe81:146c"; };
                                          { "sas1-5350.search.yandex.net"; 8348; 60.000; "2a02:6b8:b000:648:96de:80ff:fe81:1650"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "500ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                    refers = "requests_atomsearch_to_first";
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
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
                                      weighted2 = {
                                        slow_reply_time = "1s";
                                        correction_params = {
                                          max_weight = 5.000;
                                          min_weight = 0.050;
                                          history_time = "100s";
                                          feedback_time = "300s";
                                          plus_diff_per_sec = 0.050;
                                          minus_diff_per_sec = 0.100;
                                        }; -- correction_params
                                        unpack(gen_proxy_backends({
                                          { "sas1-0178.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:14e:225:90ff:fe83:44e"; };
                                          { "sas1-0227.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:150:225:90ff:fe4f:f6d4"; };
                                          { "sas1-0242.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:151:225:90ff:fe83:992"; };
                                          { "sas1-0249.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:151:225:90ff:fe83:b24"; };
                                          { "sas1-0252.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:151:225:90ff:fe83:a7a"; };
                                          { "sas1-0258.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:150:225:90ff:fe83:b52"; };
                                          { "sas1-0291.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15b:225:90ff:fe83:1396"; };
                                          { "sas1-0310.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15a:225:90ff:fe83:9d2"; };
                                          { "sas1-0418.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15d:225:90ff:fe4f:f704"; };
                                          { "sas1-0458.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:158:225:90ff:fe83:366"; };
                                          { "sas1-0463.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:158:225:90ff:fe83:5b8"; };
                                          { "sas1-0464.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:14c:225:90ff:fe83:a68"; };
                                          { "sas1-0536.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:152:225:90ff:fe83:202"; };
                                          { "sas1-0543.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:152:225:90ff:fe83:236"; };
                                          { "sas1-0546.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:152:225:90ff:fe83:20a"; };
                                          { "sas1-0575.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:145:225:90ff:fe83:a66"; };
                                          { "sas1-0598.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:15e:225:90ff:fe4f:c6ba"; };
                                          { "sas1-0991.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:14c:225:90ff:fe83:1af4"; };
                                          { "sas1-1456.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:615:922b:34ff:fecf:3cde"; };
                                          { "sas1-1523.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:65b:922b:34ff:fecf:3a6c"; };
                                          { "sas1-4820.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:619:96de:80ff:fe81:146c"; };
                                          { "sas1-5350.search.yandex.net"; 8448; 71.000; "2a02:6b8:b000:648:96de:80ff:fe81:1650"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "500ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                    refers = "requests_atomsearch_to_second";
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_sas
                        webatom_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_atomsearch_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            balancer2 = {
                              unique_policy = {};
                              attempts = 1;
                              rr = {
                                weights_file = "./controls/traffic_control_localgeo.weights";
                                webatom2_first = {
                                  weight = 1.000;
                                  report = {
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
                                      weighted2 = {
                                        slow_reply_time = "1s";
                                        correction_params = {
                                          max_weight = 5.000;
                                          min_weight = 0.050;
                                          history_time = "100s";
                                          feedback_time = "300s";
                                          plus_diff_per_sec = 0.050;
                                          minus_diff_per_sec = 0.100;
                                        }; -- correction_params
                                        unpack(gen_proxy_backends({
                                          { "man1-0068.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe74:785e"; };
                                          { "man1-0069.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7cea"; };
                                          { "man1-0070.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7d30"; };
                                          { "man1-0087.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6033:92e2:baff:fe74:7964"; };
                                          { "man1-0116.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe6f:7f0a"; };
                                          { "man1-0131.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6035:92e2:baff:fe6f:7dc4"; };
                                          { "man1-0196.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6030:92e2:baff:fe6f:81a4"; };
                                          { "man1-0270.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6034:92e2:baff:fe74:77d0"; };
                                          { "man1-0271.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7ce0"; };
                                          { "man1-0327.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602b:92e2:baff:fe74:7c7e"; };
                                          { "man1-0528.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e1e"; };
                                          { "man1-0638.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7c6a"; };
                                          { "man1-0670.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6033:92e2:baff:fe6f:7d96"; };
                                          { "man1-0731.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7800"; };
                                          { "man1-0788.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:602e:92e2:baff:fe75:4812"; };
                                          { "man1-1405.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6013:e61d:2dff:fe04:4620"; };
                                          { "man1-2050.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2150"; };
                                          { "man1-2875.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:f1e0"; };
                                          { "man1-8139.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fe20"; };
                                          { "man1-8181.search.yandex.net"; 8348; 44.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fb50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "500ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                    refers = "requests_atomsearch_to_first";
                                  }; -- report
                                }; -- webatom2_first
                                webatom2_second = {
                                  weight = 1.000;
                                  report = {
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
                                      weighted2 = {
                                        slow_reply_time = "1s";
                                        correction_params = {
                                          max_weight = 5.000;
                                          min_weight = 0.050;
                                          history_time = "100s";
                                          feedback_time = "300s";
                                          plus_diff_per_sec = 0.050;
                                          minus_diff_per_sec = 0.100;
                                        }; -- correction_params
                                        unpack(gen_proxy_backends({
                                          { "man1-0068.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe74:785e"; };
                                          { "man1-0069.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7cea"; };
                                          { "man1-0070.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7d30"; };
                                          { "man1-0087.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6033:92e2:baff:fe74:7964"; };
                                          { "man1-0116.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe6f:7f0a"; };
                                          { "man1-0131.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6035:92e2:baff:fe6f:7dc4"; };
                                          { "man1-0196.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6030:92e2:baff:fe6f:81a4"; };
                                          { "man1-0270.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6034:92e2:baff:fe74:77d0"; };
                                          { "man1-0271.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7ce0"; };
                                          { "man1-0327.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602b:92e2:baff:fe74:7c7e"; };
                                          { "man1-0528.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e1e"; };
                                          { "man1-0638.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7c6a"; };
                                          { "man1-0670.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6033:92e2:baff:fe6f:7d96"; };
                                          { "man1-0731.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7800"; };
                                          { "man1-0788.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:602e:92e2:baff:fe75:4812"; };
                                          { "man1-1405.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6013:e61d:2dff:fe04:4620"; };
                                          { "man1-2050.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2150"; };
                                          { "man1-2875.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:f1e0"; };
                                          { "man1-8139.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fe20"; };
                                          { "man1-8181.search.yandex.net"; 8448; 48.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fb50"; };
                                        }, {
                                          resolve_timeout = "10ms";
                                          connect_timeout = "100ms";
                                          backend_timeout = "500ms";
                                          fail_on_5xx = true;
                                          http_backend = true;
                                          buffering = false;
                                          keepalive_count = 0;
                                          need_resolve = true;
                                        }))
                                      }; -- weighted2
                                    }; -- balancer2
                                    refers = "requests_atomsearch_to_second";
                                  }; -- report
                                }; -- webatom2_second
                              }; -- rr
                            }; -- balancer2
                          }; -- report
                        }; -- webatom_man
                        webatom_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- webatom_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- int_atomsearch
            int_assistant = {
              priority = 5;
              match_fsm = {
                URI = "/((m)?search/)?assistant(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "assistant";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    regexp = {
                      ["CPLB-235"] = {
                        priority = 2;
                        match_and = {
                          {
                            match_fsm = {
                              cgi = "app_version=413";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                          {
                            match_fsm = {
                              cgi = "clid=1866855";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                          };
                        }; -- match_and
                        regexp = {
                          app_id_exists = {
                            priority = 2;
                            match_fsm = {
                              cgi = "app_id";
                              case_insensitive = true;
                              surround = true;
                            }; -- match_fsm
                            shared = {
                              uuid = "assistant_backends";
                            }; -- shared
                          }; -- app_id_exists
                          default = {
                            priority = 1;
                            errordocument = {
                              status = 404;
                              force_conn_close = false;
                            }; -- errordocument
                          }; -- default
                        }; -- regexp
                      }; -- ["CPLB-235"]
                      default = {
                        priority = 1;
                        shared = {
                          uuid = "assistant_backends";
                          balancer2 = {
                            unique_policy = {};
                            attempts = 3;
                            attempts_file = "./controls/assistant.attempts";
                            rr = {
                              weights_file = "./controls/traffic_control.weights";
                              assistant_vla = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_assistant_to_vla";
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
                                    weighted2 = {
                                      slow_reply_time = "1s";
                                      correction_params = {
                                        max_weight = 5.000;
                                        min_weight = 0.050;
                                        history_time = "100s";
                                        feedback_time = "300s";
                                        plus_diff_per_sec = 0.050;
                                        minus_diff_per_sec = 0.100;
                                      }; -- correction_params
                                      unpack(gen_proxy_backends({
                                        { "vla1-0014.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:9f:0:604:5cf5:bd7f"; };
                                        { "vla1-0019.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:9f:0:604:5cf4:8c5d"; };
                                        { "vla1-0040.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:4f:0:604:5cf4:8eff"; };
                                        { "vla1-0068.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:4f:0:604:5cf5:b1c0"; };
                                        { "vla1-0116.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:1:0:604:db6:1a1a"; };
                                        { "vla1-0222.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:9e:0:604:db7:a83b"; };
                                        { "vla1-0336.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:78:0:604:db7:a752"; };
                                        { "vla1-0344.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:78:0:604:db7:a7ad"; };
                                        { "vla1-0562.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:67:0:604:db7:a31d"; };
                                        { "vla1-0593.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:44:0:604:db7:9f5b"; };
                                        { "vla1-0721.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:53:0:604:db7:9d1d"; };
                                        { "vla1-0789.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:13:0:604:db7:9b11"; };
                                        { "vla1-0848.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                        { "vla1-0968.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:27:0:604:db7:9c9c"; };
                                        { "vla1-0985.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:81:0:604:db7:a8c3"; };
                                        { "vla1-1143.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:91:0:604:db7:aab8"; };
                                        { "vla1-1159.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:82:0:604:db7:a7d2"; };
                                        { "vla1-1364.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:4c:0:604:db7:a0ba"; };
                                        { "vla1-1467.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:57:0:604:db7:a5ec"; };
                                        { "vla1-1538.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:53:0:604:db7:9d66"; };
                                        { "vla1-1753.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:88:0:604:db7:a916"; };
                                        { "vla1-2038.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:34:0:604:db7:9d82"; };
                                        { "vla1-2154.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:54:0:604:db7:a593"; };
                                        { "vla1-2342.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:86:0:604:db7:aab5"; };
                                        { "vla1-2439.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:95:0:604:db7:a9ce"; };
                                        { "vla1-2819.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:32:0:604:db7:a534"; };
                                        { "vla1-3021.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:73:0:604:db7:a6ea"; };
                                        { "vla1-3713.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:87:0:604:db7:aba8"; };
                                        { "vla2-0465.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:184:0:604:5e97:dcde"; };
                                        { "vla2-0466.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:184:0:604:5e97:dcf8"; };
                                        { "vla2-0471.search.yandex.net"; 7332; 265.000; "2a02:6b8:c0e:184:0:604:5e97:da40"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "5s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                  }; -- balancer2
                                }; -- report
                              }; -- assistant_vla
                              assistant_sas = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_assistant_to_sas";
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
                                    weighted2 = {
                                      slow_reply_time = "1s";
                                      correction_params = {
                                        max_weight = 5.000;
                                        min_weight = 0.050;
                                        history_time = "100s";
                                        feedback_time = "300s";
                                        plus_diff_per_sec = 0.050;
                                        minus_diff_per_sec = 0.100;
                                      }; -- correction_params
                                      unpack(gen_proxy_backends({
                                        { "sas1-0623.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:15f:225:90ff:fe4f:f6e6"; };
                                        { "sas1-0907.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:15d:225:90ff:fe83:1892"; };
                                        { "sas1-1297.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:163:225:90ff:fe92:4a1a"; };
                                        { "sas1-1302.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:163:225:90ff:fe94:2ac8"; };
                                        { "sas1-1325.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:615:922b:34ff:fecf:3a92"; };
                                        { "sas1-1673.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:615:922b:34ff:fecf:3fc2"; };
                                        { "sas1-1764.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:61a:922b:34ff:fecf:22ac"; };
                                        { "sas1-2253.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:624:922b:34ff:fecf:4176"; };
                                        { "sas1-2349.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:627:922b:34ff:fecf:3094"; };
                                        { "sas1-2522.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:60b:225:90ff:fe83:1aca"; };
                                        { "sas1-2694.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:608:225:90ff:fe83:17d0"; };
                                        { "sas1-2801.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:60c:225:90ff:fe83:129c"; };
                                        { "sas1-4159.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:674:96de:80ff:fe81:1372"; };
                                        { "sas1-4184.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:63a:96de:80ff:fe81:ad2"; };
                                        { "sas1-4518.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:639:96de:80ff:fe81:102e"; };
                                        { "sas1-4781.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:63b:96de:80ff:fe81:100a"; };
                                        { "sas1-5000.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:642:96de:80ff:fe81:1684"; };
                                        { "sas1-5281.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:644:96de:80ff:fe81:984"; };
                                        { "sas1-5283.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:644:96de:80ff:fe81:1600"; };
                                        { "sas1-5678.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:6a8:76d4:35ff:fe62:eb84"; };
                                        { "sas1-5713.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:660:76d4:35ff:fe62:ea36"; };
                                        { "sas1-5733.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:662:76d4:35ff:fec4:22de"; };
                                        { "sas1-5757.search.yandex.net"; 7332; 352.000; "2a02:6b8:b000:1a2:76d4:35ff:fe62:eb00"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "5s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                  }; -- balancer2
                                }; -- report
                              }; -- assistant_sas
                              assistant_man = {
                                weight = 1.000;
                                report = {
                                  uuid = "requests_assistant_to_man";
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
                                    weighted2 = {
                                      slow_reply_time = "1s";
                                      correction_params = {
                                        max_weight = 5.000;
                                        min_weight = 0.050;
                                        history_time = "100s";
                                        feedback_time = "300s";
                                        plus_diff_per_sec = 0.050;
                                        minus_diff_per_sec = 0.100;
                                      }; -- correction_params
                                      unpack(gen_proxy_backends({
                                        { "man1-0391.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7d88"; };
                                        { "man1-1463.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b350"; };
                                        { "man1-2254.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:d610"; };
                                        { "man1-2953.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:edc0"; };
                                        { "man1-3955.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:6074:92e2:baff:fea1:7a44"; };
                                        { "man1-4531.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:602d:92e2:baff:fe55:f4f8"; };
                                        { "man1-4648.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:607f:e61d:2dff:fe6d:e8e0"; };
                                        { "man1-4649.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:607f:e61d:2dff:fe6d:e6f0"; };
                                        { "man1-4717.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:9790"; };
                                        { "man1-4737.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:8e50"; };
                                        { "man1-5720.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:605b:e61d:2dff:fe03:4940"; };
                                        { "man1-5879.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:6059:f652:14ff:fef5:d070"; };
                                        { "man1-6118.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:3e10"; };
                                        { "man1-6264.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5360"; };
                                        { "man1-8296.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:650d:215:b2ff:fea9:6526"; };
                                        { "man1-8298.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:650d:215:b2ff:fea9:638e"; };
                                        { "man1-8301.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:650d:215:b2ff:fea9:657a"; };
                                        { "man1-8302.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:650d:215:b2ff:fea9:6456"; };
                                        { "man1-8412.search.yandex.net"; 7332; 444.000; "2a02:6b8:b000:6080:e61d:2dff:fe6d:a600"; };
                                      }, {
                                        resolve_timeout = "10ms";
                                        connect_timeout = "100ms";
                                        backend_timeout = "5s";
                                        fail_on_5xx = true;
                                        http_backend = true;
                                        buffering = false;
                                        keepalive_count = 0;
                                        need_resolve = true;
                                      }))
                                    }; -- weighted2
                                  }; -- balancer2
                                }; -- report
                              }; -- assistant_man
                              assistant_devnull = {
                                weight = -1.000;
                                errordocument = {
                                  status = 204;
                                  force_conn_close = false;
                                }; -- errordocument
                              }; -- assistant_devnull
                            }; -- rr
                          }; -- balancer2
                        }; -- shared
                      }; -- default
                    }; -- regexp
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- int_assistant
            int_jsonproxy = {
              priority = 4;
              match_fsm = {
                URI = "/(search/)?jsonproxy(/.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "jsonproxy";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                rewrite = {
                  actions = {
                    {
                      global = false;
                      literal = false;
                      rewrite = "/%2";
                      case_insensitive = false;
                      regexp = "/(m)?search/(.*)";
                    };
                  }; -- actions
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
                              balancer2 = {
                                by_name_policy = {
                                  name = get_geo("antirobot_", "random");
                                  simple_policy = {};
                                }; -- by_name_policy
                                attempts = 1;
                                rr = {
                                  antirobot_man = {
                                    weight = 1.000;
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 2;
                                      hashing = {
                                        unpack(gen_proxy_backends({
                                          { "man1-0234.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6030:92e2:baff:fe74:7b88"; };
                                          { "man1-0313.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:bd84"; };
                                          { "man1-0401.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f06"; };
                                          { "man1-0510.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7dc4"; };
                                          { "man1-0619.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7ada"; };
                                          { "man1-0673.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602e:92e2:baff:fe6e:b630"; };
                                          { "man1-0679.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6003:92e2:baff:fe74:7bbe"; };
                                          { "man1-0694.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7a2e"; };
                                          { "man1-0805.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:602c:92e2:baff:fe6e:bd34"; };
                                          { "man1-0877.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f1ea"; };
                                          { "man1-3249.search.yandex.net"; 13512; 1293.000; "2a02:6b8:b000:6000:e61d:2dff:fe6d:bb30"; };
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
                                  }; -- antirobot_man
                                  antirobot_sas = {
                                    weight = 1.000;
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 2;
                                      hashing = {
                                        unpack(gen_proxy_backends({
                                          { "sas1-0670.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:154:225:90ff:fe83:1800"; };
                                          { "sas1-0980.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:153:225:90ff:fe83:aa2"; };
                                          { "sas1-2218.search.yandex.net"; 13512; 343.000; "2a02:6b8:b000:66a:225:90ff:fe94:1792"; };
                                          { "sas2-4686.search.yandex.net"; 13512; 435.000; "2a02:6b8:b000:665:225:90ff:fe92:894a"; };
                                          { "sas2-8870.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:5cc:0:604:9092:8666"; };
                                          { "sas2-8992.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:792:0:604:9094:2fb4"; };
                                          { "sas2-8993.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:781:0:604:9094:1530"; };
                                          { "sas2-9021.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b11:0:604:90c2:a40a"; };
                                          { "sas2-9033.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:783:0:604:9094:138e"; };
                                          { "sas2-9036.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:781:0:604:9094:1748"; };
                                          { "sas2-9189.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bb94"; };
                                          { "sas2-9190.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:3436:7284"; };
                                          { "sas2-9191.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2ce6"; };
                                          { "sas2-9192.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:3fe4"; };
                                          { "sas2-9193.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2436"; };
                                          { "sas2-9194.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:35f8"; };
                                          { "sas2-9195.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bdea"; };
                                          { "sas2-9196.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:badc"; };
                                          { "sas2-9197.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bf90"; };
                                          { "sas2-9198.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:e87e"; };
                                          { "sas2-9199.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:e962"; };
                                          { "sas2-9200.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:ea60"; };
                                          { "sas2-9201.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:354b:5cef"; };
                                          { "sas2-9202.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:bef0"; };
                                          { "sas2-9203.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dd16"; };
                                          { "sas2-9204.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:b96e"; };
                                          { "sas2-9205.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:354b:5b07"; };
                                          { "sas2-9206.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:bc22"; };
                                          { "sas2-9207.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2730"; };
                                          { "sas2-9208.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:ea16"; };
                                          { "sas2-9209.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2238"; };
                                          { "sas2-9210.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:d9e4"; };
                                          { "sas2-9211.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dd5c"; };
                                          { "sas2-9212.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2f8c"; };
                                          { "sas2-9213.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bf66"; };
                                          { "sas2-9214.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34c8:caf6"; };
                                          { "sas2-9215.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808e:7a1f"; };
                                          { "sas2-9216.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:354b:5b3f"; };
                                          { "sas2-9217.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:b90a"; };
                                          { "sas2-9218.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2df2"; };
                                          { "sas2-9219.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808e:7cce"; };
                                          { "sas2-9220.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2dca"; };
                                          { "sas2-9221.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:808e:7c06"; };
                                          { "sas2-9222.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:c018"; };
                                          { "sas2-9223.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:3c3e"; };
                                          { "sas2-9224.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:d98c"; };
                                          { "sas2-9225.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:2b10"; };
                                          { "sas2-9226.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:c006"; };
                                          { "sas2-9227.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34bb:baf8"; };
                                          { "sas2-9228.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:325e"; };
                                          { "sas2-9229.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:b93e"; };
                                          { "sas2-9230.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:2454"; };
                                          { "sas2-9231.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bb86"; };
                                          { "sas2-9232.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b21:0:604:34cf:2cfc"; };
                                          { "sas2-9233.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34bb:bb4e"; };
                                          { "sas2-9234.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:34cf:20ca"; };
                                          { "sas2-9235.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:e562"; };
                                          { "sas2-9236.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:34cf:31e2"; };
                                          { "sas2-9237.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:bf4c"; };
                                          { "sas2-9238.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:d902"; };
                                          { "sas2-9239.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:dc34"; };
                                          { "sas2-9240.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:d7a8"; };
                                          { "sas2-9241.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:c0ce"; };
                                          { "sas2-9242.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:ea3a"; };
                                          { "sas2-9243.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b36:0:604:808c:c016"; };
                                          { "sas2-9244.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:bf48"; };
                                          { "sas2-9245.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:8089:7456"; };
                                          { "sas2-9246.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b20:0:604:808c:e4ea"; };
                                          { "sas2-9247.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b35:0:604:808c:e964"; };
                                          { "sas2-9411.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b34:0:604:9083:1856"; };
                                          { "sas2-9412.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:b34:0:604:9083:17e4"; };
                                          { "sas2-9528.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:726:0:604:90c1:d270"; };
                                          { "sas2-9530.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:530:0:604:34cf:2336"; };
                                          { "sas2-9532.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:7d8:0:604:90c6:2eac"; };
                                          { "sas2-9533.search.yandex.net"; 13512; 435.000; "2a02:6b8:c02:7d8:0:604:90c6:2c94"; };
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
                                  }; -- antirobot_sas
                                  antirobot_vla = {
                                    weight = 1.000;
                                    balancer2 = {
                                      unique_policy = {};
                                      attempts = 2;
                                      hashing = {
                                        unpack(gen_proxy_backends({
                                          { "vla1-1343.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:44:0:604:db7:a0b2"; };
                                          { "vla1-1797.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:45:0:604:db7:a64b"; };
                                          { "vla1-2571.search.yandex.net"; 13512; 2042.000; "2a02:6b8:c0e:a2:0:604:db7:9b9d"; };
                                          { "vla1-3568.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:a1:0:604:db7:a2db"; };
                                          { "vla1-3679.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:87:0:604:db7:ab81"; };
                                          { "vla1-3709.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:72:0:604:db7:a71b"; };
                                          { "vla1-3710.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a836"; };
                                          { "vla1-3716.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:72:0:604:db7:a5c6"; };
                                          { "vla1-3863.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:aa08"; };
                                          { "vla1-3965.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:87:0:604:db7:aba1"; };
                                          { "vla1-4006.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a92c"; };
                                          { "vla1-4025.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:7f:0:604:db7:a3a5"; };
                                          { "vla1-4041.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8c:0:604:db7:abf2"; };
                                          { "vla1-4114.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:aab9"; };
                                          { "vla1-4117.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8a:0:604:db7:a817"; };
                                          { "vla1-4119.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8a:0:604:db7:a978"; };
                                          { "vla1-4130.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:a8d6"; };
                                          { "vla1-4153.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9b:0:604:db7:aa91"; };
                                          { "vla1-4167.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9c:0:604:db7:a8e5"; };
                                          { "vla1-4168.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:97:0:604:db7:a7a3"; };
                                          { "vla1-4177.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9b:0:604:db7:aa6b"; };
                                          { "vla1-4183.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:7c:0:604:db7:9df2"; };
                                          { "vla1-4192.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:8c:0:604:db7:ab53"; };
                                          { "vla1-4200.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:a82b"; };
                                          { "vla1-4321.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:80:0:604:db7:a842"; };
                                          { "vla1-4344.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:90:0:604:db7:ab5b"; };
                                          { "vla1-4354.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:99:0:604:db7:aa94"; };
                                          { "vla1-4406.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:95:0:604:db7:a9f9"; };
                                          { "vla1-4472.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:9c:0:604:db7:aa71"; };
                                          { "vla1-4553.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:77:0:604:d8f:eb26"; };
                                          { "vla1-4554.search.yandex.net"; 13512; 400.000; "2a02:6b8:c0e:77:0:604:d8f:eb76"; };
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
                                  }; -- antirobot_vla
                                }; -- rr
                              }; -- balancer2
                            }; -- report
                          }; -- checker
                          module = {
                            threshold = {
                              lo_bytes = 1048576;
                              hi_bytes = 1048576;
                              recv_timeout = "1s";
                              pass_timeout = "9s";
                              balancer2 = {
                                unique_policy = {};
                                attempts = 3;
                                attempts_file = "./controls/jsonproxy.attempts";
                                rr = {
                                  weights_file = "./controls/traffic_control.weights";
                                  jsonsearch_vla = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_jsonproxy_to_vla";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      shared = {
                                        uuid = "194350345336799448";
                                      }; -- shared
                                    }; -- report
                                  }; -- jsonsearch_vla
                                  jsonsearch_sas = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_jsonproxy_to_sas";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      shared = {
                                        uuid = "7140142177935784013";
                                      }; -- shared
                                    }; -- report
                                  }; -- jsonsearch_sas
                                  jsonsearch_man = {
                                    weight = 1.000;
                                    report = {
                                      uuid = "requests_jsonproxy_to_man";
                                      ranges = get_str_var("default_ranges");
                                      just_storage = false;
                                      disable_robotness = true;
                                      disable_sslness = true;
                                      events = {
                                        stats = "report";
                                      }; -- events
                                      shared = {
                                        uuid = "2823079698365554590";
                                      }; -- shared
                                    }; -- report
                                  }; -- jsonsearch_man
                                  jsonsearch_devnull = {
                                    weight = -1.000;
                                    errordocument = {
                                      status = 204;
                                      force_conn_close = false;
                                    }; -- errordocument
                                  }; -- jsonsearch_devnull
                                }; -- rr
                              }; -- balancer2
                            }; -- threshold
                          }; -- module
                        }; -- antirobot
                      }; -- cutter
                    }; -- h100
                  }; -- hasher
                }; -- rewrite
              }; -- report
            }; -- int_jsonproxy
            int_jsonsearch = {
              priority = 3;
              match_fsm = {
                URI = "/(((m)?search/)?(suggest|brosearch|geobase_search|onewizard|logverifier)|msearch/jsonsearch|jsonproxy|mobilesearch/vps|(((search|mobilesearch)/)?searchapi))(/.*)?|/mobilesearch/userhistory/api/(.*)?";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "jsonsearch";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/jsonsearch.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control.weights";
                        jsonsearch_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_jsonsearch_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "194350345336799448";
                              balancer2 = {
                                unique_policy = {};
                                attempts = 2;
                                weighted2 = {
                                  slow_reply_time = "1s";
                                  correction_params = {
                                    max_weight = 5.000;
                                    min_weight = 0.050;
                                    history_time = "100s";
                                    feedback_time = "300s";
                                    plus_diff_per_sec = 0.050;
                                    minus_diff_per_sec = 0.100;
                                  }; -- correction_params
                                  unpack(gen_proxy_backends({
                                    { "vla1-0014.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:9f:0:604:5cf5:bd7f"; };
                                    { "vla1-0019.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:9f:0:604:5cf4:8c5d"; };
                                    { "vla1-0040.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:4f:0:604:5cf4:8eff"; };
                                    { "vla1-0068.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:4f:0:604:5cf5:b1c0"; };
                                    { "vla1-0116.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:1:0:604:db6:1a1a"; };
                                    { "vla1-0222.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:9e:0:604:db7:a83b"; };
                                    { "vla1-0326.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:78:0:604:db7:a949"; };
                                    { "vla1-0336.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:78:0:604:db7:a752"; };
                                    { "vla1-0344.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:78:0:604:db7:a7ad"; };
                                    { "vla1-0562.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:67:0:604:db7:a31d"; };
                                    { "vla1-0593.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:44:0:604:db7:9f5b"; };
                                    { "vla1-0721.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:53:0:604:db7:9d1d"; };
                                    { "vla1-0737.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:13:0:604:db7:999e"; };
                                    { "vla1-0789.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:13:0:604:db7:9b11"; };
                                    { "vla1-0848.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:48:0:604:db7:a208"; };
                                    { "vla1-0968.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:27:0:604:db7:9c9c"; };
                                    { "vla1-0985.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:81:0:604:db7:a8c3"; };
                                    { "vla1-1143.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:91:0:604:db7:aab8"; };
                                    { "vla1-1159.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:82:0:604:db7:a7d2"; };
                                    { "vla1-1364.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:4c:0:604:db7:a0ba"; };
                                    { "vla1-1467.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:57:0:604:db7:a5ec"; };
                                    { "vla1-1538.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:53:0:604:db7:9d66"; };
                                    { "vla1-1753.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:88:0:604:db7:a916"; };
                                    { "vla1-2038.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:34:0:604:db7:9d82"; };
                                    { "vla1-2154.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:54:0:604:db7:a593"; };
                                    { "vla1-2342.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:86:0:604:db7:aab5"; };
                                    { "vla1-2439.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:95:0:604:db7:a9ce"; };
                                    { "vla1-2819.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:32:0:604:db7:a534"; };
                                    { "vla1-3021.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:73:0:604:db7:a6ea"; };
                                    { "vla1-3713.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:87:0:604:db7:aba8"; };
                                    { "vla1-3977.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:9b:0:604:db7:a7c4"; };
                                    { "vla1-5739.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:12c:0:604:5e18:d94"; };
                                    { "vla1-5978.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:133:0:604:5e19:449a"; };
                                    { "vla1-6018.search.yandex.net"; 7308; 65.000; "2a02:6b8:c0e:121:0:604:dbc:a0e7"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- shared
                          }; -- report
                        }; -- jsonsearch_vla
                        jsonsearch_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_jsonsearch_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "7140142177935784013";
                              balancer2 = {
                                unique_policy = {};
                                attempts = 2;
                                weighted2 = {
                                  slow_reply_time = "1s";
                                  correction_params = {
                                    max_weight = 5.000;
                                    min_weight = 0.050;
                                    history_time = "100s";
                                    feedback_time = "300s";
                                    plus_diff_per_sec = 0.050;
                                    minus_diff_per_sec = 0.100;
                                  }; -- correction_params
                                  unpack(gen_proxy_backends({
                                    { "sas1-0420.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:158:225:90ff:fe83:2f2"; };
                                    { "sas1-0613.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:144:225:90ff:fe82:ffc6"; };
                                    { "sas1-0623.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:15f:225:90ff:fe4f:f6e6"; };
                                    { "sas1-0907.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:15d:225:90ff:fe83:1892"; };
                                    { "sas1-1297.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:163:225:90ff:fe92:4a1a"; };
                                    { "sas1-1302.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:163:225:90ff:fe94:2ac8"; };
                                    { "sas1-1673.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:615:922b:34ff:fecf:3fc2"; };
                                    { "sas1-1764.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:61a:922b:34ff:fecf:22ac"; };
                                    { "sas1-2243.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:184:96de:80ff:fe8c:bcfa"; };
                                    { "sas1-2253.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:624:922b:34ff:fecf:4176"; };
                                    { "sas1-2349.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:627:922b:34ff:fecf:3094"; };
                                    { "sas1-2522.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:60b:225:90ff:fe83:1aca"; };
                                    { "sas1-2694.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:608:225:90ff:fe83:17d0"; };
                                    { "sas1-2801.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:60c:225:90ff:fe83:129c"; };
                                    { "sas1-4170.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:63a:96de:80ff:fe81:166c"; };
                                    { "sas1-4184.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:63a:96de:80ff:fe81:ad2"; };
                                    { "sas1-4518.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:639:96de:80ff:fe81:102e"; };
                                    { "sas1-4781.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:63b:96de:80ff:fe81:100a"; };
                                    { "sas1-5281.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:644:96de:80ff:fe81:984"; };
                                    { "sas1-5283.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:644:96de:80ff:fe81:1600"; };
                                    { "sas1-5678.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:6a8:76d4:35ff:fe62:eb84"; };
                                    { "sas1-5713.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:660:76d4:35ff:fe62:ea36"; };
                                    { "sas1-5733.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:662:76d4:35ff:fec4:22de"; };
                                    { "sas1-5757.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:1a2:76d4:35ff:fe62:eb00"; };
                                    { "sas1-6159.search.yandex.net"; 7308; 86.000; "2a02:6b8:b000:682:225:90ff:fe94:2bfe"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- shared
                          }; -- report
                        }; -- jsonsearch_sas
                        jsonsearch_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_jsonsearch_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "2823079698365554590";
                              balancer2 = {
                                unique_policy = {};
                                attempts = 2;
                                weighted2 = {
                                  slow_reply_time = "1s";
                                  correction_params = {
                                    max_weight = 5.000;
                                    min_weight = 0.050;
                                    history_time = "100s";
                                    feedback_time = "300s";
                                    plus_diff_per_sec = 0.050;
                                    minus_diff_per_sec = 0.100;
                                  }; -- correction_params
                                  unpack(gen_proxy_backends({
                                    { "man1-0618.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7a56"; };
                                    { "man1-1463.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:b350"; };
                                    { "man1-2254.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:d610"; };
                                    { "man1-2271.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:d4e0"; };
                                    { "man1-2953.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:edc0"; };
                                    { "man1-3951.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6074:92e2:baff:fea1:77bc"; };
                                    { "man1-4444.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7d64"; };
                                    { "man1-4649.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:607f:e61d:2dff:fe6d:e6f0"; };
                                    { "man1-4737.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6046:e61d:2dff:fe00:8e50"; };
                                    { "man1-5720.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:605b:e61d:2dff:fe03:4940"; };
                                    { "man1-5879.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6059:f652:14ff:fef5:d070"; };
                                    { "man1-6118.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:3e10"; };
                                    { "man1-6264.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5360"; };
                                    { "man1-7249.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6066:e61d:2dff:fe04:2bd0"; };
                                    { "man1-8412.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6080:e61d:2dff:fe6d:a600"; };
                                    { "man1-8889.search.yandex.net"; 7308; 100.000; "2a02:6b8:b000:6064:e61d:2dff:fe03:3730"; };
                                  }, {
                                    resolve_timeout = "10ms";
                                    connect_timeout = "100ms";
                                    backend_timeout = "5s";
                                    fail_on_5xx = true;
                                    http_backend = true;
                                    buffering = false;
                                    keepalive_count = 0;
                                    need_resolve = true;
                                  }))
                                }; -- weighted2
                              }; -- balancer2
                            }; -- shared
                          }; -- report
                        }; -- jsonsearch_man
                        jsonsearch_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- jsonsearch_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- int_jsonsearch
            int_candidate = {
              priority = 2;
              match_fsm = {
                URI = "/candidate_keys";
                case_insensitive = true;
                surround = false;
              }; -- match_fsm
              report = {
                uuid = "candidate";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  balancer2 = {
                    unique_policy = {};
                    attempts = 3;
                    attempts_file = "./controls/candidate.attempts";
                    rr = {
                      weights_file = "./controls/traffic_control_localgeo.weights";
                      webatom_vla = {
                        weight = 1.000;
                        report = {
                          uuid = "requests_candidate_to_vla";
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
                            weighted2 = {
                              slow_reply_time = "1s";
                              correction_params = {
                                max_weight = 5.000;
                                min_weight = 0.050;
                                history_time = "100s";
                                feedback_time = "300s";
                                plus_diff_per_sec = 0.050;
                                minus_diff_per_sec = 0.100;
                              }; -- correction_params
                              unpack(gen_proxy_backends({
                                { "vla1-0025.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:9f:0:604:5cf4:93c9"; };
                                { "vla1-0070.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:4f:0:604:5cf4:8a79"; };
                                { "vla1-0240.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:81:0:604:db7:a845"; };
                                { "vla1-0758.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:34:0:604:db7:9f10"; };
                                { "vla1-1307.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:78:0:604:db7:ab2b"; };
                                { "vla1-1880.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:13:0:604:db7:9b9c"; };
                                { "vla1-2361.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:87:0:604:db7:a81f"; };
                                { "vla1-2455.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:94:0:604:db7:a9ca"; };
                                { "vla1-2525.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:73:0:604:d8f:eaf9"; };
                                { "vla1-2778.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:31:0:604:db7:99e7"; };
                                { "vla1-2851.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:25:0:604:db7:9bc4"; };
                                { "vla1-3012.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:6b:0:604:db7:a730"; };
                                { "vla1-3053.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:21:0:604:db7:9ecd"; };
                                { "vla1-3065.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:11:0:604:db7:995a"; };
                                { "vla1-3079.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:74:0:604:db7:9a3f"; };
                                { "vla1-3122.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:3d:0:604:db7:997b"; };
                                { "vla1-3148.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:16:0:604:db7:9a4c"; };
                                { "vla1-3326.search.yandex.net"; 10260; 744.000; "2a02:6b8:c0e:70:0:604:db7:a22f"; };
                              }, {
                                resolve_timeout = "10ms";
                                connect_timeout = "100ms";
                                backend_timeout = "5s";
                                fail_on_5xx = true;
                                http_backend = true;
                                buffering = false;
                                keepalive_count = 0;
                                need_resolve = true;
                              }))
                            }; -- weighted2
                          }; -- balancer2
                        }; -- report
                      }; -- webatom_vla
                      webatom_sas = {
                        weight = 1.000;
                        report = {
                          uuid = "requests_candidate_to_sas";
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
                            weighted2 = {
                              slow_reply_time = "1s";
                              correction_params = {
                                max_weight = 5.000;
                                min_weight = 0.050;
                                history_time = "100s";
                                feedback_time = "300s";
                                plus_diff_per_sec = 0.050;
                                minus_diff_per_sec = 0.100;
                              }; -- correction_params
                              unpack(gen_proxy_backends({
                                { "sas1-0178.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:14e:225:90ff:fe83:44e"; };
                                { "sas1-0227.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:150:225:90ff:fe4f:f6d4"; };
                                { "sas1-0242.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:151:225:90ff:fe83:992"; };
                                { "sas1-0249.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:151:225:90ff:fe83:b24"; };
                                { "sas1-0252.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:151:225:90ff:fe83:a7a"; };
                                { "sas1-0258.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:150:225:90ff:fe83:b52"; };
                                { "sas1-0291.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:15b:225:90ff:fe83:1396"; };
                                { "sas1-0310.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:15a:225:90ff:fe83:9d2"; };
                                { "sas1-0418.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:15d:225:90ff:fe4f:f704"; };
                                { "sas1-0458.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:158:225:90ff:fe83:366"; };
                                { "sas1-0463.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:158:225:90ff:fe83:5b8"; };
                                { "sas1-0464.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:14c:225:90ff:fe83:a68"; };
                                { "sas1-0536.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:152:225:90ff:fe83:202"; };
                                { "sas1-0543.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:152:225:90ff:fe83:236"; };
                                { "sas1-0546.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:152:225:90ff:fe83:20a"; };
                                { "sas1-0575.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:145:225:90ff:fe83:a66"; };
                                { "sas1-0598.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:15e:225:90ff:fe4f:c6ba"; };
                                { "sas1-0991.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:14c:225:90ff:fe83:1af4"; };
                                { "sas1-1456.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:615:922b:34ff:fecf:3cde"; };
                                { "sas1-1523.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:65b:922b:34ff:fecf:3a6c"; };
                                { "sas1-1661.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:184:96de:80ff:fe8c:b7c2"; };
                                { "sas1-1889.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:17d:922b:34ff:fecf:27f8"; };
                                { "sas1-1898.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:e57a"; };
                                { "sas1-1908.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:be88"; };
                                { "sas1-1928.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:17c:96de:80ff:fe8c:b810"; };
                                { "sas1-1943.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:17c:96de:80ff:fe8c:bb56"; };
                                { "sas1-1961.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:17c:96de:80ff:fe8c:b9aa"; };
                                { "sas1-1980.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:17b:96de:80ff:fe8c:e972"; };
                                { "sas1-4820.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:619:96de:80ff:fe81:146c"; };
                                { "sas1-5026.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:654:96de:80ff:fe81:ada"; };
                                { "sas1-5078.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:1a9:96de:80ff:fe5e:dc44"; };
                                { "sas1-5350.search.yandex.net"; 10260; 916.000; "2a02:6b8:b000:648:96de:80ff:fe81:1650"; };
                              }, {
                                resolve_timeout = "10ms";
                                connect_timeout = "100ms";
                                backend_timeout = "5s";
                                fail_on_5xx = true;
                                http_backend = true;
                                buffering = false;
                                keepalive_count = 0;
                                need_resolve = true;
                              }))
                            }; -- weighted2
                          }; -- balancer2
                        }; -- report
                      }; -- webatom_sas
                      webatom_man = {
                        weight = 1.000;
                        report = {
                          uuid = "requests_candidate_to_man";
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
                            weighted2 = {
                              slow_reply_time = "1s";
                              correction_params = {
                                max_weight = 5.000;
                                min_weight = 0.050;
                                history_time = "100s";
                                feedback_time = "300s";
                                plus_diff_per_sec = 0.050;
                                minus_diff_per_sec = 0.100;
                              }; -- correction_params
                              unpack(gen_proxy_backends({
                                { "man1-0068.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:602e:92e2:baff:fe74:785e"; };
                                { "man1-0069.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7cea"; };
                                { "man1-0070.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:602e:92e2:baff:fe74:7d30"; };
                                { "man1-0087.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6033:92e2:baff:fe74:7964"; };
                                { "man1-0116.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:602e:92e2:baff:fe6f:7f0a"; };
                                { "man1-0131.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6035:92e2:baff:fe6f:7dc4"; };
                                { "man1-0133.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6035:92e2:baff:fe6f:8116"; };
                                { "man1-0196.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6030:92e2:baff:fe6f:81a4"; };
                                { "man1-0270.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6034:92e2:baff:fe74:77d0"; };
                                { "man1-0271.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7ce0"; };
                                { "man1-0327.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:602b:92e2:baff:fe74:7c7e"; };
                                { "man1-0349.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6036:92e2:baff:fe74:79b6"; };
                                { "man1-0350.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6036:92e2:baff:fe6f:7f34"; };
                                { "man1-0528.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e1e"; };
                                { "man1-0638.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6031:92e2:baff:fe74:7c6a"; };
                                { "man1-0670.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6033:92e2:baff:fe6f:7d96"; };
                                { "man1-0724.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6034:92e2:baff:fe6f:7e62"; };
                                { "man1-0731.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6034:92e2:baff:fe74:7800"; };
                                { "man1-0788.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:602e:92e2:baff:fe75:4812"; };
                                { "man1-1405.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6013:e61d:2dff:fe04:4620"; };
                                { "man1-1663.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6027:92e2:baff:fe55:f2e8"; };
                                { "man1-2050.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2150"; };
                                { "man1-2765.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:f680"; };
                                { "man1-2833.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:f6b0"; };
                                { "man1-2875.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:f1e0"; };
                                { "man1-3979.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6074:92e2:baff:fea1:735c"; };
                                { "man1-8053.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6072:e61d:2dff:fe6d:bc90"; };
                                { "man1-8139.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fe20"; };
                                { "man1-8149.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6072:e61d:2dff:fe6d:a640"; };
                                { "man1-8181.search.yandex.net"; 10260; 744.000; "2a02:6b8:b000:6072:e61d:2dff:fe6c:fb50"; };
                              }, {
                                resolve_timeout = "10ms";
                                connect_timeout = "100ms";
                                backend_timeout = "5s";
                                fail_on_5xx = true;
                                http_backend = true;
                                buffering = false;
                                keepalive_count = 0;
                                need_resolve = true;
                              }))
                            }; -- weighted2
                          }; -- balancer2
                        }; -- report
                      }; -- webatom_man
                      webatom_devnull = {
                        weight = -1.000;
                        errordocument = {
                          status = 204;
                          force_conn_close = false;
                        }; -- errordocument
                      }; -- webatom_devnull
                    }; -- rr
                  }; -- balancer2
                }; -- threshold
              }; -- report
            }; -- int_candidate
            default = {
              priority = 1;
              report = {
                uuid = "default";
                ranges = get_str_var("default_ranges");
                just_storage = false;
                disable_robotness = true;
                disable_sslness = true;
                events = {
                  stats = "report";
                }; -- events
                threshold = {
                  lo_bytes = 1048576;
                  hi_bytes = 1048576;
                  recv_timeout = "1s";
                  pass_timeout = "9s";
                  rewrite = {
                    actions = {
                      {
                        global = false;
                        literal = false;
                        rewrite = "/%2";
                        case_insensitive = false;
                        regexp = "/(m)?search/(.*)";
                      };
                    }; -- actions
                    balancer2 = {
                      unique_policy = {};
                      attempts = 3;
                      attempts_file = "./controls/default.attempts";
                      rr = {
                        weights_file = "./controls/traffic_control.weights";
                        jsonsearch_vla = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_default_to_vla";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "194350345336799448";
                            }; -- shared
                          }; -- report
                        }; -- jsonsearch_vla
                        jsonsearch_sas = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_default_to_sas";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "7140142177935784013";
                            }; -- shared
                          }; -- report
                        }; -- jsonsearch_sas
                        jsonsearch_man = {
                          weight = 1.000;
                          report = {
                            uuid = "requests_default_to_man";
                            ranges = get_str_var("default_ranges");
                            just_storage = false;
                            disable_robotness = true;
                            disable_sslness = true;
                            events = {
                              stats = "report";
                            }; -- events
                            shared = {
                              uuid = "2823079698365554590";
                            }; -- shared
                          }; -- report
                        }; -- jsonsearch_man
                        jsonsearch_devnull = {
                          weight = -1.000;
                          errordocument = {
                            status = 204;
                            force_conn_close = false;
                          }; -- errordocument
                        }; -- jsonsearch_devnull
                      }; -- rr
                    }; -- balancer2
                  }; -- rewrite
                }; -- threshold
              }; -- report
            }; -- default
          }; -- regexp
        }; -- shared
      }; -- http
    }; -- fake_section
  }; -- ipdispatch
}