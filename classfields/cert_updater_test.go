package updater

import (
	"github.com/hashicorp/vault/api"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestCertUpdater_GetCert(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(writer http.ResponseWriter, request *http.Request) {
		writer.Write([]byte(testResponse))
	}))
	defer srv.Close()
	vc, err := api.NewClient(&api.Config{
		Address:    srv.URL,
		HttpClient: srv.Client(),
	})
	require.NoError(t, err)
	c := newCertUpdater(vc, logrus.New())
	info, err := c.GetCert(certIssueReq{Service: "test-service"})
	require.NoError(t, err)
	expectedInfo := &certInfo{
		CaChain:        []string{"chainy"},
		Certificate:    "cert",
		Expiration:     1591857171,
		IssuingCA:      "ca",
		PrivateKey:     "key",
		PrivateKeyType: "rsa",
		SerialNumber:   "66:0a:2a:b8:8d:ab:c0:e4:7d:cc:bf:b4:6b:6e:c8:c8:5f:c1:5b:2a",
	}
	assert.Equal(t, expectedInfo, info)
}

var (
	testResponse = `{
  "request_id": "3a71715b-3b8d-388a-f633-7c4dfc1938dd",
  "lease_id": "",
  "lease_duration": 0,
  "renewable": false,
  "data": {
    "ca_chain": [
      "chainy"
    ],
    "certificate": "cert",
    "expiration": 1591857171,
    "issuing_ca": "ca",
    "private_key": "key",
    "private_key_type": "rsa",
    "serial_number": "66:0a:2a:b8:8d:ab:c0:e4:7d:cc:bf:b4:6b:6e:c8:c8:5f:c1:5b:2a"
  },
  "warnings": null
}`
)
