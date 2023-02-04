package delegation

import (
	"context"
	"fmt"
	"net"
	"testing"

	pb "github.com/YandexClassifieds/shiva/cmd/secret-service/api/grpc/delegation/protobuf_test"
	"github.com/YandexClassifieds/shiva/pkg/secrets/tokens"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
)

func TestVersionMiddleware(t *testing.T) {
	listener := newLocalListener()

	server := runServer(t, listener, "0.6.0")
	defer server.GracefulStop()

	conn, err := grpc.Dial(listener.Addr().String(), grpc.WithInsecure())
	defer conn.Close()
	require.NoError(t, err)

	client := pb.NewTestServiceClient(conn)

	testCases := []struct {
		name        string
		ctx         context.Context
		expectedErr error
	}{
		{
			name:        "version supported",
			ctx:         metadata.AppendToOutgoingContext(context.Background(), tokens.VersionTag, "0.6.0"),
			expectedErr: nil,
		},
		{
			name:        "version not supported",
			ctx:         metadata.AppendToOutgoingContext(context.Background(), tokens.VersionTag, "0.4"),
			expectedErr: notSupportedVersion,
		},
		{
			name:        "latest version",
			ctx:         metadata.AppendToOutgoingContext(context.Background(), tokens.VersionTag, tokens.LatestVersion),
			expectedErr: nil,
		},
		{
			name:        "no version",
			ctx:         metadata.AppendToOutgoingContext(context.Background(), "some_key", "some_val"),
			expectedErr: notSupportedVersion,
		},
		{
			name:        "no metadata",
			ctx:         context.Background(),
			expectedErr: notSupportedVersion,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			_, err = client.TestMethod1(tc.ctx, &pb.TestMethodRequest{})
			expectedStatus, _ := status.FromError(tc.expectedErr)
			s, _ := status.FromError(err)
			assert.Equal(t, expectedStatus.Code(), s.Code())
			assert.Equal(t, expectedStatus.Message(), s.Message())
		})
	}
}

func TestSkipMethod(t *testing.T) {
	listener := newLocalListener()

	server := runServer(t, listener, "0.6.0")
	defer server.GracefulStop()

	conn, err := grpc.Dial(listener.Addr().String(), grpc.WithInsecure())
	defer conn.Close()
	require.NoError(t, err)

	client := pb.NewTestServiceClient(conn)

	ctx := metadata.AppendToOutgoingContext(context.Background(), tokens.VersionTag, "0.4")
	_, err = client.GetRequiredVersion(ctx, &pb.TestMethodRequest{})
	require.NoError(t, err)
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

func runServer(t *testing.T, listener net.Listener, minVersion string) *grpc.Server {
	server := newTestServer()
	s := grpc.NewServer(grpc.UnaryInterceptor(versionMiddleware(minVersion, test.NewLogger(t))))

	pb.RegisterTestServiceServer(s, server)
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

func (s *TestServer) TestMethod1(ctx context.Context, request *pb.TestMethodRequest) (*pb.TestMethodResponse, error) {
	return &pb.TestMethodResponse{}, nil
}

func (s *TestServer) GetRequiredVersion(ctx context.Context, request *pb.TestMethodRequest) (*pb.TestMethodResponse, error) {
	return &pb.TestMethodResponse{}, nil
}
