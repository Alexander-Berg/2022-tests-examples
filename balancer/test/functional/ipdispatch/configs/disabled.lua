bad_ip = "good times bad times"; -- not the best ip address, I swear!

instance = {
    addrs = {
        { ip = "localhost"; port = port; };
        { ip = bad_ip; port = port; disabled = disabled; };
    }; -- addrs
    admin_addrs = {
        { ip = "localhost"; port = admin_port; };
    }; -- admin_addrs

    thread_mode = thread_mode; set_no_file = false;

    ipdispatch = {
        admin = {
            ip = "localhost";
            port = admin_port;
            http = {
                maxlen = 65536; maxreq = 65536;
                admin = {};
            }; -- http
        }; -- admin
        main = {
            ip = "localhost";
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                errordocument = {
                    status = 200;
                    content = "main";
                }; -- errordocument
            }; -- http
        }; -- localhost1
        bad = {
            ip = bad_ip;
            port = port;
            http = {
                maxlen = 65536; maxreq = 65536;
                errordocument = {
                    status = 200;
                    content = "In the days of my youth";
                }; -- errordocument
            }; -- http
        }; -- localhost2
    }; -- ipdispatch
}; -- instance
