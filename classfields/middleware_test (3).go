package srvticket

import (
	"context"
	tvm2 "github.com/YandexClassifieds/shiva/test/mock/tvm"
	"testing"

	"github.com/YandexClassifieds/go-common/tvm"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/metadata"
)

const (
	testTicket = "test-ticket"
)

func TestInterceptor(t *testing.T) {
	ctx := metadata.NewIncomingContext(context.Background(), metadata.New(map[string]string{"x-ya-service-ticket": testTicket}))
	mockTvm := &tvm2.TvmClient{}
	mockTvm.On("CheckServiceTicket", "test-ticket").Return(&tvm.ServiceTicket{SrcID: 1, DstID: 42}, nil)
	m := NewMiddleware(mockTvm, map[int][]string{}, 0)
	newCxt, err := m.Interceptor(ctx)
	require.NoError(t, err)
	st := ServiceTicketFromContext(newCxt)
	require.NotNil(t, st)
}

func TestSuccessCheck(t *testing.T) {
	testCases := []struct {
		name                 string
		method               string
		selfTvmId            int
		allowedTvmAndMethods map[int][]string
		expectedCheck        bool
	}{
		{
			name:      "Success all methods",
			selfTvmId: 5,
			allowedTvmAndMethods: map[int][]string{
				6: nil,
			},
			expectedCheck: true,
		},
		{
			name:      "Success certain method",
			selfTvmId: 5,
			method:    "test-method",
			allowedTvmAndMethods: map[int][]string{
				6: {"test-method-1", "test-method"},
			},
			expectedCheck: true,
		},
		{
			name:      "Fail wrong dstId",
			selfTvmId: 2,
			allowedTvmAndMethods: map[int][]string{
				6: nil,
			},
			expectedCheck: false,
		},
		{
			name:      "Fail not allowed tvmId",
			selfTvmId: 5,
			allowedTvmAndMethods: map[int][]string{
				1: nil,
			},
			expectedCheck: false,
		},
		{
			name:      "Fail not allowed method",
			selfTvmId: 5,
			method:    "not-allowed-method",
			allowedTvmAndMethods: map[int][]string{
				6: {"test-method-1", "test-method"},
			},
			expectedCheck: false,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			m := NewMiddleware(&tvm2.TvmClient{}, tc.allowedTvmAndMethods, tc.selfTvmId)
			ctx := ServiceTicketToContext(context.Background(), &tvm.ServiceTicket{
				SrcID: 6,
				DstID: 5,
			})
			check := m.Check(nil, tc.method, ctx)
			require.Equal(t, tc.expectedCheck, check)
		})
	}

}

func TestFailCheck(t *testing.T) {
	allowedTvmIds := map[int][]string{
		1: nil,
	}
	m := NewMiddleware(&tvm2.TvmClient{}, allowedTvmIds, 2)
	ctx := context.Background()
	ServiceTicketToContext(ctx, &tvm.ServiceTicket{
		SrcID: 5,
		DstID: 6,
	})
	check := m.Check(nil, "", ctx)
	assert.False(t, check)
}
