package paymenttoken

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestRecipientKeyVerifier_Verify(t *testing.T) {
	k1, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	require.NoError(t, err)

	k2, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	require.NoError(t, err)

	payload := []byte("payload")

	s1 := NewRecipientKeySigner(k1)
	s2 := NewRecipientKeySigner(k2)

	v1 := NewRecipientKeyVerifier([]*ecdsa.PublicKey{&k1.PublicKey})
	v2 := NewRecipientKeyVerifier([]*ecdsa.PublicKey{&k2.PublicKey})
	v12 := NewRecipientKeyVerifier([]*ecdsa.PublicKey{&k1.PublicKey, &k2.PublicKey})

	type Pair struct {
		signer   *RecipientKeySigner
		verifier *RecipientKeyVerifier
	}

	okPairs := []Pair{
		{signer: s1, verifier: v1},
		{signer: s1, verifier: v12},
		{signer: s2, verifier: v2},
		{signer: s2, verifier: v12},
	}

	failPairs := []Pair{
		{signer: s1, verifier: v2},
		{signer: s2, verifier: v1},
	}

	for _, pair := range okPairs {
		t.Run("ok", func(t *testing.T) {
			sig := pair.signer.sign(payload)
			err := pair.verifier.verify(payload, sig)
			assert.NoError(t, err)
		})
	}

	for _, pair := range okPairs {
		t.Run("wrong_payload", func(t *testing.T) {
			sig := pair.signer.sign(payload)
			err := pair.verifier.verify([]byte("wrong"), sig)
			assert.Error(t, err)
		})
	}

	for _, pair := range failPairs {
		t.Run("fail", func(t *testing.T) {
			sig := pair.signer.sign(payload)
			err := pair.verifier.verify(payload, sig)
			assert.Error(t, err)
		})
	}

	for _, sig := range []string{
		"",
		"...",
		"1.1.1",
		"1..1",
	} {
		t.Run("broken_sig", func(t *testing.T) {
			err = v1.verify(payload, sig)
			assert.Error(t, err)
		})
	}
}
