package vmagent

import (
	"github.com/YandexClassifieds/drills-helper/common"
	"github.com/YandexClassifieds/drills-helper/test/mocks/conductor"
	"github.com/YandexClassifieds/drills-helper/test/mocks/ssh"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestVMAgent_Manage(t *testing.T) {
	testCases := map[string]struct {
		Datacenter                 string
		Action                     string
		ConductorGroupToHostsCount int
		SSHConnectCount            int
		SSHRunCount                int
		ExpectedError              error
	}{
		"start": {
			Datacenter:                 common.DCSas,
			Action:                     "start",
			ConductorGroupToHostsCount: 1,
			SSHConnectCount:            1,
			SSHRunCount:                1,
		},
		"stop": {
			Datacenter:                 common.DCSas,
			Action:                     "stop",
			ConductorGroupToHostsCount: 1,
			SSHConnectCount:            1,
			SSHRunCount:                1,
		},
	}

	conductorMock := &conductor.IClient{}
	conductorMock.On("GroupToHosts", mock.Anything).
		Return([]string{"host-01-sas.prod.vertis.yandex.net", "host-01-vla.prod.vertis.yandex.net"}, nil)

	sshMock := &ssh.IClient{}
	sshMock.On("Connect", mock.Anything).Return(nil, nil)
	sshMock.On("Run", mock.Anything, mock.Anything).
		Return("", nil)

	vmagent := NewVMAgent(conductorMock, sshMock)

	for name, tc := range testCases {
		t.Run(name, func(t *testing.T) {
			err := vmagent.Manage(tc.Action, tc.Datacenter)
			require.Equal(t, err, tc.ExpectedError)

			conductorMock.Mock.AssertNumberOfCalls(t, "GroupToHosts", tc.ConductorGroupToHostsCount)
			sshMock.Mock.AssertNumberOfCalls(t, "Connect", tc.SSHConnectCount)
			sshMock.Mock.AssertNumberOfCalls(t, "Run", tc.SSHRunCount)

			conductorMock.Calls = []mock.Call{}
			sshMock.Calls = []mock.Call{}
		})
	}
}
