#!/bin/bash
lua5.3 script/gen_runtime_cfg.lua ./lib docker_development
exec "$@"
