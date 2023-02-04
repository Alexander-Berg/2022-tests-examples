package secrets

import (
	"errors"
	"net/url"
	"testing"

	"github.com/YandexClassifieds/shiva/common/user_error"
	error1 "github.com/YandexClassifieds/shiva/pb/shiva/types/error"
	proto "github.com/YandexClassifieds/shiva/pb/ss/access"
	"github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/secrets"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

func TestService_makeSecrets(t *testing.T) {
	tokenMock := &secrets.MockTokenClient{}
	svc := &Service{
		cli: tokenMock,
		conf: Conf{
			SecretsBaseUrl: "http://secrets.example.vertis.yandex.net",
		},
		log: test.NewLogger(t),
	}
	c := model.NewConfig()
	c.Params = map[string]string{
		"X":                 "true",
		"SOMESTR":           "wtf",
		"SECSTUFF":          "${sec-one:ver-onetwo:some-key}",
		"E2":                "${sec-two:ver-two:some-other-key}",
		"DEPRECATED_SECRET": "${sec-three:ver-three:key},",
	}
	svcManifest := model.Manifest{
		Name:   "test-service",
		Config: c,
	}
	expectedReq := &proto.NewTokenRequest{
		ServiceName: "test-service",
		Env: []*proto.EnvSecret{
			{EnvKey: "DEPRECATED_SECRET", SecretId: "sec-three", VersionId: "ver-three", SecretKey: "key"},
			{EnvKey: "E2", SecretId: "sec-two", VersionId: "ver-two", SecretKey: "some-other-key"},
			{EnvKey: "SECSTUFF", SecretId: "sec-one", VersionId: "ver-onetwo", SecretKey: "some-key"},
		},
	}
	tokenMock.On("CheckToken", mock.Anything, &proto.CheckTokenRequest{
		ServiceName:  "test-service",
		SecretId:     "sec-one",
		VersionId:    "ver-onetwo",
		RequiredKeys: []string{"some-key"},
	}).Return(&proto.CheckTokenResponse{IsDelegated: true}, nil)
	tokenMock.On("CheckToken", mock.Anything, &proto.CheckTokenRequest{
		ServiceName:  "test-service",
		SecretId:     "sec-two",
		VersionId:    "ver-two",
		RequiredKeys: []string{"some-other-key"},
	}).Return(&proto.CheckTokenResponse{IsDelegated: true}, nil)
	tokenMock.On("CheckToken", mock.Anything, &proto.CheckTokenRequest{
		ServiceName:  "test-service",
		SecretId:     "sec-three",
		VersionId:    "ver-three",
		RequiredKeys: []string{"key"},
	}).Return(&proto.CheckTokenResponse{IsDelegated: true}, nil)
	tokenMock.On("NewToken", mock.AnythingOfType("*context.timerCtx"), expectedReq).Return(&proto.NewTokenResponse{Token: "test-token"}, nil)

	envs, err := svcManifest.Config.GetEnvs()
	require.NoError(t, err)

	secrets, err := parseEnvs(envs)
	require.NoError(t, err)

	urlStr, err := svc.MakeSecretsArtifactURL(svcManifest.Name, secrets)
	test.Check(t, err)
	assert.NotEmpty(t, urlStr)
	u, err := url.Parse(urlStr)
	test.Check(t, err)
	assert.Equal(t, "secrets.example.vertis.yandex.net", u.Host)
	assert.Equal(t, "/s/test-token", u.Path)
}

func TestService_ValidateSecrets(t *testing.T) {
	const (
		envValue = "${sec-1:ver-1:key1}"
	)

	svc := &Service{
		conf: Conf{
			SecretsBaseUrl: "http://secrets.example.vertis.yandex.net",
		},
		log: test.NewLogger(t),
	}

	t.Run("Validation success", func(t *testing.T) {
		tockenMock := &secrets.MockTokenClient{}
		svc.cli = tockenMock
		tockenMock.On("CheckToken", mock.Anything, mock.Anything).Return(&proto.CheckTokenResponse{}, nil)
		err := svc.ValidateSecret("", envValue, 0)
		require.NoError(t, err)
	})

	t.Run("Validation error", func(t *testing.T) {
		tockenMock := &secrets.MockTokenClient{}
		svc.cli = tockenMock
		tockenMock.On("CheckToken", mock.Anything, mock.Anything).Return(&proto.CheckTokenResponse{Error: &error1.UserError{}}, nil)
		err := svc.ValidateSecret("", envValue, 0)
		userErrors := new(user_error.UserErrors)
		assert.Equal(t, errors.As(err, &userErrors), true)
	})

	t.Run("Parser error", func(t *testing.T) {
		tockenMock := &secrets.MockTokenClient{}
		svc.cli = tockenMock
		tockenMock.On("CheckToken", mock.Anything, mock.Anything).Return(&proto.CheckTokenResponse{}, nil)

		cases := []string{
			"${sec-1:ver-2}",
			"${sec-}",
			"${sec-1:ver-2:}",
			"${sec-1:ver:12}",
			"${sec-1:ver-4:12",
			"${sec-1:sec-2:12}",
			"${sec-1:vEr-2:12}",
		}

		for _, c := range cases {
			err := svc.ValidateSecret("key", c, 0)

			var userError *user_error.UserError
			require.True(t, errors.As(err, &userError))

			assert.Equal(t, *ErrParseSecret(c), *userError)
		}
	})

	t.Run("Parse not secrets", func(t *testing.T) {
		tockenMock := &secrets.MockTokenClient{}
		svc.cli = tockenMock
		tockenMock.On("CheckToken", mock.Anything, mock.Anything).Return(&proto.CheckTokenResponse{}, nil)

		cases := []string{
			"123",
			"",
			"{node.unique}",
			"{sec-1:ver-2:KEY}",
		}

		for _, c := range cases {
			err := svc.ValidateSecret("key", c, 0)

			assert.NoError(t, err)
		}
	})
}
