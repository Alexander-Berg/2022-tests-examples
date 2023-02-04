package hbf

import (
	"fmt"
	"github.com/YandexClassifieds/drills-helper/log"
	"github.com/YandexClassifieds/drills-helper/test/mocks/grafana"
	"github.com/YandexClassifieds/drills-helper/test/mocks/hbf"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"testing"
	"time"
)

var (
	testCasesStart = map[string]struct {
		Dc       string
		Duration time.Duration
		Issue    string

		ExpectedError                     error
		GrafanaCreateAnnotationTimedCount int
	}{
		"sas": {
			Dc:                                "sas",
			Duration:                          time.Hour,
			GrafanaCreateAnnotationTimedCount: 1,
		},
		"man": {
			Dc:                                "man",
			Duration:                          time.Hour,
			GrafanaCreateAnnotationTimedCount: 1,
		},
		"myt": {
			Dc:                                "myt",
			Duration:                          time.Hour,
			GrafanaCreateAnnotationTimedCount: 1,
		},
		"invalid": {
			Dc:            "invalid",
			Duration:      time.Hour,
			ExpectedError: fmt.Errorf("unknown datacenter: invalid. Allowed dc: sas/vla/man/myt"),
		},
		"negative duration": {
			Dc:            "sas",
			Duration:      -time.Minute,
			ExpectedError: fmt.Errorf("can't start training for negative duration"),
		},
		"issue": {
			Dc:                                "sas",
			Duration:                          time.Hour,
			Issue:                             "VERTISADMIN-111",
			GrafanaCreateAnnotationTimedCount: 1,
		},
		"issue case": {
			Dc:                                "sas",
			Duration:                          time.Hour,
			Issue:                             "vertisADMIn-111",
			GrafanaCreateAnnotationTimedCount: 1,
		},
		"issue with scheme": {
			Dc:                                "sas",
			Duration:                          time.Hour,
			Issue:                             "https://st.yandex-team.ru/VERTISADMIN-111",
			GrafanaCreateAnnotationTimedCount: 1,
		},
		"invalid-issue": {
			Dc:            "sas",
			Duration:      time.Hour,
			Issue:         "VOID-111",
			ExpectedError: fmt.Errorf("unknown issue format: VOID-111. Allowed only VERTISADMIN issues"),
		},
	}

	testCasesStop = map[string]struct {
		Dc                        string
		Duration                  time.Duration
		ExpectedError             error
		GrafanaEndAnnotationCount int
	}{
		"sas": {
			Dc:                        "sas",
			Duration:                  time.Hour,
			GrafanaEndAnnotationCount: 1,
		},
		"man": {
			Dc:                        "man",
			Duration:                  time.Hour,
			GrafanaEndAnnotationCount: 1,
		},
		"myt": {
			Dc:                        "myt",
			Duration:                  time.Hour,
			GrafanaEndAnnotationCount: 1,
		},
		"invalid": {
			Dc:            "invalid",
			Duration:      time.Hour,
			ExpectedError: fmt.Errorf("unknown datacenter: invalid. Allowed dc: sas/vla/man/myt"),
		},
	}
)

func TestActions_Start(t *testing.T) {
	hbfMock := makeHBFMock()

	grafanaMock := &grafana.IClient{}
	grafanaMock.On("CreateAnnotationTimed", mock.AnythingOfType("string"), mock.AnythingOfType("time.Time"), mock.AnythingOfType("time.Time"), "hbf", mock.Anything).Return(nil)

	actions := NewActions(hbfMock, grafanaMock, log.InitLogging())

	for name, tc := range testCasesStart {
		t.Run(name, func(t *testing.T) {
			err := actions.Start(tc.Dc, tc.Duration, tc.Issue)
			assert.Equal(t, err, tc.ExpectedError)
			grafanaMock.Mock.AssertNumberOfCalls(t, "CreateAnnotationTimed", tc.GrafanaCreateAnnotationTimedCount)

			grafanaMock.Mock.Calls = []mock.Call{}
		})
	}
}

func TestActions_Stop(t *testing.T) {
	hbfMock := makeHBFMock()

	grafanaMock := &grafana.IClient{}
	grafanaMock.On("EndAnnotation", "hbf", mock.Anything).Return(nil)

	actions := NewActions(hbfMock, grafanaMock, log.InitLogging())

	for name, tc := range testCasesStop {
		t.Run(name, func(t *testing.T) {
			err := actions.Stop(tc.Dc)
			assert.Equal(t, err, tc.ExpectedError)
			grafanaMock.Mock.AssertNumberOfCalls(t, "EndAnnotation", tc.GrafanaEndAnnotationCount)

			grafanaMock.Mock.Calls = []mock.Call{}
		})
	}
}

func makeHBFMock() *hbf.IClient {
	hbfMock := &hbf.IClient{}
	hbfMock.On("Start", mock.AnythingOfType("string"), mock.AnythingOfType("time.Duration"), mock.AnythingOfType("string")).Return(nil)
	hbfMock.On("Stop", mock.AnythingOfType("string")).Return(true, nil)

	return hbfMock
}
