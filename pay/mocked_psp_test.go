package server

import (
	"crypto/ecdsa"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/pay/duckgo/paymenttoken"
)

type testPSP struct {
	t  *testing.T
	te *TestEnv

	Key *ecdsa.PrivateKey
	ID  string

	pubKey          string
	pubKeySignature string
}

func newTestPaymentGateway(te *TestEnv) *testPSP {
	key := generateECP256Key(te.t)
	sig := te.SignRecipientKey(&key.PublicKey)
	return &testPSP{
		t:               te.t,
		te:              te,
		Key:             key,
		ID:              genUUID(),
		pubKey:          convertPublicKeyToDERBase64(te.t, key.Public()),
		pubKeySignature: sig,
	}
}

func (p *testPSP) GetSerializedPublicKey() string {
	return p.pubKey
}

func (p *testPSP) GetKeySignature() string {
	return p.pubKeySignature
}

func (p *testPSP) MustDecryptToken(serializedToken string, token *paymenttoken.EncryptedMessage) {
	recipient := paymenttoken.NewRecipient(p.ID, p.te.GetRootCAKeySet(), paymenttoken.WithPrivateKey(p.Key))
	err := recipient.UnsealJSONMessage(serializedToken, token)
	require.NoError(p.t, err)
}
