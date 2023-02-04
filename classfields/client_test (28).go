package tableausdk

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
	testBaseURL   = "http://tableau-win.vertis.yandex.net/api/3.3"
	testUserAgent = "test/1.0"
	testUser      = "secret"
	testPassword  = "secret"
	testSite      = "testSiteName"
	testViewName  = "testViewName"
	testViewID    = "abc123"
)

func TestGetViewsByName(t *testing.T) {
	const okResponse = `
		{
		  "pagination": {
			"pageNumber": "1",
			"pageSize": "100",
			"totalAvailable": "1"
		  },
		  "views": {
			"view": [
			  {
				"tags": {},
				"id": "abc123",
				"name": "testViewName"
			  }
			]
		  }
		}
	`

	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, testUserAgent, r.Header.Get("User-Agent"))
		_, _ = w.Write([]byte(okResponse))
	})
	testHttpClient, teardown := NewTestHTTPClient(handler)
	defer teardown()

	tableauCredentials := Credentials{
		Name:     testUser,
		Password: testPassword,
		Site: Site{
			ContentURL: testSite,
		},
	}

	client := NewClient(testBaseURL, testUserAgent, tableauCredentials, testHttpClient)
	ctx := context.Background()
	views, err := client.GetViewsByName(ctx, testViewName)

	assert.Nil(t, err)
	assert.Equal(t, testViewID, views.View[0].ID)
	assert.Equal(t, testViewName, views.View[0].Name)
}

func NewTestHTTPClient(handler http.Handler) (*http.Client, func()) {
	testServer := httptest.NewServer(handler)
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
