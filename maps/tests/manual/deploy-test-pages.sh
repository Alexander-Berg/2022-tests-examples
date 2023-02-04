host="front-jsapi-dev.sas.yp-c.yandex.net"
root_test_dir="$(realpath ./)"

ssh ${host} 'mkdir -p jsapi-v2.1-manual'
scp -r ${root_test_dir}/cases ${host}:~/jsapi-v2.1-manual
scp -r ${root_test_dir}/js ${host}:~/jsapi-v2.1-manual
scp -r ${root_test_dir}/img ${host}:~/jsapi-v2.1-manual
scp -r ${root_test_dir}/helper.js ${host}:~/jsapi-v2.1-manual/helper.js
scp -r ${root_test_dir}/pages-server.js ${host}:~/jsapi-v2.1-manual/pages-server.js

scp -r ${root_test_dir}/run-test-servers.sh ${host}:~/jsapi-v2.1-manual/run-test-servers.sh

ssh ${host} 'sh ./jsapi-v2.1-manual/run-test-servers.sh'
