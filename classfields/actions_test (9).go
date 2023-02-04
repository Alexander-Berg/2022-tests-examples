package walle

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/drills-helper/test/mocks/walle"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

type testCase struct {
	layer             string
	walleEnableCount  int
	walleDisableCount int
	expectedError     error
}

var (
	enableTestCases = map[string]testCase{
		"test": {
			layer:            "test",
			walleEnableCount: 1,
		},
		"prod": {
			layer:            "prod",
			walleEnableCount: 1,
		},
		"invalid layer": {
			layer:         "not-valid-layer",
			expectedError: fmt.Errorf("unknown layer: not-valid-layer. Allowed layers: prod/test"),
		},
	}
	disableTestCases = map[string]testCase{
		"test": {
			layer:             "test",
			walleDisableCount: 1,
		},
		"prod": {
			layer:             "prod",
			walleDisableCount: 1,
		},
		"invalid layer": {
			layer:         "not-valid-layer",
			expectedError: fmt.Errorf("unknown layer: not-valid-layer. Allowed layers: prod/test"),
		},
	}
)

func TestActions_Enable(t *testing.T) {
	wMock := makeWalleMock()
	actions := NewActions(logrus.New(), wMock)

	for name, tc := range enableTestCases {
		t.Run(name, func(t *testing.T) {
			err := actions.Enable(tc.layer)
			require.Equal(t, err, tc.expectedError)
			checkNumberOfCalls(t, wMock, tc)
		})
	}
}

func TestActions_Disable(t *testing.T) {
	wMock := makeWalleMock()
	actions := NewActions(logrus.New(), wMock)

	for name, tc := range disableTestCases {
		t.Run(name, func(t *testing.T) {
			err := actions.Disable(tc.layer)
			require.Equal(t, err, tc.expectedError)
			checkNumberOfCalls(t, wMock, tc)
		})
	}
}

func checkNumberOfCalls(t *testing.T, wMock *walle.IClient, tc testCase) {
	wMock.Mock.AssertNumberOfCalls(t, "Enable", tc.walleEnableCount)
	wMock.Mock.AssertNumberOfCalls(t, "Disable", tc.walleDisableCount)

	wMock.Calls = []mock.Call{}
}

func makeWalleMock() *walle.IClient {
	wMock := &walle.IClient{}
	wMock.On("Enable", mock.Anything).Return(nil)
	wMock.On("Disable", mock.Anything).Return(nil)

	return wMock
}
