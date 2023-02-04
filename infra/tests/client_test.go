package rpc

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	awacs_pb "a.yandex-team.ru/infra/awacs/proto"
	rpc_pb "a.yandex-team.ru/infra/nanny2/proto/rpc"
)

func TestClientConnectionError(t *testing.T) {
	ctx := context.TODO()
	reqPb := &awacs_pb.ListNamespacesRequest{}

	cfg := &ClientConfig{
		RPCURL:         "http://localhost:58982",
		RequestTimeout: 10,
	}
	client := awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err := client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "failed to post request after 0 retries")
	assert.Contains(t, err.Error(), "dial tcp [::1]:58982: connect: connection refused")

	cfg.RetryConnectionErrors = true
	client = awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err = client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "failed to post request after 5 retries")
	assert.Contains(t, err.Error(), "dial tcp [::1]:58982: connect: connection refused")

	cfg = &ClientConfig{
		RPCURL:            "http://iss3msk.yandex-team.ru:9091",
		RequestTimeout:    10,
		ConnectionTimeout: 1,
	}

	client = awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err = client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "failed to post request after 0 retries")
	assert.Contains(t, err.Error(), "i/o timeout")

	cfg.RetryConnectionErrors = true
	client = awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err = client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "failed to post request after 0 retries") // We can't retry connection timeout without retrying request timeout
	assert.Contains(t, err.Error(), "i/o timeout")
}

func TestClient429(t *testing.T) {
	ctx := context.TODO()
	reqPb := &awacs_pb.ListNamespacesRequest{}

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/x-protobuf")
		w.WriteHeader(http.StatusTooManyRequests)
		statusPb := &rpc_pb.Status{Code: http.StatusTooManyRequests}
		data, err := proto.Marshal(statusPb)
		require.NoError(t, err)
		_, err = w.Write(data)
		require.NoError(t, err)
	}))
	defer ts.Close()

	cfg := &ClientConfig{
		RPCURL:         ts.URL,
		RequestTimeout: 10,
	}
	client := awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err := client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	assert.Equal(t, err.(*ClientError).Code, int32(429))
	require.Error(t, err)
	assert.Equal(t, err.(*ClientError).RetriesCount, 0)

	cfg.Retry429 = true
	client = awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err = client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	assert.Equal(t, err.(*ClientError).Code, int32(429))
	require.Error(t, err)
	assert.Equal(t, err.(*ClientError).RetriesCount, 5)
}

func TestClient5xx(t *testing.T) {
	ctx := context.TODO()
	reqPb := &awacs_pb.ListNamespacesRequest{}

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/x-protobuf")
		w.WriteHeader(http.StatusInternalServerError)
		statusPb := &rpc_pb.Status{Code: http.StatusInternalServerError}
		data, err := proto.Marshal(statusPb)
		require.NoError(t, err)
		_, err = w.Write(data)
		require.NoError(t, err)
	}))
	defer ts.Close()

	cfg := &ClientConfig{
		RPCURL:         ts.URL,
		RequestTimeout: 10,
	}
	client := awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err := client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	assert.Equal(t, err.(*ClientError).Code, int32(500))
	require.Error(t, err)
	assert.Equal(t, err.(*ClientError).RetriesCount, 0)

	cfg.Retry5xx = true
	client = awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err = client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	assert.Equal(t, err.(*ClientError).Code, int32(500))
	require.Error(t, err)
	assert.Equal(t, err.(*ClientError).RetriesCount, 5)
}

func TestClient200(t *testing.T) {
	ctx := context.TODO()
	reqPb := &awacs_pb.ListNamespacesRequest{}

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(t, r.URL.Path, "/ListNamespaces/")
		w.WriteHeader(http.StatusOK)
		resp := &awacs_pb.ListNamespacesResponse{Total: 1}
		data, err := proto.Marshal(resp)
		require.NoError(t, err)

		_, err = w.Write(data)
		require.NoError(t, err)
	}))
	defer ts.Close()

	cfg := &ClientConfig{
		RPCURL:         ts.URL,
		RequestTimeout: 10,
	}
	client := awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err := client.ListNamespaces(ctx, reqPb)
	require.NoError(t, err)
	assert.Equal(t, respPb.Total, int32(1))
}

func TestRequestTimeout(t *testing.T) {
	ctx := context.TODO()
	reqPb := &awacs_pb.ListNamespacesRequest{}

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		time.Sleep(3 * time.Second)
	}))
	defer ts.Close()

	cfg := &ClientConfig{
		RPCURL:         ts.URL,
		RequestTimeout: 1,
	}
	client := awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err := client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "failed to post request after 0 retries")
	assert.Contains(t, err.Error(), "context deadline exceeded (Client.Timeout exceeded while awaiting headers)")

	// Test that we don't retry request (not connection) timeout (But connection timeout we can't retry yet)
	cfg.RetryConnectionErrors = true
	client = awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err = client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "failed to post request after 0 retries")
	assert.Contains(t, err.Error(), "context deadline exceeded (Client.Timeout exceeded while awaiting headers)")
}

func TestErrorResponseParsing(t *testing.T) {
	ctx := context.TODO()
	reqPb := &awacs_pb.ListNamespacesRequest{}

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusGatewayTimeout)
	}))
	defer ts.Close()

	cfg := &ClientConfig{
		RPCURL:         ts.URL,
		RequestTimeout: 10,
	}
	client := awacs_pb.NewNamespaceServiceClient(NewClient(cfg))
	respPb, err := client.ListNamespaces(ctx, reqPb)
	assert.Nil(t, respPb)
	require.Error(t, err)
	assert.Contains(t, err.Error(), "invalid response after 0 retries: 504")
}
