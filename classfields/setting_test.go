package setting

import (
	"github.com/YandexClassifieds/shiva/pb/shiva/types/deployment"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/dtype"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/state"
	"github.com/golang/protobuf/ptypes"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestCheck(t *testing.T) {

	type testCase struct {
		name       string
		setting    *Setting
		deployment *deployment.Deployment
		expected   bool
	}

	d := &deployment.Deployment{
		Id:          "1",
		ServiceName: "service1",
		Version:     "0.0.1",
		User:        "login",
		Branch:      "",
		Type:        dtype.DeploymentType_RUN,
		Layer:       layer.Layer_PROD,
		State:       state.DeploymentState_SUCCESS,
		Start:       ptypes.TimestampNow(),
		End:         ptypes.TimestampNow(),
	}

	dBranch := &deployment.Deployment{
		Id:          "1",
		ServiceName: "service1",
		Version:     "0.0.1",
		User:        "login",
		Branch:      "branch",
		Type:        dtype.DeploymentType_RUN,
		Layer:       layer.Layer_PROD,
		State:       state.DeploymentState_SUCCESS,
		Start:       ptypes.TimestampNow(),
		End:         ptypes.TimestampNow(),
	}

	cases := []testCase{
		{
			name:       "all_allow",
			deployment: d,
			setting: &Setting{
				Branch: true,
			},
			expected: true,
		},
		{
			name:       "all_success",
			deployment: d,
			setting: &Setting{
				ServiceNames: []string{"service1"},
				DTypes:       []dtype.DeploymentType{dtype.DeploymentType_RUN},
				States:       []state.DeploymentState{state.DeploymentState_SUCCESS},
			},
			expected: true,
		},
		{
			name:       "all_fail",
			deployment: d,
			setting: &Setting{
				ServiceNames: []string{"service2"},
				DTypes:       []dtype.DeploymentType{dtype.DeploymentType_STOP},
				States:       []state.DeploymentState{state.DeploymentState_CANCEL},
			},
			expected: false,
		},
		{
			name:       "success_check_service",
			deployment: d,
			setting: &Setting{
				ServiceNames: []string{"service1"},
			},
			expected: true,
		},
		{
			name:       "success_check_services",
			deployment: d,
			setting: &Setting{
				ServiceNames: []string{"service_a", "service1", "service_b"},
			},
			expected: true,
		},
		{
			name:       "fail_check_service",
			deployment: d,
			setting: &Setting{
				ServiceNames: []string{"service2"},
			},
			expected: false,
		},
		{
			name:       "fail_check_services",
			deployment: d,
			setting: &Setting{
				ServiceNames: []string{"service_a", "service2", "service_b"},
			},
			expected: false,
		},
		{
			name:       "success_check_type",
			deployment: d,
			setting: &Setting{
				DTypes: []dtype.DeploymentType{dtype.DeploymentType_RUN},
			},
			expected: true,
		},
		{
			name:       "success_check_types",
			deployment: d,
			setting: &Setting{
				DTypes: []dtype.DeploymentType{dtype.DeploymentType_PROMOTE, dtype.DeploymentType_RUN, dtype.DeploymentType_REVERT},
			},
			expected: true,
		},
		{
			name:       "fail_check_type",
			deployment: d,
			setting: &Setting{
				DTypes: []dtype.DeploymentType{dtype.DeploymentType_STOP},
			},
			expected: false,
		},
		{
			name:       "fail_check_types",
			deployment: d,
			setting: &Setting{
				DTypes: []dtype.DeploymentType{dtype.DeploymentType_PROMOTE, dtype.DeploymentType_STOP, dtype.DeploymentType_REVERT},
			},
			expected: false,
		},
		{
			name:       "success_check_state",
			deployment: d,
			setting: &Setting{
				States: []state.DeploymentState{state.DeploymentState_SUCCESS},
			},
			expected: true,
		},
		{
			name:       "success_check_state",
			deployment: d,
			setting: &Setting{
				States: []state.DeploymentState{state.DeploymentState_REVERTED, state.DeploymentState_SUCCESS, state.DeploymentState_FAILED},
			},
			expected: true,
		},
		{
			name:       "fail_check_states",
			deployment: d,
			setting: &Setting{
				States: []state.DeploymentState{state.DeploymentState_CANCEL},
			},
			expected: false,
		},
		{
			name:       "fail_check_states",
			deployment: d,
			setting: &Setting{
				States: []state.DeploymentState{state.DeploymentState_REVERTED, state.DeploymentState_CANCEL, state.DeploymentState_FAILED},
			},
			expected: false,
		},
		{
			name:       "success_check_branch",
			deployment: dBranch,
			setting: &Setting{
				Branch: true,
			},
			expected: true,
		},
		{
			name:       "fail_check_branch",
			deployment: dBranch,
			setting: &Setting{
				Branch: false,
			},
			expected: false,
		},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			actual := c.setting.Check(c.deployment)
			assert.Equal(t, c.expected, actual)
		})
	}
}
