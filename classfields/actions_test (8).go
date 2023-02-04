package _switch // revive:disable-line

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/drills-helper/common"
	"github.com/YandexClassifieds/drills-helper/test/mocks/grafana"
	s "github.com/YandexClassifieds/drills-helper/test/mocks/switch"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestActions_Switch(t *testing.T) {
	grafanaMock := &grafana.IClient{}
	grafanaMock.On("CreateAnnotation", mock.Anything, "switch", mock.Anything, mock.Anything).Return(nil)

	jenkinsMock := &s.ISystem{}
	jenkinsMock.On("Switch", mock.Anything).Return(nil)
	jenkinsMock.On("Name").Return("jenkins")

	teamcityMock := &s.ISystem{}
	teamcityMock.On("Switch", mock.Anything).Return(nil)
	teamcityMock.On("Name").Return("teamcity")

	rundeckMock := &s.ISystem{}
	rundeckMock.On("Switch", mock.Anything).Return(nil)
	rundeckMock.On("Name").Return("rundeck")

	actions := NewActions(grafanaMock, logrus.New(), jenkinsMock, teamcityMock, rundeckMock)

	testCases := map[string]struct {
		System                       string
		Datacenter                   string
		JenkinsSwitchCount           int
		TeamcitySwitchCount          int
		RundeckSwitchCount           int
		GrafanaCreateAnnotationCount int
		ExpectedError                error
	}{
		"jenkins": {
			System:                       "jenkins",
			Datacenter:                   common.DCSas,
			JenkinsSwitchCount:           1,
			GrafanaCreateAnnotationCount: 1,
		},
		"teamcity": {
			System:                       "teamcity",
			Datacenter:                   common.DCSas,
			TeamcitySwitchCount:          1,
			GrafanaCreateAnnotationCount: 1,
		},
		"rundeck": {
			System:                       "rundeck",
			Datacenter:                   common.DCSas,
			RundeckSwitchCount:           1,
			GrafanaCreateAnnotationCount: 1,
		},
		"all": {
			System:                       "all",
			Datacenter:                   common.DCSas,
			JenkinsSwitchCount:           1,
			TeamcitySwitchCount:          1,
			RundeckSwitchCount:           1,
			GrafanaCreateAnnotationCount: 3,
		},
		"invalid DC": {
			System:        "all",
			Datacenter:    "test",
			ExpectedError: fmt.Errorf("unknown datacenter: test. Allowed dc: sas/vla/myt"),
		},
		"dc man": {
			System:        "all",
			Datacenter:    common.DCMan,
			ExpectedError: fmt.Errorf("unknown datacenter: man. Allowed dc: sas/vla/myt"),
		},
	}

	for name, tc := range testCases {
		t.Run(name, func(t *testing.T) {

			err := actions.Switch(tc.System, tc.Datacenter)
			require.Equal(t, err, tc.ExpectedError)

			// check that only needed method called
			jenkinsMock.Mock.AssertNumberOfCalls(t, "Switch", tc.JenkinsSwitchCount)
			teamcityMock.Mock.AssertNumberOfCalls(t, "Switch", tc.TeamcitySwitchCount)
			rundeckMock.Mock.AssertNumberOfCalls(t, "Switch", tc.RundeckSwitchCount)
			grafanaMock.AssertNumberOfCalls(t, "CreateAnnotation", tc.GrafanaCreateAnnotationCount)

			// clear calls count for the next test
			jenkinsMock.Calls = []mock.Call{}
			teamcityMock.Calls = []mock.Call{}
			rundeckMock.Calls = []mock.Call{}
			grafanaMock.Calls = []mock.Call{}
		})
	}
}
