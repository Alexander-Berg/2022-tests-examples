package paymenttoken

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"strings"
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gopkg.in/square/go-jose.v2"
)

func TestRecipientKeySigner_Sign(t *testing.T) {
	signingKey, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	require.NoError(t, err)

	now := time.Date(2021, 03, 31, 0, 0, 0, 0, time.UTC)
	clock := clockwork.NewFakeClockAt(now)
	signer := NewRecipientKeySigner(signingKey, withRecipientKeySignerClock(clock))

	payload := []byte("test")

	signature := signer.sign(payload)
	assert.NotEmpty(t, signature)

	jwsParts := strings.Split(signature, ".")
	require.Equal(t, 3, len(jwsParts))
	assert.Empty(t, jwsParts[1])

	jws, err := jose.ParseDetached(signature, payload)
	require.NoError(t, err)

	_, err = jws.Verify(signingKey.Public())
	require.NoError(t, err)

	assert.Equal(t, 1, len(jws.Signatures))
	sig := jws.Signatures[0]
	assert.Equal(t, "ES256", sig.Protected.Algorithm)
	assert.EqualValues(t, now.Unix(), sig.Protected.ExtraHeaders["iat"])
	assert.Equal(t, signingKey.Public(), sig.Protected.JSONWebKey.Key)
}
