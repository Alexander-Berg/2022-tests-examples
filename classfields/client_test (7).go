package shiva

import (
	"context"
	"crypto/tls"
	"github.com/YandexClassifieds/drills-helper/common"
	a "github.com/YandexClassifieds/drills-helper/pb/shiva/api/admin"
	f "github.com/YandexClassifieds/drills-helper/pb/shiva/types/flags"
	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/require"
	"golang.org/x/oauth2"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/credentials/oauth"
	"os"
	"testing"
)

func getAdministrativeServiceClient(t *testing.T) a.AdministrativeServiceClient {
	oauthPerRPC := oauth.NewOauthAccess(&oauth2.Token{
		AccessToken: viper.GetString("DH_OAUTH_TOKEN"),
	})

	conn, err := grpc.Dial(viper.GetString("SHIVA_ENDPOINT"),
		grpc.WithPerRPCCredentials(oauthPerRPC),
		grpc.WithTransportCredentials(
			credentials.NewTLS(&tls.Config{InsecureSkipVerify: true}),
		),
	)
	require.NoError(t, err)

	return a.NewAdministrativeServiceClient(conn)
}

func setFlags(t *testing.T, service a.AdministrativeServiceClient) {
	_, err := service.SetFlags(context.Background(), &f.Flags{
		List: []*f.FeatureFlag{
			{Name: "ProdSasOff", Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/"},
			{Name: "ProdYdSasOff", Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/"},
		},
	})
	require.NoError(t, err)
}

func clearFlags(t *testing.T, service a.AdministrativeServiceClient) {
	_, err := service.ClearFlags(context.Background(), &f.Flags{
		List: []*f.FeatureFlag{
			{Name: "ProdSasOff"},
			{Name: "ProdYdSasOff"},
		},
	})
	require.NoError(t, err)
}

func TestShiva_DeployDisable(t *testing.T) {
	if os.Getenv("CI") == "" {
		t.Skip("Run only in Teamcity")
	}
	test.InitTestEnv()

	service := getAdministrativeServiceClient(t)
	t.Cleanup(func() {
		clearFlags(t, service)
	})

	shiva := New()

	err := shiva.Disable(common.LayerProd, common.DCSas)
	require.NoError(t, err)

	flags, err := service.GetFlags(context.Background(), &a.GetFlagsRequest{})
	require.NoError(t, err)

	require.Contains(t, flags.List, &f.FeatureFlag{
		Name:   "ProdSasOff",
		Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/",
	})

	require.Contains(t, flags.List, &f.FeatureFlag{
		Name:   "ProdYdSasOff",
		Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/",
	})
}

func TestShiva_DeployEnable(t *testing.T) {
	if os.Getenv("CI") == "" {
		t.Skip("Run only in Teamcity")
	}
	test.InitTestEnv()

	service := getAdministrativeServiceClient(t)
	t.Cleanup(func() {
		clearFlags(t, service)
	})

	setFlags(t, service)

	shiva := New()

	err := shiva.Enable(common.LayerProd, common.DCSas)
	require.NoError(t, err)

	flags, err := service.GetFlags(context.Background(), &a.GetFlagsRequest{})
	require.NoError(t, err)

	require.NotContains(t, flags.List, &f.FeatureFlag{
		Name:   "ProdSasOff",
		Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/",
	})

	require.NotContains(t, flags.List, &f.FeatureFlag{
		Name:   "ProdYdSasOff",
		Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/",
	})
}

func TestShiva_MakeFeatureFlags(t *testing.T) {
	cases := map[string]struct {
		dc             string
		env            string
		expectedResult *f.Flags
	}{
		"prod sas": {
			dc:  common.DCSas,
			env: common.LayerProd,
			expectedResult: &f.Flags{
				List: []*f.FeatureFlag{
					{Name: "ProdSasOff", Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/"},
					{Name: "ProdYdSasOff", Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/"},
				},
			},
		},
		"prod vla": {
			dc:  common.DCVla,
			env: common.LayerProd,
			expectedResult: &f.Flags{
				List: []*f.FeatureFlag{
					{Name: "ProdVlaOff", Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/"},
					{Name: "ProdYdVlaOff", Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/"},
				},
			},
		},
		"test vla": {
			dc:  common.DCVla,
			env: common.LayerTest,
			expectedResult: &f.Flags{
				List: []*f.FeatureFlag{
					{Name: "TestVlaOff", Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/"},
					{Name: "TestYdVlaOff", Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/"},
				},
			},
		},
		"test sas": {
			dc:  common.DCSas,
			env: common.LayerTest,
			expectedResult: &f.Flags{
				List: []*f.FeatureFlag{
					{Name: "TestSasOff", Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/"},
					{Name: "TestYdSasOff", Reason: "учения https://wiki.yandex-team.ru/vertis-admin/drills/"},
				},
			},
		},
	}

	for name, tc := range cases {
		t.Run(name, func(t *testing.T) {
			flags := makeFeatureFlags(tc.env, tc.dc)
			require.ElementsMatch(t, tc.expectedResult.List, flags.List)
		})
	}
}
