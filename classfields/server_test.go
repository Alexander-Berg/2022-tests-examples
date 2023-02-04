package grpc

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/cms/cmd/agent/actions"
	"github.com/YandexClassifieds/cms/cmd/agent/checks"
	"github.com/YandexClassifieds/cms/common/config"
	pbAgent "github.com/YandexClassifieds/cms/pb/cms/api/agent"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/go-common/tvm/tvmauth"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
)

func TestRunServer(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	checkService := checks.NewService(log)
	actionService := actions.New(log)

	srv := RunServer(checkService, actionService, log)
	t.Cleanup(srv.GracefulStop)

	// for start listen
	time.Sleep(5 * time.Second)

	conn, err := grpc.Dial(fmt.Sprintf("[::1]:%d", config.Int("AGENT_API_PORT")), grpc.WithTransportCredentials(insecure.NewCredentials()))
	require.NoError(t, err)
	agentService := pbAgent.NewAgentServiceClient(conn)

	t.Run("without ticket", func(t *testing.T) {
		_, err = agentService.GetActionStatus(context.Background(), &pbAgent.GetActionStatusRequest{})
		require.Error(t, err)
		st, ok := status.FromError(err)
		require.True(t, ok)
		require.Equal(t, codes.PermissionDenied, st.Code())
	})

	t.Run("with invalid ticket", func(t *testing.T) {
		md := metadata.New(map[string]string{"x-ya-service-ticket": "testtest"})
		ctx := metadata.NewOutgoingContext(context.Background(), md)

		_, err = agentService.GetActionStatus(ctx, &pbAgent.GetActionStatusRequest{})
		require.Error(t, err)
		st, ok := status.FromError(err)
		require.True(t, ok)
		require.Equal(t, codes.Internal, st.Code())
	})

	t.Run("with valid ticket", func(t *testing.T) {
		tvmClient := tvmauth.NewClient(
			tvmauth.WithIssueTicket(config.Int("AGENT_TVM_ALLOWED_ID"), []int{config.Int("AGENT_TVM_ID")}, config.TvmSecret()),
			tvmauth.WithLogger(log),
		)
		ticket, err := tvmClient.ServiceTicket(config.Int("AGENT_TVM_ALLOWED_ID"), config.Int("AGENT_TVM_ID"))
		require.NoError(t, err)

		md := metadata.New(map[string]string{"x-ya-service-ticket": ticket})
		ctx := metadata.NewOutgoingContext(context.Background(), md)

		_, err = agentService.GetActionStatus(ctx, &pbAgent.GetActionStatusRequest{})
		require.NoError(t, err)
	})
}
