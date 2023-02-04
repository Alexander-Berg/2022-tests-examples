set -e
set -x
#CN='flat.easy-mode.yandex.net'
#SAN='DNS:flat.easy-mode.yandex.net,DNS:*.easy-mode.yandex.net'
CN=test.yandex.ru
SAN='DNS:test.yandex.ru,DNS:тест.яндекс.рф'

./gen_certs.sh $CN $SAN
openssl x509 -in ./certs/$CN.crt -out ../certs/allCAs-$CN.pem -outform PEM
openssl rsa -in ./certs/$CN.key -out ../certs/$CN.pem -outform PEM
