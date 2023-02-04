package testing

import (
	"context"
	"os"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"
	"github.com/stretchr/testify/suite"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/tvm"
	"a.yandex-team.ru/library/go/core/metrics"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
	yatvm "a.yandex-team.ru/library/go/yandex/tvm"
	"a.yandex-team.ru/library/go/yandex/tvm/tvmtool"
)

type BaseSuite struct {
	suite.Suite
	ctrl *gomock.Controller
}

func (s *BaseSuite) Ctrl() *gomock.Controller {
	if s.ctrl == nil {
		s.ctrl = gomock.NewController(s.T())
	}
	return s.ctrl
}

func (s *BaseSuite) TVM() yatvm.Client {
	_ = os.Setenv(tvmtool.DeployEndpointEnvKey, "http://localhost:1")
	_ = os.Setenv(tvmtool.DeployTokenEnvKey, RandS(16))
	client, err := tvm.GetTvmClient(context.Background())
	require.NoError(s.T(), err)
	return client
}

func (s *BaseSuite) Registry() metrics.Registry {
	return solomon.NewRegistry(solomon.NewRegistryOpts())
}

func (s *BaseSuite) SolomonRegistry() *solomon.Registry {
	return solomon.NewRegistry(solomon.NewRegistryOpts())
}
