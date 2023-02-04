package lb

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/drills-helper/common"
	"github.com/YandexClassifieds/drills-helper/test/mocks/grafana"
	"github.com/YandexClassifieds/drills-helper/test/mocks/lb"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

type testCase struct {
	system                       string
	dc                           string
	ingressOpenCount             int
	ingressCloseCount            int
	upstreamOpenCount            int
	upstreamCloseCount           int
	lbIntOpenCount               int
	lbIntCloseCount              int
	lbIntNginxOpenCount          int
	lbIntNginxCloseCount         int
	grafanaStartAnnotationCount  int
	grafanaCreateAnnotationCount int
	grafanaEndAnnotationCount    int
	expectedError                error
}

var (
	openTestCases = map[string]testCase{
		"ingress": {
			system:                    "ingress",
			dc:                        "sas",
			ingressOpenCount:          1,
			grafanaEndAnnotationCount: 1,
		},
		"upstream": {
			system:                       "upstream",
			dc:                           "sas",
			upstreamOpenCount:            1,
			grafanaCreateAnnotationCount: 1,
		},
		"lbInt": {
			system:                       "lb-int",
			dc:                           "sas",
			lbIntOpenCount:               1,
			grafanaCreateAnnotationCount: 1,
		},
		"lbIntNginx": {
			system:                       "lb-int-nginx",
			dc:                           "sas",
			lbIntNginxOpenCount:          1,
			grafanaCreateAnnotationCount: 1,
		},
		"invalid DC": {
			system:        "all",
			dc:            "test",
			expectedError: fmt.Errorf("unknown datacenter: test. Allowed dc: sas/vla"),
		},
		"dc man": {
			system:        "all",
			dc:            common.DCMan,
			expectedError: fmt.Errorf("unknown datacenter: man. Allowed dc: sas/vla"),
		},
	}

	closeTestCases = map[string]testCase{
		"ingress": {
			system:                      "ingress",
			dc:                          "sas",
			ingressCloseCount:           1,
			grafanaStartAnnotationCount: 1,
		},
		"upstream": {
			system:                       "upstream",
			dc:                           "sas",
			upstreamCloseCount:           1,
			grafanaCreateAnnotationCount: 1,
		},
		"lbInt": {
			system:                       "lb-int",
			dc:                           "sas",
			lbIntCloseCount:              1,
			grafanaCreateAnnotationCount: 1,
		},
		"lbIntNginx": {
			system:                       "lb-int-nginx",
			dc:                           "sas",
			lbIntNginxCloseCount:         1,
			grafanaCreateAnnotationCount: 1,
		},
		"invalid DC": {
			system:        "all",
			dc:            "test",
			expectedError: fmt.Errorf("unknown datacenter: test. Allowed dc: sas/vla"),
		},
		"dc man": {
			system:        "all",
			dc:            common.DCMan,
			expectedError: fmt.Errorf("unknown datacenter: man. Allowed dc: sas/vla"),
		},
	}
)

func TestActions_Open(t *testing.T) {
	ingressMock := makeLBMock("ingress")
	upstreamMock := makeLBMock("upstream")
	lbIntMock := makeLBMock("lb-int")
	lbIntNginxMock := makeLBMock("lb-int-nginx")
	grafanaMock := makeGrafanaMock()

	actions := NewActions(grafanaMock, logrus.New(), ingressMock, upstreamMock, lbIntMock, lbIntNginxMock)

	for name, tc := range openTestCases {
		t.Run(name, func(t *testing.T) {
			err := actions.Open(tc.system, tc.dc)
			require.Equal(t, err, tc.expectedError)
			checkNumberOfCalls(t, grafanaMock, ingressMock, upstreamMock, lbIntMock, lbIntNginxMock, tc)
		})
	}
}

func TestActions_Close(t *testing.T) {
	ingressMock := makeLBMock("ingress")
	upstreamMock := makeLBMock("upstream")
	lbIntMock := makeLBMock("lb-int")
	lbIntNginxMock := makeLBMock("lb-int-nginx")
	grafanaMock := makeGrafanaMock()

	actions := NewActions(grafanaMock, logrus.New(), ingressMock, upstreamMock, lbIntMock, lbIntNginxMock)

	for name, tc := range closeTestCases {
		t.Run(name, func(t *testing.T) {
			err := actions.Close(tc.system, tc.dc)
			require.Equal(t, err, tc.expectedError)
			checkNumberOfCalls(t, grafanaMock, ingressMock, upstreamMock, lbIntMock, lbIntNginxMock, tc)
		})
	}
}

func makeLBMock(name string) *lb.ISystem {
	lbMock := &lb.ISystem{}
	lbMock.On("Open", mock.Anything).Return(nil)
	lbMock.On("Close", mock.Anything).Return(nil)
	lbMock.On("Name").Return(name)

	return lbMock
}

func makeGrafanaMock() *grafana.IClient {
	grafanaMock := &grafana.IClient{}
	grafanaMock.On("StartAnnotation", mock.Anything, "lb", mock.Anything, mock.Anything).Return(nil)
	grafanaMock.On("CreateAnnotation", mock.Anything, "lb", mock.Anything, mock.Anything).Return(nil)
	grafanaMock.On("EndAnnotation", "lb", mock.Anything, mock.Anything).Return(nil)

	return grafanaMock
}

func checkNumberOfCalls(t *testing.T, grafanaMock *grafana.IClient, ingressMock, upstreamMock, lbIntMock, lbIntNginxMock *lb.ISystem, tc testCase) {
	ingressMock.Mock.AssertNumberOfCalls(t, "Open", tc.ingressOpenCount)
	ingressMock.Mock.AssertNumberOfCalls(t, "Close", tc.ingressCloseCount)

	upstreamMock.Mock.AssertNumberOfCalls(t, "Open", tc.upstreamOpenCount)
	upstreamMock.Mock.AssertNumberOfCalls(t, "Close", tc.upstreamCloseCount)

	lbIntMock.Mock.AssertNumberOfCalls(t, "Open", tc.lbIntOpenCount)
	lbIntMock.Mock.AssertNumberOfCalls(t, "Close", tc.lbIntCloseCount)

	lbIntNginxMock.Mock.AssertNumberOfCalls(t, "Open", tc.lbIntNginxOpenCount)
	lbIntNginxMock.Mock.AssertNumberOfCalls(t, "Close", tc.lbIntNginxCloseCount)

	grafanaMock.AssertNumberOfCalls(t, "StartAnnotation", tc.grafanaStartAnnotationCount)
	grafanaMock.AssertNumberOfCalls(t, "CreateAnnotation", tc.grafanaCreateAnnotationCount)
	grafanaMock.AssertNumberOfCalls(t, "EndAnnotation", tc.grafanaEndAnnotationCount)

	// clear calls count for the next test
	ingressMock.Calls = []mock.Call{}
	upstreamMock.Calls = []mock.Call{}
	lbIntMock.Calls = []mock.Call{}
	lbIntNginxMock.Calls = []mock.Call{}
	grafanaMock.Calls = []mock.Call{}
}
