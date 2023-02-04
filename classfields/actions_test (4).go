package downtime

import (
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/drills-helper/common"
	"github.com/YandexClassifieds/drills-helper/test/mocks/downtime/juggler"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestActions_SetDowntime(t *testing.T) {
	jugglerMock := &juggler.IClient{}
	jugglerMock.On("SetDowntime", mock.Anything, mock.Anything, mock.Anything).Return(nil)

	testCases := map[string]struct {
		duration                time.Duration
		datacenter              string
		layer                   string
		jugglerSetDowntimeCount int
		expectedError           error
	}{
		"sas 2h": {
			datacenter:              common.DCSas,
			layer:                   common.LayerTest,
			duration:                2 * time.Hour,
			jugglerSetDowntimeCount: 1,
		},
		"man 2h": {
			datacenter:              common.DCMan,
			layer:                   common.LayerTest,
			duration:                2 * time.Hour,
			jugglerSetDowntimeCount: 1,
		},
		"invalid dc": {
			datacenter:              "not-valid-dc",
			layer:                   common.LayerTest,
			duration:                2 * time.Hour,
			jugglerSetDowntimeCount: 0,
			expectedError:           fmt.Errorf("unknown datacenter: not-valid-dc. Allowed dc: sas/vla/man/myt"),
		},
		"invalid layer": {
			datacenter:              common.DCSas,
			layer:                   "not-valid-layer",
			duration:                2 * time.Hour,
			jugglerSetDowntimeCount: 0,
			expectedError:           fmt.Errorf("unknown layer: not-valid-layer. Allowed layers: prod/test"),
		},
	}

	for name, tc := range testCases {
		t.Run(name, func(t *testing.T) {
			actions := NewActions(jugglerMock, logrus.New())
			err := actions.SetDowntime(tc.datacenter, tc.layer, tc.duration)
			require.Equal(t, err, tc.expectedError)

			// check that only needed method called
			jugglerMock.Mock.AssertNumberOfCalls(t, "SetDowntime", tc.jugglerSetDowntimeCount)

			// clear calls count for the next test
			jugglerMock.Calls = []mock.Call{}
		})
	}
}
