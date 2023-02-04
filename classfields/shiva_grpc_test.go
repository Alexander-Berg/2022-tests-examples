package old

import (
	"context"
	"fmt"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/api/deploy"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/flags"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"io"
	"net"
	"testing"
)

func TestNewShivaClient(t *testing.T) {
	test.RunUp(t)
	l := newLocalListener()
	s := grpc.NewServer()
	srvMock := &mockDeployServer{
		responses: []*proto.StateResponse{
			{State: proto.DeployState_PREPARE},
			{State: proto.DeployState_IN_PROGRESS},
			{State: proto.DeployState_SUCCESS},
		},
		sendErr: status.Error(codes.Unavailable, "mocked err"),
	}
	proto.RegisterDeployServiceServer(s, srvMock)
	defer s.GracefulStop()
	go func() {
		_ = s.Serve(l)
	}()
	conf := Config{
		Address: l.Addr().String(),
	}
	cli, err := NewDeployClient(conf, nil)
	test.Check(t, err)

	t.Run("check_retried", func(t *testing.T) {
		stateStream, err := cli.State(context.Background(), &proto.StateRequest{})
		test.Check(t, err)
		states := make([]*proto.StateResponse, 0)
		for {
			sr, err := stateStream.Recv()
			if err == io.EOF {
				break
			}
			test.Check(t, err)
			states = append(states, sr)
		}
		assert.Len(t, states, 3)
		assert.Equal(t, states[0].State, proto.DeployState_PREPARE)
		assert.Equal(t, states[1].State, proto.DeployState_IN_PROGRESS)
		assert.Equal(t, states[2].State, proto.DeployState_SUCCESS)
	})
	t.Run("check_not_retried", func(t *testing.T) {
		srvMock.sendErr = status.Error(codes.Internal, "the error")
		stateStream, err := cli.State(context.Background(), &proto.StateRequest{})
		test.Check(t, err)
		var lastErr error
		for {
			_, lastErr = stateStream.Recv()
			if lastErr != nil {
				break
			}
		}
		s, _ := status.FromError(lastErr)
		assert.Equal(t, codes.Internal, s.Code())
		assert.Equal(t, "the error", s.Message())
	})
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

type mockDeployServer struct {
	mock.Mock
	responses []*proto.StateResponse
	sendErr   error
}

func (m *mockDeployServer) Envs(ctx context.Context, request *proto.EnvsRequest) (*proto.EnvsResponse, error) {
	panic("implement me")
}

func (m *mockDeployServer) AsyncRun(context.Context, *proto.RunRequest) (*proto.AsyncResponse, error) {
	panic("implement me")
}

func (m *mockDeployServer) Status(context.Context, *proto.StatusRequest) (*proto.StatusResponse, error) {
	panic("implement me")
}

func (m *mockDeployServer) AllStatus(context.Context, *proto.AllStatusRequest) (*proto.StatusResponse, error) {
	panic("implement me")
}

func (m *mockDeployServer) BalancerStatus(context.Context, *proto.BalancerStatusRequest) (*proto.BalancerStatusResponse, error) {
	panic("implement me")
}

func (m *mockDeployServer) Run(*proto.RunRequest, proto.DeployService_RunServer) error {
	panic("implement me")
}

func (m *mockDeployServer) Stop(*proto.StopRequest, proto.DeployService_StopServer) error {
	panic("implement me")
}

func (m *mockDeployServer) Restart(*proto.RestartRequest, proto.DeployService_RestartServer) error {
	panic("implement me")
}

func (m *mockDeployServer) Revert(*proto.RevertRequest, proto.DeployService_RevertServer) error {
	panic("implement me")
}

func (m *mockDeployServer) State(req *proto.StateRequest, ss proto.DeployService_StateServer) error {
	if sendErr := m.sendErr; sendErr != nil {
		m.sendErr = nil
		return sendErr
	}
	for _, sr := range m.responses {
		if err := ss.Send(sr); err != nil {
			return err
		}
	}
	return nil
}

func (m *mockDeployServer) Cancel(*proto.CancelRequest, proto.DeployService_CancelServer) error {
	panic("implement me")
}

func (m *mockDeployServer) Promote(*proto.PromoteRequest, proto.DeployService_PromoteServer) error {
	panic("implement me")
}

func (m *mockDeployServer) Approve(*proto.ApproveRequest, proto.DeployService_ApproveServer) error {
	panic("implement me")
}

func (m *mockDeployServer) ApproveList(context.Context, *proto.ApproveListRequest) (*proto.ApproveListResponse, error) {
	panic("implement me")
}

func (d *mockDeployServer) ReleaseHistory(context.Context, *proto.ReleaseHistoryRequest) (*proto.ReleaseHistoryResponse, error) {
	panic("implement me")
}

func (d *mockDeployServer) Settings(context.Context, *proto.SettingsRequest) (*flags.Flags, error) {
	panic("implement me")
}
