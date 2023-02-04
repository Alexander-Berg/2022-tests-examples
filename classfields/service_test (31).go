package secret

import (
	"fmt"
	"testing"

	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	yavCli "github.com/YandexClassifieds/shiva/pkg/yav/client"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
)

func TestCreateSecret(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	yavSvcMock := &mock.YavService{}
	yavSvcMock.On("CreateSecret", "secret-name").Return("sec-100", nil)

	s := NewService(db, log, yavSvcMock)
	secId, err := s.CreateSecret("test-svc", layer.Layer_TEST, "secret-name")
	require.NoError(t, err)
	require.Equal(t, "sec-100", secId)
	secret, err := s.storage.GetByNameAndLayer("test-svc", layer.Layer_TEST)
	require.NoError(t, err)

	require.Equal(t, "test-svc", secret.Service)
	require.Equal(t, layer.Layer_TEST, secret.Layer)
	require.Equal(t, "sec-100", secret.SecretId)
}

func TestCreateSecretTwice(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	s := NewService(db, log, nil)
	db.GormDb.Create(&Secret{
		Service:  "svc-1",
		Layer:    layer.Layer_TEST,
		SecretId: "sec-100",
	})

	_, err := s.CreateSecret("svc-1", layer.Layer_TEST, "secret-name")
	require.NoError(t, err)
}

func TestRewriteOwners(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	owners := []string{"owner1", "owner2"}

	yavSvcMock := &mock.YavService{}
	yavSvcMock.On("RewriteOwners", "sec-200", owners).Return(nil).Once()

	s := NewService(db, log, yavSvcMock)
	db.GormDb.Create(&Secret{
		Service:  "svc-1",
		Layer:    layer.Layer_TEST,
		SecretId: "sec-100",
	})
	db.GormDb.Create(&Secret{
		Service:  "svc-2",
		Layer:    layer.Layer_PROD,
		SecretId: "sec-200",
	})

	err := s.RewriteOwners("svc-2", layer.Layer_PROD, owners)
	require.NoError(t, err)

	yavSvcMock.AssertExpectations(t)
}

func TestDeleteSecret(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	yavSvcMock := &mock.YavService{}
	yavSvcMock.On("DeleteSecret", "sec-100").Return(nil).Once()

	s := NewService(db, log, yavSvcMock)
	db.GormDb.Create(&Secret{
		Service:  "svc-1",
		Layer:    layer.Layer_TEST,
		SecretId: "sec-100",
	})

	require.NoError(t, s.DeleteSecret("svc-1", layer.Layer_TEST))

	//test double delete
	require.NoError(t, s.DeleteSecret("svc-1", layer.Layer_TEST))
	yavSvcMock.AssertExpectations(t)

}

func TestGetSecrets(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	tectCases := []struct {
		name      string
		versionId string
		yavErr    error

		expectedSecretMap map[string]string
		expectError       bool
	}{
		{
			name:      "success with version",
			versionId: "ver-1",

			expectedSecretMap: map[string]string{
				"k1": "${sec-1:ver-1:k1}",
				"k2": "${sec-1:ver-1:k2}",
			},
		},
		{
			name: "success without version",

			expectedSecretMap: map[string]string{
				"k1": "${sec-1:ver-latest:k1}",
				"k2": "${sec-1:ver-latest:k2}",
			},
		},
		{
			name:      "fail with version",
			versionId: "ver-1",
			yavErr:    fmt.Errorf("some yav error"),

			expectError: true,
		},
		{
			name:   "empty secrets: no version",
			yavErr: &yavCli.ErrorResponse{Code: yavCli.ErrCodeNonExistentEntity},
		},
		{
			name:   "fail without version",
			yavErr: &yavCli.ErrorResponse{Code: yavCli.ErrCodeValidation},

			expectError: true,
		},
	}

	s := NewService(db, log, nil)
	db.GormDb.Create(&Secret{
		Service:  "svc-1",
		Layer:    layer.Layer_TEST,
		SecretId: "sec-1",
	})

	for _, tc := range tectCases {
		t.Run(tc.name, func(t *testing.T) {
			s.yavSvc = prepareYavMock(t, tc.versionId, tc.yavErr)

			secInfo, err := s.GetSecrets("svc-1", layer.Layer_TEST, tc.versionId)
			require.True(t, (err != nil) == tc.expectError)

			require.Equal(t, tc.expectedSecretMap, secInfo)
		})
	}

}

func prepareYavMock(t *testing.T, version string, yavErr error) *mock.YavService {
	yavMock := &mock.YavService{}

	if version != "" {
		if yavErr != nil {
			yavMock.On("GetSecretByVersionOrSecretId", version).Return(nil, yavErr).Once()
			return yavMock
		}
		yavMock.On("GetSecretByVersionOrSecretId", version).Return(&yavCli.VersionInfo{
			SecretId:  "sec-1",
			VersionId: version,
			Values:    map[string]string{"k1": "v1", "k2": "v2"},
		}, nil).Once()
		return yavMock
	}

	if yavErr != nil {
		yavMock.On("GetSecretByVersionOrSecretId", "sec-1").Return(nil, yavErr).Once()
		return yavMock
	}

	yavMock.On("GetSecretByVersionOrSecretId", "sec-1").Return(&yavCli.VersionInfo{
		SecretId:  "sec-1",
		VersionId: "ver-latest",
		Values:    map[string]string{"k1": "v1", "k2": "v2"},
	}, nil).Once()

	return yavMock
}

func TestExists(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	s := NewService(db, log, nil)
	db.GormDb.Create(&Secret{
		Service:  "",
		Layer:    0,
		SecretId: "sec-1",
	})

	isExist, err := s.Exists("sec-1")
	require.NoError(t, err)
	require.True(t, isExist)

	isExist, err = s.Exists("sec-2")
	require.NoError(t, err)
	require.False(t, isExist)
}
