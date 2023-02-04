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


function get_random_timedelta(start, end_, unit)
  return math.random(start, end_) .. unit;
end


function get_str_var(name, default)
  return _G[name] or default
end


instance = {
  workers = 2;
  buffer = 65536;
  maxconn = 4000;
  tcp_fastopen = 0;
  enable_reuse_port = true;
  private_address = "127.0.0.10";
  default_tcp_rst_on_error = true;
  events = {
    stats = "report";
  }; -- events
  dns_ttl = get_random_timedelta(600, 900, "s");
  reset_dns_cache_file = "./controls/reset_dns_cache_file";
  log = get_log_path("childs_log", 15350, "/place/db/www/logs");
  admin_addrs = {
    {
      port = 15350;
      ip = "127.0.0.1";
    };
    {
      ip = "::1";
      port = 15350;
    };
  }; -- admin_addrs
  addrs = {
    {
      port = 80;
      ip = "5.255.240.36";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 80;
      ip = "2a02:6b8:0:3400::1:36";
      disabled = get_int_var("disable_external", 0);
    };
    {
      port = 15350;
      ip = get_ip_by_iproute("v4");
    };
    {
      port = 15350;
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
        15350;
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
        "5.255.240.36";
        "2a02:6b8:0:3400::1:36";
      }; -- ips
      ports = {
        80;
      }; -- ports
      shared = {
        uuid = "5365632592858049780";
        errorlog = {
          log_level = "ERROR";
          log = get_log_path("error_log", 15350, "/place/db/www/logs");
          http = {
            maxlen = 65536;
            maxreq = 65536;
            keepalive = true;
            no_keepalive_file = "./controls/keepalive_disabled";
            events = {
              stats = "report";
            }; -- events
            accesslog = {
              log = get_log_path("access_log", 15350, "/place/db/www/logs");
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
                  slbping = {
                    priority = 36;
                    match_fsm = {
                      host = "noapache\\.yandex\\.net";
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
                  sasruweb_noapache = {
                    priority = 35;
                    match_fsm = {
                      host = "sasruweb\\.noapache\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    balancer2 = {
                      unique_policy = {};
                      attempts = 5;
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
                          { "sas1-0439.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:636:225:90ff:fee5:be98"; };
                          { "sas1-0442.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:601:225:90ff:feed:30b2"; };
                          { "sas1-0469.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:635:225:90ff:feef:c914"; };
                          { "sas1-0490.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:601:225:90ff:feed:3286"; };
                          { "sas1-0510.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:636:225:90ff:feed:2f9a"; };
                          { "sas1-0517.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:601:225:90ff:feed:31a2"; };
                          { "sas1-0555.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:635:225:90ff:feed:3040"; };
                          { "sas1-1126.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61c:96de:80ff:feec:1c1e"; };
                          { "sas1-1243.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:100:225:90ff:feed:2f9c"; };
                          { "sas1-1281.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:175:225:90ff:fee7:52f6"; };
                          { "sas1-1326.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:175:225:90ff:fec4:8b42"; };
                          { "sas1-1338.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:175:225:90ff:fee7:52be"; };
                          { "sas1-1381.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:175:225:90ff:fee7:540c"; };
                          { "sas1-1390.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:601:225:90ff:feef:c9cc"; };
                          { "sas1-1393.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:174:225:90ff:feed:3036"; };
                          { "sas1-1397.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:177:feaa:14ff:fe1d:f516"; };
                          { "sas1-1405.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:cee"; };
                          { "sas1-1411.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:174:225:90ff:feed:3158"; };
                          { "sas1-1412.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:100:225:90ff:feed:30b4"; };
                          { "sas1-1416.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:174:225:90ff:feed:31aa"; };
                          { "sas1-1417.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:174:225:90ff:feed:2de6"; };
                          { "sas1-1419.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:635:225:90ff:feef:c648"; };
                          { "sas1-1427.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:618:215:b2ff:fea8:d0e"; };
                          { "sas1-1428.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:dae"; };
                          { "sas1-1437.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:d12"; };
                          { "sas1-1439.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:196:215:b2ff:fea8:d42"; };
                          { "sas1-1445.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:119:215:b2ff:fea8:d2e"; };
                          { "sas1-1447.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:dbe"; };
                          { "sas1-1452.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:625:96de:80ff:feec:1b9a"; };
                          { "sas1-1472.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:114:215:b2ff:fea8:d56"; };
                          { "sas1-1500.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:114:215:b2ff:fea8:d7e"; };
                          { "sas1-1501.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:684:215:b2ff:fea8:d16"; };
                          { "sas1-1519.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d00"; };
                          { "sas1-1536.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a6:215:b2ff:fea8:96a"; };
                          { "sas1-1547.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:628:215:b2ff:fea8:d73"; };
                          { "sas1-1562.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a6:215:b2ff:fea8:7fe"; };
                          { "sas1-1573.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:67a:215:b2ff:fea8:c12"; };
                          { "sas1-1585.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a5:215:b2ff:fea8:976"; };
                          { "sas1-1586.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:67a:215:b2ff:fea8:8944"; };
                          { "sas1-1587.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:179:215:b2ff:fea8:6d98"; };
                          { "sas1-1590.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:617:215:b2ff:fea8:d76"; };
                          { "sas1-1598.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:67a:215:b2ff:fea8:6cf0"; };
                          { "sas1-1599.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:67a:215:b2ff:fea8:6d2c"; };
                          { "sas1-1603.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cca"; };
                          { "sas1-1606.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:628:215:b2ff:fea8:d36"; };
                          { "sas1-1621.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cbe"; };
                          { "sas1-1639.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:179:215:b2ff:fea8:c82"; };
                          { "sas1-1642.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:67a:215:b2ff:fea8:c16"; };
                          { "sas1-1650.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:179:215:b2ff:fea8:c7e"; };
                          { "sas1-1666.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:179:215:b2ff:fea8:7140"; };
                          { "sas1-1676.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a0:215:b2ff:fea8:6c0c"; };
                          { "sas1-1677.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:169:215:b2ff:fea7:f8fc"; };
                          { "sas1-1684.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:179:215:b2ff:fea8:c5e"; };
                          { "sas1-1685.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:610:215:b2ff:fea8:6dc4"; };
                          { "sas1-1695.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:610:215:b2ff:fea8:6d90"; };
                          { "sas1-1697.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:197:215:b2ff:fea8:6c10"; };
                          { "sas1-1718.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:197:215:b2ff:fea8:6e01"; };
                          { "sas1-1729.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a4:215:b2ff:fea8:a6e"; };
                          { "sas1-1734.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:179:215:b2ff:fea8:6d38"; };
                          { "sas1-1736.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:17a:feaa:14ff:feab:faf4"; };
                          { "sas1-1744.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:17a:feaa:14ff:feab:f616"; };
                          { "sas1-1783.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:16e:215:b2ff:fea7:87c4"; };
                          { "sas1-1787.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12f:215:b2ff:fea8:992"; };
                          { "sas1-1789.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12c:215:b2ff:fea8:98e"; };
                          { "sas1-1800.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:68f:215:b2ff:fea8:a66"; };
                          { "sas1-1801.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a9:215:b2ff:fea8:a62"; };
                          { "sas1-1802.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:623:215:b2ff:fea8:d4a"; };
                          { "sas1-1803.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:87b0"; };
                          { "sas1-1805.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:62c:215:b2ff:fea8:d3e"; };
                          { "sas1-1806.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:62c:215:b2ff:fea8:cfa"; };
                          { "sas1-1813.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:616:215:b2ff:fea8:d86"; };
                          { "sas1-1814.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61d:215:b2ff:fea8:bde"; };
                          { "sas1-1830.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:625:96de:80ff:feec:1306"; };
                          { "sas1-1833.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ceb4"; };
                          { "sas1-1834.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:68f:215:b2ff:fea8:98a"; };
                          { "sas1-1835.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:61dc"; };
                          { "sas1-1836.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:623:215:b2ff:fea8:c8a"; };
                          { "sas1-1837.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12f:215:b2ff:fea8:bda"; };
                          { "sas1-1839.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a4:215:b2ff:fea8:6d88"; };
                          { "sas1-1921.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12c:215:b2ff:fea8:cd6"; };
                          { "sas1-1990.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:619:96de:80ff:feec:15ca"; };
                          { "sas1-2005.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:604:225:90ff:feef:c9de"; };
                          { "sas1-2013.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:63c:96de:80ff:feec:1d46"; };
                          { "sas1-2483.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a1:96de:80ff:feec:eca"; };
                          { "sas1-4219.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:635:225:90ff:feef:c966"; };
                          { "sas1-4452.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:652:96de:80ff:feec:18f2"; };
                          { "sas1-4455.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:625:96de:80ff:feec:18a8"; };
                          { "sas1-4456.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:674:96de:80ff:feec:1e42"; };
                          { "sas1-4873.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:653:96de:80ff:feec:19a2"; };
                          { "sas1-5080.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:100:225:90ff:feef:c9d0"; };
                          { "sas1-5396.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:654:96de:80ff:feec:1aee"; };
                          { "sas1-5397.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:653:96de:80ff:feec:12f6"; };
                          { "sas1-5449.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:625:96de:80ff:feec:1b94"; };
                          { "sas1-5453.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:139:96de:80ff:feec:1c80"; };
                          { "sas1-5454.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:139:96de:80ff:feec:1b52"; };
                          { "sas1-5457.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a9:96de:80ff:feec:145e"; };
                          { "sas1-5458.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:653:96de:80ff:feec:14b8"; };
                          { "sas1-5459.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61c:96de:80ff:feec:1432"; };
                          { "sas1-5460.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61c:96de:80ff:feec:ea8"; };
                          { "sas1-5461.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:111:96de:80ff:feec:1326"; };
                          { "sas1-5465.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:652:96de:80ff:feec:fd2"; };
                          { "sas1-5467.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:625:96de:80ff:feec:11c4"; };
                          { "sas1-5468.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:652:96de:80ff:feec:11c6"; };
                          { "sas1-5469.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:651:96de:80ff:feec:1216"; };
                          { "sas1-5471.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:653:96de:80ff:feec:11ce"; };
                          { "sas1-5472.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61c:96de:80ff:feec:1c30"; };
                          { "sas1-5474.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:63b:96de:80ff:feec:1014"; };
                          { "sas1-5479.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61c:96de:80ff:feec:189a"; };
                          { "sas1-5480.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1886"; };
                          { "sas1-5481.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10bc"; };
                          { "sas1-5482.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:63c:96de:80ff:feec:1948"; };
                          { "sas1-5483.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:619:96de:80ff:feec:1882"; };
                          { "sas1-5484.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:674:96de:80ff:feec:1a82"; };
                          { "sas1-5489.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61c:96de:80ff:feec:170a"; };
                          { "sas1-5490.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:63c:96de:80ff:feec:17fe"; };
                          { "sas1-5492.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:654:96de:80ff:fee9:d4"; };
                          { "sas1-5494.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:619:96de:80ff:feec:1d8c"; };
                          { "sas1-5496.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:666:96de:80ff:feec:1edc"; };
                          { "sas1-5497.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61c:96de:80ff:feec:1bb4"; };
                          { "sas1-5500.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:656:96de:80ff:feec:164c"; };
                          { "sas1-5503.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a1:96de:80ff:feec:fda"; };
                          { "sas1-5505.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:111:96de:80ff:feec:1108"; };
                          { "sas1-5509.search.yandex.net"; 9080; 192.000; "2a02:6b8:c02:53b:0:604:80ec:1a3c"; };
                          { "sas1-5510.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1ba0"; };
                          { "sas1-5511.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:651:96de:80ff:feec:1ba4"; };
                          { "sas1-5512.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:63d:96de:80ff:feec:18c8"; };
                          { "sas1-5515.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:674:96de:80ff:feec:18d8"; };
                          { "sas1-5519.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:639:96de:80ff:feec:130c"; };
                          { "sas1-5520.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:655:96de:80ff:feec:190c"; };
                          { "sas1-5521.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:656:96de:80ff:feec:191c"; };
                          { "sas1-5522.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:139:96de:80ff:feec:1a4c"; };
                          { "sas1-5523.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:63b:96de:80ff:feec:1848"; };
                          { "sas1-5626.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:160:225:90ff:fee6:481e"; };
                          { "sas1-5627.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:163:225:90ff:fee6:49be"; };
                          { "sas1-5628.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:163:225:90ff:fee6:4868"; };
                          { "sas1-5630.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:163:225:90ff:fee6:46fc"; };
                          { "sas1-5631.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:622:225:90ff:fee6:491c"; };
                          { "sas1-5632.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:160:225:90ff:fee6:4ca6"; };
                          { "sas1-5634.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:160:225:90ff:fee6:47fe"; };
                          { "sas1-5635.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:163:225:90ff:fee6:4822"; };
                          { "sas1-5636.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:622:225:90ff:fee6:486e"; };
                          { "sas1-5637.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:161:225:90ff:fee6:486c"; };
                          { "sas1-5639.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:161:225:90ff:fee5:c298"; };
                          { "sas1-5640.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:622:225:90ff:fee5:bbd0"; };
                          { "sas1-5641.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:622:225:90ff:fee4:2e26"; };
                          { "sas1-5644.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:161:225:90ff:fee6:48aa"; };
                          { "sas1-5646.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:602:225:90ff:fee8:7c0e"; };
                          { "sas1-5647.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:602:225:90ff:fee6:49b0"; };
                          { "sas1-5651.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:160:225:90ff:fee6:482e"; };
                          { "sas1-5652.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:160:225:90ff:fee6:4b3e"; };
                          { "sas1-5653.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:163:225:90ff:fee6:4864"; };
                          { "sas1-5654.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:602:225:90ff:fee6:4816"; };
                          { "sas1-5655.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:649:225:90ff:fee5:beb4"; };
                          { "sas1-5656.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:649:225:90ff:fee8:7bbc"; };
                          { "sas1-5661.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:622:225:90ff:fee6:4a46"; };
                          { "sas1-5663.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:649:225:90ff:fee6:ddc0"; };
                          { "sas1-5669.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:636:225:90ff:feed:318c"; };
                          { "sas1-5715.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:635:225:90ff:feed:3064"; };
                          { "sas1-5732.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:636:225:90ff:feef:cb98"; };
                          { "sas1-5892.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:601:225:90ff:feec:2f44"; };
                          { "sas1-5959.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:604:225:90ff:feef:c9da"; };
                          { "sas1-5961.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:604:225:90ff:feef:c974"; };
                          { "sas1-5962.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:604:225:90ff:feef:c96a"; };
                          { "sas1-5964.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:604:225:90ff:feeb:f9b0"; };
                          { "sas1-6100.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:164:428d:5cff:fe34:f33a"; };
                          { "sas1-6161.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:164:428d:5cff:fe34:f98a"; };
                          { "sas1-6171.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:164:428d:5cff:fe34:f8fa"; };
                          { "sas1-6240.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:118:215:b2ff:fea7:76c0"; };
                          { "sas1-6357.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:69f:215:b2ff:fea7:827c"; };
                          { "sas1-6398.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:115:215:b2ff:fea7:7874"; };
                          { "sas1-6406.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:7ed0"; };
                          { "sas1-6539.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:8010"; };
                          { "sas1-6626.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:119:215:b2ff:fea7:7be0"; };
                          { "sas1-6634.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:66c:215:b2ff:fea7:7e34"; };
                          { "sas1-6720.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:11e:215:b2ff:fea7:8c38"; };
                          { "sas1-6730.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a924"; };
                          { "sas1-6735.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:8d9c"; };
                          { "sas1-6743.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:a9dc"; };
                          { "sas1-6757.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:66c:215:b2ff:fea7:a9b0"; };
                          { "sas1-6777.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:6fe4"; };
                          { "sas1-6785.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:abb4"; };
                          { "sas1-6802.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:11f:215:b2ff:fea7:7f2c"; };
                          { "sas1-6805.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:62c:215:b2ff:fea7:b778"; };
                          { "sas1-6851.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:11f:215:b2ff:fea7:8d20"; };
                          { "sas1-6857.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:62c:215:b2ff:fea7:8d04"; };
                          { "sas1-6861.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:11f:215:b2ff:fea7:a9b4"; };
                          { "sas1-6869.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:62c:215:b2ff:fea7:a9d8"; };
                          { "sas1-6925.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12f:215:b2ff:fea7:bb18"; };
                          { "sas1-6935.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12f:215:b2ff:fea7:823c"; };
                          { "sas1-6938.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:123:215:b2ff:fea7:b3a4"; };
                          { "sas1-7005.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:16e:215:b2ff:fea7:abdc"; };
                          { "sas1-7013.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:16e:215:b2ff:fea7:ade4"; };
                          { "sas1-7039.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:120:215:b2ff:fea7:9054"; };
                          { "sas1-7060.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:120:215:b2ff:fea7:ad08"; };
                          { "sas1-7094.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8fc4"; };
                          { "sas1-7138.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8ea4"; };
                          { "sas1-7142.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:83cc"; };
                          { "sas1-7154.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aab0"; };
                          { "sas1-7167.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12c:215:b2ff:fea7:73b4"; };
                          { "sas1-7169.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12c:215:b2ff:fea7:73ac"; };
                          { "sas1-7192.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:122:215:b2ff:fea7:7d40"; };
                          { "sas1-7209.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:122:215:b2ff:fea7:8f24"; };
                          { "sas1-7212.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:122:215:b2ff:fea7:90e0"; };
                          { "sas1-7234.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:121:215:b2ff:fea7:abcc"; };
                          { "sas1-7276.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:128:215:b2ff:fea7:7250"; };
                          { "sas1-7458.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:7fe4"; };
                          { "sas1-7465.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:125:215:b2ff:fea7:817c"; };
                          { "sas1-7475.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:610:215:b2ff:fea7:8bf4"; };
                          { "sas1-7477.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:129:215:b2ff:fea7:8b6c"; };
                          { "sas1-7488.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:80a8"; };
                          { "sas1-7534.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:125:215:b2ff:fea7:7430"; };
                          { "sas1-7569.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:129:215:b2ff:fea7:bc08"; };
                          { "sas1-7591.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:129:215:b2ff:fea7:b990"; };
                          { "sas1-7686.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bf0c"; };
                          { "sas1-7687.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bec8"; };
                          { "sas1-7696.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bddc"; };
                          { "sas1-7727.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:127:215:b2ff:fea7:af04"; };
                          { "sas1-7732.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:127:215:b2ff:fea7:ba58"; };
                          { "sas1-7761.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:73f4"; };
                          { "sas1-7763.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b6e0"; };
                          { "sas1-7790.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b054"; };
                          { "sas1-7803.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:b56c"; };
                          { "sas1-7816.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:8cdc"; };
                          { "sas1-7868.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12e:215:b2ff:fea7:aa10"; };
                          { "sas1-7869.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12e:215:b2ff:fea7:77d4"; };
                          { "sas1-7878.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12e:215:b2ff:fea7:7db8"; };
                          { "sas1-7902.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab88"; };
                          { "sas1-7908.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:8c34"; };
                          { "sas1-7936.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:618:215:b2ff:fea7:8b98"; };
                          { "sas1-7942.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7444"; };
                          { "sas1-8160.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:132:215:b2ff:fea7:afec"; };
                          { "sas1-8179.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:132:215:b2ff:fea7:8b9c"; };
                          { "sas1-8186.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:68f:215:b2ff:fea7:9178"; };
                          { "sas1-8214.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:68f:215:b2ff:fea7:a980"; };
                          { "sas1-8234.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:131:215:b2ff:fea7:b3d4"; };
                          { "sas1-8255.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:6a6:215:b2ff:fea7:b1d0"; };
                          { "sas1-8299.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:132:215:b2ff:fea7:b754"; };
                          { "sas1-8301.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:132:215:b2ff:fea7:b750"; };
                          { "sas1-8357.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:133:215:b2ff:fea7:af7c"; };
                          { "sas1-8369.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:623:215:b2ff:fea7:b2c8"; };
                          { "sas1-8430.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:130:215:b2ff:fea7:aeb8"; };
                          { "sas1-8518.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:170:428d:5cff:fe37:fffe"; };
                          { "sas1-8524.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:617:215:b2ff:fea7:b984"; };
                          { "sas1-8573.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:135:215:b2ff:fea7:bcd8"; };
                          { "sas1-8588.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:142:428d:5cff:fe36:8b26"; };
                          { "sas1-8596.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:142:428d:5cff:fe36:8ac8"; };
                          { "sas1-8612.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:684:215:b2ff:fea7:bf10"; };
                          { "sas1-8615.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:684:215:b2ff:fea7:b834"; };
                          { "sas1-8635.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:134:215:b2ff:fea7:b83c"; };
                          { "sas1-8637.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:134:215:b2ff:fea7:b840"; };
                          { "sas1-8638.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:134:215:b2ff:fea7:b988"; };
                          { "sas1-8674.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:616:215:b2ff:fea7:bc54"; };
                          { "sas1-8694.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:137:215:b2ff:fea7:b640"; };
                          { "sas1-8701.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:137:215:b2ff:fea7:b330"; };
                          { "sas1-8730.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:616:215:b2ff:fea7:8dec"; };
                          { "sas1-8742.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:616:215:b2ff:fea7:b2a4"; };
                          { "sas1-8746.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:616:215:b2ff:fea7:b3ac"; };
                          { "sas1-8826.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1a6:215:b2ff:fea7:90f8"; };
                          { "sas1-8865.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:138:215:b2ff:fea7:bae4"; };
                          { "sas1-8932.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:141:feaa:14ff:fede:3f12"; };
                          { "sas1-8945.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:1ac:feaa:14ff:fede:3f0e"; };
                          { "sas1-8948.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:13f:feaa:14ff:fede:3ef0"; };
                          { "sas1-8960.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:13f:feaa:14ff:fede:4050"; };
                          { "sas1-8989.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:613:feaa:14ff:fede:4125"; };
                          { "sas1-9038.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:143:feaa:14ff:fede:427a"; };
                          { "sas1-9055.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:13e:feaa:14ff:fede:3f68"; };
                          { "sas1-9058.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:13e:feaa:14ff:fede:3f2e"; };
                          { "sas1-9061.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:13e:feaa:14ff:fede:41b0"; };
                          { "sas1-9085.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:143:feaa:14ff:fede:438a"; };
                          { "sas1-9142.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3fc2"; };
                          { "sas1-9151.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:143:feaa:14ff:fede:422c"; };
                          { "sas1-9164.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:143:feaa:14ff:fede:417c"; };
                          { "sas1-9218.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:143:feaa:14ff:fede:45bc"; };
                          { "sas1-9222.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:13c:feaa:14ff:fede:4040"; };
                          { "sas1-9247.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:143:feaa:14ff:fede:401a"; };
                          { "sas1-9282.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:143:feaa:14ff:fede:45f4"; };
                          { "sas1-9283.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:143:feaa:14ff:fede:3eb8"; };
                          { "sas1-9361.search.yandex.net"; 9080; 192.000; "2a02:6b8:b000:13a:feaa:14ff:fede:4322"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "10s";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- sasruweb_noapache
                  sasruvideo_noapache = {
                    priority = 34;
                    match_fsm = {
                      host = "sasruvideo\\.noapache\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    balancer2 = {
                      unique_policy = {};
                      attempts = 5;
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
                          { "sas1-0554.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:636:225:90ff:feed:2f90"; };
                          { "sas1-0582.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:601:225:90ff:feed:2e46"; };
                          { "sas1-1040.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:100:225:90ff:feef:ccba"; };
                          { "sas1-1077.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3130"; };
                          { "sas1-1346.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:174:225:90ff:feed:317c"; };
                          { "sas1-1413.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:174:225:90ff:feed:2d94"; };
                          { "sas1-1425.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:128:215:b2ff:fea8:db2"; };
                          { "sas1-1429.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:110:215:b2ff:fea8:d1a"; };
                          { "sas1-1453.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:119:215:b2ff:fea7:efac"; };
                          { "sas1-1488.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:134:215:b2ff:fea8:966"; };
                          { "sas1-1525.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:135:215:b2ff:fea8:d7a"; };
                          { "sas1-1572.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:642:96de:80ff:fe5e:dc42"; };
                          { "sas1-1643.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:653:96de:80ff:fe5e:dca0"; };
                          { "sas1-1664.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:11d:215:b2ff:fea8:6c84"; };
                          { "sas1-1671.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:117:215:b2ff:fea8:d52"; };
                          { "sas1-1758.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:de48"; };
                          { "sas1-1759.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:ba4a"; };
                          { "sas1-1809.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:67c:922b:34ff:fecf:2efa"; };
                          { "sas1-1815.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:67c:96de:80ff:fe8c:bd34"; };
                          { "sas1-1832.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:120:215:b2ff:fea8:a6a"; };
                          { "sas1-1880.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:e984"; };
                          { "sas1-1885.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:da12"; };
                          { "sas1-1915.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:17c:96de:80ff:fe8c:e432"; };
                          { "sas1-1979.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:17b:96de:80ff:fe8c:d9a8"; };
                          { "sas1-2046.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:67b:96de:80ff:fe8c:d8e6"; };
                          { "sas1-5450.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:625:96de:80ff:feec:16da"; };
                          { "sas1-5452.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1a40"; };
                          { "sas1-5470.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:654:96de:80ff:feec:10a4"; };
                          { "sas1-5504.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:63d:96de:80ff:feec:1ecc"; };
                          { "sas1-5514.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:63a:96de:80ff:feec:18ce"; };
                          { "sas1-5517.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:669:96de:80ff:feec:1902"; };
                          { "sas1-5624.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:160:225:90ff:fee6:4b3c"; };
                          { "sas1-5625.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:163:225:90ff:fee6:4d14"; };
                          { "sas1-5633.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:622:225:90ff:fee6:4860"; };
                          { "sas1-5638.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:602:225:90ff:fee5:bd36"; };
                          { "sas1-5642.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:622:225:90ff:fee5:c134"; };
                          { "sas1-5643.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:161:225:90ff:fee6:de52"; };
                          { "sas1-5645.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:161:225:90ff:fee5:bf6c"; };
                          { "sas1-5648.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:602:225:90ff:fee6:de28"; };
                          { "sas1-5650.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:602:225:90ff:fee6:de54"; };
                          { "sas1-5659.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:649:225:90ff:fee6:dd7c"; };
                          { "sas1-5660.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:649:225:90ff:fee6:df02"; };
                          { "sas1-5665.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:636:225:90ff:feed:2de4"; };
                          { "sas1-5963.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:601:225:90ff:feed:2cea"; };
                          { "sas1-6750.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                          { "sas1-6971.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:123:215:b2ff:fea7:b10c"; };
                          { "sas1-7168.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:122:215:b2ff:fea7:7c88"; };
                          { "sas1-7574.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                          { "sas1-7747.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                          { "sas1-7824.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                          { "sas1-8236.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                          { "sas1-8246.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                          { "sas1-8401.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                          { "sas1-8546.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                          { "sas1-8571.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                          { "sas1-8618.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                          { "sas1-8830.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "10s";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- sasruvideo_noapache
                  sasruimgs_noapache = {
                    priority = 33;
                    match_fsm = {
                      host = "sasruimgs\\.noapache\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    balancer2 = {
                      unique_policy = {};
                      attempts = 5;
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
                          { "sas1-1099.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:666:96de:80ff:feec:1c28"; };
                          { "sas1-1352.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:618:215:b2ff:fea8:d3a"; };
                          { "sas1-1370.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d8c"; };
                          { "sas1-1376.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:db6"; };
                          { "sas1-1383.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:a3a"; };
                          { "sas1-1422.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:6d3c"; };
                          { "sas1-1424.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:121:215:b2ff:fea8:cea"; };
                          { "sas1-1426.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:196:215:b2ff:fea8:dba"; };
                          { "sas1-1433.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:112:215:b2ff:fea8:6dc8"; };
                          { "sas1-1440.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:d2a"; };
                          { "sas1-1774.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:625:96de:80ff:feec:198a"; };
                          { "sas1-1958.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:652:96de:80ff:feec:1c24"; };
                          { "sas1-2200.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:654:96de:80ff:feec:186a"; };
                          { "sas1-4461.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:651:96de:80ff:feec:1830"; };
                          { "sas1-5360.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1b82"; };
                          { "sas1-5451.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:1a9:96de:80ff:feec:f98"; };
                          { "sas1-5455.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:652:96de:80ff:feec:1ec4"; };
                          { "sas1-5456.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1924"; };
                          { "sas1-5462.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:63d:96de:80ff:feec:1316"; };
                          { "sas1-5463.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1002"; };
                          { "sas1-5464.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6a1:96de:80ff:feec:171c"; };
                          { "sas1-5466.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:625:96de:80ff:feec:120e"; };
                          { "sas1-5473.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:61c:96de:80ff:feec:11f4"; };
                          { "sas1-5475.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:139:96de:80ff:feec:fcc"; };
                          { "sas1-5476.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:651:96de:80ff:feec:10cc"; };
                          { "sas1-5477.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:652:96de:80ff:feec:18a2"; };
                          { "sas1-5478.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:61c:96de:80ff:feec:1898"; };
                          { "sas1-5485.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:63a:96de:80ff:feec:1a5e"; };
                          { "sas1-5486.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:653:96de:80ff:feec:1876"; };
                          { "sas1-5487.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:653:96de:80ff:feec:1124"; };
                          { "sas1-5488.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:61c:96de:80ff:fee9:1aa"; };
                          { "sas1-5491.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:619:96de:80ff:feec:17de"; };
                          { "sas1-5493.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:653:96de:80ff:fee9:fd"; };
                          { "sas1-5495.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:63c:96de:80ff:feec:157c"; };
                          { "sas1-5498.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:139:96de:80ff:fee9:1de"; };
                          { "sas1-5499.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:139:96de:80ff:fee9:184"; };
                          { "sas1-5501.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:655:96de:80ff:feec:1624"; };
                          { "sas1-5502.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10fe"; };
                          { "sas1-5506.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:655:96de:80ff:feec:1a38"; };
                          { "sas1-5507.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:656:96de:80ff:feec:1a32"; };
                          { "sas1-5508.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:674:96de:80ff:feec:1b9c"; };
                          { "sas1-5513.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:111:96de:80ff:feec:1ba8"; };
                          { "sas1-5516.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:655:96de:80ff:feec:18fc"; };
                          { "sas1-5518.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1cc0"; };
                          { "sas1-5629.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:163:225:90ff:fee6:4d12"; };
                          { "sas1-5649.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:602:225:90ff:fee6:4c7a"; };
                          { "sas1-5657.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:649:225:90ff:fee6:dd88"; };
                          { "sas1-5662.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:649:225:90ff:fee5:bde0"; };
                          { "sas1-5960.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:636:225:90ff:feef:c964"; };
                          { "sas1-5965.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:604:225:90ff:feeb:fba6"; };
                          { "sas1-5966.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:635:225:90ff:feeb:fd9e"; };
                          { "sas1-5967.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:601:225:90ff:feeb:fcd6"; };
                          { "sas1-5968.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:604:225:90ff:feeb:fba2"; };
                          { "sas1-5969.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:601:225:90ff:feeb:f9b8"; };
                          { "sas1-5970.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:100:225:90ff:feeb:fce2"; };
                          { "sas1-5971.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:604:225:90ff:feed:2b8e"; };
                          { "sas1-5972.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:174:225:90ff:feed:2b4e"; };
                          { "sas1-5973.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:100:225:90ff:fee3:9cf2"; };
                          { "sas1-5974.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:100:225:90ff:feed:2fde"; };
                          { "sas1-6351.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:11a:215:b2ff:fea7:8280"; };
                          { "sas1-6752.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:9008"; };
                          { "sas1-6893.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:11d:215:b2ff:fea7:9060"; };
                          { "sas1-6939.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b324"; };
                          { "sas1-6978.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:123:215:b2ff:fea7:b720"; };
                          { "sas1-7095.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:124:215:b2ff:fea7:ab98"; };
                          { "sas1-7098.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:124:215:b2ff:fea7:8ee4"; };
                          { "sas1-7125.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:907c"; };
                          { "sas1-7155.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:122:215:b2ff:fea7:8bf0"; };
                          { "sas1-7156.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aae4"; };
                          { "sas1-7238.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:121:215:b2ff:fea7:ac10"; };
                          { "sas1-7272.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:121:215:b2ff:fea7:8300"; };
                          { "sas1-7286.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:128:215:b2ff:fea7:7aec"; };
                          { "sas1-7287.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:128:215:b2ff:fea7:7910"; };
                          { "sas1-7326.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:126:215:b2ff:fea7:ad8c"; };
                          { "sas1-7330.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:628:215:b2ff:fea7:90d0"; };
                          { "sas1-7331.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc9c"; };
                          { "sas1-7459.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8cd0"; };
                          { "sas1-7494.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:125:215:b2ff:fea7:6590"; };
                          { "sas1-7498.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:125:215:b2ff:fea7:aaa4"; };
                          { "sas1-7825.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:7fbc"; };
                          { "sas1-7843.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:82f0"; };
                          { "sas1-7929.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:12b:215:b2ff:fea7:ac24"; };
                          { "sas1-8873.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:138:215:b2ff:fea7:ba80"; };
                          { "sas1-8979.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3f9e"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "10s";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- sasruimgs_noapache
                  manruweb_noapache = {
                    priority = 32;
                    match_fsm = {
                      host = "manruweb\\.noapache\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    balancer2 = {
                      unique_policy = {};
                      attempts = 5;
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
                          { "man1-0433.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602e:92e2:baff:fe74:79d6"; };
                          { "man1-0686.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f98"; };
                          { "man1-0926.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:600e:92e2:baff:fe56:ea26"; };
                          { "man1-0934.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f3ee"; };
                          { "man1-0976.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:600f:92e2:baff:fe56:e9de"; };
                          { "man1-0977.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:600f:92e2:baff:fe56:e8f6"; };
                          { "man1-1165.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6021:f652:14ff:fe55:1d70"; };
                          { "man1-1173.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f7ba"; };
                          { "man1-1212.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601a:f652:14ff:fe55:2a70"; };
                          { "man1-1215.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6005:f652:14ff:fe55:1d80"; };
                          { "man1-1216.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602a:f652:14ff:fe55:2a50"; };
                          { "man1-1220.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601b:f652:14ff:fe55:1d30"; };
                          { "man1-1221.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6014:f652:14ff:fe48:9980"; };
                          { "man1-1222.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6014:f652:14ff:fe48:9960"; };
                          { "man1-1223.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6014:f652:14ff:fe48:8ce0"; };
                          { "man1-1284.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5270"; };
                          { "man1-1285.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6000:f652:14ff:fe44:5620"; };
                          { "man1-1287.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6029:f652:14ff:fe44:51b0"; };
                          { "man1-1288.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6039:f652:14ff:fe44:5630"; };
                          { "man1-1290.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6021:f652:14ff:fe48:a360"; };
                          { "man1-1291.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601f:f652:14ff:fe48:a310"; };
                          { "man1-1292.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601f:f652:14ff:fe44:5930"; };
                          { "man1-1295.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6016:f652:14ff:fe48:9f70"; };
                          { "man1-1306.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6013:f652:14ff:fe44:5120"; };
                          { "man1-1307.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6013:f652:14ff:fe48:8f50"; };
                          { "man1-1323.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d880"; };
                          { "man1-1324.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e730"; };
                          { "man1-1325.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6026:f652:14ff:fe8b:de30"; };
                          { "man1-1358.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:dfb0"; };
                          { "man1-1366.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:f0f0"; };
                          { "man1-1394.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:b340"; };
                          { "man1-1395.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:b830"; };
                          { "man1-1397.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:a610"; };
                          { "man1-1398.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b820"; };
                          { "man1-1447.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:d5a0"; };
                          { "man1-1449.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6020:f652:14ff:fe8b:e710"; };
                          { "man1-1451.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6023:f652:14ff:fe8b:f2e0"; };
                          { "man1-1456.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6026:f652:14ff:fe8b:e780"; };
                          { "man1-1457.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601e:f652:14ff:fe8b:e640"; };
                          { "man1-1458.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e740"; };
                          { "man1-1459.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6097:f652:14ff:fe8b:e700"; };
                          { "man1-1461.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:e610"; };
                          { "man1-1462.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:de20"; };
                          { "man1-1488.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:ac00"; };
                          { "man1-1489.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:b310"; };
                          { "man1-1490.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:abc0"; };
                          { "man1-1492.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:ac40"; };
                          { "man1-1764.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b120"; };
                          { "man1-1875.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:1d30"; };
                          { "man1-2199.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:c070"; };
                          { "man1-2290.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6097:f652:14ff:fe8c:d290"; };
                          { "man1-2572.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6022:f652:14ff:fe8c:1bc0"; };
                          { "man1-2964.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:e7b0"; };
                          { "man1-3014.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6028:f652:14ff:fe55:28b0"; };
                          { "man1-3086.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602a:f652:14ff:fe55:2ba0"; };
                          { "man1-3185.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6097:f652:14ff:fe8c:2a90"; };
                          { "man1-3374.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:d130"; };
                          { "man1-3397.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c6b0"; };
                          { "man1-3491.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6031:92e2:baff:fe75:4b24"; };
                          { "man1-3497.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6034:92e2:baff:fe74:78a0"; };
                          { "man1-3501.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602c:92e2:baff:fe74:7984"; };
                          { "man1-3502.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f37a"; };
                          { "man1-3503.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f4b4"; };
                          { "man1-3504.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f628"; };
                          { "man1-3505.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6011:92e2:baff:fe55:f29c"; };
                          { "man1-3506.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6011:92e2:baff:fe55:f442"; };
                          { "man1-3508.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:600e:92e2:baff:fe5b:974a"; };
                          { "man1-3509.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f68c"; };
                          { "man1-3511.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f4bc"; };
                          { "man1-3513.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6010:92e2:baff:fe5b:9dc4"; };
                          { "man1-3514.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f6ee"; };
                          { "man1-3516.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f4ee"; };
                          { "man1-3525.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e0b0"; };
                          { "man1-3531.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b550"; };
                          { "man1-3532.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:cd70"; };
                          { "man1-3536.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:b6f0"; };
                          { "man1-3538.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:21f0"; };
                          { "man1-3539.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:21e0"; };
                          { "man1-3540.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:2c30"; };
                          { "man1-3545.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6019:f652:14ff:fe8c:2390"; };
                          { "man1-3546.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2630"; };
                          { "man1-3547.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2cb0"; };
                          { "man1-3548.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:2250"; };
                          { "man1-3549.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:15e0"; };
                          { "man1-3550.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6011:f652:14ff:fe8c:14e0"; };
                          { "man1-3551.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:2c0"; };
                          { "man1-3553.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601f:f652:14ff:fe8c:21c0"; };
                          { "man1-3554.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6020:f652:14ff:fe8c:1940"; };
                          { "man1-3555.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6020:f652:14ff:fe8c:1390"; };
                          { "man1-3556.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:660"; };
                          { "man1-3557.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6020:e61d:2dff:fe6c:f420"; };
                          { "man1-3558.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:2030"; };
                          { "man1-3559.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:21a0"; };
                          { "man1-3560.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:20e0"; };
                          { "man1-3561.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1f70"; };
                          { "man1-3562.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:2070"; };
                          { "man1-3563.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:1ac0"; };
                          { "man1-3564.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2060"; };
                          { "man1-3565.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2090"; };
                          { "man1-3566.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f490"; };
                          { "man1-3568.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f65c"; };
                          { "man1-3570.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1ca0"; };
                          { "man1-3571.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1840"; };
                          { "man1-3572.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1780"; };
                          { "man1-3573.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6001:f652:14ff:fe8c:16e0"; };
                          { "man1-3574.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6001:f652:14ff:fe8c:1810"; };
                          { "man1-3575.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6037:f652:14ff:fe8c:2da0"; };
                          { "man1-3576.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602a:f652:14ff:fe8c:17c0"; };
                          { "man1-3577.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:ff30"; };
                          { "man1-3578.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:e340"; };
                          { "man1-3580.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602a:f652:14ff:fe8b:fd70"; };
                          { "man1-3581.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:fe40"; };
                          { "man1-3582.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:fdd0"; };
                          { "man1-3583.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6004:f652:14ff:fe8c:1720"; };
                          { "man1-3584.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:fe50"; };
                          { "man1-3585.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:fd50"; };
                          { "man1-3588.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:fda0"; };
                          { "man1-3589.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6039:f652:14ff:fe8c:1790"; };
                          { "man1-3590.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:fd60"; };
                          { "man1-3591.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:17f0"; };
                          { "man1-3593.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:fe60"; };
                          { "man1-3594.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:17e0"; };
                          { "man1-3595.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6000:f652:14ff:fe8c:1cc0"; };
                          { "man1-3596.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:4a0"; };
                          { "man1-3597.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:e290"; };
                          { "man1-3601.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1250"; };
                          { "man1-3602.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1240"; };
                          { "man1-3603.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1830"; };
                          { "man1-3604.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:1220"; };
                          { "man1-3606.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:5e0"; };
                          { "man1-3607.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:2bf0"; };
                          { "man1-3608.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6023:f652:14ff:fe8b:fce0"; };
                          { "man1-3609.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:2bd0"; };
                          { "man1-3610.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:db10"; };
                          { "man1-3611.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:e8c0"; };
                          { "man1-3612.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:eef0"; };
                          { "man1-3613.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:da90"; };
                          { "man1-3614.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7d2a"; };
                          { "man1-3615.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6002:92e2:baff:fe74:7a66"; };
                          { "man1-3616.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6002:92e2:baff:fe74:78b6"; };
                          { "man1-3657.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6002:92e2:baff:fe75:4b96"; };
                          { "man1-3691.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603c:92e2:baff:fe74:776e"; };
                          { "man1-3692.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:be1a"; };
                          { "man1-3693.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7b22"; };
                          { "man1-3694.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7e12"; };
                          { "man1-3695.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7960"; };
                          { "man1-3696.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7f6a"; };
                          { "man1-3697.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7e30"; };
                          { "man1-3698.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7a8c"; };
                          { "man1-3699.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603a:92e2:baff:fe74:779e"; };
                          { "man1-3700.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7b70"; };
                          { "man1-3701.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:b7e8"; };
                          { "man1-3703.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602d:92e2:baff:fe6b:d10e"; };
                          { "man1-3704.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602d:92e2:baff:fe74:7f82"; };
                          { "man1-3705.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602d:92e2:baff:fe6b:d230"; };
                          { "man1-3707.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603c:92e2:baff:fe74:78e8"; };
                          { "man1-3709.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7c62"; };
                          { "man1-3746.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603d:92e2:baff:fe6f:8136"; };
                          { "man1-3769.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603d:92e2:baff:fe74:777a"; };
                          { "man1-3773.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603d:92e2:baff:fe6f:7efe"; };
                          { "man1-3842.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:bd22"; };
                          { "man1-3843.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603d:92e2:baff:fe74:795e"; };
                          { "man1-3855.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7bc6"; };
                          { "man1-3883.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6079:92e2:baff:fea1:763c"; };
                          { "man1-3911.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603e:92e2:baff:fe74:79a8"; };
                          { "man1-3916.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:c006"; };
                          { "man1-3925.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7638"; };
                          { "man1-3927.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607d:92e2:baff:fea1:765c"; };
                          { "man1-3932.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7a7c"; };
                          { "man1-3933.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e80"; };
                          { "man1-3936.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7a7e"; };
                          { "man1-3967.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603e:92e2:baff:fe74:78da"; };
                          { "man1-3977.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f66"; };
                          { "man1-4015.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e38"; };
                          { "man1-4016.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7a84"; };
                          { "man1-4031.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607b:92e2:baff:fe74:7ca4"; };
                          { "man1-4032.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7a82"; };
                          { "man1-4033.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e32"; };
                          { "man1-4034.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e42"; };
                          { "man1-4035.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7650"; };
                          { "man1-4037.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603c:92e2:baff:fe75:475a"; };
                          { "man1-4040.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7622"; };
                          { "man1-4041.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7a86"; };
                          { "man1-4043.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7e40"; };
                          { "man1-4065.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7f60"; };
                          { "man1-4070.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7fce"; };
                          { "man1-4071.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7e3e"; };
                          { "man1-4072.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7e4a"; };
                          { "man1-4075.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6044:92e2:baff:fe74:774e"; };
                          { "man1-4079.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6042:92e2:baff:fe74:7628"; };
                          { "man1-4145.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7640"; };
                          { "man1-4163.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7eca"; };
                          { "man1-4176.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7ee0"; };
                          { "man1-4242.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6043:92e2:baff:fe6f:7e8e"; };
                          { "man1-4253.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:ba7e"; };
                          { "man1-4277.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:bea8"; };
                          { "man1-4339.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7a40"; };
                          { "man1-4365.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603f:92e2:baff:fe56:e9c2"; };
                          { "man1-4407.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:b8bc"; };
                          { "man1-4420.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603f:92e2:baff:fe74:79c6"; };
                          { "man1-4421.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603f:92e2:baff:fe74:77ce"; };
                          { "man1-4423.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:b90c"; };
                          { "man1-4425.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603f:92e2:baff:fe74:775a"; };
                          { "man1-4428.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:603f:92e2:baff:fe6f:7f10"; };
                          { "man1-4455.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6044:92e2:baff:fe74:77c2"; };
                          { "man1-4456.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98a"; };
                          { "man1-4467.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6041:92e2:baff:fe6e:bfae"; };
                          { "man1-4468.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6041:92e2:baff:fe74:78d8"; };
                          { "man1-4471.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7e42"; };
                          { "man1-4473.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7ed4"; };
                          { "man1-4477.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7be6"; };
                          { "man1-4480.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b990"; };
                          { "man1-4481.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bafa"; };
                          { "man1-4486.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d22"; };
                          { "man1-4498.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7730"; };
                          { "man1-4503.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f24"; };
                          { "man1-4504.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6041:92e2:baff:fe74:761c"; };
                          { "man1-4505.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6041:92e2:baff:fe75:4886"; };
                          { "man1-4520.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602d:92e2:baff:fe52:79ba"; };
                          { "man1-4522.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:ba44"; };
                          { "man1-4524.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602d:92e2:baff:fe6f:7d14"; };
                          { "man1-4526.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799e"; };
                          { "man1-4527.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:bda4"; };
                          { "man1-4528.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6007:92e2:baff:fe6f:7d72"; };
                          { "man1-4529.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602d:92e2:baff:fe74:7f7a"; };
                          { "man1-4530.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:602d:92e2:baff:fe74:772c"; };
                          { "man1-4550.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601d:92e2:baff:fe55:f6c0"; };
                          { "man1-4552.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a20"; };
                          { "man1-4555.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601d:f652:14ff:fe55:1cf0"; };
                          { "man1-4556.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a80"; };
                          { "man1-4557.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a10"; };
                          { "man1-4558.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2aa0"; };
                          { "man1-5080.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6051:f652:14ff:fe74:3850"; };
                          { "man1-5117.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:604c:e61d:2dff:fe00:9400"; };
                          { "man1-5232.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7762"; };
                          { "man1-5299.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:604e:e61d:2dff:fe01:ef10"; };
                          { "man1-5300.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7642"; };
                          { "man1-5304.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e3c"; };
                          { "man1-5334.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:604f:f652:14ff:fef5:d9a0"; };
                          { "man1-5351.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e3a"; };
                          { "man1-5378.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6050:f652:14ff:fef5:d130"; };
                          { "man1-5392.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6050:f652:14ff:fef5:d960"; };
                          { "man1-5400.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6050:e61d:2dff:fe03:45d0"; };
                          { "man1-5428.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6050:e61d:2dff:fe01:e540"; };
                          { "man1-5432.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7e7e"; };
                          { "man1-5482.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7a76"; };
                          { "man1-5552.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7764"; };
                          { "man1-5593.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6079:92e2:baff:fea1:763e"; };
                          { "man1-5612.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:86a0"; };
                          { "man1-5616.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6055:f652:14ff:fe74:3920"; };
                          { "man1-5637.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8f90"; };
                          { "man1-5645.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6079:92e2:baff:fea1:807e"; };
                          { "man1-5671.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e4c"; };
                          { "man1-5672.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7f64"; };
                          { "man1-5684.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6079:92e2:baff:fea1:8002"; };
                          { "man1-5694.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:605b:e61d:2dff:fe01:e790"; };
                          { "man1-5717.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7658"; };
                          { "man1-5746.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:605b:e61d:2dff:fe00:9bb0"; };
                          { "man1-5767.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6077:92e2:baff:fea1:78a6"; };
                          { "man1-5786.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6052:f652:14ff:fe74:4220"; };
                          { "man1-5807.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:47e0"; };
                          { "man1-5836.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:4fb0"; };
                          { "man1-5858.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6058:f652:14ff:fef5:d900"; };
                          { "man1-5875.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4360"; };
                          { "man1-5896.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4ca0"; };
                          { "man1-5899.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4f10"; };
                          { "man1-5900.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4f60"; };
                          { "man1-5902.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6059:e61d:2dff:fe01:ed00"; };
                          { "man1-5908.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:607b:92e2:baff:fea1:8050"; };
                          { "man1-5912.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:37c0"; };
                          { "man1-5938.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:605a:e61d:2dff:fe01:ef50"; };
                          { "man1-5966.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:605a:e61d:2dff:fe03:3a10"; };
                          { "man1-5967.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4cb0"; };
                          { "man1-5971.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:605a:e61d:2dff:fe03:3a00"; };
                          { "man1-5975.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:605a:e61d:2dff:fe00:8630"; };
                          { "man1-5978.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4e10"; };
                          { "man1-5980.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6058:e61d:2dff:fe03:3eb0"; };
                          { "man1-5981.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6058:e61d:2dff:fe03:4630"; };
                          { "man1-5984.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:81a0"; };
                          { "man1-5986.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8610"; };
                          { "man1-5994.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4b00"; };
                          { "man1-6003.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6056:f652:14ff:fef5:c920"; };
                          { "man1-6008.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:5180"; };
                          { "man1-6043.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6061:e61d:2dff:fe00:9b80"; };
                          { "man1-6084.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6051:e61d:2dff:fe03:4d00"; };
                          { "man1-6086.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6051:e61d:2dff:fe00:9ce0"; };
                          { "man1-8204.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6078:92e2:baff:fea1:804c"; };
                          { "man1-8205.search.yandex.net"; 9080; 190.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7a80"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "10s";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- manruweb_noapache
                  manruvideo_noapache = {
                    priority = 31;
                    match_fsm = {
                      host = "manruvideo\\.noapache\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    balancer2 = {
                      unique_policy = {};
                      attempts = 5;
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
                          { "man1-0978.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f404"; };
                          { "man1-1191.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6018:f652:14ff:fe48:9670"; };
                          { "man1-1209.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6028:f652:14ff:fe55:2a90"; };
                          { "man1-1282.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5280"; };
                          { "man1-1286.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6001:f652:14ff:fe44:56f0"; };
                          { "man1-1293.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6020:f652:14ff:fe48:9fa0"; };
                          { "man1-1294.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6015:f652:14ff:fe44:50f0"; };
                          { "man1-1296.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6016:f652:14ff:fe44:57c0"; };
                          { "man1-1396.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:a9f0"; };
                          { "man1-1400.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:b420"; };
                          { "man1-1446.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6020:f652:14ff:fe8b:f440"; };
                          { "man1-1448.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:f410"; };
                          { "man1-1450.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:601e:f652:14ff:fe8b:d8a0"; };
                          { "man1-1454.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:eaf0"; };
                          { "man1-1455.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:e5f0"; };
                          { "man1-1748.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6027:f652:14ff:fe8b:aea0"; };
                          { "man1-1765.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b0d0"; };
                          { "man1-1927.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:be70"; };
                          { "man1-1947.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6029:f652:14ff:fe8b:ea20"; };
                          { "man1-2252.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:dd50"; };
                          { "man1-2264.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:d620"; };
                          { "man1-2453.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:d1b0"; };
                          { "man1-2464.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6021:f652:14ff:fe55:3560"; };
                          { "man1-2529.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:110"; };
                          { "man1-2582.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:c780"; };
                          { "man1-2670.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:3d0"; };
                          { "man1-2733.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:db20"; };
                          { "man1-2800.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:f460"; };
                          { "man1-2848.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e8b0"; };
                          { "man1-2960.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:edb0"; };
                          { "man1-3507.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f3a4"; };
                          { "man1-3515.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f680"; };
                          { "man1-3519.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6014:f652:14ff:fe44:54d0"; };
                          { "man1-3522.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:ab20"; };
                          { "man1-3530.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aed0"; };
                          { "man1-3541.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:e0"; };
                          { "man1-3543.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd70"; };
                          { "man1-3552.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:fd00"; };
                          { "man1-3567.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f4a0"; };
                          { "man1-3569.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1850"; };
                          { "man1-3579.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:602a:f652:14ff:fe8b:fd90"; };
                          { "man1-3586.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:fe00"; };
                          { "man1-3587.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6037:f652:14ff:fe8c:70"; };
                          { "man1-3592.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:1cd0"; };
                          { "man1-3598.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6000:f652:14ff:fe8c:f30"; };
                          { "man1-3599.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:fe30"; };
                          { "man1-3600.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6028:f652:14ff:fe8c:1760"; };
                          { "man1-3605.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:1800"; };
                          { "man1-3617.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7d6e"; };
                          { "man1-3690.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:7d8a"; };
                          { "man1-3702.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7740"; };
                          { "man1-3706.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:81a0"; };
                          { "man1-3708.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:b980"; };
                          { "man1-3783.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7aa8"; };
                          { "man1-3874.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7bf8"; };
                          { "man1-3926.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:607a:92e2:baff:fea1:8052"; };
                          { "man1-3971.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f5c"; };
                          { "man1-4044.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:607c:92e2:baff:fea1:8054"; };
                          { "man1-4051.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:603c:92e2:baff:fe74:7d78"; };
                          { "man1-4426.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:bdb2"; };
                          { "man1-4461.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7a1c"; };
                          { "man1-4483.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7712"; };
                          { "man1-4489.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98c"; };
                          { "man1-4497.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f42"; };
                          { "man1-4521.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:602d:92e2:baff:fe52:7a94"; };
                          { "man1-4525.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799c"; };
                          { "man1-4548.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a00"; };
                          { "man1-4553.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:601d:f652:14ff:fe55:29d0"; };
                          { "man1-5346.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:607c:92e2:baff:fe6e:b8b6"; };
                          { "man1-5399.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7624"; };
                          { "man1-5503.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7e48"; };
                          { "man1-5622.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8f60"; };
                          { "man1-5826.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:4840"; };
                          { "man1-5914.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6059:e61d:2dff:fe00:8520"; };
                          { "man1-5965.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7640"; };
                          { "man1-6664.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:6047:f652:14ff:fe74:3c10"; };
                          { "man1-7451.search.yandex.net"; 9080; 160.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1f30"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "10s";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- manruvideo_noapache
                  manruimgs_noapache = {
                    priority = 30;
                    match_fsm = {
                      host = "manruimgs\\.noapache\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    balancer2 = {
                      unique_policy = {};
                      attempts = 5;
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
                          { "man1-1076.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f1f2"; };
                          { "man1-1150.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f2ca"; };
                          { "man1-1515.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b150"; };
                          { "man1-1885.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1d50"; };
                          { "man1-1957.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:16f0"; };
                          { "man1-1979.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:550"; };
                          { "man1-2023.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1640"; };
                          { "man1-2087.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e320"; };
                          { "man1-2092.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6098:f652:14ff:fe8c:1e0"; };
                          { "man1-2106.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:720"; };
                          { "man1-2112.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:560"; };
                          { "man1-2383.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6011:f652:14ff:fe55:1ca0"; };
                          { "man1-2873.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e840"; };
                          { "man1-2943.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dc40"; };
                          { "man1-3175.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:1ff0"; };
                          { "man1-3252.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1a10"; };
                          { "man1-3260.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c700"; };
                          { "man1-3261.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c6d0"; };
                          { "man1-3265.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:d4c0"; };
                          { "man1-3375.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c580"; };
                          { "man1-3479.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7bd0"; };
                          { "man1-3484.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:602f:92e2:baff:fe72:7bfa"; };
                          { "man1-3489.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e86"; };
                          { "man1-3493.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:602b:92e2:baff:fe6f:815a"; };
                          { "man1-3498.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:b6a4"; };
                          { "man1-3499.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:7eae"; };
                          { "man1-3500.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:80e8"; };
                          { "man1-3510.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f7b4"; };
                          { "man1-3512.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6f6"; };
                          { "man1-3517.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6014:f652:14ff:fe44:5250"; };
                          { "man1-3520.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:b7d0"; };
                          { "man1-3523.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d6b0"; };
                          { "man1-3524.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d710"; };
                          { "man1-3526.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:d5c0"; };
                          { "man1-3527.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:d6a0"; };
                          { "man1-3528.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aeb0"; };
                          { "man1-3529.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b010"; };
                          { "man1-3533.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cdb0"; };
                          { "man1-3534.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b590"; };
                          { "man1-3535.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cda0"; };
                          { "man1-3537.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:2220"; };
                          { "man1-3542.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd50"; };
                          { "man1-3544.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2610"; };
                          { "man1-3752.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:b9d0"; };
                          { "man1-3822.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7eaa"; };
                          { "man1-3904.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7866"; };
                          { "man1-3959.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:603e:92e2:baff:fe6f:80a2"; };
                          { "man1-4025.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:603c:92e2:baff:fe6e:ba8c"; };
                          { "man1-4073.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6040:92e2:baff:fe74:770a"; };
                          { "man1-4074.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7dc0"; };
                          { "man1-4076.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7d6c"; };
                          { "man1-4077.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bdda"; };
                          { "man1-4078.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b656"; };
                          { "man1-4080.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6042:92e2:baff:fe6f:7d86"; };
                          { "man1-4081.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b842"; };
                          { "man1-4082.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6043:92e2:baff:fe53:24d4"; };
                          { "man1-4083.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2610"; };
                          { "man1-4084.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2570"; };
                          { "man1-4085.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2434"; };
                          { "man1-4310.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7bcc"; };
                          { "man1-4311.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7dfc"; };
                          { "man1-4638.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:651d:215:b2ff:fea9:913a"; };
                          { "man1-5640.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8a30"; };
                          { "man1-6102.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4ed0"; };
                          { "man1-6134.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3d50"; };
                          { "man1-6150.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6054:f652:14ff:fef5:da20"; };
                          { "man1-6161.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3890"; };
                          { "man1-6167.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:82d0"; };
                          { "man1-6227.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6057:f652:14ff:fef5:c8b0"; };
                          { "man1-6242.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6057:f652:14ff:fef5:cb40"; };
                          { "man1-6263.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5370"; };
                          { "man1-6359.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6053:f652:14ff:fe55:29c0"; };
                          { "man1-6393.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:48f0"; };
                          { "man1-6413.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:1ad0"; };
                          { "man1-6419.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:4720"; };
                          { "man1-6485.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6063:e61d:2dff:fe04:2430"; };
                          { "man1-6634.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:604f:e61d:2dff:fe01:f370"; };
                          { "man1-6727.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cd90"; };
                          { "man1-6728.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cdd0"; };
                          { "man1-6763.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6058:e61d:2dff:fe04:50d0"; };
                          { "man1-6767.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:4bc0"; };
                          { "man1-6854.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:6060:f652:14ff:fef5:c950"; };
                          { "man1-6873.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:605e:e61d:2dff:fe04:1ed0"; };
                          { "man1-6886.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3d30"; };
                          { "man1-6900.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3da0"; };
                          { "man1-6903.search.yandex.net"; 9080; 240.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3cf0"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "10s";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- manruimgs_noapache
                  vlaruweb_noapache = {
                    priority = 29;
                    match_fsm = {
                      host = "vlaruweb\\.noapache\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    balancer2 = {
                      unique_policy = {};
                      attempts = 5;
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
                          { "vla1-0234.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9e:0:604:db7:a8b4"; };
                          { "vla1-0239.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9e:0:604:db7:9cc1"; };
                          { "vla1-0241.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:81:0:604:db7:a92f"; };
                          { "vla1-0242.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:81:0:604:db7:a7c6"; };
                          { "vla1-0244.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:81:0:604:db7:a8cc"; };
                          { "vla1-0259.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9e:0:604:db7:a8f4"; };
                          { "vla1-0262.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9e:0:604:db7:a8ee"; };
                          { "vla1-0265.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9e:0:604:db7:a8e8"; };
                          { "vla1-0270.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9e:0:604:db7:a844"; };
                          { "vla1-0273.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9e:0:604:db7:a8a9"; };
                          { "vla1-0279.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:98:0:604:db7:9ce2"; };
                          { "vla1-0281.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:98:0:604:db7:a3d0"; };
                          { "vla1-0303.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:98:0:604:db7:a3c5"; };
                          { "vla1-0318.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9e:0:604:db7:a8ab"; };
                          { "vla1-0333.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:89:0:604:db7:a865"; };
                          { "vla1-0334.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:93:0:604:db7:aaf4"; };
                          { "vla1-0343.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:93:0:604:db8:db3a"; };
                          { "vla1-0355.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9d:0:604:d8f:eb9d"; };
                          { "vla1-0362.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9d:0:604:d8f:eba0"; };
                          { "vla1-0366.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9d:0:604:d8f:eb7e"; };
                          { "vla1-0368.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9d:0:604:d8f:eb36"; };
                          { "vla1-0371.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:3e:0:604:db6:179d"; };
                          { "vla1-0376.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9d:0:604:d8f:eb81"; };
                          { "vla1-0385.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9d:0:604:d8f:eb10"; };
                          { "vla1-0398.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:12:0:604:db7:9b44"; };
                          { "vla1-0411.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:14:0:604:db7:9b46"; };
                          { "vla1-0429.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:27:0:604:db7:9f71"; };
                          { "vla1-0431.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:26:0:604:db7:9f9b"; };
                          { "vla1-0450.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:22:0:604:db7:9ea0"; };
                          { "vla1-0476.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:34:0:604:db7:9c8b"; };
                          { "vla1-0496.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:17:0:604:db7:9ab7"; };
                          { "vla1-0510.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:13:0:604:db7:9caa"; };
                          { "vla1-0514.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:1b:0:604:db7:9d41"; };
                          { "vla1-0523.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:27:0:604:db7:9f70"; };
                          { "vla1-0529.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:20:0:604:db7:9b67"; };
                          { "vla1-0546.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:17:0:604:db7:9927"; };
                          { "vla1-0569.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:52:0:604:db7:a462"; };
                          { "vla1-0575.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:54:0:604:db7:a4ca"; };
                          { "vla1-0600.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:53:0:604:db7:9cf8"; };
                          { "vla1-0655.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:1b:0:604:db7:9b85"; };
                          { "vla1-0671.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:52:0:604:db7:a4e8"; };
                          { "vla1-0672.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:13:0:604:db7:99ea"; };
                          { "vla1-0692.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:3c:0:604:db7:9ee6"; };
                          { "vla1-0706.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:53:0:604:db7:9ced"; };
                          { "vla1-0770.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:44:0:604:db7:a559"; };
                          { "vla1-0773.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:3c:0:604:db7:9ee0"; };
                          { "vla1-0973.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:43:0:604:db7:9e20"; };
                          { "vla1-1029.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:71:0:604:db7:a420"; };
                          { "vla1-1067.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:78:0:604:db7:aacf"; };
                          { "vla1-1092.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:54:0:604:db7:a5a7"; };
                          { "vla1-1192.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:81:0:604:d8f:eb20"; };
                          { "vla1-1219.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:8d:0:604:db7:aa47"; };
                          { "vla1-1226.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:43:0:604:db7:a545"; };
                          { "vla1-1277.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:52:0:604:db7:a466"; };
                          { "vla1-1314.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:54:0:604:db7:a67e"; };
                          { "vla1-1411.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:4c:0:604:db7:a0f2"; };
                          { "vla1-1521.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:44:0:604:db7:9f37"; };
                          { "vla1-1562.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:52:0:604:db7:a66a"; };
                          { "vla1-1637.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:67:0:604:db7:a2af"; };
                          { "vla1-1639.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:67:0:604:db7:a328"; };
                          { "vla1-1646.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:71:0:604:db7:a22d"; };
                          { "vla1-1668.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:44:0:604:db7:a73a"; };
                          { "vla1-1684.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:12:0:604:db7:9a66"; };
                          { "vla1-1716.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:67:0:604:db7:9cd7"; };
                          { "vla1-1728.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:57:0:604:db7:a604"; };
                          { "vla1-1774.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:71:0:604:db7:a789"; };
                          { "vla1-1818.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:45:0:604:db7:a6fa"; };
                          { "vla1-1826.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:88:0:604:db7:a9e5"; };
                          { "vla1-1828.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:8d:0:604:db7:ac33"; };
                          { "vla1-1830.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:78:0:604:db7:a780"; };
                          { "vla1-1850.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:8d:0:604:db7:aa43"; };
                          { "vla1-1881.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:71:0:604:db7:9d0c"; };
                          { "vla1-1883.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:71:0:604:db7:a775"; };
                          { "vla1-1933.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:78:0:604:db7:ab23"; };
                          { "vla1-1950.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:8d:0:604:db7:a9b5"; };
                          { "vla1-1967.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:5d:0:604:db7:9cc9"; };
                          { "vla1-1976.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:3c:0:604:db7:a450"; };
                          { "vla1-1977.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:57:0:604:db7:a565"; };
                          { "vla1-1993.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:43:0:604:db7:a0e8"; };
                          { "vla1-1999.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:8d:0:604:db7:aa1c"; };
                          { "vla1-2014.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:44:0:604:db7:a668"; };
                          { "vla1-2034.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:78:0:604:db7:a94d"; };
                          { "vla1-2076.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:13:0:604:db7:9be1"; };
                          { "vla1-2077.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:3c:0:604:db7:9d50"; };
                          { "vla1-2081.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:84:0:604:db7:aac4"; };
                          { "vla1-2111.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:13:0:604:db7:9af3"; };
                          { "vla1-2113.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:62:0:604:db7:a198"; };
                          { "vla1-2120.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:17:0:604:db7:9925"; };
                          { "vla1-2123.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:53:0:604:db7:9d4c"; };
                          { "vla1-2128.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:3c:0:604:db7:9ef1"; };
                          { "vla1-2136.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:13:0:604:db7:99ee"; };
                          { "vla1-2156.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:85:0:604:db7:abab"; };
                          { "vla1-2161.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:1b:0:604:db7:9a71"; };
                          { "vla1-2193.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:45:0:604:db7:9d62"; };
                          { "vla1-2206.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:88:0:604:db8:db38"; };
                          { "vla1-2221.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9a:0:604:db7:a9df"; };
                          { "vla1-2223.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:5d:0:604:db7:a229"; };
                          { "vla1-2292.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:5b:0:604:db7:a610"; };
                          { "vla1-2339.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:86:0:604:db7:a85f"; };
                          { "vla1-2340.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:7d:0:604:db7:a3d6"; };
                          { "vla1-2391.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:7d:0:604:db7:a19a"; };
                          { "vla1-2396.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:8b:0:604:db7:abe0"; };
                          { "vla1-2397.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:79:0:604:db7:a959"; };
                          { "vla1-2400.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:79:0:604:db7:a962"; };
                          { "vla1-2401.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:8a:0:604:db7:a977"; };
                          { "vla1-2402.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:95:0:604:db7:ab46"; };
                          { "vla1-2407.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:8b:0:604:db7:abdc"; };
                          { "vla1-2415.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:94:0:604:db7:aa3e"; };
                          { "vla1-2418.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:7c:0:604:db7:9df5"; };
                          { "vla1-2422.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:95:0:604:db7:a9c7"; };
                          { "vla1-2423.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:95:0:604:db7:ab1a"; };
                          { "vla1-2427.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:79:0:604:db7:ab19"; };
                          { "vla1-2429.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:8a:0:604:db7:a972"; };
                          { "vla1-2431.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:79:0:604:db7:a861"; };
                          { "vla1-2432.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:94:0:604:db7:a7c8"; };
                          { "vla1-2435.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:97:0:604:db7:a93e"; };
                          { "vla1-2438.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:97:0:604:db7:a922"; };
                          { "vla1-2441.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:94:0:604:db7:aaef"; };
                          { "vla1-2442.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:79:0:604:db7:a95c"; };
                          { "vla1-2448.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:97:0:604:db7:a7f3"; };
                          { "vla1-2449.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:23:0:604:db7:a189"; };
                          { "vla1-2461.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:95:0:604:db7:aa56"; };
                          { "vla1-2470.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:94:0:604:db7:a7ba"; };
                          { "vla1-2484.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:79:0:604:db7:a37d"; };
                          { "vla1-2498.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:79:0:604:db7:a95f"; };
                          { "vla1-2511.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9b:0:604:db7:aa2b"; };
                          { "vla1-2512.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:97:0:604:db7:a915"; };
                          { "vla1-2522.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a2:0:604:db7:9956"; };
                          { "vla1-2531.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a2:0:604:db7:9b6a"; };
                          { "vla1-2533.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:3d:0:604:db7:9b87"; };
                          { "vla1-2547.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:2f:0:604:db7:9bd3"; };
                          { "vla1-2549.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a2:0:604:db7:9bdf"; };
                          { "vla1-2563.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:1f:0:604:db7:9c52"; };
                          { "vla1-2589.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:1f:0:604:db7:9fa6"; };
                          { "vla1-2590.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:28:0:604:db7:9f6c"; };
                          { "vla1-2598.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:23:0:604:db7:9fc2"; };
                          { "vla1-2602.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:28:0:604:db7:9f72"; };
                          { "vla1-2606.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:32:0:604:db7:9f96"; };
                          { "vla1-2635.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:30:0:604:db7:9f22"; };
                          { "vla1-2637.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:65:0:604:db7:9ca2"; };
                          { "vla1-2651.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:1f:0:604:db7:9c53"; };
                          { "vla1-2653.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:31:0:604:db7:9909"; };
                          { "vla1-2667.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:2f:0:604:db7:9f86"; };
                          { "vla1-2669.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:30:0:604:db7:99fc"; };
                          { "vla1-2670.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:31:0:604:db7:9ac3"; };
                          { "vla1-2684.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:30:0:604:db7:9c75"; };
                          { "vla1-2704.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:2f:0:604:db7:9c82"; };
                          { "vla1-2714.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:31:0:604:db7:9940"; };
                          { "vla1-2722.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:2e:0:604:db7:9bbe"; };
                          { "vla1-2723.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:32:0:604:db7:a52f"; };
                          { "vla1-2724.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:28:0:604:db7:9c6a"; };
                          { "vla1-2727.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:32:0:604:db7:99af"; };
                          { "vla1-2728.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:3d:0:604:dbc:a320"; };
                          { "vla1-2739.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:2a:0:604:db7:9c3b"; };
                          { "vla1-2746.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:23:0:604:db7:9b7d"; };
                          { "vla1-2752.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:3d:0:604:db7:9f14"; };
                          { "vla1-2754.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:2a:0:604:db7:9c42"; };
                          { "vla1-2755.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:2a:0:604:db7:9c41"; };
                          { "vla1-2757.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:16:0:604:db7:9a9b"; };
                          { "vla1-2761.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:15:0:604:db7:9a67"; };
                          { "vla1-2763.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:2e:0:604:db7:9c10"; };
                          { "vla1-2764.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:15:0:604:db7:9ce6"; };
                          { "vla1-2774.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:74:0:604:db7:9efa"; };
                          { "vla1-2784.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:15:0:604:db7:9ce3"; };
                          { "vla1-2807.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:2e:0:604:db7:98f9"; };
                          { "vla1-2815.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:16:0:604:db7:9a58"; };
                          { "vla1-2880.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:16:0:604:db7:9b2f"; };
                          { "vla1-2881.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:32:0:604:db7:9c07"; };
                          { "vla1-2964.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:6e:0:604:db7:a3fd"; };
                          { "vla1-2968.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:6e:0:604:db7:a263"; };
                          { "vla1-2993.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:6c:0:604:db7:a159"; };
                          { "vla1-2998.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:70:0:604:db7:a410"; };
                          { "vla1-3011.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:19:0:604:db6:e746"; };
                          { "vla1-3015.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:70:0:604:db7:a232"; };
                          { "vla1-3016.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:50:0:604:db7:a55d"; };
                          { "vla1-3040.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a3:0:604:db7:a733"; };
                          { "vla1-3044.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:21:0:604:db7:9eb7"; };
                          { "vla1-3059.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a3:0:604:db7:9ddf"; };
                          { "vla1-3060.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:18:0:604:db7:9a98"; };
                          { "vla1-3062.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:18:0:604:db7:9acc"; };
                          { "vla1-3094.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:18:0:604:db7:9ab2"; };
                          { "vla1-3102.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:74:0:604:db7:98fd"; };
                          { "vla1-3125.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:11:0:604:db7:98f0"; };
                          { "vla1-3132.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:6c:0:604:db7:a520"; };
                          { "vla1-3149.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:70:0:604:db7:a281"; };
                          { "vla1-3174.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:11:0:604:db7:998d"; };
                          { "vla1-3178.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:21:0:604:db7:9cf1"; };
                          { "vla1-3199.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:50:0:604:db7:a697"; };
                          { "vla1-3226.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:21:0:604:db7:9ec8"; };
                          { "vla1-3247.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:6c:0:604:db7:9c25"; };
                          { "vla1-3256.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:39:0:604:db7:a12e"; };
                          { "vla1-3259.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:19:0:604:db7:9ac9"; };
                          { "vla1-3263.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:11:0:604:db7:99fd"; };
                          { "vla1-3278.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:19:0:604:db7:99e8"; };
                          { "vla1-3285.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:11:0:604:db7:98f2"; };
                          { "vla1-3296.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:19:0:604:db7:9a36"; };
                          { "vla1-3297.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a3:0:604:db7:a6f5"; };
                          { "vla1-3312.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:6e:0:604:db7:a285"; };
                          { "vla1-3354.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:50:0:604:db7:a57b"; };
                          { "vla1-3401.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:74:0:604:db7:9eb9"; };
                          { "vla1-3503.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:47:0:604:db7:a592"; };
                          { "vla1-3507.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a0:0:604:db7:a4f0"; };
                          { "vla1-3521.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:73:0:604:db7:a5de"; };
                          { "vla1-3534.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:47:0:604:db7:a1f9"; };
                          { "vla1-3553.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a0:0:604:db7:a672"; };
                          { "vla1-3559.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:47:0:604:db7:a4b5"; };
                          { "vla1-3567.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:6d:0:604:db7:a690"; };
                          { "vla1-3571.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:73:0:604:db7:a0cf"; };
                          { "vla1-3580.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:73:0:604:db7:a0d6"; };
                          { "vla1-3588.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:4b:0:604:db7:a625"; };
                          { "vla1-3594.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:4b:0:604:db7:a0c7"; };
                          { "vla1-3606.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a1:0:604:db7:a292"; };
                          { "vla1-3610.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a1:0:604:db7:a00b"; };
                          { "vla1-3619.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a1:0:604:db7:a28f"; };
                          { "vla1-3622.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:39:0:604:db7:a58e"; };
                          { "vla1-3626.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:63:0:604:db7:a309"; };
                          { "vla1-3631.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:6d:0:604:db7:a609"; };
                          { "vla1-3632.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:46:0:604:db7:a0b4"; };
                          { "vla1-3635.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:46:0:604:db7:a552"; };
                          { "vla1-3663.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:7a:0:604:db7:a88b"; };
                          { "vla1-3664.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:72:0:604:db7:9e21"; };
                          { "vla1-3667.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:80:0:604:db7:a927"; };
                          { "vla1-3671.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:65:0:604:db7:a097"; };
                          { "vla1-3672.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:63:0:604:db7:a2f9"; };
                          { "vla1-3680.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:4b:0:604:db7:a169"; };
                          { "vla1-3691.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:86:0:604:db7:abac"; };
                          { "vla1-3693.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:72:0:604:db7:a5c1"; };
                          { "vla1-3698.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:65:0:604:db7:a26a"; };
                          { "vla1-3699.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:86:0:604:db7:aba7"; };
                          { "vla1-3720.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:63:0:604:db7:a2e3"; };
                          { "vla1-3726.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:46:0:604:db7:a549"; };
                          { "vla1-3748.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:39:0:604:db7:a13f"; };
                          { "vla1-3753.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a0:0:604:db7:a47b"; };
                          { "vla1-3759.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:86:0:604:db7:a82a"; };
                          { "vla1-3761.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:6d:0:604:db7:a591"; };
                          { "vla1-3762.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:80:0:604:db7:a8e7"; };
                          { "vla1-3769.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:5b:0:604:db7:a344"; };
                          { "vla1-3776.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:7a:0:604:db7:a8f2"; };
                          { "vla1-3802.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:7f:0:604:db7:a1fe"; };
                          { "vla1-3841.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:72:0:604:db7:a5bf"; };
                          { "vla1-3850.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:7d:0:604:db7:9e5f"; };
                          { "vla1-3966.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:7f:0:604:db7:a3e7"; };
                          { "vla1-3996.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:a0:0:604:db7:a503"; };
                          { "vla1-4164.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:7f:0:604:db7:a3c9"; };
                          { "vla1-4219.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:8a:0:604:db7:a80e"; };
                          { "vla1-4269.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:90:0:604:db7:a87e"; };
                          { "vla1-4288.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:86:0:604:db7:ac10"; };
                          { "vla1-4308.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:90:0:604:db7:a996"; };
                          { "vla1-4491.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:9b:0:604:db7:aa24"; };
                          { "vla1-4575.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:77:0:604:db7:a941"; };
                          { "vla1-4581.search.yandex.net"; 9080; 273.000; "2a02:6b8:c0e:77:0:604:db7:a928"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "10s";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- vlaruweb_noapache
                  vlaruvideo_noapache = {
                    priority = 28;
                    match_fsm = {
                      host = "vlaruvideo\\.noapache\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    balancer2 = {
                      unique_policy = {};
                      attempts = 5;
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
                          { "vla1-0213.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                          { "vla1-0553.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:17:0:604:db7:9b17"; };
                          { "vla1-0557.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                          { "vla1-0762.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                          { "vla1-0852.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                          { "vla1-1073.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                          { "vla1-1167.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                          { "vla1-1206.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                          { "vla1-1245.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                          { "vla1-1266.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                          { "vla1-1372.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:85:0:604:db7:aa37"; };
                          { "vla1-1396.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:85:0:604:db7:aa39"; };
                          { "vla1-1412.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                          { "vla1-1426.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                          { "vla1-1439.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:85:0:604:db7:aa3a"; };
                          { "vla1-1444.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:4e:0:604:db7:a36b"; };
                          { "vla1-1454.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                          { "vla1-1484.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                          { "vla1-1497.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                          { "vla1-1508.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                          { "vla1-1527.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                          { "vla1-1528.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:59:0:604:db7:a036"; };
                          { "vla1-1530.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                          { "vla1-1540.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                          { "vla1-1556.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                          { "vla1-1568.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                          { "vla1-1571.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                          { "vla1-1575.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                          { "vla1-1576.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:58:0:604:db7:a24c"; };
                          { "vla1-1577.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:5d:0:604:db7:a04e"; };
                          { "vla1-1597.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:5c:0:604:db7:9cd2"; };
                          { "vla1-1616.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:5e:0:604:db7:9cc8"; };
                          { "vla1-1636.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                          { "vla1-1643.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                          { "vla1-1650.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                          { "vla1-1654.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                          { "vla1-1744.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                          { "vla1-1778.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:89:0:604:db7:a3a3"; };
                          { "vla1-1781.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:42:0:604:db7:a0ef"; };
                          { "vla1-1785.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                          { "vla1-1837.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:41:0:604:db7:a5a9"; };
                          { "vla1-1862.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                          { "vla1-1869.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                          { "vla1-1889.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                          { "vla1-1899.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:83:0:604:db7:aac2"; };
                          { "vla1-1902.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                          { "vla1-1916.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:62:0:604:db7:a2e9"; };
                          { "vla1-1920.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                          { "vla1-1936.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:22:0:604:db7:9e9b"; };
                          { "vla1-1937.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                          { "vla1-2006.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                          { "vla1-2011.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                          { "vla1-2035.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                          { "vla1-2039.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                          { "vla1-2097.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                          { "vla1-2100.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:20:0:604:db7:9d2e"; };
                          { "vla1-2131.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                          { "vla1-2141.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                          { "vla1-2153.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                          { "vla1-2209.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                          { "vla1-2228.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:59:0:604:db7:9fef"; };
                          { "vla1-2263.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:5a:0:604:db7:a343"; };
                          { "vla1-2289.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:5b:0:604:db7:9e19"; };
                          { "vla1-2338.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                          { "vla1-2341.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                          { "vla1-2363.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                          { "vla1-2368.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                          { "vla1-2392.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                          { "vla1-2440.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                          { "vla1-2759.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                          { "vla1-2901.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                          { "vla1-3477.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                          { "vla1-4120.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                          { "vla1-4143.search.yandex.net"; 9080; 232.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "10s";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- vlaruvideo_noapache
                  vlaruimgs_noapache = {
                    priority = 27;
                    match_fsm = {
                      host = "vlaruimgs\\.noapache\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    balancer2 = {
                      unique_policy = {};
                      attempts = 5;
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
                          { "vla1-0147.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:3f:0:604:db7:a6ca"; };
                          { "vla1-0152.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:3f:0:604:db7:a705"; };
                          { "vla1-0221.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:81:0:604:db7:a8cf"; };
                          { "vla1-0703.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:53:0:604:db7:9d01"; };
                          { "vla1-0951.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:71:0:604:db7:a3de"; };
                          { "vla1-1128.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:45:0:604:db7:a77d"; };
                          { "vla1-1161.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:8d:0:604:db7:ab54"; };
                          { "vla1-1184.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:5d:0:604:db7:a5f7"; };
                          { "vla1-1207.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:9a:0:604:db7:aa9b"; };
                          { "vla1-1228.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:5c:0:604:db7:a047"; };
                          { "vla1-1233.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:59:0:604:db7:9ff8"; };
                          { "vla1-1239.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:93:0:604:db7:a751"; };
                          { "vla1-1246.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:83:0:604:db7:a910"; };
                          { "vla1-1257.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:81:0:604:db7:a7e7"; };
                          { "vla1-1259.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:89:0:604:db7:abd2"; };
                          { "vla1-1278.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:4d:0:604:db7:a440"; };
                          { "vla1-1292.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:4e:0:604:db7:a4b6"; };
                          { "vla1-1294.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:1b:0:604:db7:9b7e"; };
                          { "vla1-1304.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:4c:0:604:db7:a0ce"; };
                          { "vla1-1315.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:67:0:604:db7:a325"; };
                          { "vla1-1330.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:78:0:604:db7:a9c3"; };
                          { "vla1-1341.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:5e:0:604:db7:9deb"; };
                          { "vla1-1354.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:61:0:604:db7:a5b5"; };
                          { "vla1-1356.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:96:0:604:db7:a9c1"; };
                          { "vla1-1358.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:45:0:604:db7:a764"; };
                          { "vla1-1366.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:85:0:604:db7:a877"; };
                          { "vla1-1384.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:44:0:604:db7:9f36"; };
                          { "vla1-1395.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:84:0:604:db7:abfb"; };
                          { "vla1-1421.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:58:0:604:db7:9e42"; };
                          { "vla1-1457.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:62:0:604:db7:a2ea"; };
                          { "vla1-1464.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:48:0:604:db7:a695"; };
                          { "vla1-1486.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:20:0:604:db7:9eb4"; };
                          { "vla1-1516.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:43:0:604:db7:a110"; };
                          { "vla1-1518.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:3c:0:604:db7:9eb8"; };
                          { "vla1-1598.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:13:0:604:db7:9a64"; };
                          { "vla1-1710.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:71:0:604:db7:a416"; };
                          { "vla1-1747.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:42:0:604:db7:a4e4"; };
                          { "vla1-1757.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:88:0:604:db7:a771"; };
                          { "vla1-1794.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:22:0:604:db7:9d6f"; };
                          { "vla1-1806.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:41:0:604:db7:9e2b"; };
                          { "vla1-1815.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:57:0:604:db7:a38f"; };
                          { "vla1-1911.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:45:0:604:db7:a5fd"; };
                          { "vla1-1930.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:44:0:604:db7:a6cc"; };
                          { "vla1-1987.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:82:0:604:db7:a872"; };
                          { "vla1-2016.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:91:0:604:db7:ab5e"; };
                          { "vla1-2017.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:34:0:604:db7:9d11"; };
                          { "vla1-2041.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:40:0:604:db7:a53f"; };
                          { "vla1-2078.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:53:0:604:db7:9d02"; };
                          { "vla1-2092.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:17:0:604:db7:a7f5"; };
                          { "vla1-2121.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:98:0:604:db7:a008"; };
                          { "vla1-2122.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:53:0:604:db7:9d07"; };
                          { "vla1-2143.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:27:0:604:db7:9f6f"; };
                          { "vla1-2163.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:53:0:604:db7:9cfa"; };
                          { "vla1-2253.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:5a:0:604:db7:a1dd"; };
                          { "vla1-2265.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:5b:0:604:db7:a1d7"; };
                          { "vla1-2315.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:6e:0:604:db7:a371"; };
                          { "vla1-2319.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:7f:0:604:db7:a368"; };
                          { "vla1-2330.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:7d:0:604:db7:a2b0"; };
                          { "vla1-2332.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:87:0:604:db7:a84b"; };
                          { "vla1-2345.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:86:0:604:db7:ab9e"; };
                          { "vla1-2351.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:87:0:604:db7:a7ea"; };
                          { "vla1-2358.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:7f:0:604:db7:a3d8"; };
                          { "vla1-2453.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:94:0:604:db7:a94f"; };
                          { "vla1-2472.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:97:0:604:db7:a909"; };
                          { "vla1-3780.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:7f:0:604:db7:9e7d"; };
                          { "vla1-4111.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:87:0:604:db7:a820"; };
                          { "vla1-4375.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:86:0:604:db7:a82d"; };
                          { "vla1-4428.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:8a:0:604:db7:a818"; };
                          { "vla1-4443.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:86:0:604:db7:9dee"; };
                          { "vla1-4504.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:90:0:604:db7:a7e1"; };
                          { "vla2-0560.search.yandex.net"; 9080; 348.000; "2a02:6b8:c0e:9c:0:604:db7:aa6e"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "10s";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- vlaruimgs_noapache
                  sasruweb_apphost = {
                    priority = 26;
                    match_fsm = {
                      host = "sasruweb\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "sas1-0439.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:636:225:90ff:fee5:be98"; };
                              { "sas1-0442.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:601:225:90ff:feed:30b2"; };
                              { "sas1-0469.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:635:225:90ff:feef:c914"; };
                              { "sas1-0490.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:601:225:90ff:feed:3286"; };
                              { "sas1-0510.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:636:225:90ff:feed:2f9a"; };
                              { "sas1-0517.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:601:225:90ff:feed:31a2"; };
                              { "sas1-0555.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:635:225:90ff:feed:3040"; };
                              { "sas1-1126.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:1c1e"; };
                              { "sas1-1243.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:100:225:90ff:feed:2f9c"; };
                              { "sas1-1281.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:175:225:90ff:fee7:52f6"; };
                              { "sas1-1326.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:175:225:90ff:fec4:8b42"; };
                              { "sas1-1338.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:175:225:90ff:fee7:52be"; };
                              { "sas1-1381.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:175:225:90ff:fee7:540c"; };
                              { "sas1-1390.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:601:225:90ff:feef:c9cc"; };
                              { "sas1-1393.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:174:225:90ff:feed:3036"; };
                              { "sas1-1397.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:177:feaa:14ff:fe1d:f516"; };
                              { "sas1-1405.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:cee"; };
                              { "sas1-1411.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:174:225:90ff:feed:3158"; };
                              { "sas1-1412.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:100:225:90ff:feed:30b4"; };
                              { "sas1-1416.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:174:225:90ff:feed:31aa"; };
                              { "sas1-1417.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:174:225:90ff:feed:2de6"; };
                              { "sas1-1419.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:635:225:90ff:feef:c648"; };
                              { "sas1-1427.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:618:215:b2ff:fea8:d0e"; };
                              { "sas1-1428.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:dae"; };
                              { "sas1-1437.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:d12"; };
                              { "sas1-1439.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:196:215:b2ff:fea8:d42"; };
                              { "sas1-1445.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:119:215:b2ff:fea8:d2e"; };
                              { "sas1-1447.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:dbe"; };
                              { "sas1-1452.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:625:96de:80ff:feec:1b9a"; };
                              { "sas1-1472.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:114:215:b2ff:fea8:d56"; };
                              { "sas1-1500.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:114:215:b2ff:fea8:d7e"; };
                              { "sas1-1501.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:684:215:b2ff:fea8:d16"; };
                              { "sas1-1519.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d00"; };
                              { "sas1-1536.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a6:215:b2ff:fea8:96a"; };
                              { "sas1-1547.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:628:215:b2ff:fea8:d73"; };
                              { "sas1-1562.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a6:215:b2ff:fea8:7fe"; };
                              { "sas1-1573.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:c12"; };
                              { "sas1-1585.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a5:215:b2ff:fea8:976"; };
                              { "sas1-1586.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:8944"; };
                              { "sas1-1587.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:6d98"; };
                              { "sas1-1590.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:617:215:b2ff:fea8:d76"; };
                              { "sas1-1598.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:6cf0"; };
                              { "sas1-1599.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:6d2c"; };
                              { "sas1-1603.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cca"; };
                              { "sas1-1606.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:628:215:b2ff:fea8:d36"; };
                              { "sas1-1621.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cbe"; };
                              { "sas1-1639.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:c82"; };
                              { "sas1-1642.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:c16"; };
                              { "sas1-1650.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:c7e"; };
                              { "sas1-1666.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:7140"; };
                              { "sas1-1676.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a0:215:b2ff:fea8:6c0c"; };
                              { "sas1-1677.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:169:215:b2ff:fea7:f8fc"; };
                              { "sas1-1684.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:c5e"; };
                              { "sas1-1685.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:610:215:b2ff:fea8:6dc4"; };
                              { "sas1-1695.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:610:215:b2ff:fea8:6d90"; };
                              { "sas1-1697.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:197:215:b2ff:fea8:6c10"; };
                              { "sas1-1718.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:197:215:b2ff:fea8:6e01"; };
                              { "sas1-1729.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a4:215:b2ff:fea8:a6e"; };
                              { "sas1-1734.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:6d38"; };
                              { "sas1-1736.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:17a:feaa:14ff:feab:faf4"; };
                              { "sas1-1744.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:17a:feaa:14ff:feab:f616"; };
                              { "sas1-1783.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:16e:215:b2ff:fea7:87c4"; };
                              { "sas1-1787.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12f:215:b2ff:fea8:992"; };
                              { "sas1-1789.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12c:215:b2ff:fea8:98e"; };
                              { "sas1-1800.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:68f:215:b2ff:fea8:a66"; };
                              { "sas1-1801.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a9:215:b2ff:fea8:a62"; };
                              { "sas1-1802.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:623:215:b2ff:fea8:d4a"; };
                              { "sas1-1803.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:87b0"; };
                              { "sas1-1805.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:62c:215:b2ff:fea8:d3e"; };
                              { "sas1-1806.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:62c:215:b2ff:fea8:cfa"; };
                              { "sas1-1813.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:616:215:b2ff:fea8:d86"; };
                              { "sas1-1814.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61d:215:b2ff:fea8:bde"; };
                              { "sas1-1830.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:625:96de:80ff:feec:1306"; };
                              { "sas1-1833.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ceb4"; };
                              { "sas1-1834.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:68f:215:b2ff:fea8:98a"; };
                              { "sas1-1835.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:61dc"; };
                              { "sas1-1836.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:623:215:b2ff:fea8:c8a"; };
                              { "sas1-1837.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12f:215:b2ff:fea8:bda"; };
                              { "sas1-1839.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a4:215:b2ff:fea8:6d88"; };
                              { "sas1-1921.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12c:215:b2ff:fea8:cd6"; };
                              { "sas1-1990.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:619:96de:80ff:feec:15ca"; };
                              { "sas1-2005.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:604:225:90ff:feef:c9de"; };
                              { "sas1-2013.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63c:96de:80ff:feec:1d46"; };
                              { "sas1-2483.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a1:96de:80ff:feec:eca"; };
                              { "sas1-4219.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:635:225:90ff:feef:c966"; };
                              { "sas1-4452.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:652:96de:80ff:feec:18f2"; };
                              { "sas1-4455.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:625:96de:80ff:feec:18a8"; };
                              { "sas1-4456.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:674:96de:80ff:feec:1e42"; };
                              { "sas1-4873.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:653:96de:80ff:feec:19a2"; };
                              { "sas1-5080.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:100:225:90ff:feef:c9d0"; };
                              { "sas1-5396.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:654:96de:80ff:feec:1aee"; };
                              { "sas1-5397.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:653:96de:80ff:feec:12f6"; };
                              { "sas1-5449.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:625:96de:80ff:feec:1b94"; };
                              { "sas1-5453.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:139:96de:80ff:feec:1c80"; };
                              { "sas1-5454.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:139:96de:80ff:feec:1b52"; };
                              { "sas1-5457.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a9:96de:80ff:feec:145e"; };
                              { "sas1-5458.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:653:96de:80ff:feec:14b8"; };
                              { "sas1-5459.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:1432"; };
                              { "sas1-5460.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:ea8"; };
                              { "sas1-5461.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:111:96de:80ff:feec:1326"; };
                              { "sas1-5465.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:652:96de:80ff:feec:fd2"; };
                              { "sas1-5467.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:625:96de:80ff:feec:11c4"; };
                              { "sas1-5468.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:652:96de:80ff:feec:11c6"; };
                              { "sas1-5469.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:651:96de:80ff:feec:1216"; };
                              { "sas1-5471.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:653:96de:80ff:feec:11ce"; };
                              { "sas1-5472.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:1c30"; };
                              { "sas1-5474.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63b:96de:80ff:feec:1014"; };
                              { "sas1-5479.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:189a"; };
                              { "sas1-5480.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1886"; };
                              { "sas1-5481.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10bc"; };
                              { "sas1-5482.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63c:96de:80ff:feec:1948"; };
                              { "sas1-5483.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:619:96de:80ff:feec:1882"; };
                              { "sas1-5484.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:674:96de:80ff:feec:1a82"; };
                              { "sas1-5489.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:170a"; };
                              { "sas1-5490.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63c:96de:80ff:feec:17fe"; };
                              { "sas1-5492.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:654:96de:80ff:fee9:d4"; };
                              { "sas1-5494.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:619:96de:80ff:feec:1d8c"; };
                              { "sas1-5496.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:666:96de:80ff:feec:1edc"; };
                              { "sas1-5497.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:1bb4"; };
                              { "sas1-5500.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:656:96de:80ff:feec:164c"; };
                              { "sas1-5503.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a1:96de:80ff:feec:fda"; };
                              { "sas1-5505.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:111:96de:80ff:feec:1108"; };
                              { "sas1-5509.search.yandex.net"; 32046; 38.000; "2a02:6b8:c02:53b:0:604:80ec:1a3c"; };
                              { "sas1-5510.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1ba0"; };
                              { "sas1-5511.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:651:96de:80ff:feec:1ba4"; };
                              { "sas1-5512.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63d:96de:80ff:feec:18c8"; };
                              { "sas1-5515.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:674:96de:80ff:feec:18d8"; };
                              { "sas1-5519.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:639:96de:80ff:feec:130c"; };
                              { "sas1-5520.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:655:96de:80ff:feec:190c"; };
                              { "sas1-5521.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:656:96de:80ff:feec:191c"; };
                              { "sas1-5522.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:139:96de:80ff:feec:1a4c"; };
                              { "sas1-5523.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63b:96de:80ff:feec:1848"; };
                              { "sas1-5626.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:160:225:90ff:fee6:481e"; };
                              { "sas1-5627.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:163:225:90ff:fee6:49be"; };
                              { "sas1-5628.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:163:225:90ff:fee6:4868"; };
                              { "sas1-5630.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:163:225:90ff:fee6:46fc"; };
                              { "sas1-5631.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:622:225:90ff:fee6:491c"; };
                              { "sas1-5632.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:160:225:90ff:fee6:4ca6"; };
                              { "sas1-5634.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:160:225:90ff:fee6:47fe"; };
                              { "sas1-5635.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:163:225:90ff:fee6:4822"; };
                              { "sas1-5636.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:622:225:90ff:fee6:486e"; };
                              { "sas1-5637.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:161:225:90ff:fee6:486c"; };
                              { "sas1-5639.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:161:225:90ff:fee5:c298"; };
                              { "sas1-5640.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:622:225:90ff:fee5:bbd0"; };
                              { "sas1-5641.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:622:225:90ff:fee4:2e26"; };
                              { "sas1-5644.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:161:225:90ff:fee6:48aa"; };
                              { "sas1-5646.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:602:225:90ff:fee8:7c0e"; };
                              { "sas1-5647.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:602:225:90ff:fee6:49b0"; };
                              { "sas1-5651.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:160:225:90ff:fee6:482e"; };
                              { "sas1-5652.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:160:225:90ff:fee6:4b3e"; };
                              { "sas1-5653.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:163:225:90ff:fee6:4864"; };
                              { "sas1-5654.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:602:225:90ff:fee6:4816"; };
                              { "sas1-5655.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:649:225:90ff:fee5:beb4"; };
                              { "sas1-5656.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:649:225:90ff:fee8:7bbc"; };
                              { "sas1-5661.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:622:225:90ff:fee6:4a46"; };
                              { "sas1-5663.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:649:225:90ff:fee6:ddc0"; };
                              { "sas1-5669.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:636:225:90ff:feed:318c"; };
                              { "sas1-5715.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:635:225:90ff:feed:3064"; };
                              { "sas1-5732.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:636:225:90ff:feef:cb98"; };
                              { "sas1-5892.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:601:225:90ff:feec:2f44"; };
                              { "sas1-5959.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:604:225:90ff:feef:c9da"; };
                              { "sas1-5961.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:604:225:90ff:feef:c974"; };
                              { "sas1-5962.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:604:225:90ff:feef:c96a"; };
                              { "sas1-5964.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:604:225:90ff:feeb:f9b0"; };
                              { "sas1-6100.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:164:428d:5cff:fe34:f33a"; };
                              { "sas1-6161.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:164:428d:5cff:fe34:f98a"; };
                              { "sas1-6171.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:164:428d:5cff:fe34:f8fa"; };
                              { "sas1-6240.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:118:215:b2ff:fea7:76c0"; };
                              { "sas1-6357.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:69f:215:b2ff:fea7:827c"; };
                              { "sas1-6398.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:115:215:b2ff:fea7:7874"; };
                              { "sas1-6406.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:7ed0"; };
                              { "sas1-6539.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:8010"; };
                              { "sas1-6626.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:119:215:b2ff:fea7:7be0"; };
                              { "sas1-6634.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:66c:215:b2ff:fea7:7e34"; };
                              { "sas1-6720.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:11e:215:b2ff:fea7:8c38"; };
                              { "sas1-6730.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a924"; };
                              { "sas1-6735.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:8d9c"; };
                              { "sas1-6743.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:a9dc"; };
                              { "sas1-6757.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:66c:215:b2ff:fea7:a9b0"; };
                              { "sas1-6777.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:6fe4"; };
                              { "sas1-6785.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:abb4"; };
                              { "sas1-6802.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:11f:215:b2ff:fea7:7f2c"; };
                              { "sas1-6805.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:62c:215:b2ff:fea7:b778"; };
                              { "sas1-6851.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:11f:215:b2ff:fea7:8d20"; };
                              { "sas1-6857.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:62c:215:b2ff:fea7:8d04"; };
                              { "sas1-6861.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:11f:215:b2ff:fea7:a9b4"; };
                              { "sas1-6869.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:62c:215:b2ff:fea7:a9d8"; };
                              { "sas1-6925.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12f:215:b2ff:fea7:bb18"; };
                              { "sas1-6935.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12f:215:b2ff:fea7:823c"; };
                              { "sas1-6938.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:123:215:b2ff:fea7:b3a4"; };
                              { "sas1-7005.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:16e:215:b2ff:fea7:abdc"; };
                              { "sas1-7013.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:16e:215:b2ff:fea7:ade4"; };
                              { "sas1-7039.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:120:215:b2ff:fea7:9054"; };
                              { "sas1-7060.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:120:215:b2ff:fea7:ad08"; };
                              { "sas1-7094.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8fc4"; };
                              { "sas1-7138.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8ea4"; };
                              { "sas1-7142.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:83cc"; };
                              { "sas1-7154.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aab0"; };
                              { "sas1-7167.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12c:215:b2ff:fea7:73b4"; };
                              { "sas1-7169.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12c:215:b2ff:fea7:73ac"; };
                              { "sas1-7192.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:122:215:b2ff:fea7:7d40"; };
                              { "sas1-7209.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:122:215:b2ff:fea7:8f24"; };
                              { "sas1-7212.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:122:215:b2ff:fea7:90e0"; };
                              { "sas1-7234.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:121:215:b2ff:fea7:abcc"; };
                              { "sas1-7276.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:128:215:b2ff:fea7:7250"; };
                              { "sas1-7458.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:7fe4"; };
                              { "sas1-7465.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:125:215:b2ff:fea7:817c"; };
                              { "sas1-7475.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:610:215:b2ff:fea7:8bf4"; };
                              { "sas1-7477.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:129:215:b2ff:fea7:8b6c"; };
                              { "sas1-7488.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:80a8"; };
                              { "sas1-7534.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:125:215:b2ff:fea7:7430"; };
                              { "sas1-7569.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:129:215:b2ff:fea7:bc08"; };
                              { "sas1-7591.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:129:215:b2ff:fea7:b990"; };
                              { "sas1-7686.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bf0c"; };
                              { "sas1-7687.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bec8"; };
                              { "sas1-7696.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bddc"; };
                              { "sas1-7727.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:127:215:b2ff:fea7:af04"; };
                              { "sas1-7732.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:127:215:b2ff:fea7:ba58"; };
                              { "sas1-7761.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:73f4"; };
                              { "sas1-7763.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b6e0"; };
                              { "sas1-7790.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b054"; };
                              { "sas1-7803.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:b56c"; };
                              { "sas1-7816.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:8cdc"; };
                              { "sas1-7868.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12e:215:b2ff:fea7:aa10"; };
                              { "sas1-7869.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12e:215:b2ff:fea7:77d4"; };
                              { "sas1-7878.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12e:215:b2ff:fea7:7db8"; };
                              { "sas1-7902.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab88"; };
                              { "sas1-7908.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:8c34"; };
                              { "sas1-7936.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:618:215:b2ff:fea7:8b98"; };
                              { "sas1-7942.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7444"; };
                              { "sas1-8160.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:132:215:b2ff:fea7:afec"; };
                              { "sas1-8179.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:132:215:b2ff:fea7:8b9c"; };
                              { "sas1-8186.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:68f:215:b2ff:fea7:9178"; };
                              { "sas1-8214.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:68f:215:b2ff:fea7:a980"; };
                              { "sas1-8234.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:131:215:b2ff:fea7:b3d4"; };
                              { "sas1-8255.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a6:215:b2ff:fea7:b1d0"; };
                              { "sas1-8299.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:132:215:b2ff:fea7:b754"; };
                              { "sas1-8301.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:132:215:b2ff:fea7:b750"; };
                              { "sas1-8357.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:133:215:b2ff:fea7:af7c"; };
                              { "sas1-8369.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:623:215:b2ff:fea7:b2c8"; };
                              { "sas1-8430.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:130:215:b2ff:fea7:aeb8"; };
                              { "sas1-8518.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:170:428d:5cff:fe37:fffe"; };
                              { "sas1-8524.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:617:215:b2ff:fea7:b984"; };
                              { "sas1-8573.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:135:215:b2ff:fea7:bcd8"; };
                              { "sas1-8588.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:142:428d:5cff:fe36:8b26"; };
                              { "sas1-8596.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:142:428d:5cff:fe36:8ac8"; };
                              { "sas1-8612.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:684:215:b2ff:fea7:bf10"; };
                              { "sas1-8615.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:684:215:b2ff:fea7:b834"; };
                              { "sas1-8635.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:134:215:b2ff:fea7:b83c"; };
                              { "sas1-8637.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:134:215:b2ff:fea7:b840"; };
                              { "sas1-8638.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:134:215:b2ff:fea7:b988"; };
                              { "sas1-8674.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:616:215:b2ff:fea7:bc54"; };
                              { "sas1-8694.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:137:215:b2ff:fea7:b640"; };
                              { "sas1-8701.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:137:215:b2ff:fea7:b330"; };
                              { "sas1-8730.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:616:215:b2ff:fea7:8dec"; };
                              { "sas1-8742.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:616:215:b2ff:fea7:b2a4"; };
                              { "sas1-8746.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:616:215:b2ff:fea7:b3ac"; };
                              { "sas1-8826.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a6:215:b2ff:fea7:90f8"; };
                              { "sas1-8865.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:138:215:b2ff:fea7:bae4"; };
                              { "sas1-8932.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:141:feaa:14ff:fede:3f12"; };
                              { "sas1-8945.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1ac:feaa:14ff:fede:3f0e"; };
                              { "sas1-8948.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13f:feaa:14ff:fede:3ef0"; };
                              { "sas1-8960.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13f:feaa:14ff:fede:4050"; };
                              { "sas1-8989.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:613:feaa:14ff:fede:4125"; };
                              { "sas1-9038.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:427a"; };
                              { "sas1-9055.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13e:feaa:14ff:fede:3f68"; };
                              { "sas1-9058.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13e:feaa:14ff:fede:3f2e"; };
                              { "sas1-9061.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13e:feaa:14ff:fede:41b0"; };
                              { "sas1-9085.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:438a"; };
                              { "sas1-9142.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3fc2"; };
                              { "sas1-9151.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:422c"; };
                              { "sas1-9164.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:417c"; };
                              { "sas1-9218.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:45bc"; };
                              { "sas1-9222.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13c:feaa:14ff:fede:4040"; };
                              { "sas1-9247.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:401a"; };
                              { "sas1-9282.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:45f4"; };
                              { "sas1-9283.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:3eb8"; };
                              { "sas1-9361.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13a:feaa:14ff:fede:4322"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "sas1-0439.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:636:225:90ff:fee5:be98"; };
                              { "sas1-0442.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:601:225:90ff:feed:30b2"; };
                              { "sas1-0469.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:635:225:90ff:feef:c914"; };
                              { "sas1-0490.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:601:225:90ff:feed:3286"; };
                              { "sas1-0510.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:636:225:90ff:feed:2f9a"; };
                              { "sas1-0517.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:601:225:90ff:feed:31a2"; };
                              { "sas1-0555.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:635:225:90ff:feed:3040"; };
                              { "sas1-1126.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:1c1e"; };
                              { "sas1-1243.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:100:225:90ff:feed:2f9c"; };
                              { "sas1-1281.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:175:225:90ff:fee7:52f6"; };
                              { "sas1-1326.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:175:225:90ff:fec4:8b42"; };
                              { "sas1-1338.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:175:225:90ff:fee7:52be"; };
                              { "sas1-1381.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:175:225:90ff:fee7:540c"; };
                              { "sas1-1390.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:601:225:90ff:feef:c9cc"; };
                              { "sas1-1393.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:174:225:90ff:feed:3036"; };
                              { "sas1-1397.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:177:feaa:14ff:fe1d:f516"; };
                              { "sas1-1405.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:cee"; };
                              { "sas1-1411.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:174:225:90ff:feed:3158"; };
                              { "sas1-1412.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:100:225:90ff:feed:30b4"; };
                              { "sas1-1416.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:174:225:90ff:feed:31aa"; };
                              { "sas1-1417.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:174:225:90ff:feed:2de6"; };
                              { "sas1-1419.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:635:225:90ff:feef:c648"; };
                              { "sas1-1427.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:618:215:b2ff:fea8:d0e"; };
                              { "sas1-1428.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:dae"; };
                              { "sas1-1437.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:d12"; };
                              { "sas1-1439.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:196:215:b2ff:fea8:d42"; };
                              { "sas1-1445.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:119:215:b2ff:fea8:d2e"; };
                              { "sas1-1447.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:dbe"; };
                              { "sas1-1452.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:625:96de:80ff:feec:1b9a"; };
                              { "sas1-1472.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:114:215:b2ff:fea8:d56"; };
                              { "sas1-1500.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:114:215:b2ff:fea8:d7e"; };
                              { "sas1-1501.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:684:215:b2ff:fea8:d16"; };
                              { "sas1-1519.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d00"; };
                              { "sas1-1536.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a6:215:b2ff:fea8:96a"; };
                              { "sas1-1547.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:628:215:b2ff:fea8:d73"; };
                              { "sas1-1562.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a6:215:b2ff:fea8:7fe"; };
                              { "sas1-1573.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:c12"; };
                              { "sas1-1585.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a5:215:b2ff:fea8:976"; };
                              { "sas1-1586.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:8944"; };
                              { "sas1-1587.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:6d98"; };
                              { "sas1-1590.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:617:215:b2ff:fea8:d76"; };
                              { "sas1-1598.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:6cf0"; };
                              { "sas1-1599.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:6d2c"; };
                              { "sas1-1603.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cca"; };
                              { "sas1-1606.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:628:215:b2ff:fea8:d36"; };
                              { "sas1-1621.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cbe"; };
                              { "sas1-1639.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:c82"; };
                              { "sas1-1642.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:67a:215:b2ff:fea8:c16"; };
                              { "sas1-1650.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:c7e"; };
                              { "sas1-1666.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:7140"; };
                              { "sas1-1676.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a0:215:b2ff:fea8:6c0c"; };
                              { "sas1-1677.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:169:215:b2ff:fea7:f8fc"; };
                              { "sas1-1684.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:c5e"; };
                              { "sas1-1685.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:610:215:b2ff:fea8:6dc4"; };
                              { "sas1-1695.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:610:215:b2ff:fea8:6d90"; };
                              { "sas1-1697.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:197:215:b2ff:fea8:6c10"; };
                              { "sas1-1718.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:197:215:b2ff:fea8:6e01"; };
                              { "sas1-1729.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a4:215:b2ff:fea8:a6e"; };
                              { "sas1-1734.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:179:215:b2ff:fea8:6d38"; };
                              { "sas1-1736.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:17a:feaa:14ff:feab:faf4"; };
                              { "sas1-1744.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:17a:feaa:14ff:feab:f616"; };
                              { "sas1-1783.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:16e:215:b2ff:fea7:87c4"; };
                              { "sas1-1787.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12f:215:b2ff:fea8:992"; };
                              { "sas1-1789.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12c:215:b2ff:fea8:98e"; };
                              { "sas1-1800.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:68f:215:b2ff:fea8:a66"; };
                              { "sas1-1801.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a9:215:b2ff:fea8:a62"; };
                              { "sas1-1802.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:623:215:b2ff:fea8:d4a"; };
                              { "sas1-1803.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:87b0"; };
                              { "sas1-1805.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:62c:215:b2ff:fea8:d3e"; };
                              { "sas1-1806.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:62c:215:b2ff:fea8:cfa"; };
                              { "sas1-1813.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:616:215:b2ff:fea8:d86"; };
                              { "sas1-1814.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61d:215:b2ff:fea8:bde"; };
                              { "sas1-1830.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:625:96de:80ff:feec:1306"; };
                              { "sas1-1833.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ceb4"; };
                              { "sas1-1834.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:68f:215:b2ff:fea8:98a"; };
                              { "sas1-1835.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:61dc"; };
                              { "sas1-1836.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:623:215:b2ff:fea8:c8a"; };
                              { "sas1-1837.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12f:215:b2ff:fea8:bda"; };
                              { "sas1-1839.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a4:215:b2ff:fea8:6d88"; };
                              { "sas1-1921.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12c:215:b2ff:fea8:cd6"; };
                              { "sas1-1990.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:619:96de:80ff:feec:15ca"; };
                              { "sas1-2005.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:604:225:90ff:feef:c9de"; };
                              { "sas1-2013.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63c:96de:80ff:feec:1d46"; };
                              { "sas1-2483.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a1:96de:80ff:feec:eca"; };
                              { "sas1-4219.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:635:225:90ff:feef:c966"; };
                              { "sas1-4452.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:652:96de:80ff:feec:18f2"; };
                              { "sas1-4455.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:625:96de:80ff:feec:18a8"; };
                              { "sas1-4456.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:674:96de:80ff:feec:1e42"; };
                              { "sas1-4873.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:653:96de:80ff:feec:19a2"; };
                              { "sas1-5080.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:100:225:90ff:feef:c9d0"; };
                              { "sas1-5396.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:654:96de:80ff:feec:1aee"; };
                              { "sas1-5397.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:653:96de:80ff:feec:12f6"; };
                              { "sas1-5449.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:625:96de:80ff:feec:1b94"; };
                              { "sas1-5453.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:139:96de:80ff:feec:1c80"; };
                              { "sas1-5454.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:139:96de:80ff:feec:1b52"; };
                              { "sas1-5457.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a9:96de:80ff:feec:145e"; };
                              { "sas1-5458.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:653:96de:80ff:feec:14b8"; };
                              { "sas1-5459.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:1432"; };
                              { "sas1-5460.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:ea8"; };
                              { "sas1-5461.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:111:96de:80ff:feec:1326"; };
                              { "sas1-5465.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:652:96de:80ff:feec:fd2"; };
                              { "sas1-5467.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:625:96de:80ff:feec:11c4"; };
                              { "sas1-5468.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:652:96de:80ff:feec:11c6"; };
                              { "sas1-5469.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:651:96de:80ff:feec:1216"; };
                              { "sas1-5471.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:653:96de:80ff:feec:11ce"; };
                              { "sas1-5472.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:1c30"; };
                              { "sas1-5474.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63b:96de:80ff:feec:1014"; };
                              { "sas1-5479.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:189a"; };
                              { "sas1-5480.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1886"; };
                              { "sas1-5481.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10bc"; };
                              { "sas1-5482.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63c:96de:80ff:feec:1948"; };
                              { "sas1-5483.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:619:96de:80ff:feec:1882"; };
                              { "sas1-5484.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:674:96de:80ff:feec:1a82"; };
                              { "sas1-5489.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:170a"; };
                              { "sas1-5490.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63c:96de:80ff:feec:17fe"; };
                              { "sas1-5492.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:654:96de:80ff:fee9:d4"; };
                              { "sas1-5494.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:619:96de:80ff:feec:1d8c"; };
                              { "sas1-5496.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:666:96de:80ff:feec:1edc"; };
                              { "sas1-5497.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61c:96de:80ff:feec:1bb4"; };
                              { "sas1-5500.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:656:96de:80ff:feec:164c"; };
                              { "sas1-5503.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a1:96de:80ff:feec:fda"; };
                              { "sas1-5505.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:111:96de:80ff:feec:1108"; };
                              { "sas1-5509.search.yandex.net"; 32046; 38.000; "2a02:6b8:c02:53b:0:604:80ec:1a3c"; };
                              { "sas1-5510.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1ba0"; };
                              { "sas1-5511.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:651:96de:80ff:feec:1ba4"; };
                              { "sas1-5512.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63d:96de:80ff:feec:18c8"; };
                              { "sas1-5515.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:674:96de:80ff:feec:18d8"; };
                              { "sas1-5519.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:639:96de:80ff:feec:130c"; };
                              { "sas1-5520.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:655:96de:80ff:feec:190c"; };
                              { "sas1-5521.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:656:96de:80ff:feec:191c"; };
                              { "sas1-5522.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:139:96de:80ff:feec:1a4c"; };
                              { "sas1-5523.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:63b:96de:80ff:feec:1848"; };
                              { "sas1-5626.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:160:225:90ff:fee6:481e"; };
                              { "sas1-5627.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:163:225:90ff:fee6:49be"; };
                              { "sas1-5628.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:163:225:90ff:fee6:4868"; };
                              { "sas1-5630.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:163:225:90ff:fee6:46fc"; };
                              { "sas1-5631.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:622:225:90ff:fee6:491c"; };
                              { "sas1-5632.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:160:225:90ff:fee6:4ca6"; };
                              { "sas1-5634.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:160:225:90ff:fee6:47fe"; };
                              { "sas1-5635.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:163:225:90ff:fee6:4822"; };
                              { "sas1-5636.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:622:225:90ff:fee6:486e"; };
                              { "sas1-5637.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:161:225:90ff:fee6:486c"; };
                              { "sas1-5639.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:161:225:90ff:fee5:c298"; };
                              { "sas1-5640.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:622:225:90ff:fee5:bbd0"; };
                              { "sas1-5641.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:622:225:90ff:fee4:2e26"; };
                              { "sas1-5644.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:161:225:90ff:fee6:48aa"; };
                              { "sas1-5646.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:602:225:90ff:fee8:7c0e"; };
                              { "sas1-5647.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:602:225:90ff:fee6:49b0"; };
                              { "sas1-5651.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:160:225:90ff:fee6:482e"; };
                              { "sas1-5652.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:160:225:90ff:fee6:4b3e"; };
                              { "sas1-5653.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:163:225:90ff:fee6:4864"; };
                              { "sas1-5654.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:602:225:90ff:fee6:4816"; };
                              { "sas1-5655.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:649:225:90ff:fee5:beb4"; };
                              { "sas1-5656.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:649:225:90ff:fee8:7bbc"; };
                              { "sas1-5661.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:622:225:90ff:fee6:4a46"; };
                              { "sas1-5663.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:649:225:90ff:fee6:ddc0"; };
                              { "sas1-5669.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:636:225:90ff:feed:318c"; };
                              { "sas1-5715.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:635:225:90ff:feed:3064"; };
                              { "sas1-5732.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:636:225:90ff:feef:cb98"; };
                              { "sas1-5892.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:601:225:90ff:feec:2f44"; };
                              { "sas1-5959.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:604:225:90ff:feef:c9da"; };
                              { "sas1-5961.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:604:225:90ff:feef:c974"; };
                              { "sas1-5962.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:604:225:90ff:feef:c96a"; };
                              { "sas1-5964.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:604:225:90ff:feeb:f9b0"; };
                              { "sas1-6100.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:164:428d:5cff:fe34:f33a"; };
                              { "sas1-6161.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:164:428d:5cff:fe34:f98a"; };
                              { "sas1-6171.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:164:428d:5cff:fe34:f8fa"; };
                              { "sas1-6240.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:118:215:b2ff:fea7:76c0"; };
                              { "sas1-6357.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:69f:215:b2ff:fea7:827c"; };
                              { "sas1-6398.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:115:215:b2ff:fea7:7874"; };
                              { "sas1-6406.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:7ed0"; };
                              { "sas1-6539.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:8010"; };
                              { "sas1-6626.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:119:215:b2ff:fea7:7be0"; };
                              { "sas1-6634.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:66c:215:b2ff:fea7:7e34"; };
                              { "sas1-6720.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:11e:215:b2ff:fea7:8c38"; };
                              { "sas1-6730.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a924"; };
                              { "sas1-6735.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:8d9c"; };
                              { "sas1-6743.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:a9dc"; };
                              { "sas1-6757.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:66c:215:b2ff:fea7:a9b0"; };
                              { "sas1-6777.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:6fe4"; };
                              { "sas1-6785.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:abb4"; };
                              { "sas1-6802.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:11f:215:b2ff:fea7:7f2c"; };
                              { "sas1-6805.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:62c:215:b2ff:fea7:b778"; };
                              { "sas1-6851.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:11f:215:b2ff:fea7:8d20"; };
                              { "sas1-6857.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:62c:215:b2ff:fea7:8d04"; };
                              { "sas1-6861.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:11f:215:b2ff:fea7:a9b4"; };
                              { "sas1-6869.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:62c:215:b2ff:fea7:a9d8"; };
                              { "sas1-6925.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12f:215:b2ff:fea7:bb18"; };
                              { "sas1-6935.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12f:215:b2ff:fea7:823c"; };
                              { "sas1-6938.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:123:215:b2ff:fea7:b3a4"; };
                              { "sas1-7005.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:16e:215:b2ff:fea7:abdc"; };
                              { "sas1-7013.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:16e:215:b2ff:fea7:ade4"; };
                              { "sas1-7039.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:120:215:b2ff:fea7:9054"; };
                              { "sas1-7060.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:120:215:b2ff:fea7:ad08"; };
                              { "sas1-7094.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8fc4"; };
                              { "sas1-7138.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8ea4"; };
                              { "sas1-7142.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:83cc"; };
                              { "sas1-7154.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aab0"; };
                              { "sas1-7167.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12c:215:b2ff:fea7:73b4"; };
                              { "sas1-7169.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12c:215:b2ff:fea7:73ac"; };
                              { "sas1-7192.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:122:215:b2ff:fea7:7d40"; };
                              { "sas1-7209.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:122:215:b2ff:fea7:8f24"; };
                              { "sas1-7212.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:122:215:b2ff:fea7:90e0"; };
                              { "sas1-7234.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:121:215:b2ff:fea7:abcc"; };
                              { "sas1-7276.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:128:215:b2ff:fea7:7250"; };
                              { "sas1-7458.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:7fe4"; };
                              { "sas1-7465.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:125:215:b2ff:fea7:817c"; };
                              { "sas1-7475.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:610:215:b2ff:fea7:8bf4"; };
                              { "sas1-7477.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:129:215:b2ff:fea7:8b6c"; };
                              { "sas1-7488.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:80a8"; };
                              { "sas1-7534.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:125:215:b2ff:fea7:7430"; };
                              { "sas1-7569.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:129:215:b2ff:fea7:bc08"; };
                              { "sas1-7591.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:129:215:b2ff:fea7:b990"; };
                              { "sas1-7686.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bf0c"; };
                              { "sas1-7687.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bec8"; };
                              { "sas1-7696.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bddc"; };
                              { "sas1-7727.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:127:215:b2ff:fea7:af04"; };
                              { "sas1-7732.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:127:215:b2ff:fea7:ba58"; };
                              { "sas1-7761.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:73f4"; };
                              { "sas1-7763.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b6e0"; };
                              { "sas1-7790.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b054"; };
                              { "sas1-7803.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:b56c"; };
                              { "sas1-7816.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:8cdc"; };
                              { "sas1-7868.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12e:215:b2ff:fea7:aa10"; };
                              { "sas1-7869.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12e:215:b2ff:fea7:77d4"; };
                              { "sas1-7878.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12e:215:b2ff:fea7:7db8"; };
                              { "sas1-7902.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab88"; };
                              { "sas1-7908.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:8c34"; };
                              { "sas1-7936.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:618:215:b2ff:fea7:8b98"; };
                              { "sas1-7942.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7444"; };
                              { "sas1-8160.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:132:215:b2ff:fea7:afec"; };
                              { "sas1-8179.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:132:215:b2ff:fea7:8b9c"; };
                              { "sas1-8186.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:68f:215:b2ff:fea7:9178"; };
                              { "sas1-8214.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:68f:215:b2ff:fea7:a980"; };
                              { "sas1-8234.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:131:215:b2ff:fea7:b3d4"; };
                              { "sas1-8255.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:6a6:215:b2ff:fea7:b1d0"; };
                              { "sas1-8299.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:132:215:b2ff:fea7:b754"; };
                              { "sas1-8301.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:132:215:b2ff:fea7:b750"; };
                              { "sas1-8357.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:133:215:b2ff:fea7:af7c"; };
                              { "sas1-8369.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:623:215:b2ff:fea7:b2c8"; };
                              { "sas1-8430.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:130:215:b2ff:fea7:aeb8"; };
                              { "sas1-8518.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:170:428d:5cff:fe37:fffe"; };
                              { "sas1-8524.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:617:215:b2ff:fea7:b984"; };
                              { "sas1-8573.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:135:215:b2ff:fea7:bcd8"; };
                              { "sas1-8588.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:142:428d:5cff:fe36:8b26"; };
                              { "sas1-8596.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:142:428d:5cff:fe36:8ac8"; };
                              { "sas1-8612.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:684:215:b2ff:fea7:bf10"; };
                              { "sas1-8615.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:684:215:b2ff:fea7:b834"; };
                              { "sas1-8635.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:134:215:b2ff:fea7:b83c"; };
                              { "sas1-8637.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:134:215:b2ff:fea7:b840"; };
                              { "sas1-8638.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:134:215:b2ff:fea7:b988"; };
                              { "sas1-8674.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:616:215:b2ff:fea7:bc54"; };
                              { "sas1-8694.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:137:215:b2ff:fea7:b640"; };
                              { "sas1-8701.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:137:215:b2ff:fea7:b330"; };
                              { "sas1-8730.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:616:215:b2ff:fea7:8dec"; };
                              { "sas1-8742.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:616:215:b2ff:fea7:b2a4"; };
                              { "sas1-8746.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:616:215:b2ff:fea7:b3ac"; };
                              { "sas1-8826.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1a6:215:b2ff:fea7:90f8"; };
                              { "sas1-8865.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:138:215:b2ff:fea7:bae4"; };
                              { "sas1-8932.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:141:feaa:14ff:fede:3f12"; };
                              { "sas1-8945.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:1ac:feaa:14ff:fede:3f0e"; };
                              { "sas1-8948.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13f:feaa:14ff:fede:3ef0"; };
                              { "sas1-8960.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13f:feaa:14ff:fede:4050"; };
                              { "sas1-8989.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:613:feaa:14ff:fede:4125"; };
                              { "sas1-9038.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:427a"; };
                              { "sas1-9055.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13e:feaa:14ff:fede:3f68"; };
                              { "sas1-9058.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13e:feaa:14ff:fede:3f2e"; };
                              { "sas1-9061.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13e:feaa:14ff:fede:41b0"; };
                              { "sas1-9085.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:438a"; };
                              { "sas1-9142.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3fc2"; };
                              { "sas1-9151.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:422c"; };
                              { "sas1-9164.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:417c"; };
                              { "sas1-9218.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:45bc"; };
                              { "sas1-9222.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13c:feaa:14ff:fede:4040"; };
                              { "sas1-9247.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:401a"; };
                              { "sas1-9282.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:45f4"; };
                              { "sas1-9283.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:143:feaa:14ff:fede:3eb8"; };
                              { "sas1-9361.search.yandex.net"; 32046; 38.000; "2a02:6b8:b000:13a:feaa:14ff:fede:4322"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- sasruweb_apphost
                  sasruimages_apphost = {
                    priority = 25;
                    match_fsm = {
                      host = "sasruimages\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "sas1-1099.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:666:96de:80ff:feec:1c28"; };
                              { "sas1-1352.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:618:215:b2ff:fea8:d3a"; };
                              { "sas1-1370.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d8c"; };
                              { "sas1-1376.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:db6"; };
                              { "sas1-1383.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:a3a"; };
                              { "sas1-1422.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:6d3c"; };
                              { "sas1-1424.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:121:215:b2ff:fea8:cea"; };
                              { "sas1-1426.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:196:215:b2ff:fea8:dba"; };
                              { "sas1-1433.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:112:215:b2ff:fea8:6dc8"; };
                              { "sas1-1440.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:d2a"; };
                              { "sas1-1774.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:625:96de:80ff:feec:198a"; };
                              { "sas1-1958.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:652:96de:80ff:feec:1c24"; };
                              { "sas1-2200.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:654:96de:80ff:feec:186a"; };
                              { "sas1-4461.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:651:96de:80ff:feec:1830"; };
                              { "sas1-5360.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1b82"; };
                              { "sas1-5451.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a9:96de:80ff:feec:f98"; };
                              { "sas1-5455.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:652:96de:80ff:feec:1ec4"; };
                              { "sas1-5456.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1924"; };
                              { "sas1-5462.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:63d:96de:80ff:feec:1316"; };
                              { "sas1-5463.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1002"; };
                              { "sas1-5464.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a1:96de:80ff:feec:171c"; };
                              { "sas1-5466.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:625:96de:80ff:feec:120e"; };
                              { "sas1-5473.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:61c:96de:80ff:feec:11f4"; };
                              { "sas1-5475.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:139:96de:80ff:feec:fcc"; };
                              { "sas1-5476.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:651:96de:80ff:feec:10cc"; };
                              { "sas1-5477.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:652:96de:80ff:feec:18a2"; };
                              { "sas1-5478.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:61c:96de:80ff:feec:1898"; };
                              { "sas1-5485.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:63a:96de:80ff:feec:1a5e"; };
                              { "sas1-5486.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:653:96de:80ff:feec:1876"; };
                              { "sas1-5487.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:653:96de:80ff:feec:1124"; };
                              { "sas1-5488.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:61c:96de:80ff:fee9:1aa"; };
                              { "sas1-5491.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:619:96de:80ff:feec:17de"; };
                              { "sas1-5493.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:653:96de:80ff:fee9:fd"; };
                              { "sas1-5495.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:63c:96de:80ff:feec:157c"; };
                              { "sas1-5498.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:139:96de:80ff:fee9:1de"; };
                              { "sas1-5499.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:139:96de:80ff:fee9:184"; };
                              { "sas1-5501.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:655:96de:80ff:feec:1624"; };
                              { "sas1-5502.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10fe"; };
                              { "sas1-5506.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:655:96de:80ff:feec:1a38"; };
                              { "sas1-5507.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:656:96de:80ff:feec:1a32"; };
                              { "sas1-5508.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:674:96de:80ff:feec:1b9c"; };
                              { "sas1-5513.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:111:96de:80ff:feec:1ba8"; };
                              { "sas1-5516.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:655:96de:80ff:feec:18fc"; };
                              { "sas1-5518.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1cc0"; };
                              { "sas1-5629.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:163:225:90ff:fee6:4d12"; };
                              { "sas1-5649.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:602:225:90ff:fee6:4c7a"; };
                              { "sas1-5657.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:649:225:90ff:fee6:dd88"; };
                              { "sas1-5662.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:649:225:90ff:fee5:bde0"; };
                              { "sas1-5960.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:636:225:90ff:feef:c964"; };
                              { "sas1-5965.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:604:225:90ff:feeb:fba6"; };
                              { "sas1-5966.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:635:225:90ff:feeb:fd9e"; };
                              { "sas1-5967.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:601:225:90ff:feeb:fcd6"; };
                              { "sas1-5968.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:604:225:90ff:feeb:fba2"; };
                              { "sas1-5969.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:601:225:90ff:feeb:f9b8"; };
                              { "sas1-5970.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:100:225:90ff:feeb:fce2"; };
                              { "sas1-5971.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:604:225:90ff:feed:2b8e"; };
                              { "sas1-5972.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:174:225:90ff:feed:2b4e"; };
                              { "sas1-5973.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:100:225:90ff:fee3:9cf2"; };
                              { "sas1-5974.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:100:225:90ff:feed:2fde"; };
                              { "sas1-6351.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:11a:215:b2ff:fea7:8280"; };
                              { "sas1-6752.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:9008"; };
                              { "sas1-6893.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:11d:215:b2ff:fea7:9060"; };
                              { "sas1-6939.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b324"; };
                              { "sas1-6978.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:123:215:b2ff:fea7:b720"; };
                              { "sas1-7095.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:124:215:b2ff:fea7:ab98"; };
                              { "sas1-7098.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:124:215:b2ff:fea7:8ee4"; };
                              { "sas1-7125.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:907c"; };
                              { "sas1-7155.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:122:215:b2ff:fea7:8bf0"; };
                              { "sas1-7156.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aae4"; };
                              { "sas1-7238.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:121:215:b2ff:fea7:ac10"; };
                              { "sas1-7272.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:121:215:b2ff:fea7:8300"; };
                              { "sas1-7286.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:128:215:b2ff:fea7:7aec"; };
                              { "sas1-7287.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:128:215:b2ff:fea7:7910"; };
                              { "sas1-7326.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:126:215:b2ff:fea7:ad8c"; };
                              { "sas1-7330.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:628:215:b2ff:fea7:90d0"; };
                              { "sas1-7331.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc9c"; };
                              { "sas1-7459.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8cd0"; };
                              { "sas1-7494.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:125:215:b2ff:fea7:6590"; };
                              { "sas1-7498.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:125:215:b2ff:fea7:aaa4"; };
                              { "sas1-7825.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:7fbc"; };
                              { "sas1-7843.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:82f0"; };
                              { "sas1-7929.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:12b:215:b2ff:fea7:ac24"; };
                              { "sas1-8873.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:138:215:b2ff:fea7:ba80"; };
                              { "sas1-8979.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3f9e"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "sas1-1099.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:666:96de:80ff:feec:1c28"; };
                              { "sas1-1352.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:618:215:b2ff:fea8:d3a"; };
                              { "sas1-1370.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d8c"; };
                              { "sas1-1376.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:db6"; };
                              { "sas1-1383.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:a3a"; };
                              { "sas1-1422.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:6d3c"; };
                              { "sas1-1424.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:121:215:b2ff:fea8:cea"; };
                              { "sas1-1426.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:196:215:b2ff:fea8:dba"; };
                              { "sas1-1433.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:112:215:b2ff:fea8:6dc8"; };
                              { "sas1-1440.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:d2a"; };
                              { "sas1-1774.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:625:96de:80ff:feec:198a"; };
                              { "sas1-1958.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:652:96de:80ff:feec:1c24"; };
                              { "sas1-2200.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:654:96de:80ff:feec:186a"; };
                              { "sas1-4461.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:651:96de:80ff:feec:1830"; };
                              { "sas1-5360.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1b82"; };
                              { "sas1-5451.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a9:96de:80ff:feec:f98"; };
                              { "sas1-5455.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:652:96de:80ff:feec:1ec4"; };
                              { "sas1-5456.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1924"; };
                              { "sas1-5462.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:63d:96de:80ff:feec:1316"; };
                              { "sas1-5463.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1002"; };
                              { "sas1-5464.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a1:96de:80ff:feec:171c"; };
                              { "sas1-5466.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:625:96de:80ff:feec:120e"; };
                              { "sas1-5473.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:61c:96de:80ff:feec:11f4"; };
                              { "sas1-5475.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:139:96de:80ff:feec:fcc"; };
                              { "sas1-5476.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:651:96de:80ff:feec:10cc"; };
                              { "sas1-5477.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:652:96de:80ff:feec:18a2"; };
                              { "sas1-5478.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:61c:96de:80ff:feec:1898"; };
                              { "sas1-5485.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:63a:96de:80ff:feec:1a5e"; };
                              { "sas1-5486.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:653:96de:80ff:feec:1876"; };
                              { "sas1-5487.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:653:96de:80ff:feec:1124"; };
                              { "sas1-5488.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:61c:96de:80ff:fee9:1aa"; };
                              { "sas1-5491.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:619:96de:80ff:feec:17de"; };
                              { "sas1-5493.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:653:96de:80ff:fee9:fd"; };
                              { "sas1-5495.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:63c:96de:80ff:feec:157c"; };
                              { "sas1-5498.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:139:96de:80ff:fee9:1de"; };
                              { "sas1-5499.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:139:96de:80ff:fee9:184"; };
                              { "sas1-5501.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:655:96de:80ff:feec:1624"; };
                              { "sas1-5502.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10fe"; };
                              { "sas1-5506.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:655:96de:80ff:feec:1a38"; };
                              { "sas1-5507.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:656:96de:80ff:feec:1a32"; };
                              { "sas1-5508.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:674:96de:80ff:feec:1b9c"; };
                              { "sas1-5513.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:111:96de:80ff:feec:1ba8"; };
                              { "sas1-5516.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:655:96de:80ff:feec:18fc"; };
                              { "sas1-5518.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1cc0"; };
                              { "sas1-5629.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:163:225:90ff:fee6:4d12"; };
                              { "sas1-5649.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:602:225:90ff:fee6:4c7a"; };
                              { "sas1-5657.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:649:225:90ff:fee6:dd88"; };
                              { "sas1-5662.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:649:225:90ff:fee5:bde0"; };
                              { "sas1-5960.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:636:225:90ff:feef:c964"; };
                              { "sas1-5965.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:604:225:90ff:feeb:fba6"; };
                              { "sas1-5966.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:635:225:90ff:feeb:fd9e"; };
                              { "sas1-5967.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:601:225:90ff:feeb:fcd6"; };
                              { "sas1-5968.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:604:225:90ff:feeb:fba2"; };
                              { "sas1-5969.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:601:225:90ff:feeb:f9b8"; };
                              { "sas1-5970.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:100:225:90ff:feeb:fce2"; };
                              { "sas1-5971.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:604:225:90ff:feed:2b8e"; };
                              { "sas1-5972.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:174:225:90ff:feed:2b4e"; };
                              { "sas1-5973.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:100:225:90ff:fee3:9cf2"; };
                              { "sas1-5974.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:100:225:90ff:feed:2fde"; };
                              { "sas1-6351.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:11a:215:b2ff:fea7:8280"; };
                              { "sas1-6752.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:9008"; };
                              { "sas1-6893.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:11d:215:b2ff:fea7:9060"; };
                              { "sas1-6939.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b324"; };
                              { "sas1-6978.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:123:215:b2ff:fea7:b720"; };
                              { "sas1-7095.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:124:215:b2ff:fea7:ab98"; };
                              { "sas1-7098.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:124:215:b2ff:fea7:8ee4"; };
                              { "sas1-7125.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:907c"; };
                              { "sas1-7155.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:122:215:b2ff:fea7:8bf0"; };
                              { "sas1-7156.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aae4"; };
                              { "sas1-7238.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:121:215:b2ff:fea7:ac10"; };
                              { "sas1-7272.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:121:215:b2ff:fea7:8300"; };
                              { "sas1-7286.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:128:215:b2ff:fea7:7aec"; };
                              { "sas1-7287.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:128:215:b2ff:fea7:7910"; };
                              { "sas1-7326.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:126:215:b2ff:fea7:ad8c"; };
                              { "sas1-7330.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:628:215:b2ff:fea7:90d0"; };
                              { "sas1-7331.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc9c"; };
                              { "sas1-7459.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8cd0"; };
                              { "sas1-7494.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:125:215:b2ff:fea7:6590"; };
                              { "sas1-7498.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:125:215:b2ff:fea7:aaa4"; };
                              { "sas1-7825.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:7fbc"; };
                              { "sas1-7843.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:82f0"; };
                              { "sas1-7929.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:12b:215:b2ff:fea7:ac24"; };
                              { "sas1-8873.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:138:215:b2ff:fea7:ba80"; };
                              { "sas1-8979.search.yandex.net"; 32046; 28.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3f9e"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- sasruimages_apphost
                  sasruvideo_apphost = {
                    priority = 24;
                    match_fsm = {
                      host = "sasruvideo\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "sas1-0554.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:636:225:90ff:feed:2f90"; };
                              { "sas1-0582.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:601:225:90ff:feed:2e46"; };
                              { "sas1-1040.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:100:225:90ff:feef:ccba"; };
                              { "sas1-1077.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3130"; };
                              { "sas1-1346.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:174:225:90ff:feed:317c"; };
                              { "sas1-1413.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:174:225:90ff:feed:2d94"; };
                              { "sas1-1425.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:128:215:b2ff:fea8:db2"; };
                              { "sas1-1429.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:110:215:b2ff:fea8:d1a"; };
                              { "sas1-1453.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:119:215:b2ff:fea7:efac"; };
                              { "sas1-1488.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:134:215:b2ff:fea8:966"; };
                              { "sas1-1525.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:135:215:b2ff:fea8:d7a"; };
                              { "sas1-1572.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:642:96de:80ff:fe5e:dc42"; };
                              { "sas1-1643.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:653:96de:80ff:fe5e:dca0"; };
                              { "sas1-1664.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:11d:215:b2ff:fea8:6c84"; };
                              { "sas1-1671.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:117:215:b2ff:fea8:d52"; };
                              { "sas1-1758.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:de48"; };
                              { "sas1-1759.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:ba4a"; };
                              { "sas1-1809.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:67c:922b:34ff:fecf:2efa"; };
                              { "sas1-1815.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:67c:96de:80ff:fe8c:bd34"; };
                              { "sas1-1832.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:120:215:b2ff:fea8:a6a"; };
                              { "sas1-1880.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:e984"; };
                              { "sas1-1885.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:da12"; };
                              { "sas1-1915.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17c:96de:80ff:fe8c:e432"; };
                              { "sas1-1979.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17b:96de:80ff:fe8c:d9a8"; };
                              { "sas1-2046.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:67b:96de:80ff:fe8c:d8e6"; };
                              { "sas1-5450.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:625:96de:80ff:feec:16da"; };
                              { "sas1-5452.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1a40"; };
                              { "sas1-5470.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:654:96de:80ff:feec:10a4"; };
                              { "sas1-5504.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:63d:96de:80ff:feec:1ecc"; };
                              { "sas1-5514.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:63a:96de:80ff:feec:18ce"; };
                              { "sas1-5517.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:669:96de:80ff:feec:1902"; };
                              { "sas1-5624.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:160:225:90ff:fee6:4b3c"; };
                              { "sas1-5625.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:163:225:90ff:fee6:4d14"; };
                              { "sas1-5633.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:622:225:90ff:fee6:4860"; };
                              { "sas1-5638.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:602:225:90ff:fee5:bd36"; };
                              { "sas1-5642.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:622:225:90ff:fee5:c134"; };
                              { "sas1-5643.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:161:225:90ff:fee6:de52"; };
                              { "sas1-5645.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:161:225:90ff:fee5:bf6c"; };
                              { "sas1-5648.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:602:225:90ff:fee6:de28"; };
                              { "sas1-5650.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:602:225:90ff:fee6:de54"; };
                              { "sas1-5659.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:649:225:90ff:fee6:dd7c"; };
                              { "sas1-5660.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:649:225:90ff:fee6:df02"; };
                              { "sas1-5665.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:636:225:90ff:feed:2de4"; };
                              { "sas1-5963.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:601:225:90ff:feed:2cea"; };
                              { "sas1-6750.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                              { "sas1-6971.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:123:215:b2ff:fea7:b10c"; };
                              { "sas1-7168.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:122:215:b2ff:fea7:7c88"; };
                              { "sas1-7574.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                              { "sas1-7747.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                              { "sas1-7824.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                              { "sas1-8236.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                              { "sas1-8246.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                              { "sas1-8401.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                              { "sas1-8546.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                              { "sas1-8571.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                              { "sas1-8618.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                              { "sas1-8830.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "sas1-0554.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:636:225:90ff:feed:2f90"; };
                              { "sas1-0582.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:601:225:90ff:feed:2e46"; };
                              { "sas1-1040.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:100:225:90ff:feef:ccba"; };
                              { "sas1-1077.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3130"; };
                              { "sas1-1346.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:174:225:90ff:feed:317c"; };
                              { "sas1-1413.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:174:225:90ff:feed:2d94"; };
                              { "sas1-1425.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:128:215:b2ff:fea8:db2"; };
                              { "sas1-1429.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:110:215:b2ff:fea8:d1a"; };
                              { "sas1-1453.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:119:215:b2ff:fea7:efac"; };
                              { "sas1-1488.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:134:215:b2ff:fea8:966"; };
                              { "sas1-1525.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:135:215:b2ff:fea8:d7a"; };
                              { "sas1-1572.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:642:96de:80ff:fe5e:dc42"; };
                              { "sas1-1643.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:653:96de:80ff:fe5e:dca0"; };
                              { "sas1-1664.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:11d:215:b2ff:fea8:6c84"; };
                              { "sas1-1671.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:117:215:b2ff:fea8:d52"; };
                              { "sas1-1758.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:de48"; };
                              { "sas1-1759.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:ba4a"; };
                              { "sas1-1809.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:67c:922b:34ff:fecf:2efa"; };
                              { "sas1-1815.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:67c:96de:80ff:fe8c:bd34"; };
                              { "sas1-1832.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:120:215:b2ff:fea8:a6a"; };
                              { "sas1-1880.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:e984"; };
                              { "sas1-1885.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:da12"; };
                              { "sas1-1915.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17c:96de:80ff:fe8c:e432"; };
                              { "sas1-1979.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:17b:96de:80ff:fe8c:d9a8"; };
                              { "sas1-2046.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:67b:96de:80ff:fe8c:d8e6"; };
                              { "sas1-5450.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:625:96de:80ff:feec:16da"; };
                              { "sas1-5452.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1a40"; };
                              { "sas1-5470.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:654:96de:80ff:feec:10a4"; };
                              { "sas1-5504.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:63d:96de:80ff:feec:1ecc"; };
                              { "sas1-5514.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:63a:96de:80ff:feec:18ce"; };
                              { "sas1-5517.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:669:96de:80ff:feec:1902"; };
                              { "sas1-5624.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:160:225:90ff:fee6:4b3c"; };
                              { "sas1-5625.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:163:225:90ff:fee6:4d14"; };
                              { "sas1-5633.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:622:225:90ff:fee6:4860"; };
                              { "sas1-5638.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:602:225:90ff:fee5:bd36"; };
                              { "sas1-5642.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:622:225:90ff:fee5:c134"; };
                              { "sas1-5643.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:161:225:90ff:fee6:de52"; };
                              { "sas1-5645.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:161:225:90ff:fee5:bf6c"; };
                              { "sas1-5648.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:602:225:90ff:fee6:de28"; };
                              { "sas1-5650.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:602:225:90ff:fee6:de54"; };
                              { "sas1-5659.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:649:225:90ff:fee6:dd7c"; };
                              { "sas1-5660.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:649:225:90ff:fee6:df02"; };
                              { "sas1-5665.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:636:225:90ff:feed:2de4"; };
                              { "sas1-5963.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:601:225:90ff:feed:2cea"; };
                              { "sas1-6750.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                              { "sas1-6971.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:123:215:b2ff:fea7:b10c"; };
                              { "sas1-7168.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:122:215:b2ff:fea7:7c88"; };
                              { "sas1-7574.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                              { "sas1-7747.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                              { "sas1-7824.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                              { "sas1-8236.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                              { "sas1-8246.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                              { "sas1-8401.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                              { "sas1-8546.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                              { "sas1-8571.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                              { "sas1-8618.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                              { "sas1-8830.search.yandex.net"; 32046; 43.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- sasruvideo_apphost
                  manruweb_apphost = {
                    priority = 23;
                    match_fsm = {
                      host = "manruweb\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "man1-0433.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602e:92e2:baff:fe74:79d6"; };
                              { "man1-0686.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f98"; };
                              { "man1-0926.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600e:92e2:baff:fe56:ea26"; };
                              { "man1-0934.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f3ee"; };
                              { "man1-0976.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600f:92e2:baff:fe56:e9de"; };
                              { "man1-0977.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600f:92e2:baff:fe56:e8f6"; };
                              { "man1-1165.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6021:f652:14ff:fe55:1d70"; };
                              { "man1-1173.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f7ba"; };
                              { "man1-1212.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601a:f652:14ff:fe55:2a70"; };
                              { "man1-1215.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6005:f652:14ff:fe55:1d80"; };
                              { "man1-1216.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602a:f652:14ff:fe55:2a50"; };
                              { "man1-1220.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601b:f652:14ff:fe55:1d30"; };
                              { "man1-1221.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6014:f652:14ff:fe48:9980"; };
                              { "man1-1222.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6014:f652:14ff:fe48:9960"; };
                              { "man1-1223.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6014:f652:14ff:fe48:8ce0"; };
                              { "man1-1284.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5270"; };
                              { "man1-1285.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6000:f652:14ff:fe44:5620"; };
                              { "man1-1287.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6029:f652:14ff:fe44:51b0"; };
                              { "man1-1288.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe44:5630"; };
                              { "man1-1290.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6021:f652:14ff:fe48:a360"; };
                              { "man1-1291.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601f:f652:14ff:fe48:a310"; };
                              { "man1-1292.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601f:f652:14ff:fe44:5930"; };
                              { "man1-1295.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6016:f652:14ff:fe48:9f70"; };
                              { "man1-1306.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6013:f652:14ff:fe44:5120"; };
                              { "man1-1307.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6013:f652:14ff:fe48:8f50"; };
                              { "man1-1323.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d880"; };
                              { "man1-1324.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e730"; };
                              { "man1-1325.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6026:f652:14ff:fe8b:de30"; };
                              { "man1-1358.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:dfb0"; };
                              { "man1-1366.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:f0f0"; };
                              { "man1-1394.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:b340"; };
                              { "man1-1395.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:b830"; };
                              { "man1-1397.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:a610"; };
                              { "man1-1398.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b820"; };
                              { "man1-1447.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:d5a0"; };
                              { "man1-1449.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6020:f652:14ff:fe8b:e710"; };
                              { "man1-1451.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6023:f652:14ff:fe8b:f2e0"; };
                              { "man1-1456.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6026:f652:14ff:fe8b:e780"; };
                              { "man1-1457.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601e:f652:14ff:fe8b:e640"; };
                              { "man1-1458.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e740"; };
                              { "man1-1459.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6097:f652:14ff:fe8b:e700"; };
                              { "man1-1461.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:e610"; };
                              { "man1-1462.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:de20"; };
                              { "man1-1488.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:ac00"; };
                              { "man1-1489.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:b310"; };
                              { "man1-1490.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:abc0"; };
                              { "man1-1492.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:ac40"; };
                              { "man1-1764.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b120"; };
                              { "man1-1875.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:1d30"; };
                              { "man1-2199.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:c070"; };
                              { "man1-2290.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6097:f652:14ff:fe8c:d290"; };
                              { "man1-2572.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6022:f652:14ff:fe8c:1bc0"; };
                              { "man1-2964.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:e7b0"; };
                              { "man1-3014.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6028:f652:14ff:fe55:28b0"; };
                              { "man1-3086.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602a:f652:14ff:fe55:2ba0"; };
                              { "man1-3185.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6097:f652:14ff:fe8c:2a90"; };
                              { "man1-3374.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:d130"; };
                              { "man1-3397.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c6b0"; };
                              { "man1-3491.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6031:92e2:baff:fe75:4b24"; };
                              { "man1-3497.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6034:92e2:baff:fe74:78a0"; };
                              { "man1-3501.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602c:92e2:baff:fe74:7984"; };
                              { "man1-3502.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f37a"; };
                              { "man1-3503.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f4b4"; };
                              { "man1-3504.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f628"; };
                              { "man1-3505.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6011:92e2:baff:fe55:f29c"; };
                              { "man1-3506.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6011:92e2:baff:fe55:f442"; };
                              { "man1-3508.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600e:92e2:baff:fe5b:974a"; };
                              { "man1-3509.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f68c"; };
                              { "man1-3511.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f4bc"; };
                              { "man1-3513.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6010:92e2:baff:fe5b:9dc4"; };
                              { "man1-3514.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f6ee"; };
                              { "man1-3516.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f4ee"; };
                              { "man1-3525.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e0b0"; };
                              { "man1-3531.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b550"; };
                              { "man1-3532.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:cd70"; };
                              { "man1-3536.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:b6f0"; };
                              { "man1-3538.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:21f0"; };
                              { "man1-3539.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:21e0"; };
                              { "man1-3540.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:2c30"; };
                              { "man1-3545.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6019:f652:14ff:fe8c:2390"; };
                              { "man1-3546.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2630"; };
                              { "man1-3547.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2cb0"; };
                              { "man1-3548.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:2250"; };
                              { "man1-3549.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:15e0"; };
                              { "man1-3550.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6011:f652:14ff:fe8c:14e0"; };
                              { "man1-3551.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:2c0"; };
                              { "man1-3553.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601f:f652:14ff:fe8c:21c0"; };
                              { "man1-3554.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6020:f652:14ff:fe8c:1940"; };
                              { "man1-3555.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6020:f652:14ff:fe8c:1390"; };
                              { "man1-3556.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:660"; };
                              { "man1-3557.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6020:e61d:2dff:fe6c:f420"; };
                              { "man1-3558.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:2030"; };
                              { "man1-3559.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:21a0"; };
                              { "man1-3560.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:20e0"; };
                              { "man1-3561.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1f70"; };
                              { "man1-3562.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:2070"; };
                              { "man1-3563.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:1ac0"; };
                              { "man1-3564.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2060"; };
                              { "man1-3565.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2090"; };
                              { "man1-3566.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f490"; };
                              { "man1-3568.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f65c"; };
                              { "man1-3570.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1ca0"; };
                              { "man1-3571.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1840"; };
                              { "man1-3572.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1780"; };
                              { "man1-3573.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6001:f652:14ff:fe8c:16e0"; };
                              { "man1-3574.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6001:f652:14ff:fe8c:1810"; };
                              { "man1-3575.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6037:f652:14ff:fe8c:2da0"; };
                              { "man1-3576.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602a:f652:14ff:fe8c:17c0"; };
                              { "man1-3577.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:ff30"; };
                              { "man1-3578.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:e340"; };
                              { "man1-3580.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602a:f652:14ff:fe8b:fd70"; };
                              { "man1-3581.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:fe40"; };
                              { "man1-3582.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:fdd0"; };
                              { "man1-3583.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6004:f652:14ff:fe8c:1720"; };
                              { "man1-3584.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:fe50"; };
                              { "man1-3585.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:fd50"; };
                              { "man1-3588.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:fda0"; };
                              { "man1-3589.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8c:1790"; };
                              { "man1-3590.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:fd60"; };
                              { "man1-3591.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:17f0"; };
                              { "man1-3593.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:fe60"; };
                              { "man1-3594.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:17e0"; };
                              { "man1-3595.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6000:f652:14ff:fe8c:1cc0"; };
                              { "man1-3596.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:4a0"; };
                              { "man1-3597.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:e290"; };
                              { "man1-3601.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1250"; };
                              { "man1-3602.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1240"; };
                              { "man1-3603.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1830"; };
                              { "man1-3604.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:1220"; };
                              { "man1-3606.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:5e0"; };
                              { "man1-3607.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:2bf0"; };
                              { "man1-3608.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6023:f652:14ff:fe8b:fce0"; };
                              { "man1-3609.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:2bd0"; };
                              { "man1-3610.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:db10"; };
                              { "man1-3611.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:e8c0"; };
                              { "man1-3612.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:eef0"; };
                              { "man1-3613.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:da90"; };
                              { "man1-3614.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7d2a"; };
                              { "man1-3615.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6002:92e2:baff:fe74:7a66"; };
                              { "man1-3616.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6002:92e2:baff:fe74:78b6"; };
                              { "man1-3657.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6002:92e2:baff:fe75:4b96"; };
                              { "man1-3691.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603c:92e2:baff:fe74:776e"; };
                              { "man1-3692.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:be1a"; };
                              { "man1-3693.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7b22"; };
                              { "man1-3694.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7e12"; };
                              { "man1-3695.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7960"; };
                              { "man1-3696.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7f6a"; };
                              { "man1-3697.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7e30"; };
                              { "man1-3698.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7a8c"; };
                              { "man1-3699.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603a:92e2:baff:fe74:779e"; };
                              { "man1-3700.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7b70"; };
                              { "man1-3701.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:b7e8"; };
                              { "man1-3703.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe6b:d10e"; };
                              { "man1-3704.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe74:7f82"; };
                              { "man1-3705.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe6b:d230"; };
                              { "man1-3707.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603c:92e2:baff:fe74:78e8"; };
                              { "man1-3709.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7c62"; };
                              { "man1-3746.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe6f:8136"; };
                              { "man1-3769.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:777a"; };
                              { "man1-3773.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe6f:7efe"; };
                              { "man1-3842.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:bd22"; };
                              { "man1-3843.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:795e"; };
                              { "man1-3855.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7bc6"; };
                              { "man1-3883.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6079:92e2:baff:fea1:763c"; };
                              { "man1-3911.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe74:79a8"; };
                              { "man1-3916.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:c006"; };
                              { "man1-3925.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7638"; };
                              { "man1-3927.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607d:92e2:baff:fea1:765c"; };
                              { "man1-3932.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7a7c"; };
                              { "man1-3933.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e80"; };
                              { "man1-3936.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7a7e"; };
                              { "man1-3967.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe74:78da"; };
                              { "man1-3977.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f66"; };
                              { "man1-4015.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e38"; };
                              { "man1-4016.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7a84"; };
                              { "man1-4031.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607b:92e2:baff:fe74:7ca4"; };
                              { "man1-4032.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7a82"; };
                              { "man1-4033.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e32"; };
                              { "man1-4034.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e42"; };
                              { "man1-4035.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7650"; };
                              { "man1-4037.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603c:92e2:baff:fe75:475a"; };
                              { "man1-4040.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7622"; };
                              { "man1-4041.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7a86"; };
                              { "man1-4043.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7e40"; };
                              { "man1-4065.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7f60"; };
                              { "man1-4070.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7fce"; };
                              { "man1-4071.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7e3e"; };
                              { "man1-4072.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7e4a"; };
                              { "man1-4075.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe74:774e"; };
                              { "man1-4079.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6042:92e2:baff:fe74:7628"; };
                              { "man1-4145.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7640"; };
                              { "man1-4163.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7eca"; };
                              { "man1-4176.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7ee0"; };
                              { "man1-4242.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6043:92e2:baff:fe6f:7e8e"; };
                              { "man1-4253.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:ba7e"; };
                              { "man1-4277.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:bea8"; };
                              { "man1-4339.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7a40"; };
                              { "man1-4365.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe56:e9c2"; };
                              { "man1-4407.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:b8bc"; };
                              { "man1-4420.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe74:79c6"; };
                              { "man1-4421.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe74:77ce"; };
                              { "man1-4423.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:b90c"; };
                              { "man1-4425.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe74:775a"; };
                              { "man1-4428.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe6f:7f10"; };
                              { "man1-4455.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe74:77c2"; };
                              { "man1-4456.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98a"; };
                              { "man1-4467.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe6e:bfae"; };
                              { "man1-4468.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe74:78d8"; };
                              { "man1-4471.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7e42"; };
                              { "man1-4473.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7ed4"; };
                              { "man1-4477.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7be6"; };
                              { "man1-4480.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b990"; };
                              { "man1-4481.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bafa"; };
                              { "man1-4486.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d22"; };
                              { "man1-4498.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7730"; };
                              { "man1-4503.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f24"; };
                              { "man1-4504.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe74:761c"; };
                              { "man1-4505.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe75:4886"; };
                              { "man1-4520.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe52:79ba"; };
                              { "man1-4522.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:ba44"; };
                              { "man1-4524.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe6f:7d14"; };
                              { "man1-4526.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799e"; };
                              { "man1-4527.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:bda4"; };
                              { "man1-4528.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6007:92e2:baff:fe6f:7d72"; };
                              { "man1-4529.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe74:7f7a"; };
                              { "man1-4530.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe74:772c"; };
                              { "man1-4550.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:92e2:baff:fe55:f6c0"; };
                              { "man1-4552.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a20"; };
                              { "man1-4555.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:f652:14ff:fe55:1cf0"; };
                              { "man1-4556.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a80"; };
                              { "man1-4557.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a10"; };
                              { "man1-4558.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2aa0"; };
                              { "man1-5080.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6051:f652:14ff:fe74:3850"; };
                              { "man1-5117.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:604c:e61d:2dff:fe00:9400"; };
                              { "man1-5232.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7762"; };
                              { "man1-5299.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:604e:e61d:2dff:fe01:ef10"; };
                              { "man1-5300.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7642"; };
                              { "man1-5304.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e3c"; };
                              { "man1-5334.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:604f:f652:14ff:fef5:d9a0"; };
                              { "man1-5351.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e3a"; };
                              { "man1-5378.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6050:f652:14ff:fef5:d130"; };
                              { "man1-5392.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6050:f652:14ff:fef5:d960"; };
                              { "man1-5400.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6050:e61d:2dff:fe03:45d0"; };
                              { "man1-5428.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6050:e61d:2dff:fe01:e540"; };
                              { "man1-5432.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7e7e"; };
                              { "man1-5482.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7a76"; };
                              { "man1-5552.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7764"; };
                              { "man1-5593.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6079:92e2:baff:fea1:763e"; };
                              { "man1-5612.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:86a0"; };
                              { "man1-5616.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6055:f652:14ff:fe74:3920"; };
                              { "man1-5637.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8f90"; };
                              { "man1-5645.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6079:92e2:baff:fea1:807e"; };
                              { "man1-5671.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e4c"; };
                              { "man1-5672.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7f64"; };
                              { "man1-5684.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6079:92e2:baff:fea1:8002"; };
                              { "man1-5694.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605b:e61d:2dff:fe01:e790"; };
                              { "man1-5717.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7658"; };
                              { "man1-5746.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605b:e61d:2dff:fe00:9bb0"; };
                              { "man1-5767.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6077:92e2:baff:fea1:78a6"; };
                              { "man1-5786.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6052:f652:14ff:fe74:4220"; };
                              { "man1-5807.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:47e0"; };
                              { "man1-5836.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:4fb0"; };
                              { "man1-5858.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6058:f652:14ff:fef5:d900"; };
                              { "man1-5875.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4360"; };
                              { "man1-5896.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4ca0"; };
                              { "man1-5899.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4f10"; };
                              { "man1-5900.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4f60"; };
                              { "man1-5902.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe01:ed00"; };
                              { "man1-5908.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607b:92e2:baff:fea1:8050"; };
                              { "man1-5912.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:37c0"; };
                              { "man1-5938.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605a:e61d:2dff:fe01:ef50"; };
                              { "man1-5966.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605a:e61d:2dff:fe03:3a10"; };
                              { "man1-5967.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4cb0"; };
                              { "man1-5971.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605a:e61d:2dff:fe03:3a00"; };
                              { "man1-5975.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605a:e61d:2dff:fe00:8630"; };
                              { "man1-5978.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4e10"; };
                              { "man1-5980.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6058:e61d:2dff:fe03:3eb0"; };
                              { "man1-5981.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6058:e61d:2dff:fe03:4630"; };
                              { "man1-5984.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:81a0"; };
                              { "man1-5986.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8610"; };
                              { "man1-5994.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4b00"; };
                              { "man1-6003.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6056:f652:14ff:fef5:c920"; };
                              { "man1-6008.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:5180"; };
                              { "man1-6043.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6061:e61d:2dff:fe00:9b80"; };
                              { "man1-6084.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6051:e61d:2dff:fe03:4d00"; };
                              { "man1-6086.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6051:e61d:2dff:fe00:9ce0"; };
                              { "man1-8204.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6078:92e2:baff:fea1:804c"; };
                              { "man1-8205.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7a80"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "man1-0433.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602e:92e2:baff:fe74:79d6"; };
                              { "man1-0686.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f98"; };
                              { "man1-0926.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600e:92e2:baff:fe56:ea26"; };
                              { "man1-0934.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f3ee"; };
                              { "man1-0976.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600f:92e2:baff:fe56:e9de"; };
                              { "man1-0977.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600f:92e2:baff:fe56:e8f6"; };
                              { "man1-1165.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6021:f652:14ff:fe55:1d70"; };
                              { "man1-1173.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f7ba"; };
                              { "man1-1212.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601a:f652:14ff:fe55:2a70"; };
                              { "man1-1215.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6005:f652:14ff:fe55:1d80"; };
                              { "man1-1216.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602a:f652:14ff:fe55:2a50"; };
                              { "man1-1220.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601b:f652:14ff:fe55:1d30"; };
                              { "man1-1221.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6014:f652:14ff:fe48:9980"; };
                              { "man1-1222.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6014:f652:14ff:fe48:9960"; };
                              { "man1-1223.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6014:f652:14ff:fe48:8ce0"; };
                              { "man1-1284.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5270"; };
                              { "man1-1285.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6000:f652:14ff:fe44:5620"; };
                              { "man1-1287.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6029:f652:14ff:fe44:51b0"; };
                              { "man1-1288.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe44:5630"; };
                              { "man1-1290.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6021:f652:14ff:fe48:a360"; };
                              { "man1-1291.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601f:f652:14ff:fe48:a310"; };
                              { "man1-1292.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601f:f652:14ff:fe44:5930"; };
                              { "man1-1295.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6016:f652:14ff:fe48:9f70"; };
                              { "man1-1306.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6013:f652:14ff:fe44:5120"; };
                              { "man1-1307.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6013:f652:14ff:fe48:8f50"; };
                              { "man1-1323.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d880"; };
                              { "man1-1324.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e730"; };
                              { "man1-1325.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6026:f652:14ff:fe8b:de30"; };
                              { "man1-1358.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:dfb0"; };
                              { "man1-1366.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:f0f0"; };
                              { "man1-1394.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:b340"; };
                              { "man1-1395.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:b830"; };
                              { "man1-1397.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:a610"; };
                              { "man1-1398.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b820"; };
                              { "man1-1447.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:d5a0"; };
                              { "man1-1449.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6020:f652:14ff:fe8b:e710"; };
                              { "man1-1451.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6023:f652:14ff:fe8b:f2e0"; };
                              { "man1-1456.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6026:f652:14ff:fe8b:e780"; };
                              { "man1-1457.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601e:f652:14ff:fe8b:e640"; };
                              { "man1-1458.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e740"; };
                              { "man1-1459.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6097:f652:14ff:fe8b:e700"; };
                              { "man1-1461.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:e610"; };
                              { "man1-1462.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:de20"; };
                              { "man1-1488.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:ac00"; };
                              { "man1-1489.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:b310"; };
                              { "man1-1490.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:abc0"; };
                              { "man1-1492.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:ac40"; };
                              { "man1-1764.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b120"; };
                              { "man1-1875.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:1d30"; };
                              { "man1-2199.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:c070"; };
                              { "man1-2290.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6097:f652:14ff:fe8c:d290"; };
                              { "man1-2572.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6022:f652:14ff:fe8c:1bc0"; };
                              { "man1-2964.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:e7b0"; };
                              { "man1-3014.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6028:f652:14ff:fe55:28b0"; };
                              { "man1-3086.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602a:f652:14ff:fe55:2ba0"; };
                              { "man1-3185.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6097:f652:14ff:fe8c:2a90"; };
                              { "man1-3374.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:d130"; };
                              { "man1-3397.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c6b0"; };
                              { "man1-3491.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6031:92e2:baff:fe75:4b24"; };
                              { "man1-3497.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6034:92e2:baff:fe74:78a0"; };
                              { "man1-3501.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602c:92e2:baff:fe74:7984"; };
                              { "man1-3502.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f37a"; };
                              { "man1-3503.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f4b4"; };
                              { "man1-3504.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f628"; };
                              { "man1-3505.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6011:92e2:baff:fe55:f29c"; };
                              { "man1-3506.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6011:92e2:baff:fe55:f442"; };
                              { "man1-3508.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600e:92e2:baff:fe5b:974a"; };
                              { "man1-3509.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f68c"; };
                              { "man1-3511.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f4bc"; };
                              { "man1-3513.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6010:92e2:baff:fe5b:9dc4"; };
                              { "man1-3514.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f6ee"; };
                              { "man1-3516.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f4ee"; };
                              { "man1-3525.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e0b0"; };
                              { "man1-3531.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b550"; };
                              { "man1-3532.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:cd70"; };
                              { "man1-3536.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:b6f0"; };
                              { "man1-3538.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:21f0"; };
                              { "man1-3539.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:21e0"; };
                              { "man1-3540.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:2c30"; };
                              { "man1-3545.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6019:f652:14ff:fe8c:2390"; };
                              { "man1-3546.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2630"; };
                              { "man1-3547.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2cb0"; };
                              { "man1-3548.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:2250"; };
                              { "man1-3549.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:15e0"; };
                              { "man1-3550.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6011:f652:14ff:fe8c:14e0"; };
                              { "man1-3551.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:2c0"; };
                              { "man1-3553.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601f:f652:14ff:fe8c:21c0"; };
                              { "man1-3554.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6020:f652:14ff:fe8c:1940"; };
                              { "man1-3555.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6020:f652:14ff:fe8c:1390"; };
                              { "man1-3556.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:660"; };
                              { "man1-3557.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6020:e61d:2dff:fe6c:f420"; };
                              { "man1-3558.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:2030"; };
                              { "man1-3559.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:21a0"; };
                              { "man1-3560.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:20e0"; };
                              { "man1-3561.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1f70"; };
                              { "man1-3562.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:2070"; };
                              { "man1-3563.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:1ac0"; };
                              { "man1-3564.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2060"; };
                              { "man1-3565.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2090"; };
                              { "man1-3566.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f490"; };
                              { "man1-3568.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f65c"; };
                              { "man1-3570.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1ca0"; };
                              { "man1-3571.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1840"; };
                              { "man1-3572.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1780"; };
                              { "man1-3573.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6001:f652:14ff:fe8c:16e0"; };
                              { "man1-3574.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6001:f652:14ff:fe8c:1810"; };
                              { "man1-3575.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6037:f652:14ff:fe8c:2da0"; };
                              { "man1-3576.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602a:f652:14ff:fe8c:17c0"; };
                              { "man1-3577.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:ff30"; };
                              { "man1-3578.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:e340"; };
                              { "man1-3580.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602a:f652:14ff:fe8b:fd70"; };
                              { "man1-3581.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:fe40"; };
                              { "man1-3582.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:fdd0"; };
                              { "man1-3583.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6004:f652:14ff:fe8c:1720"; };
                              { "man1-3584.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:fe50"; };
                              { "man1-3585.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:fd50"; };
                              { "man1-3588.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:fda0"; };
                              { "man1-3589.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6039:f652:14ff:fe8c:1790"; };
                              { "man1-3590.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:fd60"; };
                              { "man1-3591.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:17f0"; };
                              { "man1-3593.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:fe60"; };
                              { "man1-3594.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:17e0"; };
                              { "man1-3595.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6000:f652:14ff:fe8c:1cc0"; };
                              { "man1-3596.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:4a0"; };
                              { "man1-3597.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:e290"; };
                              { "man1-3601.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1250"; };
                              { "man1-3602.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1240"; };
                              { "man1-3603.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1830"; };
                              { "man1-3604.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:1220"; };
                              { "man1-3606.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:5e0"; };
                              { "man1-3607.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:2bf0"; };
                              { "man1-3608.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6023:f652:14ff:fe8b:fce0"; };
                              { "man1-3609.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:2bd0"; };
                              { "man1-3610.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:db10"; };
                              { "man1-3611.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:e8c0"; };
                              { "man1-3612.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:eef0"; };
                              { "man1-3613.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:da90"; };
                              { "man1-3614.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7d2a"; };
                              { "man1-3615.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6002:92e2:baff:fe74:7a66"; };
                              { "man1-3616.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6002:92e2:baff:fe74:78b6"; };
                              { "man1-3657.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6002:92e2:baff:fe75:4b96"; };
                              { "man1-3691.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603c:92e2:baff:fe74:776e"; };
                              { "man1-3692.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:be1a"; };
                              { "man1-3693.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7b22"; };
                              { "man1-3694.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7e12"; };
                              { "man1-3695.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7960"; };
                              { "man1-3696.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7f6a"; };
                              { "man1-3697.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7e30"; };
                              { "man1-3698.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7a8c"; };
                              { "man1-3699.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603a:92e2:baff:fe74:779e"; };
                              { "man1-3700.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7b70"; };
                              { "man1-3701.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:b7e8"; };
                              { "man1-3703.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe6b:d10e"; };
                              { "man1-3704.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe74:7f82"; };
                              { "man1-3705.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe6b:d230"; };
                              { "man1-3707.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603c:92e2:baff:fe74:78e8"; };
                              { "man1-3709.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7c62"; };
                              { "man1-3746.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe6f:8136"; };
                              { "man1-3769.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:777a"; };
                              { "man1-3773.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe6f:7efe"; };
                              { "man1-3842.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:bd22"; };
                              { "man1-3843.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603d:92e2:baff:fe74:795e"; };
                              { "man1-3855.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7bc6"; };
                              { "man1-3883.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6079:92e2:baff:fea1:763c"; };
                              { "man1-3911.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe74:79a8"; };
                              { "man1-3916.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:c006"; };
                              { "man1-3925.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7638"; };
                              { "man1-3927.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607d:92e2:baff:fea1:765c"; };
                              { "man1-3932.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7a7c"; };
                              { "man1-3933.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e80"; };
                              { "man1-3936.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7a7e"; };
                              { "man1-3967.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603e:92e2:baff:fe74:78da"; };
                              { "man1-3977.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f66"; };
                              { "man1-4015.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e38"; };
                              { "man1-4016.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7a84"; };
                              { "man1-4031.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607b:92e2:baff:fe74:7ca4"; };
                              { "man1-4032.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7a82"; };
                              { "man1-4033.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e32"; };
                              { "man1-4034.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e42"; };
                              { "man1-4035.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7650"; };
                              { "man1-4037.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603c:92e2:baff:fe75:475a"; };
                              { "man1-4040.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7622"; };
                              { "man1-4041.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7a86"; };
                              { "man1-4043.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7e40"; };
                              { "man1-4065.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7f60"; };
                              { "man1-4070.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7fce"; };
                              { "man1-4071.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7e3e"; };
                              { "man1-4072.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7e4a"; };
                              { "man1-4075.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe74:774e"; };
                              { "man1-4079.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6042:92e2:baff:fe74:7628"; };
                              { "man1-4145.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7640"; };
                              { "man1-4163.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7eca"; };
                              { "man1-4176.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7ee0"; };
                              { "man1-4242.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6043:92e2:baff:fe6f:7e8e"; };
                              { "man1-4253.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:ba7e"; };
                              { "man1-4277.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:bea8"; };
                              { "man1-4339.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7a40"; };
                              { "man1-4365.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe56:e9c2"; };
                              { "man1-4407.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:b8bc"; };
                              { "man1-4420.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe74:79c6"; };
                              { "man1-4421.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe74:77ce"; };
                              { "man1-4423.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:b90c"; };
                              { "man1-4425.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe74:775a"; };
                              { "man1-4428.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:603f:92e2:baff:fe6f:7f10"; };
                              { "man1-4455.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe74:77c2"; };
                              { "man1-4456.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98a"; };
                              { "man1-4467.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe6e:bfae"; };
                              { "man1-4468.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe74:78d8"; };
                              { "man1-4471.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7e42"; };
                              { "man1-4473.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7ed4"; };
                              { "man1-4477.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7be6"; };
                              { "man1-4480.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b990"; };
                              { "man1-4481.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bafa"; };
                              { "man1-4486.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d22"; };
                              { "man1-4498.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7730"; };
                              { "man1-4503.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f24"; };
                              { "man1-4504.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe74:761c"; };
                              { "man1-4505.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6041:92e2:baff:fe75:4886"; };
                              { "man1-4520.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe52:79ba"; };
                              { "man1-4522.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:ba44"; };
                              { "man1-4524.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe6f:7d14"; };
                              { "man1-4526.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799e"; };
                              { "man1-4527.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:bda4"; };
                              { "man1-4528.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6007:92e2:baff:fe6f:7d72"; };
                              { "man1-4529.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe74:7f7a"; };
                              { "man1-4530.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:602d:92e2:baff:fe74:772c"; };
                              { "man1-4550.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:92e2:baff:fe55:f6c0"; };
                              { "man1-4552.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a20"; };
                              { "man1-4555.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:f652:14ff:fe55:1cf0"; };
                              { "man1-4556.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a80"; };
                              { "man1-4557.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a10"; };
                              { "man1-4558.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2aa0"; };
                              { "man1-5080.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6051:f652:14ff:fe74:3850"; };
                              { "man1-5117.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:604c:e61d:2dff:fe00:9400"; };
                              { "man1-5232.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7762"; };
                              { "man1-5299.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:604e:e61d:2dff:fe01:ef10"; };
                              { "man1-5300.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7642"; };
                              { "man1-5304.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e3c"; };
                              { "man1-5334.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:604f:f652:14ff:fef5:d9a0"; };
                              { "man1-5351.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e3a"; };
                              { "man1-5378.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6050:f652:14ff:fef5:d130"; };
                              { "man1-5392.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6050:f652:14ff:fef5:d960"; };
                              { "man1-5400.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6050:e61d:2dff:fe03:45d0"; };
                              { "man1-5428.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6050:e61d:2dff:fe01:e540"; };
                              { "man1-5432.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7e7e"; };
                              { "man1-5482.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7a76"; };
                              { "man1-5552.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7764"; };
                              { "man1-5593.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6079:92e2:baff:fea1:763e"; };
                              { "man1-5612.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:86a0"; };
                              { "man1-5616.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6055:f652:14ff:fe74:3920"; };
                              { "man1-5637.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8f90"; };
                              { "man1-5645.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6079:92e2:baff:fea1:807e"; };
                              { "man1-5671.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e4c"; };
                              { "man1-5672.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7f64"; };
                              { "man1-5684.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6079:92e2:baff:fea1:8002"; };
                              { "man1-5694.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605b:e61d:2dff:fe01:e790"; };
                              { "man1-5717.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7658"; };
                              { "man1-5746.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605b:e61d:2dff:fe00:9bb0"; };
                              { "man1-5767.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6077:92e2:baff:fea1:78a6"; };
                              { "man1-5786.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6052:f652:14ff:fe74:4220"; };
                              { "man1-5807.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:47e0"; };
                              { "man1-5836.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:4fb0"; };
                              { "man1-5858.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6058:f652:14ff:fef5:d900"; };
                              { "man1-5875.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4360"; };
                              { "man1-5896.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4ca0"; };
                              { "man1-5899.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4f10"; };
                              { "man1-5900.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4f60"; };
                              { "man1-5902.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe01:ed00"; };
                              { "man1-5908.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:607b:92e2:baff:fea1:8050"; };
                              { "man1-5912.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:37c0"; };
                              { "man1-5938.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605a:e61d:2dff:fe01:ef50"; };
                              { "man1-5966.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605a:e61d:2dff:fe03:3a10"; };
                              { "man1-5967.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4cb0"; };
                              { "man1-5971.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605a:e61d:2dff:fe03:3a00"; };
                              { "man1-5975.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:605a:e61d:2dff:fe00:8630"; };
                              { "man1-5978.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4e10"; };
                              { "man1-5980.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6058:e61d:2dff:fe03:3eb0"; };
                              { "man1-5981.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6058:e61d:2dff:fe03:4630"; };
                              { "man1-5984.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:81a0"; };
                              { "man1-5986.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8610"; };
                              { "man1-5994.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4b00"; };
                              { "man1-6003.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6056:f652:14ff:fef5:c920"; };
                              { "man1-6008.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:5180"; };
                              { "man1-6043.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6061:e61d:2dff:fe00:9b80"; };
                              { "man1-6084.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6051:e61d:2dff:fe03:4d00"; };
                              { "man1-6086.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6051:e61d:2dff:fe00:9ce0"; };
                              { "man1-8204.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6078:92e2:baff:fea1:804c"; };
                              { "man1-8205.search.yandex.net"; 32046; 39.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7a80"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- manruweb_apphost
                  manruimages_apphost = {
                    priority = 22;
                    match_fsm = {
                      host = "manruimages\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "man1-1076.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f1f2"; };
                              { "man1-1150.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f2ca"; };
                              { "man1-1515.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b150"; };
                              { "man1-1885.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1d50"; };
                              { "man1-1957.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:16f0"; };
                              { "man1-1979.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:550"; };
                              { "man1-2023.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1640"; };
                              { "man1-2087.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e320"; };
                              { "man1-2092.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6098:f652:14ff:fe8c:1e0"; };
                              { "man1-2106.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:720"; };
                              { "man1-2112.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:560"; };
                              { "man1-2383.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6011:f652:14ff:fe55:1ca0"; };
                              { "man1-2873.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e840"; };
                              { "man1-2943.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dc40"; };
                              { "man1-3175.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:1ff0"; };
                              { "man1-3252.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1a10"; };
                              { "man1-3260.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c700"; };
                              { "man1-3261.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c6d0"; };
                              { "man1-3265.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:d4c0"; };
                              { "man1-3375.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c580"; };
                              { "man1-3479.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7bd0"; };
                              { "man1-3484.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:602f:92e2:baff:fe72:7bfa"; };
                              { "man1-3489.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e86"; };
                              { "man1-3493.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:602b:92e2:baff:fe6f:815a"; };
                              { "man1-3498.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:b6a4"; };
                              { "man1-3499.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:7eae"; };
                              { "man1-3500.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:80e8"; };
                              { "man1-3510.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f7b4"; };
                              { "man1-3512.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6f6"; };
                              { "man1-3517.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6014:f652:14ff:fe44:5250"; };
                              { "man1-3520.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:b7d0"; };
                              { "man1-3523.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d6b0"; };
                              { "man1-3524.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d710"; };
                              { "man1-3526.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:d5c0"; };
                              { "man1-3527.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:d6a0"; };
                              { "man1-3528.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aeb0"; };
                              { "man1-3529.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b010"; };
                              { "man1-3533.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cdb0"; };
                              { "man1-3534.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b590"; };
                              { "man1-3535.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cda0"; };
                              { "man1-3537.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:2220"; };
                              { "man1-3542.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd50"; };
                              { "man1-3544.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2610"; };
                              { "man1-3752.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:b9d0"; };
                              { "man1-3822.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7eaa"; };
                              { "man1-3904.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7866"; };
                              { "man1-3959.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603e:92e2:baff:fe6f:80a2"; };
                              { "man1-4025.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603c:92e2:baff:fe6e:ba8c"; };
                              { "man1-4073.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6040:92e2:baff:fe74:770a"; };
                              { "man1-4074.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7dc0"; };
                              { "man1-4076.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7d6c"; };
                              { "man1-4077.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bdda"; };
                              { "man1-4078.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b656"; };
                              { "man1-4080.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6042:92e2:baff:fe6f:7d86"; };
                              { "man1-4081.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b842"; };
                              { "man1-4082.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6043:92e2:baff:fe53:24d4"; };
                              { "man1-4083.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2610"; };
                              { "man1-4084.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2570"; };
                              { "man1-4085.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2434"; };
                              { "man1-4310.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7bcc"; };
                              { "man1-4311.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7dfc"; };
                              { "man1-4638.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:651d:215:b2ff:fea9:913a"; };
                              { "man1-5640.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8a30"; };
                              { "man1-6102.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4ed0"; };
                              { "man1-6134.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3d50"; };
                              { "man1-6150.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:f652:14ff:fef5:da20"; };
                              { "man1-6161.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3890"; };
                              { "man1-6167.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:82d0"; };
                              { "man1-6227.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6057:f652:14ff:fef5:c8b0"; };
                              { "man1-6242.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6057:f652:14ff:fef5:cb40"; };
                              { "man1-6263.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5370"; };
                              { "man1-6359.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6053:f652:14ff:fe55:29c0"; };
                              { "man1-6393.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:48f0"; };
                              { "man1-6413.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:1ad0"; };
                              { "man1-6419.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:4720"; };
                              { "man1-6485.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6063:e61d:2dff:fe04:2430"; };
                              { "man1-6634.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:604f:e61d:2dff:fe01:f370"; };
                              { "man1-6727.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cd90"; };
                              { "man1-6728.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cdd0"; };
                              { "man1-6763.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6058:e61d:2dff:fe04:50d0"; };
                              { "man1-6767.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:4bc0"; };
                              { "man1-6854.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6060:f652:14ff:fef5:c950"; };
                              { "man1-6873.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605e:e61d:2dff:fe04:1ed0"; };
                              { "man1-6886.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3d30"; };
                              { "man1-6900.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3da0"; };
                              { "man1-6903.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3cf0"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "man1-1076.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f1f2"; };
                              { "man1-1150.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f2ca"; };
                              { "man1-1515.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b150"; };
                              { "man1-1885.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1d50"; };
                              { "man1-1957.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:16f0"; };
                              { "man1-1979.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:550"; };
                              { "man1-2023.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1640"; };
                              { "man1-2087.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e320"; };
                              { "man1-2092.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6098:f652:14ff:fe8c:1e0"; };
                              { "man1-2106.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:720"; };
                              { "man1-2112.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:560"; };
                              { "man1-2383.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6011:f652:14ff:fe55:1ca0"; };
                              { "man1-2873.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e840"; };
                              { "man1-2943.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dc40"; };
                              { "man1-3175.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:1ff0"; };
                              { "man1-3252.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1a10"; };
                              { "man1-3260.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c700"; };
                              { "man1-3261.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c6d0"; };
                              { "man1-3265.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:d4c0"; };
                              { "man1-3375.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c580"; };
                              { "man1-3479.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7bd0"; };
                              { "man1-3484.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:602f:92e2:baff:fe72:7bfa"; };
                              { "man1-3489.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e86"; };
                              { "man1-3493.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:602b:92e2:baff:fe6f:815a"; };
                              { "man1-3498.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:b6a4"; };
                              { "man1-3499.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:7eae"; };
                              { "man1-3500.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:80e8"; };
                              { "man1-3510.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f7b4"; };
                              { "man1-3512.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6f6"; };
                              { "man1-3517.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6014:f652:14ff:fe44:5250"; };
                              { "man1-3520.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:b7d0"; };
                              { "man1-3523.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d6b0"; };
                              { "man1-3524.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d710"; };
                              { "man1-3526.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:d5c0"; };
                              { "man1-3527.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:d6a0"; };
                              { "man1-3528.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aeb0"; };
                              { "man1-3529.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b010"; };
                              { "man1-3533.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cdb0"; };
                              { "man1-3534.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b590"; };
                              { "man1-3535.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cda0"; };
                              { "man1-3537.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:2220"; };
                              { "man1-3542.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd50"; };
                              { "man1-3544.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2610"; };
                              { "man1-3752.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:b9d0"; };
                              { "man1-3822.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7eaa"; };
                              { "man1-3904.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7866"; };
                              { "man1-3959.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603e:92e2:baff:fe6f:80a2"; };
                              { "man1-4025.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603c:92e2:baff:fe6e:ba8c"; };
                              { "man1-4073.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6040:92e2:baff:fe74:770a"; };
                              { "man1-4074.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7dc0"; };
                              { "man1-4076.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7d6c"; };
                              { "man1-4077.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bdda"; };
                              { "man1-4078.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b656"; };
                              { "man1-4080.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6042:92e2:baff:fe6f:7d86"; };
                              { "man1-4081.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b842"; };
                              { "man1-4082.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6043:92e2:baff:fe53:24d4"; };
                              { "man1-4083.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2610"; };
                              { "man1-4084.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2570"; };
                              { "man1-4085.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2434"; };
                              { "man1-4310.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7bcc"; };
                              { "man1-4311.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7dfc"; };
                              { "man1-4638.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:651d:215:b2ff:fea9:913a"; };
                              { "man1-5640.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8a30"; };
                              { "man1-6102.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4ed0"; };
                              { "man1-6134.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3d50"; };
                              { "man1-6150.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:f652:14ff:fef5:da20"; };
                              { "man1-6161.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3890"; };
                              { "man1-6167.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:82d0"; };
                              { "man1-6227.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6057:f652:14ff:fef5:c8b0"; };
                              { "man1-6242.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6057:f652:14ff:fef5:cb40"; };
                              { "man1-6263.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5370"; };
                              { "man1-6359.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6053:f652:14ff:fe55:29c0"; };
                              { "man1-6393.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:48f0"; };
                              { "man1-6413.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:1ad0"; };
                              { "man1-6419.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:4720"; };
                              { "man1-6485.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6063:e61d:2dff:fe04:2430"; };
                              { "man1-6634.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:604f:e61d:2dff:fe01:f370"; };
                              { "man1-6727.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cd90"; };
                              { "man1-6728.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cdd0"; };
                              { "man1-6763.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6058:e61d:2dff:fe04:50d0"; };
                              { "man1-6767.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:4bc0"; };
                              { "man1-6854.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:6060:f652:14ff:fef5:c950"; };
                              { "man1-6873.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605e:e61d:2dff:fe04:1ed0"; };
                              { "man1-6886.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3d30"; };
                              { "man1-6900.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3da0"; };
                              { "man1-6903.search.yandex.net"; 32046; 29.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3cf0"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- manruimages_apphost
                  manruvideo_apphost = {
                    priority = 21;
                    match_fsm = {
                      host = "manruvideo\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "man1-0978.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f404"; };
                              { "man1-1191.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6018:f652:14ff:fe48:9670"; };
                              { "man1-1209.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6028:f652:14ff:fe55:2a90"; };
                              { "man1-1282.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5280"; };
                              { "man1-1286.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6001:f652:14ff:fe44:56f0"; };
                              { "man1-1293.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6020:f652:14ff:fe48:9fa0"; };
                              { "man1-1294.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6015:f652:14ff:fe44:50f0"; };
                              { "man1-1296.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6016:f652:14ff:fe44:57c0"; };
                              { "man1-1396.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:a9f0"; };
                              { "man1-1400.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:b420"; };
                              { "man1-1446.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6020:f652:14ff:fe8b:f440"; };
                              { "man1-1448.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:f410"; };
                              { "man1-1450.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601e:f652:14ff:fe8b:d8a0"; };
                              { "man1-1454.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:eaf0"; };
                              { "man1-1455.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:e5f0"; };
                              { "man1-1748.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6027:f652:14ff:fe8b:aea0"; };
                              { "man1-1765.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b0d0"; };
                              { "man1-1927.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:be70"; };
                              { "man1-1947.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6029:f652:14ff:fe8b:ea20"; };
                              { "man1-2252.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:dd50"; };
                              { "man1-2264.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:d620"; };
                              { "man1-2453.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:d1b0"; };
                              { "man1-2464.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6021:f652:14ff:fe55:3560"; };
                              { "man1-2529.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:110"; };
                              { "man1-2582.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:c780"; };
                              { "man1-2670.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:3d0"; };
                              { "man1-2733.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:db20"; };
                              { "man1-2800.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:f460"; };
                              { "man1-2848.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e8b0"; };
                              { "man1-2960.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:edb0"; };
                              { "man1-3507.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f3a4"; };
                              { "man1-3515.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f680"; };
                              { "man1-3519.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6014:f652:14ff:fe44:54d0"; };
                              { "man1-3522.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:ab20"; };
                              { "man1-3530.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aed0"; };
                              { "man1-3541.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:e0"; };
                              { "man1-3543.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd70"; };
                              { "man1-3552.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:fd00"; };
                              { "man1-3567.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f4a0"; };
                              { "man1-3569.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1850"; };
                              { "man1-3579.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:602a:f652:14ff:fe8b:fd90"; };
                              { "man1-3586.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:fe00"; };
                              { "man1-3587.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6037:f652:14ff:fe8c:70"; };
                              { "man1-3592.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:1cd0"; };
                              { "man1-3598.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6000:f652:14ff:fe8c:f30"; };
                              { "man1-3599.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:fe30"; };
                              { "man1-3600.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6028:f652:14ff:fe8c:1760"; };
                              { "man1-3605.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:1800"; };
                              { "man1-3617.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7d6e"; };
                              { "man1-3690.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:7d8a"; };
                              { "man1-3702.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7740"; };
                              { "man1-3706.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:81a0"; };
                              { "man1-3708.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:b980"; };
                              { "man1-3783.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7aa8"; };
                              { "man1-3874.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7bf8"; };
                              { "man1-3926.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:607a:92e2:baff:fea1:8052"; };
                              { "man1-3971.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f5c"; };
                              { "man1-4044.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:607c:92e2:baff:fea1:8054"; };
                              { "man1-4051.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603c:92e2:baff:fe74:7d78"; };
                              { "man1-4426.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:bdb2"; };
                              { "man1-4461.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7a1c"; };
                              { "man1-4483.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7712"; };
                              { "man1-4489.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98c"; };
                              { "man1-4497.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f42"; };
                              { "man1-4521.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:602d:92e2:baff:fe52:7a94"; };
                              { "man1-4525.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799c"; };
                              { "man1-4548.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a00"; };
                              { "man1-4553.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601d:f652:14ff:fe55:29d0"; };
                              { "man1-5346.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:607c:92e2:baff:fe6e:b8b6"; };
                              { "man1-5399.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7624"; };
                              { "man1-5503.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7e48"; };
                              { "man1-5622.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8f60"; };
                              { "man1-5826.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:4840"; };
                              { "man1-5914.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6059:e61d:2dff:fe00:8520"; };
                              { "man1-5965.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7640"; };
                              { "man1-6664.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6047:f652:14ff:fe74:3c10"; };
                              { "man1-7451.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1f30"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "man1-0978.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f404"; };
                              { "man1-1191.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6018:f652:14ff:fe48:9670"; };
                              { "man1-1209.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6028:f652:14ff:fe55:2a90"; };
                              { "man1-1282.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5280"; };
                              { "man1-1286.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6001:f652:14ff:fe44:56f0"; };
                              { "man1-1293.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6020:f652:14ff:fe48:9fa0"; };
                              { "man1-1294.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6015:f652:14ff:fe44:50f0"; };
                              { "man1-1296.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6016:f652:14ff:fe44:57c0"; };
                              { "man1-1396.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:a9f0"; };
                              { "man1-1400.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:b420"; };
                              { "man1-1446.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6020:f652:14ff:fe8b:f440"; };
                              { "man1-1448.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:f410"; };
                              { "man1-1450.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601e:f652:14ff:fe8b:d8a0"; };
                              { "man1-1454.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:eaf0"; };
                              { "man1-1455.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:e5f0"; };
                              { "man1-1748.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6027:f652:14ff:fe8b:aea0"; };
                              { "man1-1765.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b0d0"; };
                              { "man1-1927.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:be70"; };
                              { "man1-1947.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6029:f652:14ff:fe8b:ea20"; };
                              { "man1-2252.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:dd50"; };
                              { "man1-2264.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:d620"; };
                              { "man1-2453.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:d1b0"; };
                              { "man1-2464.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6021:f652:14ff:fe55:3560"; };
                              { "man1-2529.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:110"; };
                              { "man1-2582.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:c780"; };
                              { "man1-2670.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:3d0"; };
                              { "man1-2733.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:db20"; };
                              { "man1-2800.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:f460"; };
                              { "man1-2848.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e8b0"; };
                              { "man1-2960.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:edb0"; };
                              { "man1-3507.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f3a4"; };
                              { "man1-3515.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f680"; };
                              { "man1-3519.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6014:f652:14ff:fe44:54d0"; };
                              { "man1-3522.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:ab20"; };
                              { "man1-3530.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aed0"; };
                              { "man1-3541.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:e0"; };
                              { "man1-3543.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd70"; };
                              { "man1-3552.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:fd00"; };
                              { "man1-3567.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f4a0"; };
                              { "man1-3569.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1850"; };
                              { "man1-3579.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:602a:f652:14ff:fe8b:fd90"; };
                              { "man1-3586.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:fe00"; };
                              { "man1-3587.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6037:f652:14ff:fe8c:70"; };
                              { "man1-3592.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:1cd0"; };
                              { "man1-3598.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6000:f652:14ff:fe8c:f30"; };
                              { "man1-3599.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:fe30"; };
                              { "man1-3600.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6028:f652:14ff:fe8c:1760"; };
                              { "man1-3605.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:1800"; };
                              { "man1-3617.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7d6e"; };
                              { "man1-3690.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:7d8a"; };
                              { "man1-3702.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7740"; };
                              { "man1-3706.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:81a0"; };
                              { "man1-3708.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:b980"; };
                              { "man1-3783.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7aa8"; };
                              { "man1-3874.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7bf8"; };
                              { "man1-3926.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:607a:92e2:baff:fea1:8052"; };
                              { "man1-3971.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f5c"; };
                              { "man1-4044.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:607c:92e2:baff:fea1:8054"; };
                              { "man1-4051.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603c:92e2:baff:fe74:7d78"; };
                              { "man1-4426.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:bdb2"; };
                              { "man1-4461.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7a1c"; };
                              { "man1-4483.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7712"; };
                              { "man1-4489.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98c"; };
                              { "man1-4497.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f42"; };
                              { "man1-4521.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:602d:92e2:baff:fe52:7a94"; };
                              { "man1-4525.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799c"; };
                              { "man1-4548.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a00"; };
                              { "man1-4553.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:601d:f652:14ff:fe55:29d0"; };
                              { "man1-5346.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:607c:92e2:baff:fe6e:b8b6"; };
                              { "man1-5399.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7624"; };
                              { "man1-5503.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7e48"; };
                              { "man1-5622.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8f60"; };
                              { "man1-5826.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:4840"; };
                              { "man1-5914.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6059:e61d:2dff:fe00:8520"; };
                              { "man1-5965.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7640"; };
                              { "man1-6664.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:6047:f652:14ff:fe74:3c10"; };
                              { "man1-7451.search.yandex.net"; 32046; 22.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1f30"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- manruvideo_apphost
                  vlaruweb_apphost = {
                    priority = 20;
                    match_fsm = {
                      host = "vlaruweb\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "vla1-0234.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8b4"; };
                              { "vla1-0239.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:9cc1"; };
                              { "vla1-0241.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:81:0:604:db7:a92f"; };
                              { "vla1-0242.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:81:0:604:db7:a7c6"; };
                              { "vla1-0244.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:81:0:604:db7:a8cc"; };
                              { "vla1-0259.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8f4"; };
                              { "vla1-0262.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8ee"; };
                              { "vla1-0265.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8e8"; };
                              { "vla1-0270.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a844"; };
                              { "vla1-0273.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8a9"; };
                              { "vla1-0279.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:98:0:604:db7:9ce2"; };
                              { "vla1-0281.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:98:0:604:db7:a3d0"; };
                              { "vla1-0303.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:98:0:604:db7:a3c5"; };
                              { "vla1-0318.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8ab"; };
                              { "vla1-0334.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:93:0:604:db7:aaf4"; };
                              { "vla1-0343.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:93:0:604:db8:db3a"; };
                              { "vla1-0355.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eb9d"; };
                              { "vla1-0362.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eba0"; };
                              { "vla1-0366.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eb7e"; };
                              { "vla1-0368.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eb36"; };
                              { "vla1-0376.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eb81"; };
                              { "vla1-0385.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eb10"; };
                              { "vla1-0398.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:12:0:604:db7:9b44"; };
                              { "vla1-0429.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:27:0:604:db7:9f71"; };
                              { "vla1-0431.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:26:0:604:db7:9f9b"; };
                              { "vla1-0476.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:34:0:604:db7:9c8b"; };
                              { "vla1-0496.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:17:0:604:db7:9ab7"; };
                              { "vla1-0510.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:13:0:604:db7:9caa"; };
                              { "vla1-0514.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1b:0:604:db7:9d41"; };
                              { "vla1-0523.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:27:0:604:db7:9f70"; };
                              { "vla1-0546.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:17:0:604:db7:9927"; };
                              { "vla1-0569.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:52:0:604:db7:a462"; };
                              { "vla1-0575.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:54:0:604:db7:a4ca"; };
                              { "vla1-0600.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:53:0:604:db7:9cf8"; };
                              { "vla1-0655.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1b:0:604:db7:9b85"; };
                              { "vla1-0671.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:52:0:604:db7:a4e8"; };
                              { "vla1-0672.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:13:0:604:db7:99ea"; };
                              { "vla1-0692.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:3c:0:604:db7:9ee6"; };
                              { "vla1-0706.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:53:0:604:db7:9ced"; };
                              { "vla1-0770.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:44:0:604:db7:a559"; };
                              { "vla1-0773.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:3c:0:604:db7:9ee0"; };
                              { "vla1-0973.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:43:0:604:db7:9e20"; };
                              { "vla1-1029.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:71:0:604:db7:a420"; };
                              { "vla1-1067.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:78:0:604:db7:aacf"; };
                              { "vla1-1092.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:54:0:604:db7:a5a7"; };
                              { "vla1-1192.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:81:0:604:d8f:eb20"; };
                              { "vla1-1219.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8d:0:604:db7:aa47"; };
                              { "vla1-1226.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:43:0:604:db7:a545"; };
                              { "vla1-1277.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:52:0:604:db7:a466"; };
                              { "vla1-1314.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:54:0:604:db7:a67e"; };
                              { "vla1-1411.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:4c:0:604:db7:a0f2"; };
                              { "vla1-1521.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:44:0:604:db7:9f37"; };
                              { "vla1-1562.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:52:0:604:db7:a66a"; };
                              { "vla1-1637.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:67:0:604:db7:a2af"; };
                              { "vla1-1639.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:67:0:604:db7:a328"; };
                              { "vla1-1646.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:71:0:604:db7:a22d"; };
                              { "vla1-1668.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:44:0:604:db7:a73a"; };
                              { "vla1-1684.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:12:0:604:db7:9a66"; };
                              { "vla1-1716.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:67:0:604:db7:9cd7"; };
                              { "vla1-1728.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:57:0:604:db7:a604"; };
                              { "vla1-1774.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:71:0:604:db7:a789"; };
                              { "vla1-1818.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:45:0:604:db7:a6fa"; };
                              { "vla1-1826.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:88:0:604:db7:a9e5"; };
                              { "vla1-1828.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8d:0:604:db7:ac33"; };
                              { "vla1-1830.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:78:0:604:db7:a780"; };
                              { "vla1-1850.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8d:0:604:db7:aa43"; };
                              { "vla1-1881.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:71:0:604:db7:9d0c"; };
                              { "vla1-1883.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:71:0:604:db7:a775"; };
                              { "vla1-1933.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:78:0:604:db7:ab23"; };
                              { "vla1-1950.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8d:0:604:db7:a9b5"; };
                              { "vla1-1976.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:3c:0:604:db7:a450"; };
                              { "vla1-1977.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:57:0:604:db7:a565"; };
                              { "vla1-1993.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:43:0:604:db7:a0e8"; };
                              { "vla1-1999.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8d:0:604:db7:aa1c"; };
                              { "vla1-2014.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:44:0:604:db7:a668"; };
                              { "vla1-2034.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:78:0:604:db7:a94d"; };
                              { "vla1-2076.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:13:0:604:db7:9be1"; };
                              { "vla1-2077.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:3c:0:604:db7:9d50"; };
                              { "vla1-2081.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:84:0:604:db7:aac4"; };
                              { "vla1-2111.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:13:0:604:db7:9af3"; };
                              { "vla1-2120.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:17:0:604:db7:9925"; };
                              { "vla1-2123.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:53:0:604:db7:9d4c"; };
                              { "vla1-2128.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:3c:0:604:db7:9ef1"; };
                              { "vla1-2136.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:13:0:604:db7:99ee"; };
                              { "vla1-2161.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1b:0:604:db7:9a71"; };
                              { "vla1-2193.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:45:0:604:db7:9d62"; };
                              { "vla1-2206.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:88:0:604:db8:db38"; };
                              { "vla1-2221.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9a:0:604:db7:a9df"; };
                              { "vla1-2339.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:86:0:604:db7:a85f"; };
                              { "vla1-2340.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7d:0:604:db7:a3d6"; };
                              { "vla1-2391.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7d:0:604:db7:a19a"; };
                              { "vla1-2396.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8b:0:604:db7:abe0"; };
                              { "vla1-2397.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a959"; };
                              { "vla1-2400.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a962"; };
                              { "vla1-2401.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8a:0:604:db7:a977"; };
                              { "vla1-2402.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:95:0:604:db7:ab46"; };
                              { "vla1-2407.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8b:0:604:db7:abdc"; };
                              { "vla1-2415.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:94:0:604:db7:aa3e"; };
                              { "vla1-2418.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7c:0:604:db7:9df5"; };
                              { "vla1-2422.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:95:0:604:db7:a9c7"; };
                              { "vla1-2423.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:95:0:604:db7:ab1a"; };
                              { "vla1-2427.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:ab19"; };
                              { "vla1-2429.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8a:0:604:db7:a972"; };
                              { "vla1-2431.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a861"; };
                              { "vla1-2432.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:94:0:604:db7:a7c8"; };
                              { "vla1-2435.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:97:0:604:db7:a93e"; };
                              { "vla1-2438.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:97:0:604:db7:a922"; };
                              { "vla1-2441.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:94:0:604:db7:aaef"; };
                              { "vla1-2442.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a95c"; };
                              { "vla1-2448.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:97:0:604:db7:a7f3"; };
                              { "vla1-2461.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:95:0:604:db7:aa56"; };
                              { "vla1-2470.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:94:0:604:db7:a7ba"; };
                              { "vla1-2484.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a37d"; };
                              { "vla1-2498.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a95f"; };
                              { "vla1-2511.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9b:0:604:db7:aa2b"; };
                              { "vla1-2512.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:97:0:604:db7:a915"; };
                              { "vla1-2522.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a2:0:604:db7:9956"; };
                              { "vla1-2531.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a2:0:604:db7:9b6a"; };
                              { "vla1-2547.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:2f:0:604:db7:9bd3"; };
                              { "vla1-2549.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a2:0:604:db7:9bdf"; };
                              { "vla1-2563.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1f:0:604:db7:9c52"; };
                              { "vla1-2589.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1f:0:604:db7:9fa6"; };
                              { "vla1-2606.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:32:0:604:db7:9f96"; };
                              { "vla1-2637.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:65:0:604:db7:9ca2"; };
                              { "vla1-2651.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1f:0:604:db7:9c53"; };
                              { "vla1-2667.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:2f:0:604:db7:9f86"; };
                              { "vla1-2704.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:2f:0:604:db7:9c82"; };
                              { "vla1-2723.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:32:0:604:db7:a52f"; };
                              { "vla1-2727.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:32:0:604:db7:99af"; };
                              { "vla1-2774.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:74:0:604:db7:9efa"; };
                              { "vla1-2881.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:32:0:604:db7:9c07"; };
                              { "vla1-2964.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6e:0:604:db7:a3fd"; };
                              { "vla1-2968.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6e:0:604:db7:a263"; };
                              { "vla1-2993.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6c:0:604:db7:a159"; };
                              { "vla1-2998.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:70:0:604:db7:a410"; };
                              { "vla1-3011.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:19:0:604:db6:e746"; };
                              { "vla1-3015.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:70:0:604:db7:a232"; };
                              { "vla1-3040.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a3:0:604:db7:a733"; };
                              { "vla1-3059.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a3:0:604:db7:9ddf"; };
                              { "vla1-3102.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:74:0:604:db7:98fd"; };
                              { "vla1-3125.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:11:0:604:db7:98f0"; };
                              { "vla1-3132.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6c:0:604:db7:a520"; };
                              { "vla1-3149.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:70:0:604:db7:a281"; };
                              { "vla1-3174.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:11:0:604:db7:998d"; };
                              { "vla1-3247.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6c:0:604:db7:9c25"; };
                              { "vla1-3259.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:19:0:604:db7:9ac9"; };
                              { "vla1-3263.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:11:0:604:db7:99fd"; };
                              { "vla1-3278.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:19:0:604:db7:99e8"; };
                              { "vla1-3285.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:11:0:604:db7:98f2"; };
                              { "vla1-3296.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:19:0:604:db7:9a36"; };
                              { "vla1-3297.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a3:0:604:db7:a6f5"; };
                              { "vla1-3312.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6e:0:604:db7:a285"; };
                              { "vla1-3401.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:74:0:604:db7:9eb9"; };
                              { "vla1-3507.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a0:0:604:db7:a4f0"; };
                              { "vla1-3521.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:73:0:604:db7:a5de"; };
                              { "vla1-3553.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a0:0:604:db7:a672"; };
                              { "vla1-3571.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:73:0:604:db7:a0cf"; };
                              { "vla1-3580.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:73:0:604:db7:a0d6"; };
                              { "vla1-3606.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a1:0:604:db7:a292"; };
                              { "vla1-3610.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a1:0:604:db7:a00b"; };
                              { "vla1-3619.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a1:0:604:db7:a28f"; };
                              { "vla1-3626.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:63:0:604:db7:a309"; };
                              { "vla1-3632.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:46:0:604:db7:a0b4"; };
                              { "vla1-3635.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:46:0:604:db7:a552"; };
                              { "vla1-3664.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:72:0:604:db7:9e21"; };
                              { "vla1-3667.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:80:0:604:db7:a927"; };
                              { "vla1-3671.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:65:0:604:db7:a097"; };
                              { "vla1-3672.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:63:0:604:db7:a2f9"; };
                              { "vla1-3691.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:86:0:604:db7:abac"; };
                              { "vla1-3693.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:72:0:604:db7:a5c1"; };
                              { "vla1-3698.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:65:0:604:db7:a26a"; };
                              { "vla1-3699.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:86:0:604:db7:aba7"; };
                              { "vla1-3720.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:63:0:604:db7:a2e3"; };
                              { "vla1-3726.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:46:0:604:db7:a549"; };
                              { "vla1-3753.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a0:0:604:db7:a47b"; };
                              { "vla1-3759.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:86:0:604:db7:a82a"; };
                              { "vla1-3762.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:80:0:604:db7:a8e7"; };
                              { "vla1-3802.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7f:0:604:db7:a1fe"; };
                              { "vla1-3841.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:72:0:604:db7:a5bf"; };
                              { "vla1-3850.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7d:0:604:db7:9e5f"; };
                              { "vla1-3966.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7f:0:604:db7:a3e7"; };
                              { "vla1-3996.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a0:0:604:db7:a503"; };
                              { "vla1-4164.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7f:0:604:db7:a3c9"; };
                              { "vla1-4219.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8a:0:604:db7:a80e"; };
                              { "vla1-4269.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:90:0:604:db7:a87e"; };
                              { "vla1-4288.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:86:0:604:db7:ac10"; };
                              { "vla1-4308.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:90:0:604:db7:a996"; };
                              { "vla1-4491.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9b:0:604:db7:aa24"; };
                              { "vla1-4575.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:77:0:604:db7:a941"; };
                              { "vla1-4581.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:77:0:604:db7:a928"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "vla1-0234.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8b4"; };
                              { "vla1-0239.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:9cc1"; };
                              { "vla1-0241.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:81:0:604:db7:a92f"; };
                              { "vla1-0242.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:81:0:604:db7:a7c6"; };
                              { "vla1-0244.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:81:0:604:db7:a8cc"; };
                              { "vla1-0259.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8f4"; };
                              { "vla1-0262.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8ee"; };
                              { "vla1-0265.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8e8"; };
                              { "vla1-0270.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a844"; };
                              { "vla1-0273.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8a9"; };
                              { "vla1-0279.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:98:0:604:db7:9ce2"; };
                              { "vla1-0281.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:98:0:604:db7:a3d0"; };
                              { "vla1-0303.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:98:0:604:db7:a3c5"; };
                              { "vla1-0318.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9e:0:604:db7:a8ab"; };
                              { "vla1-0334.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:93:0:604:db7:aaf4"; };
                              { "vla1-0343.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:93:0:604:db8:db3a"; };
                              { "vla1-0355.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eb9d"; };
                              { "vla1-0362.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eba0"; };
                              { "vla1-0366.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eb7e"; };
                              { "vla1-0368.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eb36"; };
                              { "vla1-0376.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eb81"; };
                              { "vla1-0385.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9d:0:604:d8f:eb10"; };
                              { "vla1-0398.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:12:0:604:db7:9b44"; };
                              { "vla1-0429.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:27:0:604:db7:9f71"; };
                              { "vla1-0431.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:26:0:604:db7:9f9b"; };
                              { "vla1-0476.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:34:0:604:db7:9c8b"; };
                              { "vla1-0496.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:17:0:604:db7:9ab7"; };
                              { "vla1-0510.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:13:0:604:db7:9caa"; };
                              { "vla1-0514.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1b:0:604:db7:9d41"; };
                              { "vla1-0523.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:27:0:604:db7:9f70"; };
                              { "vla1-0546.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:17:0:604:db7:9927"; };
                              { "vla1-0569.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:52:0:604:db7:a462"; };
                              { "vla1-0575.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:54:0:604:db7:a4ca"; };
                              { "vla1-0600.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:53:0:604:db7:9cf8"; };
                              { "vla1-0655.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1b:0:604:db7:9b85"; };
                              { "vla1-0671.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:52:0:604:db7:a4e8"; };
                              { "vla1-0672.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:13:0:604:db7:99ea"; };
                              { "vla1-0692.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:3c:0:604:db7:9ee6"; };
                              { "vla1-0706.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:53:0:604:db7:9ced"; };
                              { "vla1-0770.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:44:0:604:db7:a559"; };
                              { "vla1-0773.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:3c:0:604:db7:9ee0"; };
                              { "vla1-0973.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:43:0:604:db7:9e20"; };
                              { "vla1-1029.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:71:0:604:db7:a420"; };
                              { "vla1-1067.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:78:0:604:db7:aacf"; };
                              { "vla1-1092.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:54:0:604:db7:a5a7"; };
                              { "vla1-1192.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:81:0:604:d8f:eb20"; };
                              { "vla1-1219.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8d:0:604:db7:aa47"; };
                              { "vla1-1226.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:43:0:604:db7:a545"; };
                              { "vla1-1277.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:52:0:604:db7:a466"; };
                              { "vla1-1314.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:54:0:604:db7:a67e"; };
                              { "vla1-1411.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:4c:0:604:db7:a0f2"; };
                              { "vla1-1521.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:44:0:604:db7:9f37"; };
                              { "vla1-1562.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:52:0:604:db7:a66a"; };
                              { "vla1-1637.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:67:0:604:db7:a2af"; };
                              { "vla1-1639.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:67:0:604:db7:a328"; };
                              { "vla1-1646.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:71:0:604:db7:a22d"; };
                              { "vla1-1668.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:44:0:604:db7:a73a"; };
                              { "vla1-1684.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:12:0:604:db7:9a66"; };
                              { "vla1-1716.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:67:0:604:db7:9cd7"; };
                              { "vla1-1728.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:57:0:604:db7:a604"; };
                              { "vla1-1774.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:71:0:604:db7:a789"; };
                              { "vla1-1818.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:45:0:604:db7:a6fa"; };
                              { "vla1-1826.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:88:0:604:db7:a9e5"; };
                              { "vla1-1828.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8d:0:604:db7:ac33"; };
                              { "vla1-1830.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:78:0:604:db7:a780"; };
                              { "vla1-1850.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8d:0:604:db7:aa43"; };
                              { "vla1-1881.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:71:0:604:db7:9d0c"; };
                              { "vla1-1883.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:71:0:604:db7:a775"; };
                              { "vla1-1933.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:78:0:604:db7:ab23"; };
                              { "vla1-1950.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8d:0:604:db7:a9b5"; };
                              { "vla1-1976.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:3c:0:604:db7:a450"; };
                              { "vla1-1977.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:57:0:604:db7:a565"; };
                              { "vla1-1993.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:43:0:604:db7:a0e8"; };
                              { "vla1-1999.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8d:0:604:db7:aa1c"; };
                              { "vla1-2014.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:44:0:604:db7:a668"; };
                              { "vla1-2034.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:78:0:604:db7:a94d"; };
                              { "vla1-2076.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:13:0:604:db7:9be1"; };
                              { "vla1-2077.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:3c:0:604:db7:9d50"; };
                              { "vla1-2081.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:84:0:604:db7:aac4"; };
                              { "vla1-2111.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:13:0:604:db7:9af3"; };
                              { "vla1-2120.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:17:0:604:db7:9925"; };
                              { "vla1-2123.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:53:0:604:db7:9d4c"; };
                              { "vla1-2128.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:3c:0:604:db7:9ef1"; };
                              { "vla1-2136.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:13:0:604:db7:99ee"; };
                              { "vla1-2161.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1b:0:604:db7:9a71"; };
                              { "vla1-2193.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:45:0:604:db7:9d62"; };
                              { "vla1-2206.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:88:0:604:db8:db38"; };
                              { "vla1-2221.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9a:0:604:db7:a9df"; };
                              { "vla1-2339.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:86:0:604:db7:a85f"; };
                              { "vla1-2340.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7d:0:604:db7:a3d6"; };
                              { "vla1-2391.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7d:0:604:db7:a19a"; };
                              { "vla1-2396.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8b:0:604:db7:abe0"; };
                              { "vla1-2397.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a959"; };
                              { "vla1-2400.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a962"; };
                              { "vla1-2401.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8a:0:604:db7:a977"; };
                              { "vla1-2402.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:95:0:604:db7:ab46"; };
                              { "vla1-2407.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8b:0:604:db7:abdc"; };
                              { "vla1-2415.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:94:0:604:db7:aa3e"; };
                              { "vla1-2418.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7c:0:604:db7:9df5"; };
                              { "vla1-2422.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:95:0:604:db7:a9c7"; };
                              { "vla1-2423.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:95:0:604:db7:ab1a"; };
                              { "vla1-2427.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:ab19"; };
                              { "vla1-2429.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8a:0:604:db7:a972"; };
                              { "vla1-2431.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a861"; };
                              { "vla1-2432.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:94:0:604:db7:a7c8"; };
                              { "vla1-2435.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:97:0:604:db7:a93e"; };
                              { "vla1-2438.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:97:0:604:db7:a922"; };
                              { "vla1-2441.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:94:0:604:db7:aaef"; };
                              { "vla1-2442.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a95c"; };
                              { "vla1-2448.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:97:0:604:db7:a7f3"; };
                              { "vla1-2461.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:95:0:604:db7:aa56"; };
                              { "vla1-2470.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:94:0:604:db7:a7ba"; };
                              { "vla1-2484.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a37d"; };
                              { "vla1-2498.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:79:0:604:db7:a95f"; };
                              { "vla1-2511.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9b:0:604:db7:aa2b"; };
                              { "vla1-2512.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:97:0:604:db7:a915"; };
                              { "vla1-2522.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a2:0:604:db7:9956"; };
                              { "vla1-2531.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a2:0:604:db7:9b6a"; };
                              { "vla1-2547.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:2f:0:604:db7:9bd3"; };
                              { "vla1-2549.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a2:0:604:db7:9bdf"; };
                              { "vla1-2563.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1f:0:604:db7:9c52"; };
                              { "vla1-2589.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1f:0:604:db7:9fa6"; };
                              { "vla1-2606.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:32:0:604:db7:9f96"; };
                              { "vla1-2637.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:65:0:604:db7:9ca2"; };
                              { "vla1-2651.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:1f:0:604:db7:9c53"; };
                              { "vla1-2667.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:2f:0:604:db7:9f86"; };
                              { "vla1-2704.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:2f:0:604:db7:9c82"; };
                              { "vla1-2723.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:32:0:604:db7:a52f"; };
                              { "vla1-2727.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:32:0:604:db7:99af"; };
                              { "vla1-2774.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:74:0:604:db7:9efa"; };
                              { "vla1-2881.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:32:0:604:db7:9c07"; };
                              { "vla1-2964.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6e:0:604:db7:a3fd"; };
                              { "vla1-2968.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6e:0:604:db7:a263"; };
                              { "vla1-2993.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6c:0:604:db7:a159"; };
                              { "vla1-2998.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:70:0:604:db7:a410"; };
                              { "vla1-3011.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:19:0:604:db6:e746"; };
                              { "vla1-3015.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:70:0:604:db7:a232"; };
                              { "vla1-3040.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a3:0:604:db7:a733"; };
                              { "vla1-3059.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a3:0:604:db7:9ddf"; };
                              { "vla1-3102.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:74:0:604:db7:98fd"; };
                              { "vla1-3125.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:11:0:604:db7:98f0"; };
                              { "vla1-3132.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6c:0:604:db7:a520"; };
                              { "vla1-3149.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:70:0:604:db7:a281"; };
                              { "vla1-3174.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:11:0:604:db7:998d"; };
                              { "vla1-3247.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6c:0:604:db7:9c25"; };
                              { "vla1-3259.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:19:0:604:db7:9ac9"; };
                              { "vla1-3263.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:11:0:604:db7:99fd"; };
                              { "vla1-3278.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:19:0:604:db7:99e8"; };
                              { "vla1-3285.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:11:0:604:db7:98f2"; };
                              { "vla1-3296.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:19:0:604:db7:9a36"; };
                              { "vla1-3297.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a3:0:604:db7:a6f5"; };
                              { "vla1-3312.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:6e:0:604:db7:a285"; };
                              { "vla1-3401.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:74:0:604:db7:9eb9"; };
                              { "vla1-3507.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a0:0:604:db7:a4f0"; };
                              { "vla1-3521.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:73:0:604:db7:a5de"; };
                              { "vla1-3553.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a0:0:604:db7:a672"; };
                              { "vla1-3571.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:73:0:604:db7:a0cf"; };
                              { "vla1-3580.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:73:0:604:db7:a0d6"; };
                              { "vla1-3606.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a1:0:604:db7:a292"; };
                              { "vla1-3610.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a1:0:604:db7:a00b"; };
                              { "vla1-3619.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a1:0:604:db7:a28f"; };
                              { "vla1-3626.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:63:0:604:db7:a309"; };
                              { "vla1-3632.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:46:0:604:db7:a0b4"; };
                              { "vla1-3635.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:46:0:604:db7:a552"; };
                              { "vla1-3664.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:72:0:604:db7:9e21"; };
                              { "vla1-3667.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:80:0:604:db7:a927"; };
                              { "vla1-3671.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:65:0:604:db7:a097"; };
                              { "vla1-3672.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:63:0:604:db7:a2f9"; };
                              { "vla1-3691.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:86:0:604:db7:abac"; };
                              { "vla1-3693.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:72:0:604:db7:a5c1"; };
                              { "vla1-3698.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:65:0:604:db7:a26a"; };
                              { "vla1-3699.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:86:0:604:db7:aba7"; };
                              { "vla1-3720.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:63:0:604:db7:a2e3"; };
                              { "vla1-3726.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:46:0:604:db7:a549"; };
                              { "vla1-3753.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a0:0:604:db7:a47b"; };
                              { "vla1-3759.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:86:0:604:db7:a82a"; };
                              { "vla1-3762.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:80:0:604:db7:a8e7"; };
                              { "vla1-3802.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7f:0:604:db7:a1fe"; };
                              { "vla1-3841.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:72:0:604:db7:a5bf"; };
                              { "vla1-3850.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7d:0:604:db7:9e5f"; };
                              { "vla1-3966.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7f:0:604:db7:a3e7"; };
                              { "vla1-3996.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:a0:0:604:db7:a503"; };
                              { "vla1-4164.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:7f:0:604:db7:a3c9"; };
                              { "vla1-4219.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:8a:0:604:db7:a80e"; };
                              { "vla1-4269.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:90:0:604:db7:a87e"; };
                              { "vla1-4288.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:86:0:604:db7:ac10"; };
                              { "vla1-4308.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:90:0:604:db7:a996"; };
                              { "vla1-4491.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:9b:0:604:db7:aa24"; };
                              { "vla1-4575.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:77:0:604:db7:a941"; };
                              { "vla1-4581.search.yandex.net"; 32046; 73.000; "2a02:6b8:c0e:77:0:604:db7:a928"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- vlaruweb_apphost
                  vlaruimages_apphost = {
                    priority = 19;
                    match_fsm = {
                      host = "vlaruimages\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "vla1-0147.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:3f:0:604:db7:a6ca"; };
                              { "vla1-0152.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:3f:0:604:db7:a705"; };
                              { "vla1-0221.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:81:0:604:db7:a8cf"; };
                              { "vla1-0703.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d01"; };
                              { "vla1-0951.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:71:0:604:db7:a3de"; };
                              { "vla1-1128.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:45:0:604:db7:a77d"; };
                              { "vla1-1161.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab54"; };
                              { "vla1-1184.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:5d:0:604:db7:a5f7"; };
                              { "vla1-1207.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:9a:0:604:db7:aa9b"; };
                              { "vla1-1228.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:5c:0:604:db7:a047"; };
                              { "vla1-1233.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:59:0:604:db7:9ff8"; };
                              { "vla1-1239.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:93:0:604:db7:a751"; };
                              { "vla1-1246.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:83:0:604:db7:a910"; };
                              { "vla1-1257.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:81:0:604:db7:a7e7"; };
                              { "vla1-1259.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:89:0:604:db7:abd2"; };
                              { "vla1-1278.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:4d:0:604:db7:a440"; };
                              { "vla1-1292.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:4e:0:604:db7:a4b6"; };
                              { "vla1-1294.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:1b:0:604:db7:9b7e"; };
                              { "vla1-1304.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:4c:0:604:db7:a0ce"; };
                              { "vla1-1315.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:67:0:604:db7:a325"; };
                              { "vla1-1330.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:78:0:604:db7:a9c3"; };
                              { "vla1-1341.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:5e:0:604:db7:9deb"; };
                              { "vla1-1354.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:61:0:604:db7:a5b5"; };
                              { "vla1-1356.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:96:0:604:db7:a9c1"; };
                              { "vla1-1358.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:45:0:604:db7:a764"; };
                              { "vla1-1366.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:85:0:604:db7:a877"; };
                              { "vla1-1384.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:44:0:604:db7:9f36"; };
                              { "vla1-1395.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:84:0:604:db7:abfb"; };
                              { "vla1-1421.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:58:0:604:db7:9e42"; };
                              { "vla1-1457.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:62:0:604:db7:a2ea"; };
                              { "vla1-1464.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:48:0:604:db7:a695"; };
                              { "vla1-1486.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:20:0:604:db7:9eb4"; };
                              { "vla1-1516.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:43:0:604:db7:a110"; };
                              { "vla1-1518.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9eb8"; };
                              { "vla1-1598.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:13:0:604:db7:9a64"; };
                              { "vla1-1710.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:71:0:604:db7:a416"; };
                              { "vla1-1747.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:42:0:604:db7:a4e4"; };
                              { "vla1-1757.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:88:0:604:db7:a771"; };
                              { "vla1-1794.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:22:0:604:db7:9d6f"; };
                              { "vla1-1806.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:41:0:604:db7:9e2b"; };
                              { "vla1-1815.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:57:0:604:db7:a38f"; };
                              { "vla1-1911.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:45:0:604:db7:a5fd"; };
                              { "vla1-1930.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:44:0:604:db7:a6cc"; };
                              { "vla1-1987.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:82:0:604:db7:a872"; };
                              { "vla1-2016.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:91:0:604:db7:ab5e"; };
                              { "vla1-2017.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:34:0:604:db7:9d11"; };
                              { "vla1-2041.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:40:0:604:db7:a53f"; };
                              { "vla1-2078.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d02"; };
                              { "vla1-2092.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:17:0:604:db7:a7f5"; };
                              { "vla1-2121.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:98:0:604:db7:a008"; };
                              { "vla1-2122.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d07"; };
                              { "vla1-2143.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:27:0:604:db7:9f6f"; };
                              { "vla1-2163.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:53:0:604:db7:9cfa"; };
                              { "vla1-2253.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:5a:0:604:db7:a1dd"; };
                              { "vla1-2265.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:5b:0:604:db7:a1d7"; };
                              { "vla1-2315.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:6e:0:604:db7:a371"; };
                              { "vla1-2319.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a368"; };
                              { "vla1-2330.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7d:0:604:db7:a2b0"; };
                              { "vla1-2332.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:87:0:604:db7:a84b"; };
                              { "vla1-2345.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:86:0:604:db7:ab9e"; };
                              { "vla1-2351.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:87:0:604:db7:a7ea"; };
                              { "vla1-2358.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a3d8"; };
                              { "vla1-2453.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:94:0:604:db7:a94f"; };
                              { "vla1-2472.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:97:0:604:db7:a909"; };
                              { "vla1-3780.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7f:0:604:db7:9e7d"; };
                              { "vla1-4111.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:87:0:604:db7:a820"; };
                              { "vla1-4375.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:86:0:604:db7:a82d"; };
                              { "vla1-4428.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8a:0:604:db7:a818"; };
                              { "vla1-4443.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:86:0:604:db7:9dee"; };
                              { "vla1-4504.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:90:0:604:db7:a7e1"; };
                              { "vla2-0560.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:9c:0:604:db7:aa6e"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "vla1-0147.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:3f:0:604:db7:a6ca"; };
                              { "vla1-0152.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:3f:0:604:db7:a705"; };
                              { "vla1-0221.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:81:0:604:db7:a8cf"; };
                              { "vla1-0703.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d01"; };
                              { "vla1-0951.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:71:0:604:db7:a3de"; };
                              { "vla1-1128.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:45:0:604:db7:a77d"; };
                              { "vla1-1161.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab54"; };
                              { "vla1-1184.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:5d:0:604:db7:a5f7"; };
                              { "vla1-1207.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:9a:0:604:db7:aa9b"; };
                              { "vla1-1228.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:5c:0:604:db7:a047"; };
                              { "vla1-1233.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:59:0:604:db7:9ff8"; };
                              { "vla1-1239.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:93:0:604:db7:a751"; };
                              { "vla1-1246.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:83:0:604:db7:a910"; };
                              { "vla1-1257.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:81:0:604:db7:a7e7"; };
                              { "vla1-1259.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:89:0:604:db7:abd2"; };
                              { "vla1-1278.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:4d:0:604:db7:a440"; };
                              { "vla1-1292.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:4e:0:604:db7:a4b6"; };
                              { "vla1-1294.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:1b:0:604:db7:9b7e"; };
                              { "vla1-1304.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:4c:0:604:db7:a0ce"; };
                              { "vla1-1315.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:67:0:604:db7:a325"; };
                              { "vla1-1330.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:78:0:604:db7:a9c3"; };
                              { "vla1-1341.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:5e:0:604:db7:9deb"; };
                              { "vla1-1354.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:61:0:604:db7:a5b5"; };
                              { "vla1-1356.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:96:0:604:db7:a9c1"; };
                              { "vla1-1358.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:45:0:604:db7:a764"; };
                              { "vla1-1366.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:85:0:604:db7:a877"; };
                              { "vla1-1384.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:44:0:604:db7:9f36"; };
                              { "vla1-1395.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:84:0:604:db7:abfb"; };
                              { "vla1-1421.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:58:0:604:db7:9e42"; };
                              { "vla1-1457.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:62:0:604:db7:a2ea"; };
                              { "vla1-1464.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:48:0:604:db7:a695"; };
                              { "vla1-1486.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:20:0:604:db7:9eb4"; };
                              { "vla1-1516.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:43:0:604:db7:a110"; };
                              { "vla1-1518.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9eb8"; };
                              { "vla1-1598.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:13:0:604:db7:9a64"; };
                              { "vla1-1710.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:71:0:604:db7:a416"; };
                              { "vla1-1747.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:42:0:604:db7:a4e4"; };
                              { "vla1-1757.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:88:0:604:db7:a771"; };
                              { "vla1-1794.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:22:0:604:db7:9d6f"; };
                              { "vla1-1806.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:41:0:604:db7:9e2b"; };
                              { "vla1-1815.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:57:0:604:db7:a38f"; };
                              { "vla1-1911.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:45:0:604:db7:a5fd"; };
                              { "vla1-1930.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:44:0:604:db7:a6cc"; };
                              { "vla1-1987.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:82:0:604:db7:a872"; };
                              { "vla1-2016.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:91:0:604:db7:ab5e"; };
                              { "vla1-2017.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:34:0:604:db7:9d11"; };
                              { "vla1-2041.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:40:0:604:db7:a53f"; };
                              { "vla1-2078.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d02"; };
                              { "vla1-2092.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:17:0:604:db7:a7f5"; };
                              { "vla1-2121.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:98:0:604:db7:a008"; };
                              { "vla1-2122.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d07"; };
                              { "vla1-2143.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:27:0:604:db7:9f6f"; };
                              { "vla1-2163.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:53:0:604:db7:9cfa"; };
                              { "vla1-2253.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:5a:0:604:db7:a1dd"; };
                              { "vla1-2265.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:5b:0:604:db7:a1d7"; };
                              { "vla1-2315.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:6e:0:604:db7:a371"; };
                              { "vla1-2319.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a368"; };
                              { "vla1-2330.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7d:0:604:db7:a2b0"; };
                              { "vla1-2332.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:87:0:604:db7:a84b"; };
                              { "vla1-2345.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:86:0:604:db7:ab9e"; };
                              { "vla1-2351.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:87:0:604:db7:a7ea"; };
                              { "vla1-2358.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7f:0:604:db7:a3d8"; };
                              { "vla1-2453.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:94:0:604:db7:a94f"; };
                              { "vla1-2472.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:97:0:604:db7:a909"; };
                              { "vla1-3780.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7f:0:604:db7:9e7d"; };
                              { "vla1-4111.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:87:0:604:db7:a820"; };
                              { "vla1-4375.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:86:0:604:db7:a82d"; };
                              { "vla1-4428.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8a:0:604:db7:a818"; };
                              { "vla1-4443.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:86:0:604:db7:9dee"; };
                              { "vla1-4504.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:90:0:604:db7:a7e1"; };
                              { "vla2-0560.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:9c:0:604:db7:aa6e"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- vlaruimages_apphost
                  vlaruvideo_apphost = {
                    priority = 18;
                    match_fsm = {
                      host = "vlaruvideo\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "vla1-0213.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                              { "vla1-0553.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:17:0:604:db7:9b17"; };
                              { "vla1-0557.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                              { "vla1-0762.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                              { "vla1-0852.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                              { "vla1-1073.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                              { "vla1-1167.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                              { "vla1-1206.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                              { "vla1-1245.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                              { "vla1-1266.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                              { "vla1-1412.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                              { "vla1-1426.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                              { "vla1-1454.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                              { "vla1-1484.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                              { "vla1-1497.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                              { "vla1-1508.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                              { "vla1-1527.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                              { "vla1-1530.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                              { "vla1-1540.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                              { "vla1-1556.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                              { "vla1-1568.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                              { "vla1-1571.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                              { "vla1-1575.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                              { "vla1-1636.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                              { "vla1-1643.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                              { "vla1-1650.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                              { "vla1-1654.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                              { "vla1-1744.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                              { "vla1-1785.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                              { "vla1-1862.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                              { "vla1-1869.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                              { "vla1-1889.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                              { "vla1-1902.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                              { "vla1-1920.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                              { "vla1-1937.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                              { "vla1-2006.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                              { "vla1-2011.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                              { "vla1-2035.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                              { "vla1-2039.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                              { "vla1-2097.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                              { "vla1-2131.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                              { "vla1-2141.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                              { "vla1-2153.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                              { "vla1-2209.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                              { "vla1-2338.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                              { "vla1-2341.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                              { "vla1-2363.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                              { "vla1-2368.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                              { "vla1-2392.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                              { "vla1-2440.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                              { "vla1-2759.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                              { "vla1-2901.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                              { "vla1-3477.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                              { "vla1-4120.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                              { "vla1-4143.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "vla1-0213.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                              { "vla1-0553.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:17:0:604:db7:9b17"; };
                              { "vla1-0557.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                              { "vla1-0762.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                              { "vla1-0852.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                              { "vla1-1073.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                              { "vla1-1167.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                              { "vla1-1206.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                              { "vla1-1245.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                              { "vla1-1266.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                              { "vla1-1412.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                              { "vla1-1426.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                              { "vla1-1454.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                              { "vla1-1484.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                              { "vla1-1497.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                              { "vla1-1508.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                              { "vla1-1527.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                              { "vla1-1530.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                              { "vla1-1540.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                              { "vla1-1556.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                              { "vla1-1568.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                              { "vla1-1571.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                              { "vla1-1575.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                              { "vla1-1636.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                              { "vla1-1643.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                              { "vla1-1650.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                              { "vla1-1654.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                              { "vla1-1744.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                              { "vla1-1785.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                              { "vla1-1862.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                              { "vla1-1869.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                              { "vla1-1889.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                              { "vla1-1902.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                              { "vla1-1920.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                              { "vla1-1937.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                              { "vla1-2006.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                              { "vla1-2011.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                              { "vla1-2035.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                              { "vla1-2039.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                              { "vla1-2097.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                              { "vla1-2131.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                              { "vla1-2141.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                              { "vla1-2153.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                              { "vla1-2209.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                              { "vla1-2338.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                              { "vla1-2341.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                              { "vla1-2363.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                              { "vla1-2368.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                              { "vla1-2392.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                              { "vla1-2440.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                              { "vla1-2759.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                              { "vla1-2901.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                              { "vla1-3477.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                              { "vla1-4120.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                              { "vla1-4143.search.yandex.net"; 32046; 60.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- vlaruvideo_apphost
                  hamsterimages_apphost = {
                    priority = 17;
                    match_fsm = {
                      host = "hamsterimages\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "man1-1076.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f1f2"; };
                              { "man1-1150.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f2ca"; };
                              { "man1-1515.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b150"; };
                              { "man1-1885.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1d50"; };
                              { "man1-1957.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:16f0"; };
                              { "man1-1979.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:550"; };
                              { "man1-2023.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1640"; };
                              { "man1-2087.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e320"; };
                              { "man1-2092.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6098:f652:14ff:fe8c:1e0"; };
                              { "man1-2106.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:720"; };
                              { "man1-2112.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:560"; };
                              { "man1-2383.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6011:f652:14ff:fe55:1ca0"; };
                              { "man1-2873.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e840"; };
                              { "man1-2943.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dc40"; };
                              { "man1-3175.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:1ff0"; };
                              { "man1-3252.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1a10"; };
                              { "man1-3260.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c700"; };
                              { "man1-3261.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c6d0"; };
                              { "man1-3265.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:d4c0"; };
                              { "man1-3375.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c580"; };
                              { "man1-3479.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7bd0"; };
                              { "man1-3484.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602f:92e2:baff:fe72:7bfa"; };
                              { "man1-3489.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e86"; };
                              { "man1-3493.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602b:92e2:baff:fe6f:815a"; };
                              { "man1-3498.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:b6a4"; };
                              { "man1-3499.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:7eae"; };
                              { "man1-3500.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:80e8"; };
                              { "man1-3510.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f7b4"; };
                              { "man1-3512.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6f6"; };
                              { "man1-3517.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6014:f652:14ff:fe44:5250"; };
                              { "man1-3520.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:b7d0"; };
                              { "man1-3523.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d6b0"; };
                              { "man1-3524.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d710"; };
                              { "man1-3526.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:d5c0"; };
                              { "man1-3527.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:d6a0"; };
                              { "man1-3528.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aeb0"; };
                              { "man1-3529.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b010"; };
                              { "man1-3533.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cdb0"; };
                              { "man1-3534.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b590"; };
                              { "man1-3535.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cda0"; };
                              { "man1-3537.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:2220"; };
                              { "man1-3542.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd50"; };
                              { "man1-3544.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2610"; };
                              { "man1-3752.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:b9d0"; };
                              { "man1-3822.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7eaa"; };
                              { "man1-3904.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7866"; };
                              { "man1-3959.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe6f:80a2"; };
                              { "man1-4025.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe6e:ba8c"; };
                              { "man1-4073.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6040:92e2:baff:fe74:770a"; };
                              { "man1-4074.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7dc0"; };
                              { "man1-4076.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7d6c"; };
                              { "man1-4077.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bdda"; };
                              { "man1-4078.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b656"; };
                              { "man1-4080.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6042:92e2:baff:fe6f:7d86"; };
                              { "man1-4081.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b842"; };
                              { "man1-4082.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:24d4"; };
                              { "man1-4083.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2610"; };
                              { "man1-4084.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2570"; };
                              { "man1-4085.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2434"; };
                              { "man1-4310.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7bcc"; };
                              { "man1-4311.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7dfc"; };
                              { "man1-4638.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:913a"; };
                              { "man1-5640.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8a30"; };
                              { "man1-6102.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4ed0"; };
                              { "man1-6134.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3d50"; };
                              { "man1-6150.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:f652:14ff:fef5:da20"; };
                              { "man1-6161.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3890"; };
                              { "man1-6167.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:82d0"; };
                              { "man1-6227.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6057:f652:14ff:fef5:c8b0"; };
                              { "man1-6242.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6057:f652:14ff:fef5:cb40"; };
                              { "man1-6263.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5370"; };
                              { "man1-6359.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6053:f652:14ff:fe55:29c0"; };
                              { "man1-6393.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:48f0"; };
                              { "man1-6413.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:1ad0"; };
                              { "man1-6419.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:4720"; };
                              { "man1-6485.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6063:e61d:2dff:fe04:2430"; };
                              { "man1-6634.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:604f:e61d:2dff:fe01:f370"; };
                              { "man1-6727.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cd90"; };
                              { "man1-6728.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cdd0"; };
                              { "man1-6763.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6058:e61d:2dff:fe04:50d0"; };
                              { "man1-6767.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:4bc0"; };
                              { "man1-6854.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6060:f652:14ff:fef5:c950"; };
                              { "man1-6873.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605e:e61d:2dff:fe04:1ed0"; };
                              { "man1-6886.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3d30"; };
                              { "man1-6900.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3da0"; };
                              { "man1-6903.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3cf0"; };
                              { "sas1-1099.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:666:96de:80ff:feec:1c28"; };
                              { "sas1-1352.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:618:215:b2ff:fea8:d3a"; };
                              { "sas1-1370.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d8c"; };
                              { "sas1-1376.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:db6"; };
                              { "sas1-1383.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:a3a"; };
                              { "sas1-1422.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:6d3c"; };
                              { "sas1-1424.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:121:215:b2ff:fea8:cea"; };
                              { "sas1-1426.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:196:215:b2ff:fea8:dba"; };
                              { "sas1-1433.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:112:215:b2ff:fea8:6dc8"; };
                              { "sas1-1440.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:d2a"; };
                              { "sas1-1774.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:625:96de:80ff:feec:198a"; };
                              { "sas1-1958.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:652:96de:80ff:feec:1c24"; };
                              { "sas1-2200.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:654:96de:80ff:feec:186a"; };
                              { "sas1-4461.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:651:96de:80ff:feec:1830"; };
                              { "sas1-5360.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1b82"; };
                              { "sas1-5451.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a9:96de:80ff:feec:f98"; };
                              { "sas1-5455.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:652:96de:80ff:feec:1ec4"; };
                              { "sas1-5456.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1924"; };
                              { "sas1-5462.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:63d:96de:80ff:feec:1316"; };
                              { "sas1-5463.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1002"; };
                              { "sas1-5464.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:171c"; };
                              { "sas1-5466.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:625:96de:80ff:feec:120e"; };
                              { "sas1-5473.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:61c:96de:80ff:feec:11f4"; };
                              { "sas1-5475.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:139:96de:80ff:feec:fcc"; };
                              { "sas1-5476.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:651:96de:80ff:feec:10cc"; };
                              { "sas1-5477.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:652:96de:80ff:feec:18a2"; };
                              { "sas1-5478.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:61c:96de:80ff:feec:1898"; };
                              { "sas1-5485.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:63a:96de:80ff:feec:1a5e"; };
                              { "sas1-5486.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:653:96de:80ff:feec:1876"; };
                              { "sas1-5487.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:653:96de:80ff:feec:1124"; };
                              { "sas1-5488.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:61c:96de:80ff:fee9:1aa"; };
                              { "sas1-5491.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:619:96de:80ff:feec:17de"; };
                              { "sas1-5493.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:653:96de:80ff:fee9:fd"; };
                              { "sas1-5495.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:63c:96de:80ff:feec:157c"; };
                              { "sas1-5498.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:139:96de:80ff:fee9:1de"; };
                              { "sas1-5499.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:139:96de:80ff:fee9:184"; };
                              { "sas1-5501.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:655:96de:80ff:feec:1624"; };
                              { "sas1-5502.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10fe"; };
                              { "sas1-5506.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:655:96de:80ff:feec:1a38"; };
                              { "sas1-5507.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:656:96de:80ff:feec:1a32"; };
                              { "sas1-5508.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:674:96de:80ff:feec:1b9c"; };
                              { "sas1-5513.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:111:96de:80ff:feec:1ba8"; };
                              { "sas1-5516.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:655:96de:80ff:feec:18fc"; };
                              { "sas1-5518.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1cc0"; };
                              { "sas1-5629.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:163:225:90ff:fee6:4d12"; };
                              { "sas1-5649.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602:225:90ff:fee6:4c7a"; };
                              { "sas1-5657.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:649:225:90ff:fee6:dd88"; };
                              { "sas1-5662.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:649:225:90ff:fee5:bde0"; };
                              { "sas1-5960.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:636:225:90ff:feef:c964"; };
                              { "sas1-5965.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:604:225:90ff:feeb:fba6"; };
                              { "sas1-5966.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:635:225:90ff:feeb:fd9e"; };
                              { "sas1-5967.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601:225:90ff:feeb:fcd6"; };
                              { "sas1-5968.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:604:225:90ff:feeb:fba2"; };
                              { "sas1-5969.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601:225:90ff:feeb:f9b8"; };
                              { "sas1-5970.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:100:225:90ff:feeb:fce2"; };
                              { "sas1-5971.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:604:225:90ff:feed:2b8e"; };
                              { "sas1-5972.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:174:225:90ff:feed:2b4e"; };
                              { "sas1-5973.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:100:225:90ff:fee3:9cf2"; };
                              { "sas1-5974.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:100:225:90ff:feed:2fde"; };
                              { "sas1-6351.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:11a:215:b2ff:fea7:8280"; };
                              { "sas1-6752.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:9008"; };
                              { "sas1-6893.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:11d:215:b2ff:fea7:9060"; };
                              { "sas1-6939.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b324"; };
                              { "sas1-6978.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:123:215:b2ff:fea7:b720"; };
                              { "sas1-7095.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:124:215:b2ff:fea7:ab98"; };
                              { "sas1-7098.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:124:215:b2ff:fea7:8ee4"; };
                              { "sas1-7125.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:907c"; };
                              { "sas1-7155.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:122:215:b2ff:fea7:8bf0"; };
                              { "sas1-7156.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aae4"; };
                              { "sas1-7238.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:121:215:b2ff:fea7:ac10"; };
                              { "sas1-7272.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:121:215:b2ff:fea7:8300"; };
                              { "sas1-7286.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:128:215:b2ff:fea7:7aec"; };
                              { "sas1-7287.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:128:215:b2ff:fea7:7910"; };
                              { "sas1-7326.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:126:215:b2ff:fea7:ad8c"; };
                              { "sas1-7330.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:628:215:b2ff:fea7:90d0"; };
                              { "sas1-7331.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc9c"; };
                              { "sas1-7459.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8cd0"; };
                              { "sas1-7494.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:125:215:b2ff:fea7:6590"; };
                              { "sas1-7498.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:125:215:b2ff:fea7:aaa4"; };
                              { "sas1-7825.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:7fbc"; };
                              { "sas1-7843.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:82f0"; };
                              { "sas1-7929.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:12b:215:b2ff:fea7:ac24"; };
                              { "sas1-8873.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:138:215:b2ff:fea7:ba80"; };
                              { "sas1-8979.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3f9e"; };
                              { "vla1-0147.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:3f:0:604:db7:a6ca"; };
                              { "vla1-0152.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:3f:0:604:db7:a705"; };
                              { "vla1-0221.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:81:0:604:db7:a8cf"; };
                              { "vla1-0703.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:53:0:604:db7:9d01"; };
                              { "vla1-0951.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:71:0:604:db7:a3de"; };
                              { "vla1-1128.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:45:0:604:db7:a77d"; };
                              { "vla1-1161.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:8d:0:604:db7:ab54"; };
                              { "vla1-1184.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:5d:0:604:db7:a5f7"; };
                              { "vla1-1207.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:9a:0:604:db7:aa9b"; };
                              { "vla1-1228.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:5c:0:604:db7:a047"; };
                              { "vla1-1233.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:59:0:604:db7:9ff8"; };
                              { "vla1-1239.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:93:0:604:db7:a751"; };
                              { "vla1-1246.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:83:0:604:db7:a910"; };
                              { "vla1-1257.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:81:0:604:db7:a7e7"; };
                              { "vla1-1259.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:89:0:604:db7:abd2"; };
                              { "vla1-1278.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:4d:0:604:db7:a440"; };
                              { "vla1-1292.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:4e:0:604:db7:a4b6"; };
                              { "vla1-1294.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:1b:0:604:db7:9b7e"; };
                              { "vla1-1304.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:4c:0:604:db7:a0ce"; };
                              { "vla1-1315.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:67:0:604:db7:a325"; };
                              { "vla1-1330.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:78:0:604:db7:a9c3"; };
                              { "vla1-1341.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:5e:0:604:db7:9deb"; };
                              { "vla1-1354.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:61:0:604:db7:a5b5"; };
                              { "vla1-1356.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:96:0:604:db7:a9c1"; };
                              { "vla1-1358.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:45:0:604:db7:a764"; };
                              { "vla1-1366.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:85:0:604:db7:a877"; };
                              { "vla1-1384.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:44:0:604:db7:9f36"; };
                              { "vla1-1395.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:84:0:604:db7:abfb"; };
                              { "vla1-1421.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:58:0:604:db7:9e42"; };
                              { "vla1-1457.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:62:0:604:db7:a2ea"; };
                              { "vla1-1464.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:48:0:604:db7:a695"; };
                              { "vla1-1486.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:20:0:604:db7:9eb4"; };
                              { "vla1-1516.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:43:0:604:db7:a110"; };
                              { "vla1-1518.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:3c:0:604:db7:9eb8"; };
                              { "vla1-1598.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:13:0:604:db7:9a64"; };
                              { "vla1-1710.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:71:0:604:db7:a416"; };
                              { "vla1-1747.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:42:0:604:db7:a4e4"; };
                              { "vla1-1757.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:88:0:604:db7:a771"; };
                              { "vla1-1794.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:22:0:604:db7:9d6f"; };
                              { "vla1-1806.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:41:0:604:db7:9e2b"; };
                              { "vla1-1815.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:57:0:604:db7:a38f"; };
                              { "vla1-1911.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:45:0:604:db7:a5fd"; };
                              { "vla1-1930.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:44:0:604:db7:a6cc"; };
                              { "vla1-1987.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:82:0:604:db7:a872"; };
                              { "vla1-2016.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:91:0:604:db7:ab5e"; };
                              { "vla1-2017.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:34:0:604:db7:9d11"; };
                              { "vla1-2041.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:40:0:604:db7:a53f"; };
                              { "vla1-2078.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:53:0:604:db7:9d02"; };
                              { "vla1-2092.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:17:0:604:db7:a7f5"; };
                              { "vla1-2121.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:98:0:604:db7:a008"; };
                              { "vla1-2122.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:53:0:604:db7:9d07"; };
                              { "vla1-2143.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:27:0:604:db7:9f6f"; };
                              { "vla1-2163.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:53:0:604:db7:9cfa"; };
                              { "vla1-2253.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:5a:0:604:db7:a1dd"; };
                              { "vla1-2265.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:5b:0:604:db7:a1d7"; };
                              { "vla1-2315.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:6e:0:604:db7:a371"; };
                              { "vla1-2319.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:7f:0:604:db7:a368"; };
                              { "vla1-2330.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:7d:0:604:db7:a2b0"; };
                              { "vla1-2332.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:87:0:604:db7:a84b"; };
                              { "vla1-2345.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:86:0:604:db7:ab9e"; };
                              { "vla1-2351.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:87:0:604:db7:a7ea"; };
                              { "vla1-2358.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:7f:0:604:db7:a3d8"; };
                              { "vla1-2453.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:94:0:604:db7:a94f"; };
                              { "vla1-2472.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:97:0:604:db7:a909"; };
                              { "vla1-3780.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:7f:0:604:db7:9e7d"; };
                              { "vla1-4111.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:87:0:604:db7:a820"; };
                              { "vla1-4375.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:86:0:604:db7:a82d"; };
                              { "vla1-4428.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:8a:0:604:db7:a818"; };
                              { "vla1-4443.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:86:0:604:db7:9dee"; };
                              { "vla1-4504.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:90:0:604:db7:a7e1"; };
                              { "vla2-0560.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:9c:0:604:db7:aa6e"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "man1-1076.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f1f2"; };
                              { "man1-1150.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f2ca"; };
                              { "man1-1515.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b150"; };
                              { "man1-1885.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1d50"; };
                              { "man1-1957.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:16f0"; };
                              { "man1-1979.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:550"; };
                              { "man1-2023.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1640"; };
                              { "man1-2087.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e320"; };
                              { "man1-2092.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6098:f652:14ff:fe8c:1e0"; };
                              { "man1-2106.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:720"; };
                              { "man1-2112.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:560"; };
                              { "man1-2383.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6011:f652:14ff:fe55:1ca0"; };
                              { "man1-2873.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e840"; };
                              { "man1-2943.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dc40"; };
                              { "man1-3175.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:1ff0"; };
                              { "man1-3252.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1a10"; };
                              { "man1-3260.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c700"; };
                              { "man1-3261.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c6d0"; };
                              { "man1-3265.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:d4c0"; };
                              { "man1-3375.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c580"; };
                              { "man1-3479.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7bd0"; };
                              { "man1-3484.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602f:92e2:baff:fe72:7bfa"; };
                              { "man1-3489.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e86"; };
                              { "man1-3493.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602b:92e2:baff:fe6f:815a"; };
                              { "man1-3498.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:b6a4"; };
                              { "man1-3499.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:7eae"; };
                              { "man1-3500.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:80e8"; };
                              { "man1-3510.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f7b4"; };
                              { "man1-3512.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6f6"; };
                              { "man1-3517.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6014:f652:14ff:fe44:5250"; };
                              { "man1-3520.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:b7d0"; };
                              { "man1-3523.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d6b0"; };
                              { "man1-3524.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d710"; };
                              { "man1-3526.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:d5c0"; };
                              { "man1-3527.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:d6a0"; };
                              { "man1-3528.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aeb0"; };
                              { "man1-3529.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b010"; };
                              { "man1-3533.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cdb0"; };
                              { "man1-3534.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b590"; };
                              { "man1-3535.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cda0"; };
                              { "man1-3537.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:2220"; };
                              { "man1-3542.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd50"; };
                              { "man1-3544.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2610"; };
                              { "man1-3752.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:b9d0"; };
                              { "man1-3822.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7eaa"; };
                              { "man1-3904.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7866"; };
                              { "man1-3959.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe6f:80a2"; };
                              { "man1-4025.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe6e:ba8c"; };
                              { "man1-4073.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6040:92e2:baff:fe74:770a"; };
                              { "man1-4074.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7dc0"; };
                              { "man1-4076.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7d6c"; };
                              { "man1-4077.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bdda"; };
                              { "man1-4078.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b656"; };
                              { "man1-4080.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6042:92e2:baff:fe6f:7d86"; };
                              { "man1-4081.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b842"; };
                              { "man1-4082.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:24d4"; };
                              { "man1-4083.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2610"; };
                              { "man1-4084.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2570"; };
                              { "man1-4085.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2434"; };
                              { "man1-4310.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7bcc"; };
                              { "man1-4311.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7dfc"; };
                              { "man1-4638.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:651d:215:b2ff:fea9:913a"; };
                              { "man1-5640.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8a30"; };
                              { "man1-6102.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4ed0"; };
                              { "man1-6134.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3d50"; };
                              { "man1-6150.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:f652:14ff:fef5:da20"; };
                              { "man1-6161.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3890"; };
                              { "man1-6167.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:82d0"; };
                              { "man1-6227.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6057:f652:14ff:fef5:c8b0"; };
                              { "man1-6242.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6057:f652:14ff:fef5:cb40"; };
                              { "man1-6263.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5370"; };
                              { "man1-6359.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6053:f652:14ff:fe55:29c0"; };
                              { "man1-6393.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:48f0"; };
                              { "man1-6413.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:1ad0"; };
                              { "man1-6419.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:4720"; };
                              { "man1-6485.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6063:e61d:2dff:fe04:2430"; };
                              { "man1-6634.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:604f:e61d:2dff:fe01:f370"; };
                              { "man1-6727.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cd90"; };
                              { "man1-6728.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cdd0"; };
                              { "man1-6763.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6058:e61d:2dff:fe04:50d0"; };
                              { "man1-6767.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:4bc0"; };
                              { "man1-6854.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6060:f652:14ff:fef5:c950"; };
                              { "man1-6873.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605e:e61d:2dff:fe04:1ed0"; };
                              { "man1-6886.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3d30"; };
                              { "man1-6900.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3da0"; };
                              { "man1-6903.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3cf0"; };
                              { "sas1-1099.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:666:96de:80ff:feec:1c28"; };
                              { "sas1-1352.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:618:215:b2ff:fea8:d3a"; };
                              { "sas1-1370.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d8c"; };
                              { "sas1-1376.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:db6"; };
                              { "sas1-1383.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:a3a"; };
                              { "sas1-1422.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:6d3c"; };
                              { "sas1-1424.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:121:215:b2ff:fea8:cea"; };
                              { "sas1-1426.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:196:215:b2ff:fea8:dba"; };
                              { "sas1-1433.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:112:215:b2ff:fea8:6dc8"; };
                              { "sas1-1440.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:d2a"; };
                              { "sas1-1774.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:625:96de:80ff:feec:198a"; };
                              { "sas1-1958.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:652:96de:80ff:feec:1c24"; };
                              { "sas1-2200.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:654:96de:80ff:feec:186a"; };
                              { "sas1-4461.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:651:96de:80ff:feec:1830"; };
                              { "sas1-5360.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1b82"; };
                              { "sas1-5451.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a9:96de:80ff:feec:f98"; };
                              { "sas1-5455.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:652:96de:80ff:feec:1ec4"; };
                              { "sas1-5456.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1924"; };
                              { "sas1-5462.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:63d:96de:80ff:feec:1316"; };
                              { "sas1-5463.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1002"; };
                              { "sas1-5464.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:171c"; };
                              { "sas1-5466.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:625:96de:80ff:feec:120e"; };
                              { "sas1-5473.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:61c:96de:80ff:feec:11f4"; };
                              { "sas1-5475.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:139:96de:80ff:feec:fcc"; };
                              { "sas1-5476.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:651:96de:80ff:feec:10cc"; };
                              { "sas1-5477.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:652:96de:80ff:feec:18a2"; };
                              { "sas1-5478.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:61c:96de:80ff:feec:1898"; };
                              { "sas1-5485.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:63a:96de:80ff:feec:1a5e"; };
                              { "sas1-5486.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:653:96de:80ff:feec:1876"; };
                              { "sas1-5487.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:653:96de:80ff:feec:1124"; };
                              { "sas1-5488.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:61c:96de:80ff:fee9:1aa"; };
                              { "sas1-5491.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:619:96de:80ff:feec:17de"; };
                              { "sas1-5493.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:653:96de:80ff:fee9:fd"; };
                              { "sas1-5495.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:63c:96de:80ff:feec:157c"; };
                              { "sas1-5498.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:139:96de:80ff:fee9:1de"; };
                              { "sas1-5499.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:139:96de:80ff:fee9:184"; };
                              { "sas1-5501.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:655:96de:80ff:feec:1624"; };
                              { "sas1-5502.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10fe"; };
                              { "sas1-5506.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:655:96de:80ff:feec:1a38"; };
                              { "sas1-5507.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:656:96de:80ff:feec:1a32"; };
                              { "sas1-5508.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:674:96de:80ff:feec:1b9c"; };
                              { "sas1-5513.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:111:96de:80ff:feec:1ba8"; };
                              { "sas1-5516.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:655:96de:80ff:feec:18fc"; };
                              { "sas1-5518.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1cc0"; };
                              { "sas1-5629.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:163:225:90ff:fee6:4d12"; };
                              { "sas1-5649.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:602:225:90ff:fee6:4c7a"; };
                              { "sas1-5657.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:649:225:90ff:fee6:dd88"; };
                              { "sas1-5662.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:649:225:90ff:fee5:bde0"; };
                              { "sas1-5960.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:636:225:90ff:feef:c964"; };
                              { "sas1-5965.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:604:225:90ff:feeb:fba6"; };
                              { "sas1-5966.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:635:225:90ff:feeb:fd9e"; };
                              { "sas1-5967.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601:225:90ff:feeb:fcd6"; };
                              { "sas1-5968.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:604:225:90ff:feeb:fba2"; };
                              { "sas1-5969.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:601:225:90ff:feeb:f9b8"; };
                              { "sas1-5970.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:100:225:90ff:feeb:fce2"; };
                              { "sas1-5971.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:604:225:90ff:feed:2b8e"; };
                              { "sas1-5972.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:174:225:90ff:feed:2b4e"; };
                              { "sas1-5973.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:100:225:90ff:fee3:9cf2"; };
                              { "sas1-5974.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:100:225:90ff:feed:2fde"; };
                              { "sas1-6351.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:11a:215:b2ff:fea7:8280"; };
                              { "sas1-6752.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:9008"; };
                              { "sas1-6893.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:11d:215:b2ff:fea7:9060"; };
                              { "sas1-6939.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b324"; };
                              { "sas1-6978.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:123:215:b2ff:fea7:b720"; };
                              { "sas1-7095.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:124:215:b2ff:fea7:ab98"; };
                              { "sas1-7098.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:124:215:b2ff:fea7:8ee4"; };
                              { "sas1-7125.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:907c"; };
                              { "sas1-7155.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:122:215:b2ff:fea7:8bf0"; };
                              { "sas1-7156.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aae4"; };
                              { "sas1-7238.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:121:215:b2ff:fea7:ac10"; };
                              { "sas1-7272.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:121:215:b2ff:fea7:8300"; };
                              { "sas1-7286.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:128:215:b2ff:fea7:7aec"; };
                              { "sas1-7287.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:128:215:b2ff:fea7:7910"; };
                              { "sas1-7326.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:126:215:b2ff:fea7:ad8c"; };
                              { "sas1-7330.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:628:215:b2ff:fea7:90d0"; };
                              { "sas1-7331.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc9c"; };
                              { "sas1-7459.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8cd0"; };
                              { "sas1-7494.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:125:215:b2ff:fea7:6590"; };
                              { "sas1-7498.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:125:215:b2ff:fea7:aaa4"; };
                              { "sas1-7825.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:7fbc"; };
                              { "sas1-7843.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:82f0"; };
                              { "sas1-7929.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:12b:215:b2ff:fea7:ac24"; };
                              { "sas1-8873.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:138:215:b2ff:fea7:ba80"; };
                              { "sas1-8979.search.yandex.net"; 22950; 40.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3f9e"; };
                              { "vla1-0147.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:3f:0:604:db7:a6ca"; };
                              { "vla1-0152.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:3f:0:604:db7:a705"; };
                              { "vla1-0221.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:81:0:604:db7:a8cf"; };
                              { "vla1-0703.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:53:0:604:db7:9d01"; };
                              { "vla1-0951.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:71:0:604:db7:a3de"; };
                              { "vla1-1128.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:45:0:604:db7:a77d"; };
                              { "vla1-1161.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:8d:0:604:db7:ab54"; };
                              { "vla1-1184.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:5d:0:604:db7:a5f7"; };
                              { "vla1-1207.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:9a:0:604:db7:aa9b"; };
                              { "vla1-1228.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:5c:0:604:db7:a047"; };
                              { "vla1-1233.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:59:0:604:db7:9ff8"; };
                              { "vla1-1239.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:93:0:604:db7:a751"; };
                              { "vla1-1246.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:83:0:604:db7:a910"; };
                              { "vla1-1257.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:81:0:604:db7:a7e7"; };
                              { "vla1-1259.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:89:0:604:db7:abd2"; };
                              { "vla1-1278.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:4d:0:604:db7:a440"; };
                              { "vla1-1292.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:4e:0:604:db7:a4b6"; };
                              { "vla1-1294.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:1b:0:604:db7:9b7e"; };
                              { "vla1-1304.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:4c:0:604:db7:a0ce"; };
                              { "vla1-1315.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:67:0:604:db7:a325"; };
                              { "vla1-1330.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:78:0:604:db7:a9c3"; };
                              { "vla1-1341.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:5e:0:604:db7:9deb"; };
                              { "vla1-1354.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:61:0:604:db7:a5b5"; };
                              { "vla1-1356.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:96:0:604:db7:a9c1"; };
                              { "vla1-1358.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:45:0:604:db7:a764"; };
                              { "vla1-1366.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:85:0:604:db7:a877"; };
                              { "vla1-1384.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:44:0:604:db7:9f36"; };
                              { "vla1-1395.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:84:0:604:db7:abfb"; };
                              { "vla1-1421.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:58:0:604:db7:9e42"; };
                              { "vla1-1457.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:62:0:604:db7:a2ea"; };
                              { "vla1-1464.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:48:0:604:db7:a695"; };
                              { "vla1-1486.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:20:0:604:db7:9eb4"; };
                              { "vla1-1516.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:43:0:604:db7:a110"; };
                              { "vla1-1518.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:3c:0:604:db7:9eb8"; };
                              { "vla1-1598.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:13:0:604:db7:9a64"; };
                              { "vla1-1710.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:71:0:604:db7:a416"; };
                              { "vla1-1747.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:42:0:604:db7:a4e4"; };
                              { "vla1-1757.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:88:0:604:db7:a771"; };
                              { "vla1-1794.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:22:0:604:db7:9d6f"; };
                              { "vla1-1806.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:41:0:604:db7:9e2b"; };
                              { "vla1-1815.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:57:0:604:db7:a38f"; };
                              { "vla1-1911.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:45:0:604:db7:a5fd"; };
                              { "vla1-1930.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:44:0:604:db7:a6cc"; };
                              { "vla1-1987.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:82:0:604:db7:a872"; };
                              { "vla1-2016.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:91:0:604:db7:ab5e"; };
                              { "vla1-2017.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:34:0:604:db7:9d11"; };
                              { "vla1-2041.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:40:0:604:db7:a53f"; };
                              { "vla1-2078.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:53:0:604:db7:9d02"; };
                              { "vla1-2092.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:17:0:604:db7:a7f5"; };
                              { "vla1-2121.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:98:0:604:db7:a008"; };
                              { "vla1-2122.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:53:0:604:db7:9d07"; };
                              { "vla1-2143.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:27:0:604:db7:9f6f"; };
                              { "vla1-2163.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:53:0:604:db7:9cfa"; };
                              { "vla1-2253.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:5a:0:604:db7:a1dd"; };
                              { "vla1-2265.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:5b:0:604:db7:a1d7"; };
                              { "vla1-2315.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:6e:0:604:db7:a371"; };
                              { "vla1-2319.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:7f:0:604:db7:a368"; };
                              { "vla1-2330.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:7d:0:604:db7:a2b0"; };
                              { "vla1-2332.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:87:0:604:db7:a84b"; };
                              { "vla1-2345.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:86:0:604:db7:ab9e"; };
                              { "vla1-2351.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:87:0:604:db7:a7ea"; };
                              { "vla1-2358.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:7f:0:604:db7:a3d8"; };
                              { "vla1-2453.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:94:0:604:db7:a94f"; };
                              { "vla1-2472.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:97:0:604:db7:a909"; };
                              { "vla1-3780.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:7f:0:604:db7:9e7d"; };
                              { "vla1-4111.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:87:0:604:db7:a820"; };
                              { "vla1-4375.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:86:0:604:db7:a82d"; };
                              { "vla1-4428.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:8a:0:604:db7:a818"; };
                              { "vla1-4443.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:86:0:604:db7:9dee"; };
                              { "vla1-4504.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:90:0:604:db7:a7e1"; };
                              { "vla2-0560.search.yandex.net"; 22950; 40.000; "2a02:6b8:c0e:9c:0:604:db7:aa6e"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- hamsterimages_apphost
                  hamstervideo_apphost = {
                    priority = 16;
                    match_fsm = {
                      host = "hamstervideo\\.apphost\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
                    regexp = {
                      jsonctxweb0 = {
                        priority = 2;
                        match_fsm = {
                          URI = "/(_json|_ctx)/(_ctx|_json)/web0(.*)?";
                          case_insensitive = true;
                          surround = false;
                        }; -- match_fsm
                        balancer2 = {
                          timeout_policy = {
                            timeout = "100ms";
                            unique_policy = {};
                          }; -- timeout_policy
                          attempts = 1;
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
                              { "man1-0978.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f404"; };
                              { "man1-1191.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6018:f652:14ff:fe48:9670"; };
                              { "man1-1209.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6028:f652:14ff:fe55:2a90"; };
                              { "man1-1282.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5280"; };
                              { "man1-1286.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe44:56f0"; };
                              { "man1-1293.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6020:f652:14ff:fe48:9fa0"; };
                              { "man1-1294.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6015:f652:14ff:fe44:50f0"; };
                              { "man1-1296.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6016:f652:14ff:fe44:57c0"; };
                              { "man1-1396.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:a9f0"; };
                              { "man1-1400.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:b420"; };
                              { "man1-1446.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6020:f652:14ff:fe8b:f440"; };
                              { "man1-1448.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:f410"; };
                              { "man1-1450.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601e:f652:14ff:fe8b:d8a0"; };
                              { "man1-1454.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:eaf0"; };
                              { "man1-1455.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:e5f0"; };
                              { "man1-1748.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6027:f652:14ff:fe8b:aea0"; };
                              { "man1-1765.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b0d0"; };
                              { "man1-1927.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:be70"; };
                              { "man1-1947.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8b:ea20"; };
                              { "man1-2252.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:dd50"; };
                              { "man1-2264.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:d620"; };
                              { "man1-2453.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:d1b0"; };
                              { "man1-2464.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6021:f652:14ff:fe55:3560"; };
                              { "man1-2529.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:110"; };
                              { "man1-2582.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:c780"; };
                              { "man1-2670.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:3d0"; };
                              { "man1-2733.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:db20"; };
                              { "man1-2800.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:f460"; };
                              { "man1-2848.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e8b0"; };
                              { "man1-2960.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:edb0"; };
                              { "man1-3507.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f3a4"; };
                              { "man1-3515.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f680"; };
                              { "man1-3519.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6014:f652:14ff:fe44:54d0"; };
                              { "man1-3522.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:ab20"; };
                              { "man1-3530.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aed0"; };
                              { "man1-3541.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:e0"; };
                              { "man1-3543.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd70"; };
                              { "man1-3552.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:fd00"; };
                              { "man1-3567.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f4a0"; };
                              { "man1-3569.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1850"; };
                              { "man1-3579.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602a:f652:14ff:fe8b:fd90"; };
                              { "man1-3586.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:fe00"; };
                              { "man1-3587.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6037:f652:14ff:fe8c:70"; };
                              { "man1-3592.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:1cd0"; };
                              { "man1-3598.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6000:f652:14ff:fe8c:f30"; };
                              { "man1-3599.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:fe30"; };
                              { "man1-3600.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6028:f652:14ff:fe8c:1760"; };
                              { "man1-3605.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:1800"; };
                              { "man1-3617.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7d6e"; };
                              { "man1-3690.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:7d8a"; };
                              { "man1-3702.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7740"; };
                              { "man1-3706.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:81a0"; };
                              { "man1-3708.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:b980"; };
                              { "man1-3783.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7aa8"; };
                              { "man1-3874.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7bf8"; };
                              { "man1-3926.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:607a:92e2:baff:fea1:8052"; };
                              { "man1-3971.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f5c"; };
                              { "man1-4044.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:607c:92e2:baff:fea1:8054"; };
                              { "man1-4051.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe74:7d78"; };
                              { "man1-4426.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:bdb2"; };
                              { "man1-4461.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7a1c"; };
                              { "man1-4483.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7712"; };
                              { "man1-4489.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98c"; };
                              { "man1-4497.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f42"; };
                              { "man1-4521.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602d:92e2:baff:fe52:7a94"; };
                              { "man1-4525.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799c"; };
                              { "man1-4548.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a00"; };
                              { "man1-4553.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601d:f652:14ff:fe55:29d0"; };
                              { "man1-5346.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:607c:92e2:baff:fe6e:b8b6"; };
                              { "man1-5399.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7624"; };
                              { "man1-5503.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7e48"; };
                              { "man1-5622.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8f60"; };
                              { "man1-5826.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:4840"; };
                              { "man1-5914.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6059:e61d:2dff:fe00:8520"; };
                              { "man1-5965.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7640"; };
                              { "man1-6664.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6047:f652:14ff:fe74:3c10"; };
                              { "man1-7451.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1f30"; };
                              { "sas1-0554.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:636:225:90ff:feed:2f90"; };
                              { "sas1-0582.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601:225:90ff:feed:2e46"; };
                              { "sas1-1040.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:100:225:90ff:feef:ccba"; };
                              { "sas1-1077.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3130"; };
                              { "sas1-1346.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:174:225:90ff:feed:317c"; };
                              { "sas1-1413.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:174:225:90ff:feed:2d94"; };
                              { "sas1-1425.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:128:215:b2ff:fea8:db2"; };
                              { "sas1-1429.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:110:215:b2ff:fea8:d1a"; };
                              { "sas1-1453.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:119:215:b2ff:fea7:efac"; };
                              { "sas1-1488.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:134:215:b2ff:fea8:966"; };
                              { "sas1-1525.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:135:215:b2ff:fea8:d7a"; };
                              { "sas1-1572.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:642:96de:80ff:fe5e:dc42"; };
                              { "sas1-1643.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:653:96de:80ff:fe5e:dca0"; };
                              { "sas1-1664.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:11d:215:b2ff:fea8:6c84"; };
                              { "sas1-1671.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:117:215:b2ff:fea8:d52"; };
                              { "sas1-1758.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:de48"; };
                              { "sas1-1759.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:ba4a"; };
                              { "sas1-1809.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:67c:922b:34ff:fecf:2efa"; };
                              { "sas1-1815.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:67c:96de:80ff:fe8c:bd34"; };
                              { "sas1-1832.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:120:215:b2ff:fea8:a6a"; };
                              { "sas1-1880.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:e984"; };
                              { "sas1-1885.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:da12"; };
                              { "sas1-1915.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17c:96de:80ff:fe8c:e432"; };
                              { "sas1-1979.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17b:96de:80ff:fe8c:d9a8"; };
                              { "sas1-2046.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:67b:96de:80ff:fe8c:d8e6"; };
                              { "sas1-5450.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:625:96de:80ff:feec:16da"; };
                              { "sas1-5452.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1a40"; };
                              { "sas1-5470.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:654:96de:80ff:feec:10a4"; };
                              { "sas1-5504.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:63d:96de:80ff:feec:1ecc"; };
                              { "sas1-5514.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:63a:96de:80ff:feec:18ce"; };
                              { "sas1-5517.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:669:96de:80ff:feec:1902"; };
                              { "sas1-5624.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:160:225:90ff:fee6:4b3c"; };
                              { "sas1-5625.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:163:225:90ff:fee6:4d14"; };
                              { "sas1-5633.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:622:225:90ff:fee6:4860"; };
                              { "sas1-5638.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602:225:90ff:fee5:bd36"; };
                              { "sas1-5642.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:622:225:90ff:fee5:c134"; };
                              { "sas1-5643.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:161:225:90ff:fee6:de52"; };
                              { "sas1-5645.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:161:225:90ff:fee5:bf6c"; };
                              { "sas1-5648.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602:225:90ff:fee6:de28"; };
                              { "sas1-5650.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602:225:90ff:fee6:de54"; };
                              { "sas1-5659.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:649:225:90ff:fee6:dd7c"; };
                              { "sas1-5660.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:649:225:90ff:fee6:df02"; };
                              { "sas1-5665.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:636:225:90ff:feed:2de4"; };
                              { "sas1-5963.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601:225:90ff:feed:2cea"; };
                              { "sas1-6750.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                              { "sas1-6971.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:123:215:b2ff:fea7:b10c"; };
                              { "sas1-7168.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:122:215:b2ff:fea7:7c88"; };
                              { "sas1-7574.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                              { "sas1-7747.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                              { "sas1-7824.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                              { "sas1-8236.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                              { "sas1-8246.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                              { "sas1-8401.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                              { "sas1-8546.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                              { "sas1-8571.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                              { "sas1-8618.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                              { "sas1-8830.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                              { "vla1-0213.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                              { "vla1-0553.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:17:0:604:db7:9b17"; };
                              { "vla1-0557.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                              { "vla1-0762.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                              { "vla1-0852.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                              { "vla1-1073.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                              { "vla1-1167.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                              { "vla1-1206.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                              { "vla1-1245.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                              { "vla1-1266.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                              { "vla1-1412.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                              { "vla1-1426.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                              { "vla1-1454.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                              { "vla1-1484.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                              { "vla1-1497.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                              { "vla1-1508.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                              { "vla1-1527.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                              { "vla1-1530.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                              { "vla1-1540.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                              { "vla1-1556.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                              { "vla1-1568.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                              { "vla1-1571.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                              { "vla1-1575.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                              { "vla1-1636.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                              { "vla1-1643.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                              { "vla1-1650.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                              { "vla1-1654.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                              { "vla1-1744.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                              { "vla1-1785.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                              { "vla1-1862.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                              { "vla1-1869.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                              { "vla1-1889.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                              { "vla1-1902.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                              { "vla1-1920.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                              { "vla1-1937.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                              { "vla1-2006.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                              { "vla1-2011.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                              { "vla1-2035.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                              { "vla1-2039.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                              { "vla1-2097.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                              { "vla1-2131.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                              { "vla1-2141.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                              { "vla1-2153.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                              { "vla1-2209.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                              { "vla1-2338.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                              { "vla1-2341.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                              { "vla1-2363.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                              { "vla1-2368.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                              { "vla1-2392.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                              { "vla1-2440.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                              { "vla1-2759.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                              { "vla1-2901.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                              { "vla1-3477.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                              { "vla1-4120.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                              { "vla1-4143.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "100ms";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- jsonctxweb0
                      default = {
                        priority = 1;
                        balancer2 = {
                          unique_policy = {};
                          attempts = 5;
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
                              { "man1-0978.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f404"; };
                              { "man1-1191.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6018:f652:14ff:fe48:9670"; };
                              { "man1-1209.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6028:f652:14ff:fe55:2a90"; };
                              { "man1-1282.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5280"; };
                              { "man1-1286.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe44:56f0"; };
                              { "man1-1293.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6020:f652:14ff:fe48:9fa0"; };
                              { "man1-1294.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6015:f652:14ff:fe44:50f0"; };
                              { "man1-1296.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6016:f652:14ff:fe44:57c0"; };
                              { "man1-1396.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:a9f0"; };
                              { "man1-1400.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:b420"; };
                              { "man1-1446.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6020:f652:14ff:fe8b:f440"; };
                              { "man1-1448.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:f410"; };
                              { "man1-1450.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601e:f652:14ff:fe8b:d8a0"; };
                              { "man1-1454.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:eaf0"; };
                              { "man1-1455.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:e5f0"; };
                              { "man1-1748.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6027:f652:14ff:fe8b:aea0"; };
                              { "man1-1765.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b0d0"; };
                              { "man1-1927.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:be70"; };
                              { "man1-1947.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6029:f652:14ff:fe8b:ea20"; };
                              { "man1-2252.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:dd50"; };
                              { "man1-2264.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:d620"; };
                              { "man1-2453.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:d1b0"; };
                              { "man1-2464.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6021:f652:14ff:fe55:3560"; };
                              { "man1-2529.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:110"; };
                              { "man1-2582.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:c780"; };
                              { "man1-2670.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:3d0"; };
                              { "man1-2733.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:db20"; };
                              { "man1-2800.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:f460"; };
                              { "man1-2848.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e8b0"; };
                              { "man1-2960.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:edb0"; };
                              { "man1-3507.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f3a4"; };
                              { "man1-3515.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f680"; };
                              { "man1-3519.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6014:f652:14ff:fe44:54d0"; };
                              { "man1-3522.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:ab20"; };
                              { "man1-3530.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aed0"; };
                              { "man1-3541.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:e0"; };
                              { "man1-3543.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd70"; };
                              { "man1-3552.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:fd00"; };
                              { "man1-3567.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f4a0"; };
                              { "man1-3569.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1850"; };
                              { "man1-3579.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602a:f652:14ff:fe8b:fd90"; };
                              { "man1-3586.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:fe00"; };
                              { "man1-3587.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6037:f652:14ff:fe8c:70"; };
                              { "man1-3592.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:1cd0"; };
                              { "man1-3598.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6000:f652:14ff:fe8c:f30"; };
                              { "man1-3599.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:fe30"; };
                              { "man1-3600.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6028:f652:14ff:fe8c:1760"; };
                              { "man1-3605.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:1800"; };
                              { "man1-3617.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7d6e"; };
                              { "man1-3690.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:7d8a"; };
                              { "man1-3702.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7740"; };
                              { "man1-3706.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:81a0"; };
                              { "man1-3708.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:b980"; };
                              { "man1-3783.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7aa8"; };
                              { "man1-3874.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7bf8"; };
                              { "man1-3926.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:607a:92e2:baff:fea1:8052"; };
                              { "man1-3971.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f5c"; };
                              { "man1-4044.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:607c:92e2:baff:fea1:8054"; };
                              { "man1-4051.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603c:92e2:baff:fe74:7d78"; };
                              { "man1-4426.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:bdb2"; };
                              { "man1-4461.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7a1c"; };
                              { "man1-4483.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7712"; };
                              { "man1-4489.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98c"; };
                              { "man1-4497.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f42"; };
                              { "man1-4521.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602d:92e2:baff:fe52:7a94"; };
                              { "man1-4525.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799c"; };
                              { "man1-4548.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a00"; };
                              { "man1-4553.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601d:f652:14ff:fe55:29d0"; };
                              { "man1-5346.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:607c:92e2:baff:fe6e:b8b6"; };
                              { "man1-5399.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7624"; };
                              { "man1-5503.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7e48"; };
                              { "man1-5622.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8f60"; };
                              { "man1-5826.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:4840"; };
                              { "man1-5914.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6059:e61d:2dff:fe00:8520"; };
                              { "man1-5965.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7640"; };
                              { "man1-6664.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:6047:f652:14ff:fe74:3c10"; };
                              { "man1-7451.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1f30"; };
                              { "sas1-0554.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:636:225:90ff:feed:2f90"; };
                              { "sas1-0582.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601:225:90ff:feed:2e46"; };
                              { "sas1-1040.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:100:225:90ff:feef:ccba"; };
                              { "sas1-1077.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3130"; };
                              { "sas1-1346.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:174:225:90ff:feed:317c"; };
                              { "sas1-1413.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:174:225:90ff:feed:2d94"; };
                              { "sas1-1425.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:128:215:b2ff:fea8:db2"; };
                              { "sas1-1429.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:110:215:b2ff:fea8:d1a"; };
                              { "sas1-1453.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:119:215:b2ff:fea7:efac"; };
                              { "sas1-1488.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:134:215:b2ff:fea8:966"; };
                              { "sas1-1525.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:135:215:b2ff:fea8:d7a"; };
                              { "sas1-1572.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:642:96de:80ff:fe5e:dc42"; };
                              { "sas1-1643.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:653:96de:80ff:fe5e:dca0"; };
                              { "sas1-1664.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:11d:215:b2ff:fea8:6c84"; };
                              { "sas1-1671.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:117:215:b2ff:fea8:d52"; };
                              { "sas1-1758.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:de48"; };
                              { "sas1-1759.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:ba4a"; };
                              { "sas1-1809.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:67c:922b:34ff:fecf:2efa"; };
                              { "sas1-1815.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:67c:96de:80ff:fe8c:bd34"; };
                              { "sas1-1832.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:120:215:b2ff:fea8:a6a"; };
                              { "sas1-1880.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:e984"; };
                              { "sas1-1885.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:da12"; };
                              { "sas1-1915.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17c:96de:80ff:fe8c:e432"; };
                              { "sas1-1979.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:17b:96de:80ff:fe8c:d9a8"; };
                              { "sas1-2046.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:67b:96de:80ff:fe8c:d8e6"; };
                              { "sas1-5450.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:625:96de:80ff:feec:16da"; };
                              { "sas1-5452.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1a40"; };
                              { "sas1-5470.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:654:96de:80ff:feec:10a4"; };
                              { "sas1-5504.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:63d:96de:80ff:feec:1ecc"; };
                              { "sas1-5514.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:63a:96de:80ff:feec:18ce"; };
                              { "sas1-5517.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:669:96de:80ff:feec:1902"; };
                              { "sas1-5624.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:160:225:90ff:fee6:4b3c"; };
                              { "sas1-5625.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:163:225:90ff:fee6:4d14"; };
                              { "sas1-5633.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:622:225:90ff:fee6:4860"; };
                              { "sas1-5638.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602:225:90ff:fee5:bd36"; };
                              { "sas1-5642.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:622:225:90ff:fee5:c134"; };
                              { "sas1-5643.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:161:225:90ff:fee6:de52"; };
                              { "sas1-5645.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:161:225:90ff:fee5:bf6c"; };
                              { "sas1-5648.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602:225:90ff:fee6:de28"; };
                              { "sas1-5650.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:602:225:90ff:fee6:de54"; };
                              { "sas1-5659.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:649:225:90ff:fee6:dd7c"; };
                              { "sas1-5660.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:649:225:90ff:fee6:df02"; };
                              { "sas1-5665.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:636:225:90ff:feed:2de4"; };
                              { "sas1-5963.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:601:225:90ff:feed:2cea"; };
                              { "sas1-6750.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                              { "sas1-6971.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:123:215:b2ff:fea7:b10c"; };
                              { "sas1-7168.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:122:215:b2ff:fea7:7c88"; };
                              { "sas1-7574.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                              { "sas1-7747.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                              { "sas1-7824.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                              { "sas1-8236.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                              { "sas1-8246.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                              { "sas1-8401.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                              { "sas1-8546.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                              { "sas1-8571.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                              { "sas1-8618.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                              { "sas1-8830.search.yandex.net"; 31230; 40.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                              { "vla1-0213.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                              { "vla1-0553.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:17:0:604:db7:9b17"; };
                              { "vla1-0557.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                              { "vla1-0762.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                              { "vla1-0852.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                              { "vla1-1073.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                              { "vla1-1167.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                              { "vla1-1206.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                              { "vla1-1245.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                              { "vla1-1266.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                              { "vla1-1412.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                              { "vla1-1426.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                              { "vla1-1454.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                              { "vla1-1484.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                              { "vla1-1497.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                              { "vla1-1508.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                              { "vla1-1527.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                              { "vla1-1530.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                              { "vla1-1540.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                              { "vla1-1556.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                              { "vla1-1568.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                              { "vla1-1571.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                              { "vla1-1575.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                              { "vla1-1636.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                              { "vla1-1643.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                              { "vla1-1650.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                              { "vla1-1654.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                              { "vla1-1744.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                              { "vla1-1785.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                              { "vla1-1862.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                              { "vla1-1869.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                              { "vla1-1889.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                              { "vla1-1902.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                              { "vla1-1920.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                              { "vla1-1937.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                              { "vla1-2006.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                              { "vla1-2011.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                              { "vla1-2035.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                              { "vla1-2039.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                              { "vla1-2097.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                              { "vla1-2131.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                              { "vla1-2141.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                              { "vla1-2153.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                              { "vla1-2209.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                              { "vla1-2338.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                              { "vla1-2341.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                              { "vla1-2363.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                              { "vla1-2368.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                              { "vla1-2392.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                              { "vla1-2440.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                              { "vla1-2759.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                              { "vla1-2901.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                              { "vla1-3477.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                              { "vla1-4120.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                              { "vla1-4143.search.yandex.net"; 31230; 40.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                            }, {
                              resolve_timeout = "10ms";
                              connect_timeout = "40ms";
                              backend_timeout = "10s";
                              fail_on_5xx = true;
                              http_backend = true;
                              buffering = false;
                              keepalive_count = 0;
                              need_resolve = true;
                            }))
                          }; -- weighted2
                        }; -- balancer2
                      }; -- default
                    }; -- regexp
                  }; -- hamstervideo_apphost
                  sasruweb_reportrenderer = {
                    priority = 15;
                    match_fsm = {
                      host = "sasruweb\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "sas1-0439.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:636:225:90ff:fee5:be98"; };
                          { "sas1-0442.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:601:225:90ff:feed:30b2"; };
                          { "sas1-0469.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:635:225:90ff:feef:c914"; };
                          { "sas1-0490.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:601:225:90ff:feed:3286"; };
                          { "sas1-0510.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:636:225:90ff:feed:2f9a"; };
                          { "sas1-0517.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:601:225:90ff:feed:31a2"; };
                          { "sas1-0555.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:635:225:90ff:feed:3040"; };
                          { "sas1-1126.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61c:96de:80ff:feec:1c1e"; };
                          { "sas1-1243.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:100:225:90ff:feed:2f9c"; };
                          { "sas1-1281.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:175:225:90ff:fee7:52f6"; };
                          { "sas1-1326.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:175:225:90ff:fec4:8b42"; };
                          { "sas1-1338.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:175:225:90ff:fee7:52be"; };
                          { "sas1-1381.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:175:225:90ff:fee7:540c"; };
                          { "sas1-1390.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:601:225:90ff:feef:c9cc"; };
                          { "sas1-1393.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:174:225:90ff:feed:3036"; };
                          { "sas1-1397.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:177:feaa:14ff:fe1d:f516"; };
                          { "sas1-1405.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:cee"; };
                          { "sas1-1411.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:174:225:90ff:feed:3158"; };
                          { "sas1-1412.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:100:225:90ff:feed:30b4"; };
                          { "sas1-1416.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:174:225:90ff:feed:31aa"; };
                          { "sas1-1417.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:174:225:90ff:feed:2de6"; };
                          { "sas1-1419.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:635:225:90ff:feef:c648"; };
                          { "sas1-1427.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:618:215:b2ff:fea8:d0e"; };
                          { "sas1-1428.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:dae"; };
                          { "sas1-1437.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:d12"; };
                          { "sas1-1439.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:196:215:b2ff:fea8:d42"; };
                          { "sas1-1445.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:119:215:b2ff:fea8:d2e"; };
                          { "sas1-1447.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:dbe"; };
                          { "sas1-1452.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:625:96de:80ff:feec:1b9a"; };
                          { "sas1-1472.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:114:215:b2ff:fea8:d56"; };
                          { "sas1-1500.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:114:215:b2ff:fea8:d7e"; };
                          { "sas1-1501.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:684:215:b2ff:fea8:d16"; };
                          { "sas1-1519.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d00"; };
                          { "sas1-1536.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a6:215:b2ff:fea8:96a"; };
                          { "sas1-1547.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:628:215:b2ff:fea8:d73"; };
                          { "sas1-1562.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a6:215:b2ff:fea8:7fe"; };
                          { "sas1-1573.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:67a:215:b2ff:fea8:c12"; };
                          { "sas1-1585.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a5:215:b2ff:fea8:976"; };
                          { "sas1-1586.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:67a:215:b2ff:fea8:8944"; };
                          { "sas1-1587.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:179:215:b2ff:fea8:6d98"; };
                          { "sas1-1590.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:617:215:b2ff:fea8:d76"; };
                          { "sas1-1598.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:67a:215:b2ff:fea8:6cf0"; };
                          { "sas1-1599.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:67a:215:b2ff:fea8:6d2c"; };
                          { "sas1-1603.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cca"; };
                          { "sas1-1606.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:628:215:b2ff:fea8:d36"; };
                          { "sas1-1621.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:67a:215:b2ff:fea8:cbe"; };
                          { "sas1-1639.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:179:215:b2ff:fea8:c82"; };
                          { "sas1-1642.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:67a:215:b2ff:fea8:c16"; };
                          { "sas1-1650.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:179:215:b2ff:fea8:c7e"; };
                          { "sas1-1666.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:179:215:b2ff:fea8:7140"; };
                          { "sas1-1676.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a0:215:b2ff:fea8:6c0c"; };
                          { "sas1-1677.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:169:215:b2ff:fea7:f8fc"; };
                          { "sas1-1684.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:179:215:b2ff:fea8:c5e"; };
                          { "sas1-1685.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:610:215:b2ff:fea8:6dc4"; };
                          { "sas1-1695.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:610:215:b2ff:fea8:6d90"; };
                          { "sas1-1697.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:197:215:b2ff:fea8:6c10"; };
                          { "sas1-1718.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:197:215:b2ff:fea8:6e01"; };
                          { "sas1-1729.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a4:215:b2ff:fea8:a6e"; };
                          { "sas1-1734.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:179:215:b2ff:fea8:6d38"; };
                          { "sas1-1736.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:17a:feaa:14ff:feab:faf4"; };
                          { "sas1-1744.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:17a:feaa:14ff:feab:f616"; };
                          { "sas1-1783.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:16e:215:b2ff:fea7:87c4"; };
                          { "sas1-1787.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12f:215:b2ff:fea8:992"; };
                          { "sas1-1789.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12c:215:b2ff:fea8:98e"; };
                          { "sas1-1800.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:68f:215:b2ff:fea8:a66"; };
                          { "sas1-1801.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a9:215:b2ff:fea8:a62"; };
                          { "sas1-1802.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:623:215:b2ff:fea8:d4a"; };
                          { "sas1-1803.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:87b0"; };
                          { "sas1-1805.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:62c:215:b2ff:fea8:d3e"; };
                          { "sas1-1806.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:62c:215:b2ff:fea8:cfa"; };
                          { "sas1-1813.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:616:215:b2ff:fea8:d86"; };
                          { "sas1-1814.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61d:215:b2ff:fea8:bde"; };
                          { "sas1-1830.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:625:96de:80ff:feec:1306"; };
                          { "sas1-1833.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ceb4"; };
                          { "sas1-1834.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:68f:215:b2ff:fea8:98a"; };
                          { "sas1-1835.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:61dc"; };
                          { "sas1-1836.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:623:215:b2ff:fea8:c8a"; };
                          { "sas1-1837.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12f:215:b2ff:fea8:bda"; };
                          { "sas1-1839.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a4:215:b2ff:fea8:6d88"; };
                          { "sas1-1921.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12c:215:b2ff:fea8:cd6"; };
                          { "sas1-1990.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:619:96de:80ff:feec:15ca"; };
                          { "sas1-2005.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:604:225:90ff:feef:c9de"; };
                          { "sas1-2013.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:63c:96de:80ff:feec:1d46"; };
                          { "sas1-2483.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a1:96de:80ff:feec:eca"; };
                          { "sas1-4219.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:635:225:90ff:feef:c966"; };
                          { "sas1-4452.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:652:96de:80ff:feec:18f2"; };
                          { "sas1-4455.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:625:96de:80ff:feec:18a8"; };
                          { "sas1-4456.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:674:96de:80ff:feec:1e42"; };
                          { "sas1-4873.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:653:96de:80ff:feec:19a2"; };
                          { "sas1-5080.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:100:225:90ff:feef:c9d0"; };
                          { "sas1-5396.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:654:96de:80ff:feec:1aee"; };
                          { "sas1-5397.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:653:96de:80ff:feec:12f6"; };
                          { "sas1-5449.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:625:96de:80ff:feec:1b94"; };
                          { "sas1-5453.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:139:96de:80ff:feec:1c80"; };
                          { "sas1-5454.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:139:96de:80ff:feec:1b52"; };
                          { "sas1-5457.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a9:96de:80ff:feec:145e"; };
                          { "sas1-5458.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:653:96de:80ff:feec:14b8"; };
                          { "sas1-5459.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61c:96de:80ff:feec:1432"; };
                          { "sas1-5460.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61c:96de:80ff:feec:ea8"; };
                          { "sas1-5461.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:111:96de:80ff:feec:1326"; };
                          { "sas1-5465.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:652:96de:80ff:feec:fd2"; };
                          { "sas1-5467.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:625:96de:80ff:feec:11c4"; };
                          { "sas1-5468.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:652:96de:80ff:feec:11c6"; };
                          { "sas1-5469.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:651:96de:80ff:feec:1216"; };
                          { "sas1-5471.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:653:96de:80ff:feec:11ce"; };
                          { "sas1-5472.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61c:96de:80ff:feec:1c30"; };
                          { "sas1-5474.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:63b:96de:80ff:feec:1014"; };
                          { "sas1-5479.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61c:96de:80ff:feec:189a"; };
                          { "sas1-5480.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1886"; };
                          { "sas1-5481.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10bc"; };
                          { "sas1-5482.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:63c:96de:80ff:feec:1948"; };
                          { "sas1-5483.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:619:96de:80ff:feec:1882"; };
                          { "sas1-5484.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:674:96de:80ff:feec:1a82"; };
                          { "sas1-5489.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61c:96de:80ff:feec:170a"; };
                          { "sas1-5490.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:63c:96de:80ff:feec:17fe"; };
                          { "sas1-5492.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:654:96de:80ff:fee9:d4"; };
                          { "sas1-5494.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:619:96de:80ff:feec:1d8c"; };
                          { "sas1-5496.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:666:96de:80ff:feec:1edc"; };
                          { "sas1-5497.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61c:96de:80ff:feec:1bb4"; };
                          { "sas1-5500.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:656:96de:80ff:feec:164c"; };
                          { "sas1-5503.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a1:96de:80ff:feec:fda"; };
                          { "sas1-5505.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:111:96de:80ff:feec:1108"; };
                          { "sas1-5509.search.yandex.net"; 10240; 360.000; "2a02:6b8:c02:53b:0:604:80ec:1a3c"; };
                          { "sas1-5510.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1ba0"; };
                          { "sas1-5511.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:651:96de:80ff:feec:1ba4"; };
                          { "sas1-5512.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:63d:96de:80ff:feec:18c8"; };
                          { "sas1-5515.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:674:96de:80ff:feec:18d8"; };
                          { "sas1-5519.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:639:96de:80ff:feec:130c"; };
                          { "sas1-5520.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:655:96de:80ff:feec:190c"; };
                          { "sas1-5521.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:656:96de:80ff:feec:191c"; };
                          { "sas1-5522.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:139:96de:80ff:feec:1a4c"; };
                          { "sas1-5523.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:63b:96de:80ff:feec:1848"; };
                          { "sas1-5626.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:160:225:90ff:fee6:481e"; };
                          { "sas1-5627.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:163:225:90ff:fee6:49be"; };
                          { "sas1-5628.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:163:225:90ff:fee6:4868"; };
                          { "sas1-5630.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:163:225:90ff:fee6:46fc"; };
                          { "sas1-5631.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:622:225:90ff:fee6:491c"; };
                          { "sas1-5632.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:160:225:90ff:fee6:4ca6"; };
                          { "sas1-5634.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:160:225:90ff:fee6:47fe"; };
                          { "sas1-5635.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:163:225:90ff:fee6:4822"; };
                          { "sas1-5636.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:622:225:90ff:fee6:486e"; };
                          { "sas1-5637.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:161:225:90ff:fee6:486c"; };
                          { "sas1-5639.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:161:225:90ff:fee5:c298"; };
                          { "sas1-5640.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:622:225:90ff:fee5:bbd0"; };
                          { "sas1-5641.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:622:225:90ff:fee4:2e26"; };
                          { "sas1-5644.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:161:225:90ff:fee6:48aa"; };
                          { "sas1-5646.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:602:225:90ff:fee8:7c0e"; };
                          { "sas1-5647.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:602:225:90ff:fee6:49b0"; };
                          { "sas1-5651.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:160:225:90ff:fee6:482e"; };
                          { "sas1-5652.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:160:225:90ff:fee6:4b3e"; };
                          { "sas1-5653.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:163:225:90ff:fee6:4864"; };
                          { "sas1-5654.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:602:225:90ff:fee6:4816"; };
                          { "sas1-5655.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:649:225:90ff:fee5:beb4"; };
                          { "sas1-5656.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:649:225:90ff:fee8:7bbc"; };
                          { "sas1-5661.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:622:225:90ff:fee6:4a46"; };
                          { "sas1-5663.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:649:225:90ff:fee6:ddc0"; };
                          { "sas1-5669.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:636:225:90ff:feed:318c"; };
                          { "sas1-5715.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:635:225:90ff:feed:3064"; };
                          { "sas1-5732.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:636:225:90ff:feef:cb98"; };
                          { "sas1-5892.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:601:225:90ff:feec:2f44"; };
                          { "sas1-5959.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:604:225:90ff:feef:c9da"; };
                          { "sas1-5961.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:604:225:90ff:feef:c974"; };
                          { "sas1-5962.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:604:225:90ff:feef:c96a"; };
                          { "sas1-5964.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:604:225:90ff:feeb:f9b0"; };
                          { "sas1-6100.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:164:428d:5cff:fe34:f33a"; };
                          { "sas1-6161.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:164:428d:5cff:fe34:f98a"; };
                          { "sas1-6171.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:164:428d:5cff:fe34:f8fa"; };
                          { "sas1-6240.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:118:215:b2ff:fea7:76c0"; };
                          { "sas1-6357.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:69f:215:b2ff:fea7:827c"; };
                          { "sas1-6398.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:115:215:b2ff:fea7:7874"; };
                          { "sas1-6406.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:7ed0"; };
                          { "sas1-6539.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a9:215:b2ff:fea7:8010"; };
                          { "sas1-6626.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:119:215:b2ff:fea7:7be0"; };
                          { "sas1-6634.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:66c:215:b2ff:fea7:7e34"; };
                          { "sas1-6720.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:11e:215:b2ff:fea7:8c38"; };
                          { "sas1-6730.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a924"; };
                          { "sas1-6735.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:8d9c"; };
                          { "sas1-6743.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:a9dc"; };
                          { "sas1-6757.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:66c:215:b2ff:fea7:a9b0"; };
                          { "sas1-6777.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:6fe4"; };
                          { "sas1-6785.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a0:215:b2ff:fea7:abb4"; };
                          { "sas1-6802.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:11f:215:b2ff:fea7:7f2c"; };
                          { "sas1-6805.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:62c:215:b2ff:fea7:b778"; };
                          { "sas1-6851.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:11f:215:b2ff:fea7:8d20"; };
                          { "sas1-6857.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:62c:215:b2ff:fea7:8d04"; };
                          { "sas1-6861.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:11f:215:b2ff:fea7:a9b4"; };
                          { "sas1-6869.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:62c:215:b2ff:fea7:a9d8"; };
                          { "sas1-6925.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12f:215:b2ff:fea7:bb18"; };
                          { "sas1-6935.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12f:215:b2ff:fea7:823c"; };
                          { "sas1-6938.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:123:215:b2ff:fea7:b3a4"; };
                          { "sas1-7005.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:16e:215:b2ff:fea7:abdc"; };
                          { "sas1-7013.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:16e:215:b2ff:fea7:ade4"; };
                          { "sas1-7039.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:120:215:b2ff:fea7:9054"; };
                          { "sas1-7060.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:120:215:b2ff:fea7:ad08"; };
                          { "sas1-7094.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8fc4"; };
                          { "sas1-7138.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:8ea4"; };
                          { "sas1-7142.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:83cc"; };
                          { "sas1-7154.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aab0"; };
                          { "sas1-7167.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12c:215:b2ff:fea7:73b4"; };
                          { "sas1-7169.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12c:215:b2ff:fea7:73ac"; };
                          { "sas1-7192.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:122:215:b2ff:fea7:7d40"; };
                          { "sas1-7209.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:122:215:b2ff:fea7:8f24"; };
                          { "sas1-7212.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:122:215:b2ff:fea7:90e0"; };
                          { "sas1-7234.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:121:215:b2ff:fea7:abcc"; };
                          { "sas1-7276.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:128:215:b2ff:fea7:7250"; };
                          { "sas1-7458.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:7fe4"; };
                          { "sas1-7465.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:125:215:b2ff:fea7:817c"; };
                          { "sas1-7475.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:610:215:b2ff:fea7:8bf4"; };
                          { "sas1-7477.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:129:215:b2ff:fea7:8b6c"; };
                          { "sas1-7488.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:80a8"; };
                          { "sas1-7534.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:125:215:b2ff:fea7:7430"; };
                          { "sas1-7569.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:129:215:b2ff:fea7:bc08"; };
                          { "sas1-7591.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:129:215:b2ff:fea7:b990"; };
                          { "sas1-7686.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bf0c"; };
                          { "sas1-7687.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bec8"; };
                          { "sas1-7696.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:61d:215:b2ff:fea7:bddc"; };
                          { "sas1-7727.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:127:215:b2ff:fea7:af04"; };
                          { "sas1-7732.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:127:215:b2ff:fea7:ba58"; };
                          { "sas1-7761.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:73f4"; };
                          { "sas1-7763.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b6e0"; };
                          { "sas1-7790.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12d:215:b2ff:fea7:b054"; };
                          { "sas1-7803.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:b56c"; };
                          { "sas1-7816.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:8cdc"; };
                          { "sas1-7868.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12e:215:b2ff:fea7:aa10"; };
                          { "sas1-7869.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12e:215:b2ff:fea7:77d4"; };
                          { "sas1-7878.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12e:215:b2ff:fea7:7db8"; };
                          { "sas1-7902.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:ab88"; };
                          { "sas1-7908.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a3:215:b2ff:fea7:8c34"; };
                          { "sas1-7936.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:618:215:b2ff:fea7:8b98"; };
                          { "sas1-7942.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:12b:215:b2ff:fea7:7444"; };
                          { "sas1-8160.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:132:215:b2ff:fea7:afec"; };
                          { "sas1-8179.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:132:215:b2ff:fea7:8b9c"; };
                          { "sas1-8186.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:68f:215:b2ff:fea7:9178"; };
                          { "sas1-8214.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:68f:215:b2ff:fea7:a980"; };
                          { "sas1-8234.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:131:215:b2ff:fea7:b3d4"; };
                          { "sas1-8255.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:6a6:215:b2ff:fea7:b1d0"; };
                          { "sas1-8299.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:132:215:b2ff:fea7:b754"; };
                          { "sas1-8301.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:132:215:b2ff:fea7:b750"; };
                          { "sas1-8357.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:133:215:b2ff:fea7:af7c"; };
                          { "sas1-8369.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:623:215:b2ff:fea7:b2c8"; };
                          { "sas1-8430.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:130:215:b2ff:fea7:aeb8"; };
                          { "sas1-8518.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:170:428d:5cff:fe37:fffe"; };
                          { "sas1-8524.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:617:215:b2ff:fea7:b984"; };
                          { "sas1-8573.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:135:215:b2ff:fea7:bcd8"; };
                          { "sas1-8588.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:142:428d:5cff:fe36:8b26"; };
                          { "sas1-8596.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:142:428d:5cff:fe36:8ac8"; };
                          { "sas1-8612.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:684:215:b2ff:fea7:bf10"; };
                          { "sas1-8615.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:684:215:b2ff:fea7:b834"; };
                          { "sas1-8635.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:134:215:b2ff:fea7:b83c"; };
                          { "sas1-8637.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:134:215:b2ff:fea7:b840"; };
                          { "sas1-8638.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:134:215:b2ff:fea7:b988"; };
                          { "sas1-8674.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:616:215:b2ff:fea7:bc54"; };
                          { "sas1-8694.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:137:215:b2ff:fea7:b640"; };
                          { "sas1-8701.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:137:215:b2ff:fea7:b330"; };
                          { "sas1-8730.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:616:215:b2ff:fea7:8dec"; };
                          { "sas1-8742.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:616:215:b2ff:fea7:b2a4"; };
                          { "sas1-8746.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:616:215:b2ff:fea7:b3ac"; };
                          { "sas1-8826.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1a6:215:b2ff:fea7:90f8"; };
                          { "sas1-8865.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:138:215:b2ff:fea7:bae4"; };
                          { "sas1-8932.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:141:feaa:14ff:fede:3f12"; };
                          { "sas1-8945.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:1ac:feaa:14ff:fede:3f0e"; };
                          { "sas1-8948.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:13f:feaa:14ff:fede:3ef0"; };
                          { "sas1-8960.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:13f:feaa:14ff:fede:4050"; };
                          { "sas1-8989.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:613:feaa:14ff:fede:4125"; };
                          { "sas1-9038.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:143:feaa:14ff:fede:427a"; };
                          { "sas1-9055.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:13e:feaa:14ff:fede:3f68"; };
                          { "sas1-9058.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:13e:feaa:14ff:fede:3f2e"; };
                          { "sas1-9061.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:13e:feaa:14ff:fede:41b0"; };
                          { "sas1-9085.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:143:feaa:14ff:fede:438a"; };
                          { "sas1-9142.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3fc2"; };
                          { "sas1-9151.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:143:feaa:14ff:fede:422c"; };
                          { "sas1-9164.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:143:feaa:14ff:fede:417c"; };
                          { "sas1-9218.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:143:feaa:14ff:fede:45bc"; };
                          { "sas1-9222.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:13c:feaa:14ff:fede:4040"; };
                          { "sas1-9247.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:143:feaa:14ff:fede:401a"; };
                          { "sas1-9282.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:143:feaa:14ff:fede:45f4"; };
                          { "sas1-9283.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:143:feaa:14ff:fede:3eb8"; };
                          { "sas1-9361.search.yandex.net"; 10240; 360.000; "2a02:6b8:b000:13a:feaa:14ff:fede:4322"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- sasruweb_reportrenderer
                  sasruimgs_reportrenderer = {
                    priority = 14;
                    match_fsm = {
                      host = "sasruimgs\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "sas1-1099.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:666:96de:80ff:feec:1c28"; };
                          { "sas1-1352.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:618:215:b2ff:fea8:d3a"; };
                          { "sas1-1370.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:69f:215:b2ff:fea8:6d8c"; };
                          { "sas1-1376.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:1a8:215:b2ff:fea8:db6"; };
                          { "sas1-1383.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:1a7:215:b2ff:fea8:a3a"; };
                          { "sas1-1422.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6a7:215:b2ff:fea8:6d3c"; };
                          { "sas1-1424.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:121:215:b2ff:fea8:cea"; };
                          { "sas1-1426.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:196:215:b2ff:fea8:dba"; };
                          { "sas1-1433.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:112:215:b2ff:fea8:6dc8"; };
                          { "sas1-1440.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:1a6:215:b2ff:fea8:d2a"; };
                          { "sas1-1774.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:625:96de:80ff:feec:198a"; };
                          { "sas1-1958.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:652:96de:80ff:feec:1c24"; };
                          { "sas1-2200.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:654:96de:80ff:feec:186a"; };
                          { "sas1-4461.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:651:96de:80ff:feec:1830"; };
                          { "sas1-5360.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1b82"; };
                          { "sas1-5451.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:1a9:96de:80ff:feec:f98"; };
                          { "sas1-5455.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:652:96de:80ff:feec:1ec4"; };
                          { "sas1-5456.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6a2:96de:80ff:feec:1924"; };
                          { "sas1-5462.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:63d:96de:80ff:feec:1316"; };
                          { "sas1-5463.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1002"; };
                          { "sas1-5464.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6a1:96de:80ff:feec:171c"; };
                          { "sas1-5466.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:625:96de:80ff:feec:120e"; };
                          { "sas1-5473.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:61c:96de:80ff:feec:11f4"; };
                          { "sas1-5475.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:139:96de:80ff:feec:fcc"; };
                          { "sas1-5476.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:651:96de:80ff:feec:10cc"; };
                          { "sas1-5477.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:652:96de:80ff:feec:18a2"; };
                          { "sas1-5478.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:61c:96de:80ff:feec:1898"; };
                          { "sas1-5485.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:63a:96de:80ff:feec:1a5e"; };
                          { "sas1-5486.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:653:96de:80ff:feec:1876"; };
                          { "sas1-5487.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:653:96de:80ff:feec:1124"; };
                          { "sas1-5488.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:61c:96de:80ff:fee9:1aa"; };
                          { "sas1-5491.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:619:96de:80ff:feec:17de"; };
                          { "sas1-5493.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:653:96de:80ff:fee9:fd"; };
                          { "sas1-5495.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:63c:96de:80ff:feec:157c"; };
                          { "sas1-5498.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:139:96de:80ff:fee9:1de"; };
                          { "sas1-5499.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:139:96de:80ff:fee9:184"; };
                          { "sas1-5501.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:655:96de:80ff:feec:1624"; };
                          { "sas1-5502.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6a1:96de:80ff:feec:10fe"; };
                          { "sas1-5506.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:655:96de:80ff:feec:1a38"; };
                          { "sas1-5507.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:656:96de:80ff:feec:1a32"; };
                          { "sas1-5508.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:674:96de:80ff:feec:1b9c"; };
                          { "sas1-5513.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:111:96de:80ff:feec:1ba8"; };
                          { "sas1-5516.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:655:96de:80ff:feec:18fc"; };
                          { "sas1-5518.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6a1:96de:80ff:feec:1cc0"; };
                          { "sas1-5629.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:163:225:90ff:fee6:4d12"; };
                          { "sas1-5649.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:602:225:90ff:fee6:4c7a"; };
                          { "sas1-5657.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:649:225:90ff:fee6:dd88"; };
                          { "sas1-5662.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:649:225:90ff:fee5:bde0"; };
                          { "sas1-5960.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:636:225:90ff:feef:c964"; };
                          { "sas1-5965.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:604:225:90ff:feeb:fba6"; };
                          { "sas1-5966.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:635:225:90ff:feeb:fd9e"; };
                          { "sas1-5967.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601:225:90ff:feeb:fcd6"; };
                          { "sas1-5968.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:604:225:90ff:feeb:fba2"; };
                          { "sas1-5969.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601:225:90ff:feeb:f9b8"; };
                          { "sas1-5970.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:100:225:90ff:feeb:fce2"; };
                          { "sas1-5971.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:604:225:90ff:feed:2b8e"; };
                          { "sas1-5972.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:174:225:90ff:feed:2b4e"; };
                          { "sas1-5973.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:100:225:90ff:fee3:9cf2"; };
                          { "sas1-5974.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:100:225:90ff:feed:2fde"; };
                          { "sas1-6351.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:11a:215:b2ff:fea7:8280"; };
                          { "sas1-6752.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6a4:215:b2ff:fea7:9008"; };
                          { "sas1-6893.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:11d:215:b2ff:fea7:9060"; };
                          { "sas1-6939.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:12f:215:b2ff:fea7:b324"; };
                          { "sas1-6978.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:123:215:b2ff:fea7:b720"; };
                          { "sas1-7095.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:124:215:b2ff:fea7:ab98"; };
                          { "sas1-7098.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:124:215:b2ff:fea7:8ee4"; };
                          { "sas1-7125.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:1a5:215:b2ff:fea7:907c"; };
                          { "sas1-7155.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:122:215:b2ff:fea7:8bf0"; };
                          { "sas1-7156.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:12c:215:b2ff:fea7:aae4"; };
                          { "sas1-7238.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:121:215:b2ff:fea7:ac10"; };
                          { "sas1-7272.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:121:215:b2ff:fea7:8300"; };
                          { "sas1-7286.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:128:215:b2ff:fea7:7aec"; };
                          { "sas1-7287.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:128:215:b2ff:fea7:7910"; };
                          { "sas1-7326.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:126:215:b2ff:fea7:ad8c"; };
                          { "sas1-7330.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:628:215:b2ff:fea7:90d0"; };
                          { "sas1-7331.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:126:215:b2ff:fea7:bc9c"; };
                          { "sas1-7459.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:1a7:215:b2ff:fea7:8cd0"; };
                          { "sas1-7494.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:125:215:b2ff:fea7:6590"; };
                          { "sas1-7498.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:125:215:b2ff:fea7:aaa4"; };
                          { "sas1-7825.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:7fbc"; };
                          { "sas1-7843.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:1a4:215:b2ff:fea7:82f0"; };
                          { "sas1-7929.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:12b:215:b2ff:fea7:ac24"; };
                          { "sas1-8873.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:138:215:b2ff:fea7:ba80"; };
                          { "sas1-8979.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:13d:feaa:14ff:fede:3f9e"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- sasruimgs_reportrenderer
                  sasruvideo_reportrenderer = {
                    priority = 13;
                    match_fsm = {
                      host = "sasruvideo\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "sas1-0554.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:636:225:90ff:feed:2f90"; };
                          { "sas1-0582.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:601:225:90ff:feed:2e46"; };
                          { "sas1-1040.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:100:225:90ff:feef:ccba"; };
                          { "sas1-1077.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:62f:922b:34ff:fecf:3130"; };
                          { "sas1-1346.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:174:225:90ff:feed:317c"; };
                          { "sas1-1413.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:174:225:90ff:feed:2d94"; };
                          { "sas1-1425.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:128:215:b2ff:fea8:db2"; };
                          { "sas1-1429.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:110:215:b2ff:fea8:d1a"; };
                          { "sas1-1453.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:119:215:b2ff:fea7:efac"; };
                          { "sas1-1488.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:134:215:b2ff:fea8:966"; };
                          { "sas1-1525.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:135:215:b2ff:fea8:d7a"; };
                          { "sas1-1572.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:642:96de:80ff:fe5e:dc42"; };
                          { "sas1-1643.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:653:96de:80ff:fe5e:dca0"; };
                          { "sas1-1664.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:11d:215:b2ff:fea8:6c84"; };
                          { "sas1-1671.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:117:215:b2ff:fea8:d52"; };
                          { "sas1-1758.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:de48"; };
                          { "sas1-1759.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:17e:96de:80ff:fe8c:ba4a"; };
                          { "sas1-1809.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:67c:922b:34ff:fecf:2efa"; };
                          { "sas1-1815.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:67c:96de:80ff:fe8c:bd34"; };
                          { "sas1-1832.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:120:215:b2ff:fea8:a6a"; };
                          { "sas1-1880.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:e984"; };
                          { "sas1-1885.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:17d:96de:80ff:fe8c:da12"; };
                          { "sas1-1915.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:17c:96de:80ff:fe8c:e432"; };
                          { "sas1-1979.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:17b:96de:80ff:fe8c:d9a8"; };
                          { "sas1-2046.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:67b:96de:80ff:fe8c:d8e6"; };
                          { "sas1-5450.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:625:96de:80ff:feec:16da"; };
                          { "sas1-5452.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:1a9:96de:80ff:feec:1a40"; };
                          { "sas1-5470.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:654:96de:80ff:feec:10a4"; };
                          { "sas1-5504.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:63d:96de:80ff:feec:1ecc"; };
                          { "sas1-5514.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:63a:96de:80ff:feec:18ce"; };
                          { "sas1-5517.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:669:96de:80ff:feec:1902"; };
                          { "sas1-5624.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:160:225:90ff:fee6:4b3c"; };
                          { "sas1-5625.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:163:225:90ff:fee6:4d14"; };
                          { "sas1-5633.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:622:225:90ff:fee6:4860"; };
                          { "sas1-5638.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:602:225:90ff:fee5:bd36"; };
                          { "sas1-5642.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:622:225:90ff:fee5:c134"; };
                          { "sas1-5643.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:161:225:90ff:fee6:de52"; };
                          { "sas1-5645.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:161:225:90ff:fee5:bf6c"; };
                          { "sas1-5648.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:602:225:90ff:fee6:de28"; };
                          { "sas1-5650.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:602:225:90ff:fee6:de54"; };
                          { "sas1-5659.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:649:225:90ff:fee6:dd7c"; };
                          { "sas1-5660.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:649:225:90ff:fee6:df02"; };
                          { "sas1-5665.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:636:225:90ff:feed:2de4"; };
                          { "sas1-5963.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:601:225:90ff:feed:2cea"; };
                          { "sas1-6750.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:11e:215:b2ff:fea7:a8c0"; };
                          { "sas1-6971.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:123:215:b2ff:fea7:b10c"; };
                          { "sas1-7168.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:122:215:b2ff:fea7:7c88"; };
                          { "sas1-7574.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:129:215:b2ff:fea7:b1d8"; };
                          { "sas1-7747.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:127:215:b2ff:fea7:af8c"; };
                          { "sas1-7824.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:12d:215:b2ff:fea7:aa44"; };
                          { "sas1-8236.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:131:215:b2ff:fea7:b11c"; };
                          { "sas1-8246.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:131:215:b2ff:fea7:b0b4"; };
                          { "sas1-8401.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:130:215:b2ff:fea7:b514"; };
                          { "sas1-8546.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:135:215:b2ff:fea7:8264"; };
                          { "sas1-8571.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:617:215:b2ff:fea7:8e60"; };
                          { "sas1-8618.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:134:215:b2ff:fea7:b038"; };
                          { "sas1-8830.search.yandex.net"; 10240; 230.000; "2a02:6b8:b000:138:215:b2ff:fea7:8fa4"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- sasruvideo_reportrenderer
                  manruweb_reportrenderer = {
                    priority = 12;
                    match_fsm = {
                      host = "manruweb\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "man1-0433.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602e:92e2:baff:fe74:79d6"; };
                          { "man1-0686.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f98"; };
                          { "man1-0926.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:600e:92e2:baff:fe56:ea26"; };
                          { "man1-0934.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f3ee"; };
                          { "man1-0976.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:600f:92e2:baff:fe56:e9de"; };
                          { "man1-0977.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:600f:92e2:baff:fe56:e8f6"; };
                          { "man1-1165.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6021:f652:14ff:fe55:1d70"; };
                          { "man1-1173.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f7ba"; };
                          { "man1-1212.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601a:f652:14ff:fe55:2a70"; };
                          { "man1-1215.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6005:f652:14ff:fe55:1d80"; };
                          { "man1-1216.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602a:f652:14ff:fe55:2a50"; };
                          { "man1-1220.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601b:f652:14ff:fe55:1d30"; };
                          { "man1-1221.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6014:f652:14ff:fe48:9980"; };
                          { "man1-1222.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6014:f652:14ff:fe48:9960"; };
                          { "man1-1223.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6014:f652:14ff:fe48:8ce0"; };
                          { "man1-1284.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5270"; };
                          { "man1-1285.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6000:f652:14ff:fe44:5620"; };
                          { "man1-1287.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6029:f652:14ff:fe44:51b0"; };
                          { "man1-1288.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6039:f652:14ff:fe44:5630"; };
                          { "man1-1290.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6021:f652:14ff:fe48:a360"; };
                          { "man1-1291.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601f:f652:14ff:fe48:a310"; };
                          { "man1-1292.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601f:f652:14ff:fe44:5930"; };
                          { "man1-1295.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6016:f652:14ff:fe48:9f70"; };
                          { "man1-1306.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6013:f652:14ff:fe44:5120"; };
                          { "man1-1307.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6013:f652:14ff:fe48:8f50"; };
                          { "man1-1323.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d880"; };
                          { "man1-1324.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e730"; };
                          { "man1-1325.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6026:f652:14ff:fe8b:de30"; };
                          { "man1-1358.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:dfb0"; };
                          { "man1-1366.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:f0f0"; };
                          { "man1-1394.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:b340"; };
                          { "man1-1395.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:b830"; };
                          { "man1-1397.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:a610"; };
                          { "man1-1398.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b820"; };
                          { "man1-1447.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:d5a0"; };
                          { "man1-1449.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6020:f652:14ff:fe8b:e710"; };
                          { "man1-1451.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6023:f652:14ff:fe8b:f2e0"; };
                          { "man1-1456.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6026:f652:14ff:fe8b:e780"; };
                          { "man1-1457.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601e:f652:14ff:fe8b:e640"; };
                          { "man1-1458.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e740"; };
                          { "man1-1459.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6097:f652:14ff:fe8b:e700"; };
                          { "man1-1461.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:e610"; };
                          { "man1-1462.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:de20"; };
                          { "man1-1488.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:ac00"; };
                          { "man1-1489.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:b310"; };
                          { "man1-1490.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:abc0"; };
                          { "man1-1492.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:ac40"; };
                          { "man1-1764.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b120"; };
                          { "man1-1875.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:1d30"; };
                          { "man1-2199.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:c070"; };
                          { "man1-2290.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6097:f652:14ff:fe8c:d290"; };
                          { "man1-2572.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6022:f652:14ff:fe8c:1bc0"; };
                          { "man1-2964.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:e7b0"; };
                          { "man1-3014.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6028:f652:14ff:fe55:28b0"; };
                          { "man1-3086.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602a:f652:14ff:fe55:2ba0"; };
                          { "man1-3185.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6097:f652:14ff:fe8c:2a90"; };
                          { "man1-3374.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:d130"; };
                          { "man1-3397.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c6b0"; };
                          { "man1-3491.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6031:92e2:baff:fe75:4b24"; };
                          { "man1-3497.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6034:92e2:baff:fe74:78a0"; };
                          { "man1-3501.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602c:92e2:baff:fe74:7984"; };
                          { "man1-3502.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f37a"; };
                          { "man1-3503.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f4b4"; };
                          { "man1-3504.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6007:92e2:baff:fe55:f628"; };
                          { "man1-3505.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6011:92e2:baff:fe55:f29c"; };
                          { "man1-3506.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6011:92e2:baff:fe55:f442"; };
                          { "man1-3508.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:600e:92e2:baff:fe5b:974a"; };
                          { "man1-3509.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f68c"; };
                          { "man1-3511.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f4bc"; };
                          { "man1-3513.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6010:92e2:baff:fe5b:9dc4"; };
                          { "man1-3514.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f6ee"; };
                          { "man1-3516.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f4ee"; };
                          { "man1-3525.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:e0b0"; };
                          { "man1-3531.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b550"; };
                          { "man1-3532.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:cd70"; };
                          { "man1-3536.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:b6f0"; };
                          { "man1-3538.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:21f0"; };
                          { "man1-3539.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:21e0"; };
                          { "man1-3540.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:2c30"; };
                          { "man1-3545.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6019:f652:14ff:fe8c:2390"; };
                          { "man1-3546.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2630"; };
                          { "man1-3547.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2cb0"; };
                          { "man1-3548.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:2250"; };
                          { "man1-3549.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:15e0"; };
                          { "man1-3550.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6011:f652:14ff:fe8c:14e0"; };
                          { "man1-3551.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:2c0"; };
                          { "man1-3553.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601f:f652:14ff:fe8c:21c0"; };
                          { "man1-3554.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6020:f652:14ff:fe8c:1940"; };
                          { "man1-3555.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6020:f652:14ff:fe8c:1390"; };
                          { "man1-3556.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:660"; };
                          { "man1-3557.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6020:e61d:2dff:fe6c:f420"; };
                          { "man1-3558.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:2030"; };
                          { "man1-3559.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:21a0"; };
                          { "man1-3560.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:20e0"; };
                          { "man1-3561.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1f70"; };
                          { "man1-3562.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:2070"; };
                          { "man1-3563.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:1ac0"; };
                          { "man1-3564.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2060"; };
                          { "man1-3565.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:2090"; };
                          { "man1-3566.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f490"; };
                          { "man1-3568.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f65c"; };
                          { "man1-3570.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1ca0"; };
                          { "man1-3571.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1840"; };
                          { "man1-3572.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1780"; };
                          { "man1-3573.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6001:f652:14ff:fe8c:16e0"; };
                          { "man1-3574.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6001:f652:14ff:fe8c:1810"; };
                          { "man1-3575.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6037:f652:14ff:fe8c:2da0"; };
                          { "man1-3576.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602a:f652:14ff:fe8c:17c0"; };
                          { "man1-3577.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:ff30"; };
                          { "man1-3578.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:e340"; };
                          { "man1-3580.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602a:f652:14ff:fe8b:fd70"; };
                          { "man1-3581.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:fe40"; };
                          { "man1-3582.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:fdd0"; };
                          { "man1-3583.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6004:f652:14ff:fe8c:1720"; };
                          { "man1-3584.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6037:f652:14ff:fe8b:fe50"; };
                          { "man1-3585.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:fd50"; };
                          { "man1-3588.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:fda0"; };
                          { "man1-3589.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6039:f652:14ff:fe8c:1790"; };
                          { "man1-3590.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:fd60"; };
                          { "man1-3591.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:17f0"; };
                          { "man1-3593.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6000:f652:14ff:fe8b:fe60"; };
                          { "man1-3594.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:17e0"; };
                          { "man1-3595.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6000:f652:14ff:fe8c:1cc0"; };
                          { "man1-3596.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:4a0"; };
                          { "man1-3597.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:e290"; };
                          { "man1-3601.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1250"; };
                          { "man1-3602.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1240"; };
                          { "man1-3603.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6038:f652:14ff:fe8c:1830"; };
                          { "man1-3604.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:1220"; };
                          { "man1-3606.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:5e0"; };
                          { "man1-3607.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:2bf0"; };
                          { "man1-3608.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6023:f652:14ff:fe8b:fce0"; };
                          { "man1-3609.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:2bd0"; };
                          { "man1-3610.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:db10"; };
                          { "man1-3611.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:e8c0"; };
                          { "man1-3612.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:eef0"; };
                          { "man1-3613.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:da90"; };
                          { "man1-3614.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7d2a"; };
                          { "man1-3615.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6002:92e2:baff:fe74:7a66"; };
                          { "man1-3616.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6002:92e2:baff:fe74:78b6"; };
                          { "man1-3657.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6002:92e2:baff:fe75:4b96"; };
                          { "man1-3691.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603c:92e2:baff:fe74:776e"; };
                          { "man1-3692.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:be1a"; };
                          { "man1-3693.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7b22"; };
                          { "man1-3694.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7e12"; };
                          { "man1-3695.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7960"; };
                          { "man1-3696.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7f6a"; };
                          { "man1-3697.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7e30"; };
                          { "man1-3698.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7a8c"; };
                          { "man1-3699.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603a:92e2:baff:fe74:779e"; };
                          { "man1-3700.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7b70"; };
                          { "man1-3701.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:b7e8"; };
                          { "man1-3703.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602d:92e2:baff:fe6b:d10e"; };
                          { "man1-3704.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602d:92e2:baff:fe74:7f82"; };
                          { "man1-3705.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602d:92e2:baff:fe6b:d230"; };
                          { "man1-3707.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603c:92e2:baff:fe74:78e8"; };
                          { "man1-3709.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7c62"; };
                          { "man1-3746.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603d:92e2:baff:fe6f:8136"; };
                          { "man1-3769.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603d:92e2:baff:fe74:777a"; };
                          { "man1-3773.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603d:92e2:baff:fe6f:7efe"; };
                          { "man1-3842.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:bd22"; };
                          { "man1-3843.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603d:92e2:baff:fe74:795e"; };
                          { "man1-3855.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7bc6"; };
                          { "man1-3883.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6079:92e2:baff:fea1:763c"; };
                          { "man1-3911.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603e:92e2:baff:fe74:79a8"; };
                          { "man1-3916.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603e:92e2:baff:fe6e:c006"; };
                          { "man1-3925.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7638"; };
                          { "man1-3927.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607d:92e2:baff:fea1:765c"; };
                          { "man1-3932.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7a7c"; };
                          { "man1-3933.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e80"; };
                          { "man1-3936.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7a7e"; };
                          { "man1-3967.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603e:92e2:baff:fe74:78da"; };
                          { "man1-3977.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f66"; };
                          { "man1-4015.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e38"; };
                          { "man1-4016.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7a84"; };
                          { "man1-4031.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607b:92e2:baff:fe74:7ca4"; };
                          { "man1-4032.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7a82"; };
                          { "man1-4033.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e32"; };
                          { "man1-4034.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607a:92e2:baff:fea1:7e42"; };
                          { "man1-4035.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7650"; };
                          { "man1-4037.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603c:92e2:baff:fe75:475a"; };
                          { "man1-4040.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7622"; };
                          { "man1-4041.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7a86"; };
                          { "man1-4043.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7e40"; };
                          { "man1-4065.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7f60"; };
                          { "man1-4070.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7fce"; };
                          { "man1-4071.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7e3e"; };
                          { "man1-4072.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7e4a"; };
                          { "man1-4075.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe74:774e"; };
                          { "man1-4079.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6042:92e2:baff:fe74:7628"; };
                          { "man1-4145.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6040:92e2:baff:fe74:7640"; };
                          { "man1-4163.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7eca"; };
                          { "man1-4176.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7ee0"; };
                          { "man1-4242.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6043:92e2:baff:fe6f:7e8e"; };
                          { "man1-4253.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:ba7e"; };
                          { "man1-4277.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6043:92e2:baff:fe6e:bea8"; };
                          { "man1-4339.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7a40"; };
                          { "man1-4365.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603f:92e2:baff:fe56:e9c2"; };
                          { "man1-4407.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:b8bc"; };
                          { "man1-4420.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603f:92e2:baff:fe74:79c6"; };
                          { "man1-4421.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603f:92e2:baff:fe74:77ce"; };
                          { "man1-4423.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:b90c"; };
                          { "man1-4425.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603f:92e2:baff:fe74:775a"; };
                          { "man1-4428.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:603f:92e2:baff:fe6f:7f10"; };
                          { "man1-4455.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe74:77c2"; };
                          { "man1-4456.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98a"; };
                          { "man1-4467.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6041:92e2:baff:fe6e:bfae"; };
                          { "man1-4468.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6041:92e2:baff:fe74:78d8"; };
                          { "man1-4471.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe6f:7e42"; };
                          { "man1-4473.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7ed4"; };
                          { "man1-4477.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7be6"; };
                          { "man1-4480.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b990"; };
                          { "man1-4481.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bafa"; };
                          { "man1-4486.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7d22"; };
                          { "man1-4498.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7730"; };
                          { "man1-4503.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f24"; };
                          { "man1-4504.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6041:92e2:baff:fe74:761c"; };
                          { "man1-4505.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6041:92e2:baff:fe75:4886"; };
                          { "man1-4520.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602d:92e2:baff:fe52:79ba"; };
                          { "man1-4522.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:ba44"; };
                          { "man1-4524.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602d:92e2:baff:fe6f:7d14"; };
                          { "man1-4526.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799e"; };
                          { "man1-4527.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:bda4"; };
                          { "man1-4528.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6007:92e2:baff:fe6f:7d72"; };
                          { "man1-4529.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602d:92e2:baff:fe74:7f7a"; };
                          { "man1-4530.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:602d:92e2:baff:fe74:772c"; };
                          { "man1-4550.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601d:92e2:baff:fe55:f6c0"; };
                          { "man1-4552.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a20"; };
                          { "man1-4555.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601d:f652:14ff:fe55:1cf0"; };
                          { "man1-4556.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a80"; };
                          { "man1-4557.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a10"; };
                          { "man1-4558.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2aa0"; };
                          { "man1-5080.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6051:f652:14ff:fe74:3850"; };
                          { "man1-5117.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:604c:e61d:2dff:fe00:9400"; };
                          { "man1-5232.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6076:92e2:baff:fea1:7762"; };
                          { "man1-5299.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:604e:e61d:2dff:fe01:ef10"; };
                          { "man1-5300.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7642"; };
                          { "man1-5304.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e3c"; };
                          { "man1-5334.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:604f:f652:14ff:fef5:d9a0"; };
                          { "man1-5351.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e3a"; };
                          { "man1-5378.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6050:f652:14ff:fef5:d130"; };
                          { "man1-5392.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6050:f652:14ff:fef5:d960"; };
                          { "man1-5400.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6050:e61d:2dff:fe03:45d0"; };
                          { "man1-5428.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6050:e61d:2dff:fe01:e540"; };
                          { "man1-5432.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607c:92e2:baff:fea1:7e7e"; };
                          { "man1-5482.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7a76"; };
                          { "man1-5552.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7764"; };
                          { "man1-5593.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6079:92e2:baff:fea1:763e"; };
                          { "man1-5612.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:86a0"; };
                          { "man1-5616.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6055:f652:14ff:fe74:3920"; };
                          { "man1-5637.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8f90"; };
                          { "man1-5645.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6079:92e2:baff:fea1:807e"; };
                          { "man1-5671.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6077:92e2:baff:fea1:7e4c"; };
                          { "man1-5672.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7f64"; };
                          { "man1-5684.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6079:92e2:baff:fea1:8002"; };
                          { "man1-5694.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:605b:e61d:2dff:fe01:e790"; };
                          { "man1-5717.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7658"; };
                          { "man1-5746.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:605b:e61d:2dff:fe00:9bb0"; };
                          { "man1-5767.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6077:92e2:baff:fea1:78a6"; };
                          { "man1-5786.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6052:f652:14ff:fe74:4220"; };
                          { "man1-5807.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:47e0"; };
                          { "man1-5836.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:4fb0"; };
                          { "man1-5858.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6058:f652:14ff:fef5:d900"; };
                          { "man1-5875.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4360"; };
                          { "man1-5896.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4ca0"; };
                          { "man1-5899.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4f10"; };
                          { "man1-5900.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:4f60"; };
                          { "man1-5902.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6059:e61d:2dff:fe01:ed00"; };
                          { "man1-5908.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:607b:92e2:baff:fea1:8050"; };
                          { "man1-5912.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6059:e61d:2dff:fe03:37c0"; };
                          { "man1-5938.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:605a:e61d:2dff:fe01:ef50"; };
                          { "man1-5966.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:605a:e61d:2dff:fe03:3a10"; };
                          { "man1-5967.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4cb0"; };
                          { "man1-5971.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:605a:e61d:2dff:fe03:3a00"; };
                          { "man1-5975.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:605a:e61d:2dff:fe00:8630"; };
                          { "man1-5978.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4e10"; };
                          { "man1-5980.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6058:e61d:2dff:fe03:3eb0"; };
                          { "man1-5981.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6058:e61d:2dff:fe03:4630"; };
                          { "man1-5984.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:81a0"; };
                          { "man1-5986.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8610"; };
                          { "man1-5994.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4b00"; };
                          { "man1-6003.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6056:f652:14ff:fef5:c920"; };
                          { "man1-6008.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:5180"; };
                          { "man1-6043.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6061:e61d:2dff:fe00:9b80"; };
                          { "man1-6084.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6051:e61d:2dff:fe03:4d00"; };
                          { "man1-6086.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6051:e61d:2dff:fe00:9ce0"; };
                          { "man1-8204.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6078:92e2:baff:fea1:804c"; };
                          { "man1-8205.search.yandex.net"; 10240; 240.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7a80"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- manruweb_reportrenderer
                  manruimgs_reportrenderer = {
                    priority = 11;
                    match_fsm = {
                      host = "manruimgs\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "man1-1076.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f1f2"; };
                          { "man1-1150.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6010:92e2:baff:fe55:f2ca"; };
                          { "man1-1515.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b150"; };
                          { "man1-1885.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1d50"; };
                          { "man1-1957.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:16f0"; };
                          { "man1-1979.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:550"; };
                          { "man1-2023.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:1640"; };
                          { "man1-2087.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601c:f652:14ff:fe8b:e320"; };
                          { "man1-2092.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6098:f652:14ff:fe8c:1e0"; };
                          { "man1-2106.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601c:f652:14ff:fe8c:720"; };
                          { "man1-2112.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601e:f652:14ff:fe8c:560"; };
                          { "man1-2383.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6011:f652:14ff:fe55:1ca0"; };
                          { "man1-2873.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e840"; };
                          { "man1-2943.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dc40"; };
                          { "man1-3175.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6005:f652:14ff:fe8c:1ff0"; };
                          { "man1-3252.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1a10"; };
                          { "man1-3260.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c700"; };
                          { "man1-3261.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:c6d0"; };
                          { "man1-3265.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6038:f652:14ff:fe8b:d4c0"; };
                          { "man1-3375.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:c580"; };
                          { "man1-3479.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6036:92e2:baff:fe74:7bd0"; };
                          { "man1-3484.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:602f:92e2:baff:fe72:7bfa"; };
                          { "man1-3489.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:602f:92e2:baff:fe6f:7e86"; };
                          { "man1-3493.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:602b:92e2:baff:fe6f:815a"; };
                          { "man1-3498.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:b6a4"; };
                          { "man1-3499.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:7eae"; };
                          { "man1-3500.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:602c:92e2:baff:fe6f:80e8"; };
                          { "man1-3510.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:600e:92e2:baff:fe55:f7b4"; };
                          { "man1-3512.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f6f6"; };
                          { "man1-3517.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6014:f652:14ff:fe44:5250"; };
                          { "man1-3520.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:b7d0"; };
                          { "man1-3523.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d6b0"; };
                          { "man1-3524.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6015:f652:14ff:fe8b:d710"; };
                          { "man1-3526.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:d5c0"; };
                          { "man1-3527.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:d6a0"; };
                          { "man1-3528.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aeb0"; };
                          { "man1-3529.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:b010"; };
                          { "man1-3533.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cdb0"; };
                          { "man1-3534.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6018:f652:14ff:fe8b:b590"; };
                          { "man1-3535.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6017:f652:14ff:fe8b:cda0"; };
                          { "man1-3537.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6027:f652:14ff:fe8c:2220"; };
                          { "man1-3542.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd50"; };
                          { "man1-3544.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:2610"; };
                          { "man1-3752.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603d:92e2:baff:fe6e:b9d0"; };
                          { "man1-3822.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603a:92e2:baff:fe74:7eaa"; };
                          { "man1-3904.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7866"; };
                          { "man1-3959.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603e:92e2:baff:fe6f:80a2"; };
                          { "man1-4025.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603c:92e2:baff:fe6e:ba8c"; };
                          { "man1-4073.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6040:92e2:baff:fe74:770a"; };
                          { "man1-4074.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6040:92e2:baff:fe6f:7dc0"; };
                          { "man1-4076.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7d6c"; };
                          { "man1-4077.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:bdda"; };
                          { "man1-4078.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b656"; };
                          { "man1-4080.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6042:92e2:baff:fe6f:7d86"; };
                          { "man1-4081.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6042:92e2:baff:fe6e:b842"; };
                          { "man1-4082.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6043:92e2:baff:fe53:24d4"; };
                          { "man1-4083.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2610"; };
                          { "man1-4084.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2570"; };
                          { "man1-4085.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6043:92e2:baff:fe53:2434"; };
                          { "man1-4310.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7bcc"; };
                          { "man1-4311.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603f:92e2:baff:fe74:7dfc"; };
                          { "man1-4638.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:651d:215:b2ff:fea9:913a"; };
                          { "man1-5640.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8a30"; };
                          { "man1-6102.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:4ed0"; };
                          { "man1-6134.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3d50"; };
                          { "man1-6150.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6054:f652:14ff:fef5:da20"; };
                          { "man1-6161.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6054:e61d:2dff:fe03:3890"; };
                          { "man1-6167.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:82d0"; };
                          { "man1-6227.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6057:f652:14ff:fef5:c8b0"; };
                          { "man1-6242.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6057:f652:14ff:fef5:cb40"; };
                          { "man1-6263.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6057:e61d:2dff:fe03:5370"; };
                          { "man1-6359.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6053:f652:14ff:fe55:29c0"; };
                          { "man1-6393.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:48f0"; };
                          { "man1-6413.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:1ad0"; };
                          { "man1-6419.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6053:e61d:2dff:fe04:4720"; };
                          { "man1-6485.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6063:e61d:2dff:fe04:2430"; };
                          { "man1-6634.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:604f:e61d:2dff:fe01:f370"; };
                          { "man1-6727.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cd90"; };
                          { "man1-6728.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6054:f652:14ff:fef5:cdd0"; };
                          { "man1-6763.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6058:e61d:2dff:fe04:50d0"; };
                          { "man1-6767.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:605d:e61d:2dff:fe04:4bc0"; };
                          { "man1-6854.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6060:f652:14ff:fef5:c950"; };
                          { "man1-6873.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:605e:e61d:2dff:fe04:1ed0"; };
                          { "man1-6886.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3d30"; };
                          { "man1-6900.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3da0"; };
                          { "man1-6903.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:3cf0"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- manruimgs_reportrenderer
                  manruvideo_reportrenderer = {
                    priority = 10;
                    match_fsm = {
                      host = "manruvideo\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "man1-0978.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f404"; };
                          { "man1-1191.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6018:f652:14ff:fe48:9670"; };
                          { "man1-1209.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6028:f652:14ff:fe55:2a90"; };
                          { "man1-1282.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6029:f652:14ff:fe44:5280"; };
                          { "man1-1286.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6001:f652:14ff:fe44:56f0"; };
                          { "man1-1293.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6020:f652:14ff:fe48:9fa0"; };
                          { "man1-1294.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6015:f652:14ff:fe44:50f0"; };
                          { "man1-1296.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6016:f652:14ff:fe44:57c0"; };
                          { "man1-1396.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6005:f652:14ff:fe8b:a9f0"; };
                          { "man1-1400.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601b:f652:14ff:fe8b:b420"; };
                          { "man1-1446.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6020:f652:14ff:fe8b:f440"; };
                          { "man1-1448.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:f410"; };
                          { "man1-1450.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601e:f652:14ff:fe8b:d8a0"; };
                          { "man1-1454.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:eaf0"; };
                          { "man1-1455.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:e5f0"; };
                          { "man1-1748.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6027:f652:14ff:fe8b:aea0"; };
                          { "man1-1765.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:600d:f652:14ff:fe8b:b0d0"; };
                          { "man1-1927.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:be70"; };
                          { "man1-1947.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6029:f652:14ff:fe8b:ea20"; };
                          { "man1-2252.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:dd50"; };
                          { "man1-2264.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601a:f652:14ff:fe8c:d620"; };
                          { "man1-2453.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:d1b0"; };
                          { "man1-2464.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6021:f652:14ff:fe55:3560"; };
                          { "man1-2529.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6023:f652:14ff:fe8c:110"; };
                          { "man1-2582.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6022:f652:14ff:fe8b:c780"; };
                          { "man1-2670.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:3d0"; };
                          { "man1-2733.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6024:f652:14ff:fe8b:db20"; };
                          { "man1-2800.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6025:f652:14ff:fe8b:f460"; };
                          { "man1-2848.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e8b0"; };
                          { "man1-2960.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:edb0"; };
                          { "man1-3507.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:600f:92e2:baff:fe55:f3a4"; };
                          { "man1-3515.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6012:92e2:baff:fe55:f680"; };
                          { "man1-3519.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6014:f652:14ff:fe44:54d0"; };
                          { "man1-3522.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6016:f652:14ff:fe8b:ab20"; };
                          { "man1-3530.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6013:f652:14ff:fe8b:aed0"; };
                          { "man1-3541.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:600d:f652:14ff:fe8c:e0"; };
                          { "man1-3543.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6019:f652:14ff:fe8b:bd70"; };
                          { "man1-3552.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:fd00"; };
                          { "man1-3567.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6022:92e2:baff:fe55:f4a0"; };
                          { "man1-3569.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6026:f652:14ff:fe8c:1850"; };
                          { "man1-3579.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:602a:f652:14ff:fe8b:fd90"; };
                          { "man1-3586.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6004:f652:14ff:fe8b:fe00"; };
                          { "man1-3587.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6037:f652:14ff:fe8c:70"; };
                          { "man1-3592.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6024:f652:14ff:fe8c:1cd0"; };
                          { "man1-3598.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6000:f652:14ff:fe8c:f30"; };
                          { "man1-3599.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6028:f652:14ff:fe8b:fe30"; };
                          { "man1-3600.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6028:f652:14ff:fe8c:1760"; };
                          { "man1-3605.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6021:f652:14ff:fe8c:1800"; };
                          { "man1-3617.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6002:92e2:baff:fe6f:7d6e"; };
                          { "man1-3690.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:7d8a"; };
                          { "man1-3702.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7740"; };
                          { "man1-3706.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603c:92e2:baff:fe6f:81a0"; };
                          { "man1-3708.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:602d:92e2:baff:fe6e:b980"; };
                          { "man1-3783.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603d:92e2:baff:fe74:7aa8"; };
                          { "man1-3874.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603e:92e2:baff:fe74:7bf8"; };
                          { "man1-3926.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:607a:92e2:baff:fea1:8052"; };
                          { "man1-3971.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6075:92e2:baff:fea1:7f5c"; };
                          { "man1-4044.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:607c:92e2:baff:fea1:8054"; };
                          { "man1-4051.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603c:92e2:baff:fe74:7d78"; };
                          { "man1-4426.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:603f:92e2:baff:fe6e:bdb2"; };
                          { "man1-4461.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7a1c"; };
                          { "man1-4483.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6044:92e2:baff:fe74:7712"; };
                          { "man1-4489.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6044:92e2:baff:fe6e:b98c"; };
                          { "man1-4497.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6041:92e2:baff:fe74:7f42"; };
                          { "man1-4521.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:602d:92e2:baff:fe52:7a94"; };
                          { "man1-4525.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:602d:92e2:baff:fe74:799c"; };
                          { "man1-4548.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601d:f652:14ff:fe55:2a00"; };
                          { "man1-4553.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:601d:f652:14ff:fe55:29d0"; };
                          { "man1-5346.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:607c:92e2:baff:fe6e:b8b6"; };
                          { "man1-5399.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:607b:92e2:baff:fea1:7624"; };
                          { "man1-5503.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:607d:92e2:baff:fea1:7e48"; };
                          { "man1-5622.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6055:e61d:2dff:fe00:8f60"; };
                          { "man1-5826.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6061:e61d:2dff:fe03:4840"; };
                          { "man1-5914.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6059:e61d:2dff:fe00:8520"; };
                          { "man1-5965.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6078:92e2:baff:fea1:7640"; };
                          { "man1-6664.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:6047:f652:14ff:fe74:3c10"; };
                          { "man1-7451.search.yandex.net"; 10240; 160.000; "2a02:6b8:b000:606b:e61d:2dff:fe04:1f30"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- manruvideo_reportrenderer
                  vlaruweb_reportrenderer = {
                    priority = 9;
                    match_fsm = {
                      host = "vlaruweb\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "vla1-0234.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9e:0:604:db7:a8b4"; };
                          { "vla1-0239.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9e:0:604:db7:9cc1"; };
                          { "vla1-0241.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:81:0:604:db7:a92f"; };
                          { "vla1-0242.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:81:0:604:db7:a7c6"; };
                          { "vla1-0244.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:81:0:604:db7:a8cc"; };
                          { "vla1-0259.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9e:0:604:db7:a8f4"; };
                          { "vla1-0262.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9e:0:604:db7:a8ee"; };
                          { "vla1-0265.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9e:0:604:db7:a8e8"; };
                          { "vla1-0270.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9e:0:604:db7:a844"; };
                          { "vla1-0273.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9e:0:604:db7:a8a9"; };
                          { "vla1-0279.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:98:0:604:db7:9ce2"; };
                          { "vla1-0281.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:98:0:604:db7:a3d0"; };
                          { "vla1-0303.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:98:0:604:db7:a3c5"; };
                          { "vla1-0318.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9e:0:604:db7:a8ab"; };
                          { "vla1-0334.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:93:0:604:db7:aaf4"; };
                          { "vla1-0343.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:93:0:604:db8:db3a"; };
                          { "vla1-0355.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9d:0:604:d8f:eb9d"; };
                          { "vla1-0362.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9d:0:604:d8f:eba0"; };
                          { "vla1-0366.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9d:0:604:d8f:eb7e"; };
                          { "vla1-0368.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9d:0:604:d8f:eb36"; };
                          { "vla1-0376.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9d:0:604:d8f:eb81"; };
                          { "vla1-0385.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9d:0:604:d8f:eb10"; };
                          { "vla1-0398.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:12:0:604:db7:9b44"; };
                          { "vla1-0429.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:27:0:604:db7:9f71"; };
                          { "vla1-0431.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:26:0:604:db7:9f9b"; };
                          { "vla1-0476.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:34:0:604:db7:9c8b"; };
                          { "vla1-0496.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:17:0:604:db7:9ab7"; };
                          { "vla1-0510.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:13:0:604:db7:9caa"; };
                          { "vla1-0514.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:1b:0:604:db7:9d41"; };
                          { "vla1-0523.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:27:0:604:db7:9f70"; };
                          { "vla1-0546.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:17:0:604:db7:9927"; };
                          { "vla1-0569.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:52:0:604:db7:a462"; };
                          { "vla1-0575.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:54:0:604:db7:a4ca"; };
                          { "vla1-0600.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:53:0:604:db7:9cf8"; };
                          { "vla1-0655.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:1b:0:604:db7:9b85"; };
                          { "vla1-0671.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:52:0:604:db7:a4e8"; };
                          { "vla1-0672.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:13:0:604:db7:99ea"; };
                          { "vla1-0692.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:3c:0:604:db7:9ee6"; };
                          { "vla1-0706.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:53:0:604:db7:9ced"; };
                          { "vla1-0770.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:44:0:604:db7:a559"; };
                          { "vla1-0773.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:3c:0:604:db7:9ee0"; };
                          { "vla1-0973.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:43:0:604:db7:9e20"; };
                          { "vla1-1029.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:71:0:604:db7:a420"; };
                          { "vla1-1067.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:78:0:604:db7:aacf"; };
                          { "vla1-1092.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:54:0:604:db7:a5a7"; };
                          { "vla1-1192.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:81:0:604:d8f:eb20"; };
                          { "vla1-1219.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:8d:0:604:db7:aa47"; };
                          { "vla1-1226.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:43:0:604:db7:a545"; };
                          { "vla1-1277.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:52:0:604:db7:a466"; };
                          { "vla1-1314.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:54:0:604:db7:a67e"; };
                          { "vla1-1411.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:4c:0:604:db7:a0f2"; };
                          { "vla1-1521.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:44:0:604:db7:9f37"; };
                          { "vla1-1562.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:52:0:604:db7:a66a"; };
                          { "vla1-1637.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:67:0:604:db7:a2af"; };
                          { "vla1-1639.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:67:0:604:db7:a328"; };
                          { "vla1-1646.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:71:0:604:db7:a22d"; };
                          { "vla1-1668.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:44:0:604:db7:a73a"; };
                          { "vla1-1684.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:12:0:604:db7:9a66"; };
                          { "vla1-1716.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:67:0:604:db7:9cd7"; };
                          { "vla1-1728.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:57:0:604:db7:a604"; };
                          { "vla1-1774.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:71:0:604:db7:a789"; };
                          { "vla1-1818.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:45:0:604:db7:a6fa"; };
                          { "vla1-1826.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:88:0:604:db7:a9e5"; };
                          { "vla1-1828.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:8d:0:604:db7:ac33"; };
                          { "vla1-1830.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:78:0:604:db7:a780"; };
                          { "vla1-1850.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:8d:0:604:db7:aa43"; };
                          { "vla1-1881.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:71:0:604:db7:9d0c"; };
                          { "vla1-1883.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:71:0:604:db7:a775"; };
                          { "vla1-1933.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:78:0:604:db7:ab23"; };
                          { "vla1-1950.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:8d:0:604:db7:a9b5"; };
                          { "vla1-1976.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:3c:0:604:db7:a450"; };
                          { "vla1-1977.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:57:0:604:db7:a565"; };
                          { "vla1-1993.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:43:0:604:db7:a0e8"; };
                          { "vla1-1999.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:8d:0:604:db7:aa1c"; };
                          { "vla1-2014.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:44:0:604:db7:a668"; };
                          { "vla1-2034.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:78:0:604:db7:a94d"; };
                          { "vla1-2076.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:13:0:604:db7:9be1"; };
                          { "vla1-2077.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:3c:0:604:db7:9d50"; };
                          { "vla1-2081.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:84:0:604:db7:aac4"; };
                          { "vla1-2111.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:13:0:604:db7:9af3"; };
                          { "vla1-2120.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:17:0:604:db7:9925"; };
                          { "vla1-2123.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:53:0:604:db7:9d4c"; };
                          { "vla1-2128.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:3c:0:604:db7:9ef1"; };
                          { "vla1-2136.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:13:0:604:db7:99ee"; };
                          { "vla1-2161.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:1b:0:604:db7:9a71"; };
                          { "vla1-2193.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:45:0:604:db7:9d62"; };
                          { "vla1-2206.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:88:0:604:db8:db38"; };
                          { "vla1-2221.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9a:0:604:db7:a9df"; };
                          { "vla1-2339.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:86:0:604:db7:a85f"; };
                          { "vla1-2340.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:7d:0:604:db7:a3d6"; };
                          { "vla1-2391.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:7d:0:604:db7:a19a"; };
                          { "vla1-2396.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:8b:0:604:db7:abe0"; };
                          { "vla1-2397.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:79:0:604:db7:a959"; };
                          { "vla1-2400.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:79:0:604:db7:a962"; };
                          { "vla1-2401.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:8a:0:604:db7:a977"; };
                          { "vla1-2402.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:95:0:604:db7:ab46"; };
                          { "vla1-2407.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:8b:0:604:db7:abdc"; };
                          { "vla1-2415.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:94:0:604:db7:aa3e"; };
                          { "vla1-2418.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:7c:0:604:db7:9df5"; };
                          { "vla1-2422.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:95:0:604:db7:a9c7"; };
                          { "vla1-2423.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:95:0:604:db7:ab1a"; };
                          { "vla1-2427.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:79:0:604:db7:ab19"; };
                          { "vla1-2429.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:8a:0:604:db7:a972"; };
                          { "vla1-2431.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:79:0:604:db7:a861"; };
                          { "vla1-2432.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:94:0:604:db7:a7c8"; };
                          { "vla1-2435.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:97:0:604:db7:a93e"; };
                          { "vla1-2438.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:97:0:604:db7:a922"; };
                          { "vla1-2441.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:94:0:604:db7:aaef"; };
                          { "vla1-2442.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:79:0:604:db7:a95c"; };
                          { "vla1-2448.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:97:0:604:db7:a7f3"; };
                          { "vla1-2461.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:95:0:604:db7:aa56"; };
                          { "vla1-2470.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:94:0:604:db7:a7ba"; };
                          { "vla1-2484.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:79:0:604:db7:a37d"; };
                          { "vla1-2498.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:79:0:604:db7:a95f"; };
                          { "vla1-2511.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9b:0:604:db7:aa2b"; };
                          { "vla1-2512.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:97:0:604:db7:a915"; };
                          { "vla1-2522.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a2:0:604:db7:9956"; };
                          { "vla1-2531.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a2:0:604:db7:9b6a"; };
                          { "vla1-2547.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:2f:0:604:db7:9bd3"; };
                          { "vla1-2549.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a2:0:604:db7:9bdf"; };
                          { "vla1-2563.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:1f:0:604:db7:9c52"; };
                          { "vla1-2589.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:1f:0:604:db7:9fa6"; };
                          { "vla1-2606.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:32:0:604:db7:9f96"; };
                          { "vla1-2637.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:65:0:604:db7:9ca2"; };
                          { "vla1-2651.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:1f:0:604:db7:9c53"; };
                          { "vla1-2667.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:2f:0:604:db7:9f86"; };
                          { "vla1-2704.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:2f:0:604:db7:9c82"; };
                          { "vla1-2723.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:32:0:604:db7:a52f"; };
                          { "vla1-2727.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:32:0:604:db7:99af"; };
                          { "vla1-2774.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:74:0:604:db7:9efa"; };
                          { "vla1-2881.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:32:0:604:db7:9c07"; };
                          { "vla1-2964.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:6e:0:604:db7:a3fd"; };
                          { "vla1-2968.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:6e:0:604:db7:a263"; };
                          { "vla1-2993.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:6c:0:604:db7:a159"; };
                          { "vla1-2998.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:70:0:604:db7:a410"; };
                          { "vla1-3011.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:19:0:604:db6:e746"; };
                          { "vla1-3015.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:70:0:604:db7:a232"; };
                          { "vla1-3040.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a3:0:604:db7:a733"; };
                          { "vla1-3059.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a3:0:604:db7:9ddf"; };
                          { "vla1-3102.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:74:0:604:db7:98fd"; };
                          { "vla1-3125.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:11:0:604:db7:98f0"; };
                          { "vla1-3132.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:6c:0:604:db7:a520"; };
                          { "vla1-3149.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:70:0:604:db7:a281"; };
                          { "vla1-3174.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:11:0:604:db7:998d"; };
                          { "vla1-3247.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:6c:0:604:db7:9c25"; };
                          { "vla1-3259.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:19:0:604:db7:9ac9"; };
                          { "vla1-3263.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:11:0:604:db7:99fd"; };
                          { "vla1-3278.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:19:0:604:db7:99e8"; };
                          { "vla1-3285.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:11:0:604:db7:98f2"; };
                          { "vla1-3296.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:19:0:604:db7:9a36"; };
                          { "vla1-3297.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a3:0:604:db7:a6f5"; };
                          { "vla1-3312.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:6e:0:604:db7:a285"; };
                          { "vla1-3401.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:74:0:604:db7:9eb9"; };
                          { "vla1-3507.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a0:0:604:db7:a4f0"; };
                          { "vla1-3521.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:73:0:604:db7:a5de"; };
                          { "vla1-3553.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a0:0:604:db7:a672"; };
                          { "vla1-3571.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:73:0:604:db7:a0cf"; };
                          { "vla1-3580.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:73:0:604:db7:a0d6"; };
                          { "vla1-3606.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a1:0:604:db7:a292"; };
                          { "vla1-3610.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a1:0:604:db7:a00b"; };
                          { "vla1-3619.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a1:0:604:db7:a28f"; };
                          { "vla1-3626.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:63:0:604:db7:a309"; };
                          { "vla1-3632.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:46:0:604:db7:a0b4"; };
                          { "vla1-3635.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:46:0:604:db7:a552"; };
                          { "vla1-3664.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:72:0:604:db7:9e21"; };
                          { "vla1-3667.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:80:0:604:db7:a927"; };
                          { "vla1-3671.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:65:0:604:db7:a097"; };
                          { "vla1-3672.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:63:0:604:db7:a2f9"; };
                          { "vla1-3691.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:86:0:604:db7:abac"; };
                          { "vla1-3693.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:72:0:604:db7:a5c1"; };
                          { "vla1-3698.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:65:0:604:db7:a26a"; };
                          { "vla1-3699.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:86:0:604:db7:aba7"; };
                          { "vla1-3720.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:63:0:604:db7:a2e3"; };
                          { "vla1-3726.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:46:0:604:db7:a549"; };
                          { "vla1-3753.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a0:0:604:db7:a47b"; };
                          { "vla1-3759.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:86:0:604:db7:a82a"; };
                          { "vla1-3762.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:80:0:604:db7:a8e7"; };
                          { "vla1-3802.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:7f:0:604:db7:a1fe"; };
                          { "vla1-3841.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:72:0:604:db7:a5bf"; };
                          { "vla1-3850.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:7d:0:604:db7:9e5f"; };
                          { "vla1-3966.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:7f:0:604:db7:a3e7"; };
                          { "vla1-3996.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:a0:0:604:db7:a503"; };
                          { "vla1-4164.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:7f:0:604:db7:a3c9"; };
                          { "vla1-4219.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:8a:0:604:db7:a80e"; };
                          { "vla1-4269.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:90:0:604:db7:a87e"; };
                          { "vla1-4288.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:86:0:604:db7:ac10"; };
                          { "vla1-4308.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:90:0:604:db7:a996"; };
                          { "vla1-4491.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:9b:0:604:db7:aa24"; };
                          { "vla1-4575.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:77:0:604:db7:a941"; };
                          { "vla1-4581.search.yandex.net"; 10240; 348.000; "2a02:6b8:c0e:77:0:604:db7:a928"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- vlaruweb_reportrenderer
                  vlaruimgs_reportrenderer = {
                    priority = 8;
                    match_fsm = {
                      host = "vlaruimgs\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "vla1-0147.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:3f:0:604:db7:a6ca"; };
                          { "vla1-0152.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:3f:0:604:db7:a705"; };
                          { "vla1-0221.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:81:0:604:db7:a8cf"; };
                          { "vla1-0703.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:53:0:604:db7:9d01"; };
                          { "vla1-0951.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:71:0:604:db7:a3de"; };
                          { "vla1-1128.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:45:0:604:db7:a77d"; };
                          { "vla1-1161.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:8d:0:604:db7:ab54"; };
                          { "vla1-1184.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:5d:0:604:db7:a5f7"; };
                          { "vla1-1207.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:9a:0:604:db7:aa9b"; };
                          { "vla1-1228.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:5c:0:604:db7:a047"; };
                          { "vla1-1233.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:59:0:604:db7:9ff8"; };
                          { "vla1-1239.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:93:0:604:db7:a751"; };
                          { "vla1-1246.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:83:0:604:db7:a910"; };
                          { "vla1-1257.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:81:0:604:db7:a7e7"; };
                          { "vla1-1259.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:89:0:604:db7:abd2"; };
                          { "vla1-1278.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:4d:0:604:db7:a440"; };
                          { "vla1-1292.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:4e:0:604:db7:a4b6"; };
                          { "vla1-1294.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:1b:0:604:db7:9b7e"; };
                          { "vla1-1304.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:4c:0:604:db7:a0ce"; };
                          { "vla1-1315.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:67:0:604:db7:a325"; };
                          { "vla1-1330.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:78:0:604:db7:a9c3"; };
                          { "vla1-1341.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:5e:0:604:db7:9deb"; };
                          { "vla1-1354.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:61:0:604:db7:a5b5"; };
                          { "vla1-1356.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:96:0:604:db7:a9c1"; };
                          { "vla1-1358.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:45:0:604:db7:a764"; };
                          { "vla1-1366.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:85:0:604:db7:a877"; };
                          { "vla1-1384.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:44:0:604:db7:9f36"; };
                          { "vla1-1395.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:84:0:604:db7:abfb"; };
                          { "vla1-1421.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:58:0:604:db7:9e42"; };
                          { "vla1-1457.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:62:0:604:db7:a2ea"; };
                          { "vla1-1464.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:48:0:604:db7:a695"; };
                          { "vla1-1486.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:20:0:604:db7:9eb4"; };
                          { "vla1-1516.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:43:0:604:db7:a110"; };
                          { "vla1-1518.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:3c:0:604:db7:9eb8"; };
                          { "vla1-1598.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:13:0:604:db7:9a64"; };
                          { "vla1-1710.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:71:0:604:db7:a416"; };
                          { "vla1-1747.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:42:0:604:db7:a4e4"; };
                          { "vla1-1757.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:88:0:604:db7:a771"; };
                          { "vla1-1794.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:22:0:604:db7:9d6f"; };
                          { "vla1-1806.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:41:0:604:db7:9e2b"; };
                          { "vla1-1815.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:57:0:604:db7:a38f"; };
                          { "vla1-1911.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:45:0:604:db7:a5fd"; };
                          { "vla1-1930.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:44:0:604:db7:a6cc"; };
                          { "vla1-1987.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:82:0:604:db7:a872"; };
                          { "vla1-2016.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:91:0:604:db7:ab5e"; };
                          { "vla1-2017.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:34:0:604:db7:9d11"; };
                          { "vla1-2041.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:40:0:604:db7:a53f"; };
                          { "vla1-2078.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:53:0:604:db7:9d02"; };
                          { "vla1-2092.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:17:0:604:db7:a7f5"; };
                          { "vla1-2121.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:98:0:604:db7:a008"; };
                          { "vla1-2122.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:53:0:604:db7:9d07"; };
                          { "vla1-2143.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:27:0:604:db7:9f6f"; };
                          { "vla1-2163.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:53:0:604:db7:9cfa"; };
                          { "vla1-2253.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:5a:0:604:db7:a1dd"; };
                          { "vla1-2265.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:5b:0:604:db7:a1d7"; };
                          { "vla1-2315.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:6e:0:604:db7:a371"; };
                          { "vla1-2319.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:7f:0:604:db7:a368"; };
                          { "vla1-2330.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:7d:0:604:db7:a2b0"; };
                          { "vla1-2332.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:87:0:604:db7:a84b"; };
                          { "vla1-2345.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:86:0:604:db7:ab9e"; };
                          { "vla1-2351.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:87:0:604:db7:a7ea"; };
                          { "vla1-2358.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:7f:0:604:db7:a3d8"; };
                          { "vla1-2453.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:94:0:604:db7:a94f"; };
                          { "vla1-2472.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:97:0:604:db7:a909"; };
                          { "vla1-3780.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:7f:0:604:db7:9e7d"; };
                          { "vla1-4111.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:87:0:604:db7:a820"; };
                          { "vla1-4375.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:86:0:604:db7:a82d"; };
                          { "vla1-4428.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:8a:0:604:db7:a818"; };
                          { "vla1-4443.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:86:0:604:db7:9dee"; };
                          { "vla1-4504.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:90:0:604:db7:a7e1"; };
                          { "vla2-0560.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:9c:0:604:db7:aa6e"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- vlaruimgs_reportrenderer
                  vlaruvideo_reportrenderer = {
                    priority = 7;
                    match_fsm = {
                      host = "vlaruvideo\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "vla1-0213.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:82:0:604:db7:a8c1"; };
                          { "vla1-0553.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:17:0:604:db7:9b17"; };
                          { "vla1-0557.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:1b:0:604:db7:998b"; };
                          { "vla1-0762.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:17:0:604:db7:9923"; };
                          { "vla1-0852.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:57:0:604:db7:a51f"; };
                          { "vla1-1073.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:88:0:604:db7:a9f2"; };
                          { "vla1-1167.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:8d:0:604:db7:aa16"; };
                          { "vla1-1206.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:8d:0:604:db7:ab09"; };
                          { "vla1-1245.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:8d:0:604:db7:ab32"; };
                          { "vla1-1266.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:82:0:604:db7:a86c"; };
                          { "vla1-1412.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:45:0:604:db7:a78c"; };
                          { "vla1-1426.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:67:0:604:db7:9ed2"; };
                          { "vla1-1454.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:6f:0:604:db7:a423"; };
                          { "vla1-1484.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:67:0:604:db7:a23f"; };
                          { "vla1-1497.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:48:0:604:db7:9db8"; };
                          { "vla1-1508.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:48:0:604:db7:9db0"; };
                          { "vla1-1527.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:3c:0:604:db7:9cfe"; };
                          { "vla1-1530.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:43:0:604:db7:a71a"; };
                          { "vla1-1540.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:3c:0:604:db7:9edd"; };
                          { "vla1-1556.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:9a:0:604:db7:aa02"; };
                          { "vla1-1568.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:4c:0:604:db7:a1c3"; };
                          { "vla1-1571.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:45:0:604:db7:a56a"; };
                          { "vla1-1575.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:84:0:604:db7:a7f9"; };
                          { "vla1-1636.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:67:0:604:db7:a252"; };
                          { "vla1-1643.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:67:0:604:db7:a32b"; };
                          { "vla1-1650.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:71:0:604:db7:a599"; };
                          { "vla1-1654.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:48:0:604:db7:a0bf"; };
                          { "vla1-1744.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:71:0:604:db7:a734"; };
                          { "vla1-1785.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:4d:0:604:db7:a50a"; };
                          { "vla1-1862.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:88:0:604:db7:a9ec"; };
                          { "vla1-1869.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:93:0:604:db7:a760"; };
                          { "vla1-1889.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:88:0:604:db7:a9fc"; };
                          { "vla1-1902.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:78:0:604:db7:a76c"; };
                          { "vla1-1920.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:1b:0:604:db7:98fa"; };
                          { "vla1-1937.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:13:0:604:db7:98de"; };
                          { "vla1-2006.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:13:0:604:db7:9ce4"; };
                          { "vla1-2011.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:43:0:604:db7:a53a"; };
                          { "vla1-2035.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:57:0:604:db7:a616"; };
                          { "vla1-2039.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:91:0:604:db7:ab38"; };
                          { "vla1-2097.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:53:0:604:db7:9d55"; };
                          { "vla1-2131.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:17:0:604:db7:9adb"; };
                          { "vla1-2141.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:17:0:604:db7:9928"; };
                          { "vla1-2153.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:82:0:604:db7:a7d0"; };
                          { "vla1-2209.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:44:0:604:db7:a6b3"; };
                          { "vla1-2338.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:7d:0:604:db7:a3d4"; };
                          { "vla1-2341.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:87:0:604:db7:a824"; };
                          { "vla1-2363.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:86:0:604:db7:a9bd"; };
                          { "vla1-2368.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:7c:0:604:db7:a248"; };
                          { "vla1-2392.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:79:0:604:db7:a3ad"; };
                          { "vla1-2440.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:95:0:604:db7:a9cc"; };
                          { "vla1-2759.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:32:0:604:db7:9bfa"; };
                          { "vla1-2901.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:32:0:604:db7:990f"; };
                          { "vla1-3477.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:a0:0:604:db7:a205"; };
                          { "vla1-4120.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:9b:0:604:db7:aa5b"; };
                          { "vla1-4143.search.yandex.net"; 10240; 232.000; "2a02:6b8:c0e:8a:0:604:db7:abe1"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- vlaruvideo_reportrenderer
                  saslowloadweb_reportrenderer = {
                    priority = 6;
                    match_fsm = {
                      host = "saslowloadweb\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "sas1-0073.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:147:225:90ff:fe82:ff5c"; };
                          { "sas1-0389.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:15f:225:90ff:fe83:210"; };
                          { "sas1-1166.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:65e:225:90ff:fe94:2ed6"; };
                          { "sas1-2083.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:67b:96de:80ff:fe8c:e77e"; };
                          { "sas1-2124.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:67d:96de:80ff:fe8e:7c4e"; };
                          { "sas1-2257.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:624:922b:34ff:fecf:4014"; };
                          { "sas1-2377.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:627:76d4:35ff:fe64:4ecd"; };
                          { "sas1-2980.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:108:225:90ff:fe83:1f18"; };
                          { "sas1-3116.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:10e:225:90ff:fe88:37a8"; };
                          { "sas1-3167.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:109:225:90ff:fe83:2e24"; };
                          { "sas1-3313.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:634:225:90ff:fe88:b108"; };
                          { "sas1-3823.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:634:225:90ff:fe88:b1d2"; };
                          { "sas1-4038.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:634:225:90ff:fe83:2d32"; };
                          { "sas1-4167.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:63a:96de:80ff:fe81:1762"; };
                          { "sas1-4488.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:65b:96de:80ff:fe8c:e71e"; };
                          { "sas1-4705.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:639:96de:80ff:fe81:138a"; };
                          { "sas1-4926.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:65c:96de:80ff:fe81:7f8"; };
                          { "sas1-4946.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:63b:96de:80ff:fe81:10cc"; };
                          { "sas1-5990.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:15e:225:90ff:fe88:b14e"; };
                          { "sas1-6230.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:11b:215:b2ff:fea7:75f9"; };
                          { "sas1-6759.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:11e:215:b2ff:fea7:7fe0"; };
                          { "sas1-8382.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:130:215:b2ff:fea7:b574"; };
                          { "sas2-0464.search.yandex.net"; 21430; 215.000; "2a02:6b8:c02:415:0:604:df5:d6f2"; };
                          { "slovo004.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:66e:225:90ff:fe6c:1e0"; };
                          { "slovo020.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:677:225:90ff:fe92:45c2"; };
                          { "slovo040.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:677:225:90ff:fe94:2c9e"; };
                          { "slovo061.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:64e:922b:34ff:fecf:36f0"; };
                          { "slovo062.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:64e:922b:34ff:fecf:257a"; };
                          { "slovo064.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:64e:922b:34ff:fecf:22e2"; };
                          { "slovo068.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:64e:922b:34ff:fecf:2bf8"; };
                          { "slovo095.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:64d:922b:34ff:fecf:26e4"; };
                          { "slovo104.search.yandex.net"; 21430; 215.000; "2a02:6b8:b000:64d:922b:34ff:fecf:23d6"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- saslowloadweb_reportrenderer
                  manlowloadweb_reportrenderer = {
                    priority = 5;
                    match_fsm = {
                      host = "manlowloadweb\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "man1-0007.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6033:92e2:baff:fe6e:b9e4"; };
                          { "man1-0159.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6035:92e2:baff:fe74:7cf2"; };
                          { "man1-0310.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6031:92e2:baff:fe75:47a2"; };
                          { "man1-0325.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:602b:92e2:baff:fe6e:b9a0"; };
                          { "man1-0326.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:602b:92e2:baff:fe74:76c4"; };
                          { "man1-0391.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:602f:92e2:baff:fe74:7d88"; };
                          { "man1-0473.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:602b:92e2:baff:fe6e:b67c"; };
                          { "man1-0730.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6034:92e2:baff:fe6e:bdf0"; };
                          { "man1-1887.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6029:f652:14ff:fe8c:1cf0"; };
                          { "man1-2018.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:601b:f652:14ff:fe8c:2350"; };
                          { "man1-2153.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:601e:f652:14ff:fe55:4300"; };
                          { "man1-2195.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:601f:f652:14ff:fe8b:c870"; };
                          { "man1-2386.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:601b:f652:14ff:fe55:3160"; };
                          { "man1-2849.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:e820"; };
                          { "man1-2893.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6001:f652:14ff:fe8b:efe0"; };
                          { "man1-2919.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:ec60"; };
                          { "man1-2944.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:f7b0"; };
                          { "man1-2978.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6039:f652:14ff:fe8b:dbd0"; };
                          { "man1-3201.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6000:f652:14ff:fe55:1dd0"; };
                          { "man1-3681.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6002:92e2:baff:fe74:7f5a"; };
                          { "man1-3923.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6074:92e2:baff:fea1:78d2"; };
                          { "man1-4354.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6041:92e2:baff:fe74:761e"; };
                          { "man1-4433.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:603f:92e2:baff:fe6f:8072"; };
                          { "man1-4600.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:601d:f652:14ff:fe8b:df60"; };
                          { "man1-5063.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6051:e61d:2dff:fe00:80a0"; };
                          { "man1-6011.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6054:e61d:2dff:fe00:8220"; };
                          { "man1-6066.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6056:e61d:2dff:fe00:9570"; };
                          { "man1-6118.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6056:e61d:2dff:fe03:3e10"; };
                          { "man1-6176.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6062:e61d:2dff:fe00:a160"; };
                          { "man1-6622.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:604d:e61d:2dff:fe03:36d0"; };
                          { "man1-6833.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6062:92e2:baff:fe55:f3e0"; };
                          { "man1-7025.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:4c50"; };
                          { "man1-7028.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:605f:e61d:2dff:fe04:31d0"; };
                          { "man1-7069.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:605e:e61d:2dff:fe6d:3860"; };
                          { "man1-7325.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6067:e61d:2dff:fe6c:e5d0"; };
                          { "man1-7793.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:606e:e61d:2dff:fe6e:dd0"; };
                          { "man1-8386.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6080:e61d:2dff:fe6d:4270"; };
                          { "man1-8388.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6080:e61d:2dff:fe6d:a690"; };
                          { "man1-8480.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6080:e61d:2dff:fe6d:cbe0"; };
                          { "man1-8484.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6081:e61d:2dff:fe6e:f00"; };
                          { "man1-8980.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:651e:e61d:2dff:fe6e:3bc0"; };
                          { "man1-9348.search.yandex.net"; 29227; 160.000; "2a02:6b8:b000:6085:92e2:baff:fea2:361a"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- manlowloadweb_reportrenderer
                  vlalowloadweb_reportrenderer = {
                    priority = 4;
                    match_fsm = {
                      host = "vlalowloadweb\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "vla1-0068.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:4f:0:604:5cf5:b1c0"; };
                          { "vla1-0116.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:1:0:604:db6:1a1a"; };
                          { "vla1-0341.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:93:0:604:db7:a946"; };
                          { "vla1-0367.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:9d:0:604:d8f:eb9e"; };
                          { "vla1-0433.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:27:0:604:db7:9fbf"; };
                          { "vla1-0497.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:17:0:604:db7:9acb"; };
                          { "vla1-0556.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:1b:0:604:db7:9a06"; };
                          { "vla1-0595.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:54:0:604:db7:a671"; };
                          { "vla1-0603.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:20:0:604:db7:9eeb"; };
                          { "vla1-1467.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:57:0:604:db7:a5ec"; };
                          { "vla1-2371.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:7f:0:604:db7:9e41"; };
                          { "vla1-2654.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:31:0:604:db7:99d0"; };
                          { "vla1-2897.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:2e:0:604:db7:9a81"; };
                          { "vla1-3020.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:6c:0:604:db7:a636"; };
                          { "vla1-3120.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:11:0:604:db7:98da"; };
                          { "vla1-3192.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:15:0:604:db7:9aa6"; };
                          { "vla1-3343.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:3d:0:604:db7:9ac1"; };
                          { "vla1-3350.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:a2:0:604:db7:9f0a"; };
                          { "vla1-3490.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:39:0:604:db7:a58f"; };
                          { "vla1-3924.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:72:0:604:db7:a5c7"; };
                          { "vla1-4564.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:77:0:604:db7:aa2e"; };
                          { "vla1-4565.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:77:0:604:db7:a938"; };
                          { "vla1-4689.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:4e:0:604:db7:a446"; };
                          { "vla1-4693.search.yandex.net"; 13240; 280.000; "2a02:6b8:c0e:43:0:604:db7:a46d"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- vlalowloadweb_reportrenderer
                  hamsterimgs_reportrenderer = {
                    priority = 3;
                    match_fsm = {
                      host = "hamsterimgs\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "vla1-0383.search.yandex.net"; 15485; 160.000; "2a02:6b8:c0e:9d:0:604:d8f:eb87"; };
                          { "vla1-1189.search.yandex.net"; 15485; 160.000; "2a02:6b8:c0e:82:0:604:db7:a8bc"; };
                          { "vla1-1658.search.yandex.net"; 15485; 160.000; "2a02:6b8:c0e:43:0:604:db7:9d76"; };
                          { "vla1-3185.search.yandex.net"; 15485; 160.000; "2a02:6b8:c0e:70:0:604:db7:a231"; };
                          { "vla1-3320.search.yandex.net"; 15485; 160.000; "2a02:6b8:c0e:19:0:604:db7:9a3a"; };
                          { "vla1-3866.search.yandex.net"; 15485; 160.000; "2a02:6b8:c0e:a0:0:604:db7:a4fe"; };
                          { "vla1-3932.search.yandex.net"; 15485; 160.000; "2a02:6b8:c0e:63:0:604:db7:a2e2"; };
                          { "vla1-4278.search.yandex.net"; 15485; 160.000; "2a02:6b8:c0e:65:0:604:db7:a713"; };
                          { "vla1-4383.search.yandex.net"; 15485; 160.000; "2a02:6b8:c0e:65:0:604:db7:a71e"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- hamsterimgs_reportrenderer
                  hamstervideo_reportrenderer = {
                    priority = 2;
                    match_fsm = {
                      host = "hamstervideo\\.reportrenderer\\.yandex\\.net";
                      case_insensitive = true;
                      surround = false;
                    }; -- match_fsm
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
                          { "vla1-1261.search.yandex.net"; 15445; 160.000; "2a02:6b8:c0e:45:0:604:db7:a56c"; };
                          { "vla1-1658.search.yandex.net"; 15445; 160.000; "2a02:6b8:c0e:43:0:604:db7:9d76"; };
                          { "vla1-3185.search.yandex.net"; 15445; 160.000; "2a02:6b8:c0e:70:0:604:db7:a231"; };
                          { "vla1-3866.search.yandex.net"; 15445; 160.000; "2a02:6b8:c0e:a0:0:604:db7:a4fe"; };
                          { "vla1-3932.search.yandex.net"; 15445; 160.000; "2a02:6b8:c0e:63:0:604:db7:a2e2"; };
                          { "vla1-4278.search.yandex.net"; 15445; 160.000; "2a02:6b8:c0e:65:0:604:db7:a713"; };
                        }, {
                          resolve_timeout = "10ms";
                          connect_timeout = "40ms";
                          backend_timeout = "1500ms";
                          fail_on_5xx = true;
                          http_backend = true;
                          buffering = false;
                          keepalive_count = 0;
                          need_resolve = true;
                        }))
                      }; -- weighted2
                    }; -- balancer2
                  }; -- hamstervideo_reportrenderer
                  default = {
                    priority = 1;
                    errordocument = {
                      status = 404;
                      content = "Unknown host";
                      force_conn_close = false;
                    }; -- errordocument
                  }; -- default
                }; -- regexp
              }; -- report
            }; -- accesslog
          }; -- http
        }; -- errorlog
      }; -- shared
    }; -- http_section_80
    http_section_15350 = {
      ips = {
        get_ip_by_iproute("v4");
        get_ip_by_iproute("v6");
      }; -- ips
      ports = {
        15350;
      }; -- ports
      shared = {
        uuid = "5365632592858049780";
      }; -- shared
    }; -- http_section_15350
  }; -- ipdispatch
}