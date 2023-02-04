package deploy

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/drills-helper/common"
	"github.com/YandexClassifieds/drills-helper/test/mocks/deploy"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

type testCase struct {
	systemName                  string
	datacenter                  string
	layer                       string
	conductorEnableCount        int
	conductorDisableCount       int
	jenkinsEnableCount          int
	jenkinsDisableCount         int
	shivaEnableCount            int
	shivaDisableCount           int
	grafanaStartAnnotationCount int
	grafanaEndAnnotationCount   int
	expectedError               error
}

var (
	enableTestCases = map[string]testCase{
		"conductorEnable": {
			systemName:                "conductor",
			datacenter:                common.DCSas,
			layer:                     common.LayerProd,
			conductorEnableCount:      1,
			grafanaEndAnnotationCount: 1,
		},
		"jenkinsEnable": {
			systemName:                "jenkins",
			datacenter:                common.DCSas,
			layer:                     common.LayerProd,
			jenkinsEnableCount:        1,
			grafanaEndAnnotationCount: 1,
		},
		"shivaEnable": {
			systemName:                "shiva",
			datacenter:                common.DCSas,
			layer:                     common.LayerProd,
			shivaEnableCount:          1,
			grafanaEndAnnotationCount: 1,
		},
		"allEnable": {
			systemName:                "all",
			datacenter:                common.DCSas,
			layer:                     common.LayerProd,
			conductorEnableCount:      1,
			jenkinsEnableCount:        1,
			shivaEnableCount:          1,
			grafanaEndAnnotationCount: 3,
		},
		"invalid DC": {
			systemName:    "all",
			datacenter:    "test",
			layer:         common.LayerProd,
			expectedError: fmt.Errorf("unknown datacenter: test. Allowed dc: sas/vla"),
		},
		"dc man": {
			systemName:    "all",
			datacenter:    common.DCMan,
			layer:         common.LayerProd,
			expectedError: fmt.Errorf("unknown datacenter: man. Allowed dc: sas/vla"),
		},
		"invalid Layer": {
			systemName:    "all",
			datacenter:    common.DCSas,
			layer:         "not valid",
			expectedError: fmt.Errorf("unknown layer: not valid. Allowed layers: prod/test"),
		},
	}
	disableTestCases = map[string]testCase{
		"conductorDisable": {
			systemName:                  "conductor",
			datacenter:                  common.DCSas,
			layer:                       common.LayerProd,
			conductorDisableCount:       1,
			grafanaStartAnnotationCount: 1,
		},
		"jenkinsDisable": {
			systemName:                  "jenkins",
			datacenter:                  common.DCSas,
			layer:                       common.LayerProd,
			jenkinsDisableCount:         1,
			grafanaStartAnnotationCount: 1,
		},
		"shivaDisable": {
			systemName:                  "shiva",
			datacenter:                  common.DCSas,
			layer:                       common.LayerProd,
			shivaDisableCount:           1,
			grafanaStartAnnotationCount: 1,
		},
		"allDisable": {
			systemName:                  "all",
			datacenter:                  common.DCSas,
			layer:                       common.LayerProd,
			conductorDisableCount:       1,
			jenkinsDisableCount:         1,
			shivaDisableCount:           1,
			grafanaStartAnnotationCount: 3,
		},
		"invalid DC": {
			systemName:    "all",
			datacenter:    "test",
			layer:         common.LayerProd,
			expectedError: fmt.Errorf("unknown datacenter: test. Allowed dc: sas/vla"),
		},
		"dc man": {
			systemName:    "all",
			datacenter:    common.DCMan,
			layer:         common.LayerProd,
			expectedError: fmt.Errorf("unknown datacenter: man. Allowed dc: sas/vla"),
		},
		"invalid Layer": {
			systemName:    "all",
			datacenter:    common.DCSas,
			layer:         "not valid",
			expectedError: fmt.Errorf("unknown layer: not valid. Allowed layers: prod/test"),
		},
	}
)

func TestActions_Enable(t *testing.T) {
	conductorMock := makeDeployMock("conductor")
	jenkinsMock := makeDeployMock("jenkins")
	shivaMock := makeDeployMock("shiva")

	actions := NewActions(logrus.New(), jenkinsMock, shivaMock, conductorMock)

	for name, tc := range enableTestCases {
		t.Run(name, func(t *testing.T) {
			err := actions.Enable(tc.systemName, tc.layer, tc.datacenter)
			require.Equal(t, err, tc.expectedError)
			checkNumberOfCalls(t, conductorMock, jenkinsMock, shivaMock, tc)
		})
	}
}

func TestActions_Disable(t *testing.T) {
	conductorMock := makeDeployMock("conductor")
	jenkinsMock := makeDeployMock("jenkins")
	shivaMock := makeDeployMock("shiva")

	actions := NewActions(logrus.New(), jenkinsMock, shivaMock, conductorMock)

	for name, tc := range disableTestCases {
		t.Run(name, func(t *testing.T) {
			err := actions.Disable(tc.systemName, tc.layer, tc.datacenter)
			require.Equal(t, err, tc.expectedError)
			checkNumberOfCalls(t, conductorMock, jenkinsMock, shivaMock, tc)
		})
	}
}

func makeDeployMock(name string) *deploy.ISystem {
	deloyMock := &deploy.ISystem{}
	deloyMock.On("Enable", mock.Anything, mock.Anything).Return(nil)
	deloyMock.On("Disable", mock.Anything, mock.Anything).Return(nil)
	deloyMock.On("Name").Return(name)

	return deloyMock
}

func checkNumberOfCalls(t *testing.T, conductorMock, jenkinsMock, shivaMock *deploy.ISystem, tc testCase) {
	conductorMock.Mock.AssertNumberOfCalls(t, "Enable", tc.conductorEnableCount)
	conductorMock.Mock.AssertNumberOfCalls(t, "Disable", tc.conductorDisableCount)
	jenkinsMock.Mock.AssertNumberOfCalls(t, "Enable", tc.jenkinsEnableCount)
	jenkinsMock.Mock.AssertNumberOfCalls(t, "Disable", tc.jenkinsDisableCount)
	shivaMock.Mock.AssertNumberOfCalls(t, "Enable", tc.shivaEnableCount)
	shivaMock.Mock.AssertNumberOfCalls(t, "Disable", tc.shivaDisableCount)

	// clear calls count for the next test
	conductorMock.Calls = []mock.Call{}
	jenkinsMock.Calls = []mock.Call{}
	shivaMock.Calls = []mock.Call{}
}
