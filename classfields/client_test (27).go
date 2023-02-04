package realtysdk

import (
	"context"
	"crypto/tls"
	"net"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
)

const (
	testBaseURL     = "https://api.realty.test.vertis.yandex.net/1.0"
	testUserAgent   = "test/1.0"
	testVertisToken = "secret"
)

func TestGetOfferWithSiteSearchCount(t *testing.T) {
	const okResponse = `
		{
		  "response": {
			"total": 100500,
			"timeStamp": "2020-04-10T06:05:08.900Z"
		  }
		}
	`

	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, testUserAgent, r.Header.Get("User-Agent"))
		assert.Equal(t, "Vertis "+testVertisToken, r.Header.Get("X-Authorization"))
		_, _ = w.Write([]byte(okResponse))
	})
	testHttpClient, teardown := NewTestHTTPClient(handler)
	defer teardown()

	client := NewClient(testBaseURL, testUserAgent, testVertisToken, testHttpClient)
	ctx := context.Background()

	searchOptions := &OfferWithSiteSearchOptions{Type: TypeRent, Category: CategoryApartment}
	searchResult, err := client.GetOfferWithSiteSearchCount(ctx, searchOptions)

	assert.Nil(t, err)
	assert.Equal(t, uint64(100500), searchResult.Response.Total)
}

func NewTestHTTPClient(handler http.Handler) (*http.Client, func()) {
	testServer := httptest.NewTLSServer(handler)
	testClient := &http.Client{
		Transport: &http.Transport{
			DialContext: func(_ context.Context, network, _ string) (net.Conn, error) {
				return net.Dial(network, testServer.Listener.Addr().String())
			},
			TLSClientConfig: &tls.Config{
				InsecureSkipVerify: true,
			},
		},
	}
	return testClient, testServer.Close
}
