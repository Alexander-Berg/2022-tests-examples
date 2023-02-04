package oauth

import (
	"context"
	"fmt"
	"github.com/YandexClassifieds/go-common/blackbox"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/peer"
	"net"
	"testing"
)

const (
	testToken = "oauth_token"
	testLogin = "user_login"
	testUid   = "uid"
)

func TestExtract(t *testing.T) {
	ctx := ctx()
	m := NewMiddleware(ClientMock{})
	result, err := m.Extract(ctx)
	require.Nil(t, err)
	assert.Equal(t, testLogin, LoginFromContext(result))
}

func TestIsAuthorized(t *testing.T) {

	ctx := ctx()
	ctx = LoginToContext(ctx, testLogin)
	m := NewMiddleware(ClientMock{})
	assert.True(t, m.IsAuthorized(nil, "", ctx))
}

func TestFailIsAuthorized(t *testing.T) {
	ctx := ctx()
	m := NewMiddleware(ClientMock{})
	assert.False(t, m.IsAuthorized(nil, "", ctx))
}

func ctx() context.Context {
	ctx := metadata.NewIncomingContext(context.Background(), metadata.New(map[string]string{"authorization": testToken}))
	ctx = peer.NewContext(ctx, &peer.Peer{Addr: &net.TCPAddr{}})
	return ctx
}

type ClientMock struct {
}

func (c ClientMock) CheckOAuth(ctx context.Context, token, ip string) (*blackbox.CheckResult, error) {
	if token != testToken {
		return nil, fmt.Errorf("test error: token not equals")
	}
	return &blackbox.CheckResult{
		Login:  testLogin,
		UID:    testUid,
		Status: "VALID",
	}, nil
}

func (c ClientMock) CheckSession(ctx context.Context, sid1, sid2, ip string) (*blackbox.CheckResult, error) {
	panic("implement me")
}
