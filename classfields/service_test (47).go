package abc

import (
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/pkg/abc/client/mocks"
	"github.com/YandexClassifieds/shiva/pkg/abc/domain"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

const (
	verticalsId = int64(100)
	consumerId  = int64(4321)
)

var (
	resource = &domain.Resource{ResourceID: 1234, ExternalID: "1234", ConsumerID: 0, Name: "shiva-test"}
)

func TestService_RegenerateTvmSecret(t *testing.T) {
	abcClientMock := &mocks.Client{}
	abcClientMock.On("ResourceByName", mock.Anything, mock.Anything, verticalsId, resource.Name).Return(resource, nil)
	abcClientMock.On("ConsumerID", verticalsId, resource.ExternalID).Return(consumerId, nil)
	abcClientMock.On("RecreateSecret", consumerId).Return(nil, nil)

	svc := NewService(test.NewLogger(t), Conf{VerticalsID: verticalsId}, abcClientMock)

	err := svc.RegenerateTvmSecret("shiva-test")
	require.NoError(t, err)
	abcClientMock.AssertExpectations(t)

}

func TestService_DeleteOldTvmSecret(t *testing.T) {
	abcClientMock := &mocks.Client{}
	abcClientMock.On("ResourceByName", mock.Anything, mock.Anything, verticalsId, resource.Name).Return(resource, nil)
	abcClientMock.On("ConsumerID", verticalsId, resource.ExternalID).Return(consumerId, nil)
	abcClientMock.On("DeleteOldSecret", consumerId).Return(nil, nil)

	service := NewService(test.NewLogger(t), Conf{VerticalsID: verticalsId}, abcClientMock)

	err := service.DeleteOldTvmSecret("shiva-test")
	require.NoError(t, err)
	abcClientMock.AssertExpectations(t)
}

func TestService_NewTvmResource(t *testing.T) {
	abcClientMock := &mocks.Client{}
	abcClientMock.On("ResourceByName", mock.Anything, mock.Anything, verticalsId, resource.Name).Return(nil, common.ErrNotFound)
	abcClientMock.On("NewResource", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(resource.ExternalID, nil)
	abcClientMock.On("ResourceByExternalID", mock.Anything, resource.ExternalID).Return(resource, nil)
	abcClientMock.On("ConsumerID", verticalsId, resource.ExternalID).Return(consumerId, nil)

	info := map[string]string{
		secretUuidKey:  "uuid-1",
		versionUuidKey: "ver-1",
		vaultLinkKey:   "link-1",
	}
	abcClientMock.On("MetaInfo", consumerId).Return(info, nil)

	service := NewService(test.NewLogger(t), Conf{VerticalsID: verticalsId}, abcClientMock)

	res, err := service.NewTvmResource("shiva-test")
	require.NoError(t, err)
	require.NotNil(t, res)
}
