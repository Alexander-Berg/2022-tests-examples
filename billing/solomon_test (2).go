package cpf

import (
	"strconv"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/billing/hot/payout/internal/core"
)

func (s *CpfTestSuite) TestRequestMetricsBase() {
	data, err := GetSelfMetrics(s.ctx)
	require.NoError(s.T(), err)

	for _, status := range Statuses {
		for _, dryRun := range []bool{true, false} {
			_, err = core.FindMetric(data, core.SolomonLabels{"sensor": "total", "status": status,
				"dry_run": strconv.FormatBool(dryRun)})
			require.NoError(s.T(), err)
			_, err = core.FindMetric(data, core.SolomonLabels{"sensor": "amount", "status": status,
				"dry_run": strconv.FormatBool(dryRun), "type": "sum"})
			require.NoError(s.T(), err)
			_, err = core.FindMetric(data, core.SolomonLabels{"sensor": "last", "status": status,
				"dry_run": strconv.FormatBool(dryRun)})
			require.NoError(s.T(), err)
			_, err = core.FindMetric(data, core.SolomonLabels{"sensor": "amount", "status": status,
				"dry_run": strconv.FormatBool(dryRun), "type": "last"})
			require.NoError(s.T(), err)
		}
	}
	_, err = core.FindMetric(data, core.SolomonLabels{"sensor": "partition_count"})
	require.NoError(s.T(), err)
}
