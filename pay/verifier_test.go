package visa

import (
	"bytes"
	"net/http"
	"net/url"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestCantOverrideSecret(t *testing.T) {
	defer func() {
		if r := recover(); r == nil {
			t.Errorf("Must catch panic!")
		}
	}()

	apiKey := "key"
	NewVerifier(
		WithNamedSecret(apiKey, "s1"),
		WithNamedSecret(apiKey, "s2"),
	)
}

func TestVerifierCanCheckValidSignature(t *testing.T) {
	apiKey := "inbound-key"
	secret := "secretsecretsecret"

	req, data, signature := signedRequest(t, apiKey, secret)
	require.NotEmpty(t, signature)

	verifier := NewVerifier(WithNamedSecret(apiKey, secret))
	err := verifier.VerifyRequest(req.URL, data, signature)
	require.NoError(t, err, "signature is valid")
}

func TestVerifierCanWorkWithMultipleKeys(t *testing.T) {
	apiKey1 := "inbound-key1"
	secret1 := "secret_4_1"

	apiKey2 := "inbound-key2"
	secret2 := "secret_4_2"

	req1, data1, signature1 := signedRequest(t, apiKey1, secret1)
	req2, data2, signature2 := signedRequest(t, apiKey2, secret2)

	verifier := NewVerifier(
		WithNamedSecret(apiKey1, secret1),
		WithNamedSecret(apiKey2, secret2),
	)

	err := verifier.VerifyRequest(req1.URL, data1, signature1)
	require.NoError(t, err, "signature is valid")

	err = verifier.VerifyRequest(req2.URL, data2, signature2)
	require.NoError(t, err, "signature is valid")

	err = verifier.VerifyRequest(req2.URL, data1, signature1)
	require.Error(t, err)
	require.Equal(t, err.Error(), "visa: wrong signature")

}

func TestVerifierErrors(t *testing.T) {
	apiKey := "inbound-key"
	secret := "secretsecretsecret"

	urlString := "http://domain.test"
	data := []byte("hello world")

	u, err := url.Parse(urlString)
	require.NoError(t, err)

	req, err := http.NewRequest("POST", u.String(), bytes.NewReader(data))
	require.NoError(t, err)

	verifier := NewVerifier(WithNamedSecret(apiKey, secret))
	signer := NewSigner(WithSignerAPIKey(apiKey), WithSignerSecret(secret))

	err = signer.Sign(req)
	require.NoError(t, err)

	validSignature := req.Header.Get("x-pay-token")

	tests := []struct {
		name      string
		url       string
		data      []byte
		isError   bool
		signature string
		message   string
	}{
		{
			name:      "simple",
			url:       req.URL.String(),
			data:      data,
			isError:   false,
			signature: validSignature,
		},
		{
			name:      "wrong_data",
			url:       req.URL.String(),
			data:      []byte("wrong data"),
			isError:   true,
			message:   "visa: wrong signature",
			signature: validSignature,
		},
		{
			name:      "missingAPIKey",
			url:       "http://domain.test",
			data:      data,
			isError:   true,
			message:   "visa: apiKey is missing",
			signature: validSignature,
		},
		{
			name:      "wrongAPIKey",
			url:       "http://domain.test?apiKey=some_key",
			data:      data,
			isError:   true,
			message:   "visa: missing apiKey some_key",
			signature: validSignature,
		},
		{
			name:      "expiredSignature",
			url:       req.URL.String(),
			data:      data,
			isError:   true,
			message:   "visa: signature expired",
			signature: MakeAuthHeaderForTimestamp(int64(1337228322), req.URL, data, secret),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			u, err := url.Parse(tt.url)
			require.NoError(t, err)

			err = verifier.VerifyRequest(u, tt.data, tt.signature)
			if tt.isError {
				require.Error(t, err)
				require.Equal(t, err.Error(), tt.message)
			} else {
				require.NoError(t, err)
			}
		})
	}
}

func signedRequest(t *testing.T, apiKey, secret string) (req *http.Request, data []byte, signature string) {
	u, err := url.Parse("http://domain.test")
	require.NoError(t, err)

	data = []byte("hello world")
	req, err = http.NewRequest("POST", u.String(), bytes.NewReader(data))
	require.NoError(t, err)

	signer := NewSigner(WithSignerAPIKey(apiKey), WithSignerSecret(secret))

	err = signer.Sign(req)
	require.NoError(t, err)

	signature = req.Header.Get("x-pay-token")
	require.NotEmpty(t, signature)

	return req, data, signature
}
