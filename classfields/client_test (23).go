package blackbox

import (
	"io"
	"io/ioutil"
	"net"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"golang.org/x/oauth2"
)

func TestClient_CheckOAuthToken(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		q := r.URL.Query()
		assert.Equal(t, "xyz", q.Get("oauth_token"))
		assert.Equal(t, "::1", q.Get("userip"))
		assert.Equal(t, "oauth", q.Get("method"))
		assert.Equal(t, "json", q.Get("format"))
		responseStr := `{"Error":"OK","login":"test-user","uid":{"value":"42"}}`
		io.Copy(w, strings.NewReader(responseStr))
	}))
	defer server.Close()
	bb := &Client{
		url:        server.URL,
		httpClient: server.Client(),
	}
	i, err := bb.CheckOAuthToken(&oauth2.Token{AccessToken: "xyz"}, net.ParseIP("::1"))
	if !assert.NoError(t, err) {
		return
	}
	assert.Equal(t, "test-user", i.Login)
	assert.Equal(t, uint64(42), i.UID)
}

func TestClient_CheckSessionId(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		require.Equal(t, "POST", r.Method)
		q := r.URL.Query()
		defer r.Body.Close()
		bytes, err := ioutil.ReadAll(r.Body)
		require.NoError(t, err)
		body, err := url.ParseQuery(string(bytes))
		require.NoError(t, err)
		assert.Equal(t, "some-sessionId", body.Get("sessionid"))
		assert.Equal(t, "ssl-session-id", body.Get("sslsessionid"))
		assert.Equal(t, "::1", q.Get("userip"))
		assert.Equal(t, "sessionid", q.Get("method"))
		assert.Equal(t, "yandex-team.ru", q.Get("host"))
		assert.Equal(t, "json", q.Get("format"))
		responseStr := `{"Error":"OK","login":"test-user","uid":{"value":"42"}}`
		io.Copy(w, strings.NewReader(responseStr))
	}))
	defer server.Close()
	bb := &Client{
		url:        server.URL,
		httpClient: server.Client(),
	}
	i, err := bb.CheckSessionId("some-sessionId", "ssl-session-id", "yandex-team.ru", net.ParseIP("::1"))
	if !assert.NoError(t, err) {
		return
	}
	assert.Equal(t, "test-user", i.Login)
	assert.Equal(t, uint64(42), i.UID)
}

func TestClient_CheckUserTicket(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		q := r.URL.Query()
		assert.Equal(t, "some-user-ticket", q.Get("user_ticket"))
		assert.Equal(t, "::1", q.Get("userip"))
		assert.Equal(t, "user_ticket", q.Get("method"))
		assert.Equal(t, "json", q.Get("format"))
		responseStr := `{"users":[{"login":"test-user","id":"42"}]}`
		io.Copy(w, strings.NewReader(responseStr))
	}))
	defer server.Close()
	bb := &Client{
		url:        server.URL,
		httpClient: server.Client(),
	}
	i, err := bb.CheckUserTicket("some-user-ticket", net.ParseIP("::1"))
	if !assert.NoError(t, err) {
		return
	}
	assert.Equal(t, "test-user", i.Login)
	assert.Equal(t, uint64(42), i.UID)
}
