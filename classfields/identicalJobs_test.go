package cms

import (
	"fmt"
	"net/http"
	"testing"

	pbCheckStatuses "github.com/YandexClassifieds/cms/pb/cms/domains/check_statuses"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/hashicorp/nomad/api"
	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/require"
)

const (
	testNodeID = "test-id"
	testLimit  = 3
)

func TestIdenticalJobs_Run(t *testing.T) {
	conf := api.DefaultConfig()
	conf.HttpClient = &http.Client{}
	check := NewIdenticalJobs(conf, logrus.New())

	httpmock.ActivateNonDefault(conf.HttpClient)
	defer httpmock.Deactivate()

	httpmock.RegisterResponder("GET", "http://127.0.0.1:4646/v1/agent/self",
		httpmock.NewJsonResponderOrPanic(200, &api.AgentSelf{
			Stats: map[string]map[string]string{
				"client": {"node_id": testNodeID},
			},
		}),
	)
	httpmock.RegisterResponder("GET", fmt.Sprintf("http://127.0.0.1:4646/v1/node/%s/allocations", testNodeID),
		httpmock.NewJsonResponderOrPanic(200, getAllocations(t)))

	check.limit = testLimit
	status, description := check.Run()
	require.Equal(t, pbCheckStatuses.Status_WARN, status)
	require.Equal(t, fmt.Sprintf("job service-run-many has %d allocations on host when limit is %d", testLimit, testLimit), description)

	check.limit = testLimit + 1
	status, description = check.Run()
	require.Equal(t, pbCheckStatuses.Status_OK, status)
	require.Equal(t, "", description)
}

func getAllocations(t *testing.T) []*api.Allocation {
	t.Helper()

	var allocations []*api.Allocation
	var typeService = "service"
	var typeBatch = "batch"

	allocations = append(allocations, &api.Allocation{
		JobID:        "service-run",
		Job:          &api.Job{Type: &typeService},
		ClientStatus: "running",
	})

	for i := 0; i < testLimit; i++ {
		allocations = append(allocations, &api.Allocation{
			JobID:        "service-run-many",
			Job:          &api.Job{Type: &typeService},
			ClientStatus: "running",
		})
		allocations = append(allocations, &api.Allocation{
			JobID:        "batch-run-many",
			Job:          &api.Job{Type: &typeBatch},
			ClientStatus: "running",
		})
	}

	return allocations
}
