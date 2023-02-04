package cms

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/drills-helper/common"
	"github.com/YandexClassifieds/drills-helper/test/mocks/cms"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

type testCase struct {
	dc              string
	layer           string
	cmsEnableCount  int
	cmsDisableCount int
	expectedError   error
}

var (
	defaultDC = common.DCSas

	enableTestCases = map[string]testCase{
		"test": {
			dc:             defaultDC,
			layer:          common.LayerTest,
			cmsEnableCount: 1,
		},
		"prod": {
			dc:             defaultDC,
			layer:          common.LayerProd,
			cmsEnableCount: 1,
		},
		"invalid layer": {
			dc:            defaultDC,
			layer:         "not-valid-layer",
			expectedError: fmt.Errorf("unknown layer: not-valid-layer. Allowed layers: prod/test"),
		},
		"invalid datacenter": {
			dc:            "not-valid-dc",
			expectedError: fmt.Errorf("unknown datacenter: not-valid-dc. Allowed dc: sas/vla"),
		},
	}
	disableTestCases = map[string]testCase{
		"test": {
			dc:              defaultDC,
			layer:           common.LayerTest,
			cmsDisableCount: 1,
		},
		"prod": {
			dc:              defaultDC,
			layer:           common.LayerProd,
			cmsDisableCount: 1,
		},
		"invalid layer": {
			dc:            defaultDC,
			layer:         "not-valid-layer",
			expectedError: fmt.Errorf("unknown layer: not-valid-layer. Allowed layers: prod/test"),
		},
		"invalid datacenter": {
			dc:            "not-valid-dc",
			expectedError: fmt.Errorf("unknown datacenter: not-valid-dc. Allowed dc: sas/vla"),
		},
	}
)

func TestActions_Enable(t *testing.T) {
	cmsMock := makeCMSMock()
	actions := NewActions(logrus.New(), cmsMock)

	for name, tc := range enableTestCases {
		t.Run(name, func(t *testing.T) {
			err := actions.Enable(tc.layer, tc.dc)
			require.Equal(t, err, tc.expectedError)
			checkNumberOfCalls(t, cmsMock, tc)
		})
	}
}

func TestActions_Disable(t *testing.T) {
	cmsMock := makeCMSMock()
	actions := NewActions(logrus.New(), cmsMock)

	for name, tc := range disableTestCases {
		t.Run(name, func(t *testing.T) {
			err := actions.Disable(tc.layer, tc.dc)
			require.Equal(t, err, tc.expectedError)
			checkNumberOfCalls(t, cmsMock, tc)
		})
	}
}

func checkNumberOfCalls(t *testing.T, cmsMock *cms.IClient, tc testCase) {
	cmsMock.Mock.AssertNumberOfCalls(t, "Enable", tc.cmsEnableCount)
	cmsMock.Mock.AssertNumberOfCalls(t, "Disable", tc.cmsDisableCount)

	cmsMock.Calls = []mock.Call{}
}

func makeCMSMock() *cms.IClient {
	cmsMock := &cms.IClient{}
	cmsMock.On("Enable", mock.Anything, mock.Anything).Return(nil)
	cmsMock.On("Disable", mock.Anything, mock.Anything).Return(nil)

	return cmsMock
}
