package secret

import (
	"testing"

	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pb/ss/secret"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	mock2 "github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestService_GetEnvGetter(t *testing.T) {
	log := test.NewLogger(t)

	secretMap := map[string]string{"k1": "${sec_id-1:ver-1:k1}", "k2": "${sec_id-1:ver-1:k2"}
	ssMock := &mocks.SecretClient{}
	ssMock.On("GetSecrets", mock2.Anything, &secret.GetSecretsRequest{
		ServiceName: "svc-1",
		Layer:       layer.Layer_TEST,
	}).Return(&secret.GetSecretResponse{SecretEnvs: secretMap}, nil).Once()
	s := NewService(log, ssMock)

	getter := s.GetEnvGetter("svc-1", layer.Layer_TEST)
	require.Equal(t, "ver-1", getter.GetSecretVersion())

	envs, err := getter.GetEnvs()
	require.NoError(t, err)
	require.Equal(t, secretMap, envs)
	ssMock.AssertExpectations(t)
}

func TestService_GetRestoreEnvGetter(t *testing.T) {
	log := test.NewLogger(t)

	secretMap := map[string]string{"k1": "${sec_id-1:ver-1:k1}", "k2": "${sec_id-1:ver-1:k2}"}
	ssMock := &mocks.SecretClient{}
	ssMock.On("GetSecrets", mock2.Anything, &secret.GetSecretsRequest{
		ServiceName: "svc-1",
		Layer:       layer.Layer_TEST,
		VersionId:   "ver-1",
	}).Return(&secret.GetSecretResponse{SecretEnvs: secretMap}, nil).Once()

	s := NewService(log, ssMock)

	getter := s.GetRestoreEnvGetter("svc-1", layer.Layer_TEST, "ver-1")
	require.Equal(t, "ver-1", getter.GetSecretVersion())

	envs, err := getter.GetEnvs()
	require.NoError(t, err)
	require.Equal(t, secretMap, envs)
	ssMock.AssertExpectations(t)
}

func TestService_GetRestoreEnvWithEmptyVersion(t *testing.T) {
	log := test.NewLogger(t)

	s := NewService(log, nil)

	getter := s.GetRestoreEnvGetter("svc-1", layer.Layer_TEST, "")
	require.Equal(t, "", getter.GetSecretVersion())

	envs, err := getter.GetEnvs()
	require.NoError(t, err)
	require.Nil(t, envs)
}
