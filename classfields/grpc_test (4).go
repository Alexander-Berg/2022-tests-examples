package delegation

import (
	"context"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/secret-service/secret"
	delegation2 "github.com/YandexClassifieds/shiva/cmd/secret-service/store/delegation"
	sm "github.com/YandexClassifieds/shiva/pb/shiva/api/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	proto "github.com/YandexClassifieds/shiva/pb/ss/tokens"
	"github.com/YandexClassifieds/shiva/pkg/auth/blackbox"
	"github.com/YandexClassifieds/shiva/pkg/auth/oauth/grpc"
	"github.com/YandexClassifieds/shiva/test"
	mock2 "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestGRPCHandler_AddToken(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	yavMock := new(mock2.YavService)
	mapMock := new(mock2.ServiceMapsClient)
	h := &GRPCHandler{
		dtStore:   delegation2.NewStorage(db, log),
		mapCli:    mapMock,
		secretSvc: secret.NewService(db, log, nil),
		yavCli:    yavMock,
		log:       log,
	}
	ctx := grpc.UserInfoContext(context.Background(), &blackbox.UserInfo{Login: "test-user", UID: 1224})
	yavMock.On("CanRead", "some-id", "1224").Return(true, nil)
	expectedOwnerRequest := &sm.GetRequest{Service: "my-service"}
	mapMock.On("Get", mock.Anything, expectedOwnerRequest).Return(&sm.ServiceData{Service: nil}, nil)
	_, err := h.AddToken(ctx, &proto.TokenData{
		ServiceName: "my-service",
		SecretId:    "some-id",
		Token:       "the-token",
		TokenId:     "token-id",
		TvmId:       42,
	})
	require.NoError(t, err)
	yavMock.AssertExpectations(t)
	mapMock.AssertExpectations(t)

	result, err := h.dtStore.GetBySecret("my-service", "some-id", 42)
	require.NoError(t, err)
	require.Equal(t, "token-id", result.TokenId)
	require.Equal(t, "the-token", result.Token)
}

func TestGRPCHandler_RevokeToken(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	yavMock := new(mock2.YavService)
	mapMock := new(mock2.ServiceMapsClient)
	h := &GRPCHandler{
		mapCli:    mapMock,
		secretSvc: secret.NewService(db, log, nil),
		yavCli:    yavMock,
		dtStore:   delegation2.NewStorage(db, log),
		log:       log,
	}
	err := h.dtStore.Save(&delegation2.Token{ServiceName: "service-to-revoke", SecretId: "secret-to-delete"})
	require.NoError(t, err)

	ctx := grpc.UserInfoContext(context.Background(), &blackbox.UserInfo{Login: "test-user", UID: 42})

	expectedReq := &sm.GetRequest{Service: "service-to-revoke"}
	mapMock.On("Get", mock.Anything, expectedReq).Return(&sm.ServiceData{Service: nil}, nil)
	yavMock.On("CanRead", "secret-to-delete", "42").Return(true, nil)

	_, err = h.RevokeToken(ctx, &proto.RevokeTokenRequest{ServiceName: "service-to-revoke", SecretId: "secret-to-delete"})
	require.NoError(t, err)
}

func TestGRPCHandler_Validate(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	yavMock := new(mock2.YavService)
	mapMock := new(mock2.ServiceMapsClient)

	yavMock.On("CanRead", "some-id", "1224").Return(true, nil)
	expectedOwnerRequest := &sm.GetRequest{Service: "my-service"}
	mapMock.On("Get", mock.Anything, expectedOwnerRequest).Return(&sm.ServiceData{Service: nil}, nil)

	h := &GRPCHandler{
		dtStore:   delegation2.NewStorage(db, log),
		mapCli:    mapMock,
		secretSvc: secret.NewService(db, log, nil),
		yavCli:    yavMock,
		log:       log,
		conf:      newConf(),
	}
	db.GormDb.Create(&secret.Secret{
		Service:  "my-service",
		Layer:    layer.Layer_TEST,
		SecretId: "some-id",
	})

	testCases := []struct {
		name          string
		login         string
		expectedError error
	}{
		{
			name:          "fail validate",
			login:         "user1",
			expectedError: errNotAllowed,
		},
		{
			name:          "success validate",
			login:         "robot-vertis-shiva",
			expectedError: nil,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			ctx := grpc.UserInfoContext(context.Background(), &blackbox.UserInfo{Login: tc.login, UID: 1224})

			err := h.validateRequest(ctx, &proto.TokenData{
				ServiceName: "my-service",
				SecretId:    "some-id",
				Token:       "the-token",
				TokenId:     "token-id",
				TvmId:       42,
			})

			require.Equal(t, tc.expectedError, err)
		})
	}
}
