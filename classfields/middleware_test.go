package srvticket

import (
	"context"
	"testing"

	"github.com/YandexClassifieds/go-common/tvm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/metadata"
)

const (
	testTicket = "test-ticket"
)

func TestExtract(t *testing.T) {
	ctx := metadata.NewIncomingContext(context.Background(), metadata.New(map[string]string{"x-ya-service-ticket": testTicket}))
	mockTvm := &TvmClient{}
	mockTvm.On("CheckServiceTicket", testTicket).Return(&tvm.ServiceTicket{SrcID: 1, DstID: 42}, nil)
	m := NewMiddleware(mockTvm, 0, []int{})
	newCxt, err := m.Extract(ctx)
	require.NoError(t, err)
	st := ServiceTicketFromContext(newCxt)
	assert.NotNil(t, st)
}

func TestDefaultIsAuthorized(t *testing.T) {
	m := NewMiddleware(&TvmClient{}, 5, nil)
	ctx := ServiceTicketToContext(context.Background(), &tvm.ServiceTicket{SrcID: 6, DstID: 5})
	check := m.IsAuthorized(nil, "", ctx)
	assert.True(t, check)
}

func TestIsAuthorized(t *testing.T) {
	m := NewMiddleware(&TvmClient{}, 5, []int{6})
	ctx := ServiceTicketToContext(context.Background(), &tvm.ServiceTicket{SrcID: 6, DstID: 5})
	check := m.IsAuthorized(nil, "", ctx)
	assert.True(t, check)
}

func TestFailIsAuthorized(t *testing.T) {
	m := NewMiddleware(&TvmClient{}, 2, []int{1})
	ctx := context.Background()
	ServiceTicketToContext(ctx, &tvm.ServiceTicket{SrcID: 6, DstID: 5})
	check := m.IsAuthorized(nil, "", ctx)
	assert.False(t, check)
}
