package paymenttoken

import (
	"crypto/ecdsa"
	"encoding/json"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

/*
Это небольшой гайд, как всем пользоватья. Читать сверху вниз.
Для начала немного переменных, часть не нужна, нужно порефакторит
	export dir= ???
	export cadir= ???
	export format=pem

	mkdir $dir
	cd $dir
	mkdir certs crl csr newcerts private public
	chmod 700 private
	touch index.txt
	touch serial
	sn=8

	countryName="/C=RU"
	stateOrProvinceName=""
	localityName="/L=Moscow"
	organizationName="/O=Yandex LLC"
	organizationalUnitName="/OU=ITO"
	commonName="/CN=Yandex Pay Root CA"
	DN=$countryName$stateOrProvinceName$localityName
	DN=$DN$organizationName$organizationalUnitName$commonName
*/

/*
Создадим приватный ключ
	openssl genpkey -aes256 -algorithm ec\
	-pkeyopt ec_paramgen_curve:prime256v1\
	-outform $format -pkeyopt ec_param_enc:named_curve\
	-out $dir/private/ca.key.$format

и переведем его в удобный формат
	openssl pkcs8 -topk8 -nocrypt -outform der -in $dir/private/ca.key.$format -out $dir/private/ca.private.der
	cat $dir/private/ca.private.der | base64 -w 0
		-> MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg0TVmzjylfIx0RxjzNYMk97FWPfaRTw6buY5aVMZs612hRANCAATvQjv3go0dY40r2dOUWoE7t2igW/GrIVOi26GvzcKzGkivJmCU8yjCJTQkukGXTlWCATLvLAHinkuOCDsie52v

Распарсим и сохраним приватный ключ
*/
/*
Создадим приватный ключ
	openssl genpkey -aes256 -algorithm ec\
		 -pkeyopt ec_paramgen_curve:prime256v1\
		 -outform $format -pkeyopt ec_param_enc:named_curve\
		 -out $dir/private/ca.key.$format
И сертификат
	openssl req -set_serial 0x$(openssl rand -hex $sn)\
		 -keyform $format -outform $format     -key $dir/private/ca.key.$format -subj "$DN"     -new -x509 -days 7300 -sha256 -extensions v3_ca     -out $dir/certs/ca.cert.$format

Коввертируем приватный ключ
	openssl pkcs8 -topk8 -nocrypt -outform der -in $dir/private/ca.key.$format -out $dir/private/ca.private.der
	cat private/ca.private.der | base64 -w 0
		-> MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgkvWkzF3EoZbYSFEaQ5WhAny4sBBEbKIAVTKCKqrez6ihRANCAAQqmSdBfHrb/oTf0jz/+M+R0AAFYvK2Yrq9IDLkj6oTPKec8/MkTddauroscIZhn9hjVslCkCzhmVVfEaqiC2s/vkokarev@vkokarev-vm:~/keys$
*/
var sampleSigningCAPrivateKey = mustParsePKCS8PrivateKey("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgkvWkzF3EoZbYSFEaQ5WhAny4sBBEbKIAVTKCKqrez6ihRANCAAQqmSdBfHrb/oTf0jz/+M+R0AAFYvK2Yrq9IDLkj6oTPKec8/MkTddauroscIZhn9hjVslCkCzhmVVfEaqiC2s/")

/*
Создадим рубличный ключ и загрузим его
 	openssl x509 -pubkey -noout -in certs/ca.cert.pem > public/ca.pubkey.pem
	openssl asn1parse -in public/ca.pubkey.pem -out /public/ca.pubkey.der
	cat public/ca.pubkey.der | base64 -w 0
		-> MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEKpknQXx62/6E39I8//jPkdAABWLytmK6vSAy5I+qEzynnPPzJE3XWrq6LHCGYZ/YY1bJQpAs4ZlVXxGqogtrPw==
*/
var sampleSigningCAPublicKey = mustParsePKIXPublicKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEKpknQXx62/6E39I8//jPkdAABWLytmK6vSAy5I+qEzynnPPzJE3XWrq6LHCGYZ/YY1bJQpAs4ZlVXxGqogtrPw==")

// Теперь проверим следующее: мы можем подписать приватным ключом некоторую строку (промежуточный ключ в последствии) и подтвердить подпись
func TestCanCreateValidIntermediateSigningKeyWithOwnKeys(t *testing.T) {
	// валидный ключ, который подпишем, конкретное значение пока не важно
	const someKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEZnemx+Dtw9XYvQ7jigEIhnYq1kwhjgBhhrlumALhnA9nD77sUHwYMvCoBuSjGUmaIj6B5m/2W0zMg1kUTbJ8lg=="
	expireTime := time.Now().Add(720 * time.Hour)
	certFactory, err := NewIntermediateSigningCertFactory(
		[]*ecdsa.PrivateKey{sampleSigningCAPrivateKey},
		ProtocolVersionECv2,
		senderID,
		someKey,
		expireTime)
	require.NoError(t, err)

	cert, err := certFactory.CreateIntermediateSigningCert()
	require.NoError(t, err)

	verifyingKeys := &SenderVerifyingKeySet{
		Keys: []*SenderVerifyingKey{
			{
				ProtocolVersion: ecV2.protocolVersion,
				PublicKey:       sampleSigningCAPublicKey,
				ExpirationTime:  time.Now().Add(24 * time.Hour),
			},
		},
	}

	// тут будут проверяться подписи
	_, err = cert.validateAndGetSigningKey(senderID, ecV2.protocolVersion, verifyingKeys)
	require.NoError(t, err)

	// в рантайме приватного ключа не будет. Имеет смысл сделать так:

	serialized, _ := json.Marshal(cert)
	// fmt.Println(string(serialized))
	/*
		{
			//signedKey - строка!
			"signedKey":"{
				\"keyValue\":\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEZnemx+Dtw9XYvQ7jigEIhnYq1kwhjgBhhrlumALhnA9nD77sUHwYMvCoBuSjGUmaIj6B5m/2W0zMg1kUTbJ8lg==\",
				\"keyExpiration\":\"1608921887227\"
			}",
			"signatures":["MEUCIG9Trbr/5v2ggwXQRDDkO7I/peLdrHGpQVcL5hRnKaaYAiEA8lBxvvx5jRDN2HpKSRYEuzEE8MB3Xv1E5stj30v3MRA="]}
	*/
	// serialized стосит сохранить в условный intermediate.json. Его можно сформировать и руками, но так - удобнее
	cert = new(IntermediateSigningCert)
	err = json.Unmarshal(serialized, &cert)
	require.NoError(t, err)
	_, err = cert.validateAndGetSigningKey(senderID, ecV2.protocolVersion, verifyingKeys)
	require.NoError(t, err)

}

/*
Теперь создадим промежуточный ключ
	countryName="/C=RU"
	stateOrProvinceName=""
	localityName="/L=Moscow"
	organizationName="/O=Yandex LLC"
	organizationalUnitName="/OU=ITO"
	commonName="/CN=Yandex Pay Intermediate CA"
	DN=$countryName$stateOrProvinceName$localityName
	DN=$DN$organizationName$organizationalUnitName$commonName

	openssl genpkey -aes256 -algorithm ec\
		-pkeyopt ec_paramgen_curve:prime256v1 \
		-outform $format -pkeyopt ec_param_enc:named_curve\
		-out $dir/private/intermediate.key.$format

	openssl req -set_serial 0x$(openssl rand -hex $sn)\
		 -keyform $format -outform $format     -key $dir/private/intermediate.key.$format -subj "$DN"     -new -x509 -days 1825 -sha256 -extensions v3_ca     -out $dir/certs/intermediate.cert.$format

Конвертируем приватный промежуточный ключ
	openssl pkcs8 -topk8 -nocrypt -outform der -in $dir/private/intermediate.key.$format -out $dir/certs/intermediate.private.der
	cat $dir/certs/intermediate.private.der | base64 -w 0
		-> MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgGFclXr8UUifAvAEYuvnYX6d0+UXBW0rFDjUVLXwQtg2hRANCAASv1CU14Q3gLW0SnTQeVhRSmnR21Ifkas32FAroiP1aiUSHtPXCxQ0ZmMcQuBkOIuoGOG4qG1bDavFygNP6Yxg3
*/
var intermediateSigningPrivateKey = mustParsePKCS8PrivateKey("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgGFclXr8UUifAvAEYuvnYX6d0+UXBW0rFDjUVLXwQtg2hRANCAASv1CU14Q3gLW0SnTQeVhRSmnR21Ifkas32FAroiP1aiUSHtPXCxQ0ZmMcQuBkOIuoGOG4qG1bDavFygNP6Yxg3")

/*
Теперь сгенерируем публичный промежуточный ключ
	openssl x509 -pubkey -noout -in certs/intermediate.cert.pem > public/intermediate.pubkey.pem
	openssl asn1parse -in public/intermediate.pubkey.pem -out public/intermediate.pubkey.der

	cat public/intermediate.pubkey.der | base64 -w 0
		-> MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEr9QlNeEN4C1tEp00HlYUUpp0dtSH5GrN9hQK6Ij9WolEh7T1wsUNGZjHELgZDiLqBjhuKhtWw2rxcoDT+mMYNw==
	Парсить его не нужно, т.к. мы его положим в IntermediateSigningCert и подпишем
*/
var intermediateSigningPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEr9QlNeEN4C1tEp00HlYUUpp0dtSH5GrN9hQK6Ij9WolEh7T1wsUNGZjHELgZDiLqBjhuKhtWw2rxcoDT+mMYNw=="

/*
	У нас должен быть сформировван root.json. Если поддерживаем только один протокол, то формат такой:
	{
	  "keyValue": "...", -- рутовый публичный ключ
	  "keyExpiration": "...", -- срок годности
	  "protocolVersion": "ECv2"
	}
	keyValue - см sampleSigningCAPublicKey
*/
var rootJSON = fmt.Sprintf(`
{
  "keys": [
	{
	  "keyValue": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEKpknQXx62/6E39I8//jPkdAABWLytmK6vSAy5I+qEzynnPPzJE3XWrq6LHCGYZ/YY1bJQpAs4ZlVXxGqogtrPw==",
	  "keyExpiration": "%s",
	  "protocolVersion": "ECv2"
	}
  ]
}`, tomorrow)

// Не хватает публичного и приватного ключа получателя для шифрования, возьмем происзвольнве из соседнего теста

func TestCanSealAndUnsealUsingGeneratedKeys(t *testing.T) {
	// msg := "this is my message text"
	msg := []byte("this")
	expireTime := time.Now().Add(720 * time.Hour)
	certFactory, err := NewIntermediateSigningCertFactory(
		[]*ecdsa.PrivateKey{sampleSigningCAPrivateKey},
		ProtocolVersionECv2,
		senderID,
		intermediateSigningPublicKey, // <- сюда кладем строку с промежуточным публичным ключом
		expireTime)
	require.NoError(t, err)

	cert, err := certFactory.CreateIntermediateSigningCert()
	require.NoError(t, err)

	sender, err := NewSender(
		intermediateSigningPrivateKey, // <- промежуточный приватный ключ. Им будем подписыват зашифрованное сообщение
		cert,                          // <- пакет с публичой частью промежуточного ключа. Подписан рутовым ключом. По хорошему, надо брать из intermediate.json
		WithSenderID(senderID),        // <- не обязательно
	)
	require.NoError(t, err)

	token, err := sender.Seal(recipientID, msg, merchantPublicKeyBase64)
	require.NoError(t, err)

	// теперь создадим получателя

	// Тут мы парсим root.json и делаем из него объект, который отдает публичные ключи для разных версий
	verifyingKeys, err := ParseSenderVerifyingKeys(rootJSON)
	require.NoError(t, err)

	client := NewRecipient(
		recipientID,
		verifyingKeys,
		WithFromSenderID(senderID),
		WithPrivateKey(merchantPrivateKeyPkcs8Base64),
	)
	require.NoError(t, err)

	r, err := client.Unseal(token)
	require.NoError(t, err)
	// fmt.Println(string(r))
	require.Equal(t, msg, r, "msg must be decoded")
}
