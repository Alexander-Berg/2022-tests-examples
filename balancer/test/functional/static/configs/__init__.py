# -*- coding: utf-8 -*-
from balancer.test.util.config import gen_config_class


gen_config_class('StaticDirConfig', 'static_dir.lua',
                 logs=['accesslog'], args=['static_dir'])
gen_config_class('StaticFileConfig', 'static_file.lua',
                 logs=['accesslog'], args=['static_file'], kwargs={"expires": None, "etag_inode": None})
