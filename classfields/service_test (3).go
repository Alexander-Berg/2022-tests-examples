package checks

import (
	"testing"
	"time"

	pbAgent "github.com/YandexClassifieds/cms/pb/cms/api/agent"
	pbCheckStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/check_statuses"
	pbChecks "github.com/YandexClassifieds/cms/pb/cms/domains/checks"
	mChecks "github.com/YandexClassifieds/cms/test/mocks/mockery/agent/checks"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
)

func TestService_RunService(t *testing.T) {
	log := logrus.New()

	check := &mChecks.ICheck{}
	check.On("Run").Return(pbCheckStatuses.Status_OK, "test")

	s := NewService(log)
	s.checks = map[pbChecks.Check]ICheck{
		pbChecks.Check_ANSIBLE_PULL: check,
	}

	s.RunService()
	require.Eventually(t, func() bool {
		return check.AssertCalled(t, "Run")
	}, 10*time.Second, 100*time.Millisecond)
}

func TestService_GetResults(t *testing.T) {
	log := logrus.New()

	s := NewService(log)
	s.results = map[pbChecks.Check]*pbAgent.CheckResult{
		pbChecks.Check_ANSIBLE_PULL: {
			Check:       pbChecks.Check_ANSIBLE_PULL,
			Status:      pbCheckStatuses.Status_OK,
			Description: "test",
		},
	}

	results := s.GetResults()
	require.Equal(t, 1, len(results))
	require.Equal(t, pbChecks.Check_ANSIBLE_PULL, results[0].Check)
	require.Equal(t, pbCheckStatuses.Status_OK, results[0].Status)
	require.Equal(t, "test", results[0].Description)
}
