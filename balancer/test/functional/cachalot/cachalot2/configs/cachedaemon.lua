instance = {
--    monitoring = {
--        answer_time_buckets = "1.0, 0.5, 0.25, 0.1, 0.05, 0.025, 0.01, 0.005, 0.001";
--    };
--    access_log = {
--        file_name = {(access_log_file and access_log_file or './cached-access-log') .. '-' .. server_port};
--        max_log_queue_size = 0;
--    };
    server = {
        port = port;
        threads = {n_cpu - 4};
        max_queue_size = {1000 * n_cpu};
    };
    modules = {
        yastatic = {
            _cool_format = true;
            key_builder = {
                use_sub_path = 1;
            };
            collection = {
                id = '*';
                allow_sub_path = 1;
            };
            storage = {
                dir = cache_dir;
                arenas = {n_cpu};
                memory_limit = {'2M'};
                file_cache_size = {'10M'};
                block_size = '1K';
                background_storing = {
                    threads = 4;
                    max_queue_size = 0;
                    fail_on_busy = false;
                };
                --monitoring = {
                --    allowed = 1;
                --    signal_prefix = 'yastat'
                --};
            };
        };
    };
};
