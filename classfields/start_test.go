package vmagent

import (
	"fmt"
	"github.com/YandexClassifieds/drills-helper/test/mocks/vmagent"
	"github.com/sirupsen/logrus"
	"github.com/spf13/cobra"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestStartCmd_run(t *testing.T) {
	testCases := map[string]struct {
		Datacenter         string
		VMAgentManageCount int
		ExpectedError      error
	}{
		"dc sas": {
			Datacenter:         "sas",
			VMAgentManageCount: 1,
		},
		"invalid DC": {
			Datacenter:    "man",
			ExpectedError: fmt.Errorf("unknown datacenter: man. Allowed dc: sas/vla"),
		},
	}

	vmagentMock := &vmagent.IVMAgent{}
	vmagentMock.On("Manage", "start", mock.Anything).Return(nil)

	cmd := NewStart(vmagentMock, logrus.StandardLogger())

	for name, tc := range testCases {
		t.Run(name, func(t *testing.T) {
			c := cobra.Command{}
			c.Flags().String("dc", tc.Datacenter, "")

			err := cmd.run(&c, []string{})
			require.Equal(t, err, tc.ExpectedError)

			vmagentMock.Mock.AssertNumberOfCalls(t, "Manage", tc.VMAgentManageCount)
			vmagentMock.Calls = []mock.Call{}
		})
	}
}
