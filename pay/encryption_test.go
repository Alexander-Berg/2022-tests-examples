package visa_test

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/pay/duckgo/visa"
)

const (
	secret = "some-secret"
	keyID  = "Key-id"
)

type someObject struct {
	F1 int    `json:"f1"`
	F2 string `json:"f2"`
}

func newEncrypter() *visa.JWEEncrypter {
	return &visa.JWEEncrypter{Key: &visa.SharedKey{
		KeyID:  keyID,
		Secret: secret,
	}}
}

func TestCanEncryptAndDecrypt(t *testing.T) {
	jwe := newEncrypter()
	payload := "{\"pay\": \"load\"}"
	r, err := jwe.Encrypt(payload)
	require.NoError(t, err)

	decrypted, err := jwe.Decrypt(r)
	require.NoError(t, err)

	require.Equal(t, payload, string(decrypted))
}

func TestCanEncryptAndDecryptObject(t *testing.T) {
	jwe := newEncrypter()
	payload := someObject{
		F1: 1,
		F2: "2",
	}

	r, err := jwe.MarshalAndEncrypt(payload)
	require.NoError(t, err)

	decrypted := someObject{}
	err = jwe.DecryptAndUnmarshal(r, &decrypted)
	require.NoError(t, err)

	require.Equal(t, payload, decrypted)
}

func TestCanNotDecryptWrongChiper(t *testing.T) {
	jwe := newEncrypter()
	payload := "{\"pay\": \"load\"}"
	r, err := jwe.Encrypt(payload)
	require.NoError(t, err)

	tmp := []rune(r)
	tmp[2] = tmp[3]
	r = string(tmp)

	_, err = jwe.Decrypt(r)
	require.Error(t, err)
}
