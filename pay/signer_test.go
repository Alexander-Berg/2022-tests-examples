package visa

import (
	"bytes"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSignerSignAlgorithm(t *testing.T) {
	url, err := url.Parse("http://domain.test/pa/th?key=value&k=v")
	require.NoError(t, err)

	actual := MakeAuthHeaderForTimestamp(int64(1337228322), url, []byte("{\"pay\": \"load\"}"), "secretsecretsecret")

	expected := "xv2:1337228322:626bb6a90e22cc29619c96b1522b5c53beab6d0b36960079a087e02eee8d746e"
	require.Equal(t, expected, actual)
}

func TestParamsOrderNotMatter(t *testing.T) {
	url1, err := url.Parse("http://domain.test/pa/th?a=1&b=2")
	require.NoError(t, err)
	url2, err := url.Parse("http://domain.test/pa/th?b=2&a=1")
	require.NoError(t, err)

	payload := []byte("{\"pay\": \"load\"}")
	secret := "secretsecretsecret"
	ts := int64(1337228322)
	sig1 := MakeAuthHeaderForTimestamp(ts, url1, payload, secret)
	sig2 := MakeAuthHeaderForTimestamp(ts, url2, payload, secret)

	require.Equal(t, sig1, sig2)
}

func TestGetRequestBody(t *testing.T) {
	body := []byte{104, 101, 108, 108, 111}
	req, err := http.NewRequest("GET", "http://domain.test", bytes.NewBuffer(body))
	require.NoError(t, err)

	actualBody, err := getRequestBody(req)
	require.NoError(t, err)

	require.Equal(t, body, actualBody)
}

func TestNewSignerErrors(t *testing.T) {
	tests := []struct {
		name          string
		key           string
		secret        string
		expectedError error
	}{
		{
			name:          "no inbound key",
			key:           "",
			secret:        "secret",
			expectedError: errorInvalidAPIKey,
		},
		{
			name:          "no inbound secret",
			key:           "Key",
			secret:        "",
			expectedError: errorInvalidSecret,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			opts := []SignerOption{WithSignerAPIKey(test.key), WithSignerSecret(test.secret)}
			assert.PanicsWithValue(t, test.expectedError, func() { NewSigner(opts...) })
		})
	}
}

func TestSignerSignKey(t *testing.T) {
	req, err := http.NewRequest("GET", "http://domain.test", new(bytes.Buffer))
	require.NoError(t, err)
	signer := NewSigner(WithSignerAPIKey("inbound-key"), WithSignerSecret("secretsecretsecret"))

	err = signer.Sign(req)
	require.NoError(t, err)

	apiKey := req.URL.Query().Get(AuthorizationQueryParamName)
	require.Equal(t, "inbound-key", apiKey)
}

func TestSignerSignAuthorization(t *testing.T) {
	body := []byte{104, 101, 108, 108, 111}
	req, err := http.NewRequest("GET", "http://domain.test", bytes.NewBuffer(body))
	require.NoError(t, err)
	signer := NewSigner(WithSignerAPIKey("inbound-key"), WithSignerSecret("secretsecretsecret"))

	err = signer.Sign(req)
	require.NoError(t, err)
	authorization := req.Header.Get(AuthorizationHeaderName)
	authorizationSplit := strings.Split(authorization, ":")
	require.Len(t, authorizationSplit, 3)
	timestamp, err := strconv.ParseInt(authorizationSplit[1], 10, 64)
	require.NoError(t, err)

	expectedAuthorization := MakeAuthHeaderForTimestamp(timestamp, req.URL, body, signer.secret)
	require.Equal(t, expectedAuthorization, authorization)
}
