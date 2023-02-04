package test

import (
	"github.com/YandexClassifieds/drills-helper/test/mocks/conductor"
	"github.com/YandexClassifieds/drills-helper/test/mocks/ssh"
	"github.com/stretchr/testify/mock"
)

func PrepareConductorMock() *conductor.IClient {
	hostList := []string{"host-01-sas.prod.vertis.yandex.net"}

	conductorMock := &conductor.IClient{}
	conductorMock.On("GroupToHosts", mock.Anything).Return(hostList, nil)
	conductorMock.On("GroupsToHosts", mock.Anything).Return(hostList, nil)
	conductorMock.On("FilterHostsByDC", mock.Anything, mock.Anything).Return(hostList)

	return conductorMock
}

func PrepareSSHMock() *ssh.IClient {
	sshMock := &ssh.IClient{}
	sshMock.On("ConcurrentRun", mock.Anything, mock.Anything).Return(nil)

	return sshMock
}
