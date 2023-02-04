package blackbox

import (
	"context"
	"net"
	"testing"

	"google.golang.org/grpc/peer"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/metadata"
)

var (
	_ Checker = &Client{}
)

func TestInterceptor(t *testing.T) {
	ctx := metadata.NewIncomingContext(context.Background(), metadata.New(map[string]string{"x-ya-user-ticket": "test-ticket"}))
	ctx = peer.NewContext(ctx, &peer.Peer{Addr: &net.TCPAddr{IP: net.ParseIP("::2")}})
	mockBB := &MockChecker{}
	mockBB.On("CheckUserTicket", "test-ticket", net.ParseIP("::2")).Return(&UserInfo{UID: 42, Login: "someone"}, nil)
	m := NewMiddleware(mockBB)

	newCtx, err := m.Interceptor(ctx)
	require.NoError(t, err)
	st := UserTicketFromContext(newCtx)
	require.NotNil(t, st)
	assert.Equal(t, uint64(42), st.UID)
	assert.Equal(t, "someone", st.Login)
}
