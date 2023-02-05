#! /bin/bash
set -e

/usr/local/bin/template_generator prepare --apps-root=/ --dst-root=/
/usr/local/bin/template_generator generate --dst=/ --cfgdir=/etc/template_generator --overwrite-is-failure

run() {
    local scripts_dir=$1
    for f in `ls -v "$scripts_dir"/*.sh 2> /dev/null || :`; do
        echo "Run $f"
        source $f
        echo "Done $f"
    done
}

run /etc/runner/postbuild_tests.d
