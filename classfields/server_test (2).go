package api

import (
	"context"
	"fmt"
	"net"
	"testing"
	"time"

	"github.com/YandexClassifieds/cms/cmd/server/checks"
	"github.com/YandexClassifieds/cms/cmd/server/clusters"
	"github.com/YandexClassifieds/cms/cmd/server/hosts"
	"github.com/YandexClassifieds/cms/cmd/server/hostsStatus"
	"github.com/YandexClassifieds/cms/common/config"
	pbServer "github.com/YandexClassifieds/cms/pb/cms/api/server"
	pbDC "github.com/YandexClassifieds/cms/pb/cms/domains/datacenters"
	pbHostTypes "github.com/YandexClassifieds/cms/pb/cms/domains/host_types"
	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
)

const (
	bbURL = "blackbox.yandex-team.ru:443"
)

func TestRunServer(t *testing.T) {
	test.InitTestEnv()

	if !config.Bool("CI") {
		t.Skip("Для этого теста нужен блэкбокс и айпишник клиента. Проверяется в CI")
	}

	log := logrus.New()
	db := test.NewSeparatedGorm(t)

	clusterStorage := clusters.NewStorage(db, log)
	require.NoError(t, clusterStorage.Save(pbHostTypes.HostType_BAREMETAL, pbDC.DC_SAS, false, 1))

	srv := RunServer(
		clusterStorage,
		hosts.NewStorage(db, log),
		hostsStatus.NewStorage(db, log),
		checks.NewStorage(db, log),
		log,
	)
	t.Cleanup(srv.GracefulStop)

	// for start listen
	time.Sleep(5 * time.Second)

	// get self ip for blackbox
	bbConn, err := net.Dial("tcp6", bbURL)
	require.NoError(t, err)
	host, _, err := net.SplitHostPort(bbConn.LocalAddr().String())
	require.NoError(t, err)
	require.NoError(t, bbConn.Close())

	conn, err := grpc.Dial(fmt.Sprintf("[%s]:%d", host, config.Int("SERVER_API_PORT")), grpc.WithTransportCredentials(insecure.NewCredentials()))
	require.NoError(t, err)
	client := pbServer.NewServerServiceClient(conn)

	t.Run("without token", func(t *testing.T) {
		_, err = client.GetClusterStatus(context.Background(), &pbServer.GetClusterStatusRequest{
			HostType: pbHostTypes.HostType_BAREMETAL,
			Dc:       pbDC.DC_SAS,
		})
		require.Error(t, err)
		st, ok := status.FromError(err)
		require.True(t, ok)
		require.Equal(t, codes.PermissionDenied, st.Code())
	})

	t.Run("with invalid token", func(t *testing.T) {
		md := metadata.New(map[string]string{"authorization": "Bearer testtest"})
		ctx := metadata.NewOutgoingContext(context.Background(), md)

		_, err = client.GetClusterStatus(ctx, &pbServer.GetClusterStatusRequest{
			HostType: pbHostTypes.HostType_BAREMETAL,
			Dc:       pbDC.DC_SAS,
		})
		require.Error(t, err)
		st, ok := status.FromError(err)
		require.True(t, ok)
		require.Equal(t, codes.Unauthenticated, st.Code())
	})

	t.Run("with admin token", func(t *testing.T) {
		md := metadata.New(map[string]string{"authorization": fmt.Sprintf("Bearer %s", config.Str("TEST_VALID_OAUTH_TOKEN"))})
		ctx := metadata.NewOutgoingContext(context.Background(), md)

		_, err = client.GetClusterStatus(ctx, &pbServer.GetClusterStatusRequest{
			HostType: pbHostTypes.HostType_BAREMETAL,
			Dc:       pbDC.DC_SAS,
		})
		require.NoError(t, err)
	})

	t.Run("with non-admin valid token", func(t *testing.T) {
		srv.adminLogins = []string{"test"}
		t.Cleanup(func() {
			srv.adminLogins = config.StrList("ADMIN_LOGINS")
		})

		md := metadata.New(map[string]string{"authorization": fmt.Sprintf("Bearer %s", config.Str("TEST_VALID_OAUTH_TOKEN"))})
		ctx := metadata.NewOutgoingContext(context.Background(), md)

		_, err = client.GetClusterStatus(ctx, &pbServer.GetClusterStatusRequest{
			HostType: pbHostTypes.HostType_BAREMETAL,
			Dc:       pbDC.DC_SAS,
		})
		require.Error(t, err)
		st, ok := status.FromError(err)
		require.True(t, ok)
		require.Equal(t, codes.PermissionDenied, st.Code())
	})
}
