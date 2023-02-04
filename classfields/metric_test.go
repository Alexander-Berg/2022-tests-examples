package request

import (
	"context"
	"fmt"
	"net"
	"testing"

	"github.com/YandexClassifieds/go-common/monitoring/request/protobuf_test"
	"github.com/prometheus/client_golang/prometheus"
	dto "github.com/prometheus/client_model/go"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
)

func TestBaseMetrics(t *testing.T) {
	listener := newLocalListener()

	middleware := NewMiddleware()

	server := runServer(middleware, listener)
	defer server.GracefulStop()

	conn, err := grpc.Dial(listener.Addr().String(), grpc.WithInsecure())
	defer conn.Close()
	require.NoError(t, err)

	client := proto.NewTestServiceClient(conn)

	for i := 0; i < 5; i++ {
		_, err = client.TestMethod1(context.TODO(), &proto.TestMethodRequest{})
		require.NoError(t, err)
	}

	requireHistCountWithLabels(t, 5, "TestMethod1", "stub")
}

func TestWithHandler(t *testing.T) {
	listener := newLocalListener()

	middleware := NewMiddleware(WithServiceName())

	server := runServer(middleware, listener)
	defer server.GracefulStop()

	conn, err := grpc.Dial(listener.Addr().String(), grpc.WithInsecure())
	defer conn.Close()
	require.NoError(t, err)

	client := proto.NewTestServiceClient(conn)

	for i := 0; i < 5; i++ {
		_, err = client.TestMethod1(context.TODO(), &proto.TestMethodRequest{})
		require.NoError(t, err)
	}

	requireHistCountWithLabels(t, 5, "TestService.TestMethod1", "stub")
}

func TestWithHandlerAndProvide(t *testing.T) {
	listener := newLocalListener()

	middleware := NewMiddleware(WithServiceName(), WithProvide("apiName"))

	server := runServer(middleware, listener)
	defer server.GracefulStop()

	conn, err := grpc.Dial(listener.Addr().String(), grpc.WithInsecure())
	defer conn.Close()
	require.NoError(t, err)

	client := proto.NewTestServiceClient(conn)

	for i := 0; i < 5; i++ {
		_, err = client.TestMethod1(context.TODO(), &proto.TestMethodRequest{})
		require.NoError(t, err)
	}

	requireHistCountWithLabels(t, 5, "TestService.TestMethod1", "apiName")
}

func requireHistCountWithLabels(t *testing.T, expected int, labels ...string) {
	observer := requestHistogramMetric.WithLabelValues(labels...)

	met := &dto.Metric{}
	observerHist, ok := observer.(prometheus.Histogram)
	if !ok {
		t.Fatal("non histogram metric")
	}

	require.NoError(t, observerHist.Write(met))
	require.Equal(t, uint64(expected), met.Histogram.GetSampleCount())
}

func newLocalListener() net.Listener {
	l, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		if l, err = net.Listen("tcp6", "[::1]:0"); err != nil {
			panic(fmt.Sprintf("grpc: failed to listen on a port: %v", err))
		}
	}
	return l
}
func runServer(m *Middleware, listener net.Listener) *grpc.Server {
	server := newTestServer()
	s := grpc.NewServer(grpc.StreamInterceptor(m.StreamServerInterceptor()),
		grpc.UnaryInterceptor(m.UnaryServerInterceptor()))

	proto.RegisterTestServiceServer(s, server)
	go func() {
		err := s.Serve(listener)
		if err != nil {
			panic("failed serve grpc server")
		}
	}()

	return s
}

type TestServer struct{}

func newTestServer() *TestServer {
	return &TestServer{}
}

func (s *TestServer) TestMethod1(ctx context.Context, request *proto.TestMethodRequest) (*proto.TestMethodResponse, error) {
	return &proto.TestMethodResponse{}, nil
}
