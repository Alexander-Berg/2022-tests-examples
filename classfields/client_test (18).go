package blackbox

import (
	"context"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/YandexClassifieds/go-common/tvm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	_ Client = &blackboxCli{}
)

func TestClient_CheckOAuth(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		q := r.URL.Query()
		assert.Equal(t, "xyz", r.FormValue("oauth_token"))
		assert.Equal(t, "::1", r.FormValue("userip"))
		assert.Equal(t, "oauth", r.FormValue("method"))
		assert.Equal(t, "json", q.Get("format"))
		responseStr := `{"Error":"OK","login":"test-user","uid":{"value":"42"}}`
		io.Copy(w, strings.NewReader(responseStr))
	}))
	defer server.Close()
	bb := NewClient(Config{SelfTvmId: 42}, new(mockTvmClient), WithEndpoint(server.URL, 2))
	info, err := bb.CheckOAuth(context.Background(), "xyz", "::1")
	if !assert.NoError(t, err) {
		return
	}
	assert.Equal(t, "test-user", info.Login)
	assert.Equal(t, "42", info.UID)
}

func TestClient_Check_SessionID(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		q := r.URL.Query()
		assert.Equal(t, "sid1", r.FormValue("sessionid"))
		assert.Equal(t, "sid2", r.FormValue("sslsessionid"))
		assert.Equal(t, "::1", r.FormValue("userip"))
		assert.Equal(t, "sessionid", r.FormValue("method"))
		assert.Equal(t, "json", q.Get("format"))
		responseStr := `{"Error":"OK","login":"test-user","uid":{"value":"42"}}`
		io.Copy(w, strings.NewReader(responseStr))
	}))
	defer server.Close()
	bb := NewClient(Config{SelfTvmId: 42}, new(mockTvmClient), WithEndpoint(server.URL, 2))
	info, err := bb.CheckSession(context.Background(), "sid1", "sid2", "::1")
	require.NoError(t, err)
	assert.Equal(t, "test-user", info.Login)
	assert.Equal(t, "42", info.UID)
}

type mockTvmClient struct{}

func (m *mockTvmClient) ServiceTicket(src, dst int) (string, error) {
	return "some-ticket", nil
}

func (m *mockTvmClient) CheckServiceTicket(ticket string) (*tvm.ServiceTicket, error) {
	panic("implement me")
}

func (m *mockTvmClient) Ping(ctx context.Context) error {
	panic("implement me")
}
