package pipeline

import (
	"testing"

	"github.com/YandexClassifieds/cms/cmd/server/checks"
	pbAction "github.com/YandexClassifieds/cms/pb/cms/domains/actions/action"
	pbCheckStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/check_statuses"
	pbChecks "github.com/YandexClassifieds/cms/pb/cms/domains/checks"
	"github.com/YandexClassifieds/cms/test"
	"github.com/stretchr/testify/require"
)

func TestUnispace_Match(t *testing.T) {
	test.InitTestEnv()

	tests := map[string]struct {
		Checks  []*checks.Check
		Verdict bool
	}{
		"match crit": {
			Checks: []*checks.Check{
				{
					Type:   pbChecks.Check_NOMAD,
					Status: pbCheckStatuses.Status_OK,
				},
				{
					Type:   pbChecks.Check_UNISPACE,
					Status: pbCheckStatuses.Status_CRIT,
				},
			},
			Verdict: true,
		},
		"match warn": {
			Checks: []*checks.Check{
				{
					Type:   pbChecks.Check_NOMAD,
					Status: pbCheckStatuses.Status_OK,
				},
				{
					Type:   pbChecks.Check_UNISPACE,
					Status: pbCheckStatuses.Status_WARN,
				},
			},
			Verdict: true,
		},
		"another checks": {
			Checks: []*checks.Check{
				{
					Type:   pbChecks.Check_NOMAD,
					Status: pbCheckStatuses.Status_CRIT,
				},
				{
					Type:   pbChecks.Check_CRON,
					Status: pbCheckStatuses.Status_WARN,
				},
			},
			Verdict: false,
		},
	}
	a := NewUnispace(nil, nil)

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			result := a.Match(tc.Checks)
			require.Equal(t, tc.Verdict, result)
		})
	}
}

func TestUnispace_Actions(t *testing.T) {
	a := NewUnispace(nil, nil)

	actions := make(map[pbAction.Action]struct{})
	for _, action := range a.actions {
		_, ok := actions[action.Type()]
		require.False(t, ok)
		actions[action.Type()] = struct{}{}
	}
}
