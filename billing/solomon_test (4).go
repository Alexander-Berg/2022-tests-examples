package request

import (
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/payout/internal/core"
)

func (s *ProcessorTestSuite) TestRequestMetrics() {

	data, err := GetSelfMetrics(s.ctx)
	require.NoError(s.T(), err)

	for _, status := range Statuses {
		require.True(s.T(), data.ContainMetric(core.SolomonLabels{"sensor": "last", "status": status}))
		require.True(s.T(), data.ContainMetric(core.SolomonLabels{"sensor": "total", "status": status}))
		require.True(s.T(), data.ContainMetric(core.SolomonLabels{"sensor": "retardation", "status": status}))
	}
}
