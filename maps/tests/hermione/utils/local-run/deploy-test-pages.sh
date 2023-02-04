host="front-jsapi-dev.sas.yp-c.yandex.net"
root_test_dir="$(realpath ./)"

ssh ${host} 'mkdir -p public/hermione'
scp -r ${root_test_dir}/pages ${host}:~/public/hermione/pages
scp -r ${root_test_dir}/img ${host}:~/public/hermione/img
scp -r ${root_test_dir}/helper.js ${host}:~/public/hermione/helper.js
scp -r ${root_test_dir}/servers/pages-server.js ${host}:~/public/pages-server.js
scp -r ${root_test_dir}/servers/lom.js ${host}:~/public/lom-server.js
scp -r ${root_test_dir}/servers/rom.js ${host}:~/public/rom-server.js
scp -r ${root_test_dir}/../../build/release/init.js ${host}:~/public/init.js
scp -r ${root_test_dir}/utils/local-run/run-test-servers.sh ${host}:~/public/run-test-servers.sh

ssh ${host} 'sh ./public/run-test-servers.sh'
