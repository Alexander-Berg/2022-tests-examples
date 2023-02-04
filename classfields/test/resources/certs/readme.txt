# pem, pkcs12 pass 12345

# openssl genrsa -des3 -out private_key.pem 2048
# openssl req -new -key private_key.pem -out certificate.csr
# openssl x509 -req -days 1825 -in certificate.csr -signkey private_key.pem -out certificate.pem
# openssl pkcs12 -export -inkey private_key.pem -in certificate.pem -out certificate.pkcs12

# jks pass 123456

#keytool -import -v -trustcacerts -alias endeca-ca -file certificate.pem -keystore certificate.jks
