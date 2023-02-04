package hbf

import (
	"testing"

	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestActions_Open(t *testing.T) {
	conductorMock := test.PrepareConductorMock()
	sshMock := test.PrepareSSHMock()

	actions := NewActions(logrus.New(), conductorMock, sshMock)
	require.NoError(t, actions.Open("sas"))

	conductorMock.Mock.AssertNumberOfCalls(t, "GroupToHosts", 1)
	conductorMock.Mock.AssertNumberOfCalls(t, "FilterHostsByDC", 1)
	sshMock.Mock.AssertNumberOfCalls(t, "ConcurrentRun", 1)
	sshMock.Mock.AssertCalled(t, "ConcurrentRun", mock.AnythingOfType("[]string"), openCmd)
}

func TestActions_Close(t *testing.T) {
	conductorMock := test.PrepareConductorMock()
	sshMock := test.PrepareSSHMock()

	actions := NewActions(logrus.New(), conductorMock, sshMock)
	require.NoError(t, actions.Close("sas"))

	conductorMock.Mock.AssertNumberOfCalls(t, "GroupToHosts", 1)
	conductorMock.Mock.AssertNumberOfCalls(t, "FilterHostsByDC", 1)
	sshMock.Mock.AssertNumberOfCalls(t, "ConcurrentRun", 1)
	sshMock.Mock.AssertCalled(t, "ConcurrentRun", mock.AnythingOfType("[]string"), closeCmd)
}
