package app

import (
	"testing"

	proto "github.com/YandexClassifieds/shiva/pb/ss/tokens"
	"github.com/YandexClassifieds/shiva/pkg/yav/client"
	mock2 "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/mock/mockery/mocks"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestApp_DelegateToken(t *testing.T) {
	viper.Set("ss_tvm_id", 42)
	viper.Set("yd_tvm_id", 44)
	viper.Set("app_version", "0.6.0")
	mockedYav := new(mock2.YavService)
	mockedDelegation := new(mocks.SSTokenClient)
	a := App{
		ssCli:  mockedDelegation,
		yavSvc: mockedYav,
	}
	a.InitCommands()
	a.initOnce.Do(func() {})

	tokenResp := &client.CreatedTokenInfo{TokenID: "new-token-id", Token: "new-token"}
	mockedYav.On("CreateDelegationToken", "some-secret", "dt-service", uint(42)).Return(tokenResp, nil)
	mockedYav.On("CreateDelegationToken", "some-secret", "", uint(44)).Return(tokenResp, nil)
	mockedYav.On("ListDelegationTokens", "some-secret").Return([]client.TokenInfo{
		{TokenID: "old-token", Signature: "dt-service", TvmClientId: 42},
		{TokenID: "old-token", Signature: "other-service", TvmClientId: 42},
		{TokenID: "other-token", Signature: "dt-service", TvmClientId: 43},
	}, nil)
	mockedYav.On("RevokeDelegationToken", "old-token").Return(nil)

	expectedToken := &proto.TokenData{
		ServiceName: "dt-service",
		SecretId:    "some-secret",
		TokenId:     "new-token-id",
		Token:       "new-token",
		TvmId:       42,
	}
	et2 := &proto.TokenData{
		ServiceName: "dt-service",
		SecretId:    "some-secret",
		TokenId:     "new-token-id",
		Token:       "new-token",
		TvmId:       44,
	}
	mockedDelegation.On("AddToken", mock.Anything, expectedToken).Return(&proto.AddTokenResponse{}, nil)
	mockedDelegation.On("AddToken", mock.Anything, et2).Return(&proto.AddTokenResponse{}, nil)
	mockedDelegation.On("CheckAllowed", mock.Anything, &proto.CheckAllowedRequest{SecretId: "some-secret"}).
		Return(&proto.CheckAllowedResponse{IsAllowed: true}, nil)
	mockedDelegation.On("GetRequiredVersion", mock.Anything, mock.Anything).
		Return(&proto.GetRequiredVersionResponse{Version: "0.6.0"}, nil)
	a.rootCmd.SetArgs([]string{"--oauth-token", "wtf", "delegate", "--secret-id", "some-secret", "dt-service"})
	err := a.Exec()

	require.NoError(t, err)
	mockedYav.AssertExpectations(t)
	mockedDelegation.AssertExpectations(t)
}

func TestApp_NotAllowed(t *testing.T) {
	viper.Set("ss_tvm_id", 42)
	viper.Set("yd_tvm_id", 44)
	viper.Set("app_version", "0.6.0")
	mockedDelegation := new(mocks.SSTokenClient)
	a := App{
		ssCli:  mockedDelegation,
		yavSvc: nil,
	}
	a.InitCommands()
	a.initOnce.Do(func() {})

	mockedDelegation.On("CheckAllowed", mock.Anything, mock.Anything).
		Return(&proto.CheckAllowedResponse{IsAllowed: false}, nil).Once()
	mockedDelegation.On("GetRequiredVersion", mock.Anything, mock.Anything).
		Return(&proto.GetRequiredVersionResponse{Version: "0.6.0"}, nil)
	a.rootCmd.SetArgs([]string{"--oauth-token", "wtf", "delegate", "--secret-id", "some-secret", "dt-service"})
	err := a.Exec()

	require.Error(t, err)
	mockedDelegation.AssertExpectations(t)
}
func TestApp_CheckVersion(t *testing.T) {
	viper.Set("app_version", "0.6.0")
	a := App{}
	a.InitCommands()
	a.initOnce.Do(func() {})

	testCases := []struct {
		name            string
		requiredVersion string
		expectedErr     error
	}{
		{
			name:            "version supported",
			requiredVersion: "0.6.0",
			expectedErr:     nil,
		},
		{
			name:            "version not supported",
			requiredVersion: "1.0.4",
			expectedErr:     notSupportedVersionError,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			mockedDelegation := new(mocks.SSTokenClient)
			mockedDelegation.On("GetRequiredVersion", mock.Anything, mock.Anything).
				Return(&proto.GetRequiredVersionResponse{Version: tc.requiredVersion}, nil)
			a.ssCli = mockedDelegation
			err := a.checkVersion()
			require.Equal(t, tc.expectedErr, err)
		})
	}
}
