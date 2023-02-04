package common

import (
	"context"
	"testing"

	"github.com/YandexClassifieds/go-common/tvm"
	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/YandexClassifieds/shiva/pkg/auth/blackbox"
	"github.com/YandexClassifieds/shiva/pkg/auth/oauth/grpc"
	"github.com/YandexClassifieds/shiva/pkg/auth/tvm/grpc/srvticket"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestDeploymentStatesFromProto(t *testing.T) {
	for _, i := range state.DeploymentState_value {
		dState := state.DeploymentState(i)
		_, err := DeploymentStateFromProto(dState)
		if dState == state.DeploymentState_UNKNOWN {
			require.Error(t, err)
			continue
		}
		require.NoError(t, err)
	}
}

func TestExtractSource_tvm(t *testing.T) {
	test.RunUp(t)
	viper.Set("ALLOWED_TVM_SOURCES", "SERVICE_NAME_TEST:2015222")
	strMap := config.StrMap("ALLOWED_TVM_SOURCES")
	ctx := context.Background()
	ctx = srvticket.ServiceTicketToContext(ctx, &tvm.ServiceTicket{
		SrcID: 2015222,
	})
	source := ExtractSource(ctx, strMap, "", "")
	assert.Equal(t, "SERVICE_NAME_TEST", source)
}

func TestExtractSource_ci(t *testing.T) {
	test.RunUp(t)
	viper.Set("ALLOWED_TVM_SOURCES", "SERVICE_NAME_TEST:2015222")
	strMap := config.StrMap("ALLOWED_TVM_SOURCES")
	source := ExtractSource(context.Background(), strMap, "robot-vertis-shiva", "robot-vertis-shiva")
	assert.Equal(t, config.CISource, source)
}

func TestExtractSource_oauth(t *testing.T) {
	test.RunUp(t)
	viper.Set("ALLOWED_TVM_SOURCES", "SERVICE_NAME_TEST:2015222")
	strMap := config.StrMap("ALLOWED_TVM_SOURCES")
	source := ExtractSource(context.Background(), strMap, "anybody", "robot-vertis-shiva")
	assert.Equal(t, config.OAuthSource, source)
}

func TestExtractLoginFromContext(t *testing.T) {
	test.RunUp(t)
	login := "test-login"
	user := &blackbox.UserInfo{
		Login: login,
		UID:   42,
	}
	ctx := grpc.UserInfoContext(context.Background(), user)
	got, err := ExtractLoginFromContext(ctx, test.NewLogger(t))
	require.NoError(t, err)
	require.Equal(t, login, got)

	ctx = blackbox.UserTicketContext(context.Background(), user)
	got, err = ExtractLoginFromContext(ctx, test.NewLogger(t))
	require.NoError(t, err)
	require.Equal(t, login, got)
}
