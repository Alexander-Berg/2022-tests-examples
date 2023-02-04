package artifact

import (
	"bufio"
	"bytes"
	"io"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"testing"

	auth2 "github.com/YandexClassifieds/shiva/cmd/secret-service/store/auth"
	delegation2 "github.com/YandexClassifieds/shiva/cmd/secret-service/store/delegation"
	"github.com/YandexClassifieds/shiva/pkg/yav/client"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
)

func TestSecretHttpApi_EnvByToken(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)
	yavMock := new(mock.YavService)
	h := &SecretHttpApi{
		sf: &secretFetcher{
			tp:        new(fakeServiceTicketProvider),
			yavSvc:    yavMock,
			dtStore:   delegation2.NewStorage(db, log),
			selfTvmId: 42,
		},
		authStore: auth2.NewStorage(db, log),
		log:       log,
	}
	db.GormDb.Create(&auth2.Token{
		AccessToken: "env-by-token",
		ServiceName: "env-svc",
		Data: auth2.TokenData{
			{EnvKey: "EN1", SecretId: "secret-one", VersionId: "ver-one", SecretKey: "k1"},
			{EnvKey: "EN2", SecretId: "secret-one", VersionId: "ver-onetwo", SecretKey: "k2"},
			{EnvKey: "EN3", SecretId: "secret-two", VersionId: "ver-two", SecretKey: "k2"},
			{EnvKey: "EN4", SecretId: "secret-two", VersionId: "ver-two", SecretKey: "k2"},
			{EnvKey: "LINES", SecretId: "secret-two", VersionId: "ver-two", SecretKey: "lines"},
		},
	})
	tokens := []*delegation2.Token{
		{ServiceName: "env-svc", SecretId: "secret-one", Token: "token-one", TvmId: 42},
		{ServiceName: "env-svc", SecretId: "secret-two", Token: "token-two-other", TvmId: 44},
		{ServiceName: "env-svc", SecretId: "secret-two", Token: "token-two", TvmId: 42},
	}
	for i, m := range tokens {
		require.NoError(t, db.GormDb.Create(m).Error, "error for idx %d", i)
	}

	req, err := http.NewRequest("GET", "/s/env-by-token", nil)
	require.NoError(t, err)

	recorder := httptest.NewRecorder()
	sv1 := client.SecretValues{"k1": "v1", "k2": "v2"}
	sv11 := client.SecretValues{"k1": "v1", "k2": "v22"}
	sv2 := client.SecretValues{"k2": "s2-v2", "lines": "a\nb=c"}
	yavMock.On("SecretFromToken", "token-one", "env-svc", "ver-one", "mock-tvm-ticket").Return(sv1, nil)
	yavMock.On("SecretFromToken", "token-one", "env-svc", "ver-onetwo", "mock-tvm-ticket").Return(sv11, nil)
	yavMock.On("SecretFromToken", "token-two", "env-svc", "ver-two", "mock-tvm-ticket").Return(sv2, nil)

	h.envByToken(recorder, req)
	response := recorder.Result()
	responseBody, _ := ioutil.ReadAll(response.Body)
	require.Equal(t, http.StatusOK, response.StatusCode)

	yavMock.AssertExpectations(t)
	require.Equal(t, 200, response.StatusCode, "response was: %s", responseBody)

	env := parseEnv(bytes.NewBuffer(responseBody))
	require.Len(t, env, 5)
	expectedEnv := map[string]string{
		"EN2":   strconv.Quote("v22"),
		"EN1":   strconv.Quote("v1"),
		"EN3":   strconv.Quote("s2-v2"),
		"EN4":   strconv.Quote("s2-v2"),
		"LINES": strconv.Quote("a\nb=c"),
	}
	require.Equal(t, expectedEnv, env)
}

func parseEnv(reader io.Reader) map[string]string {
	env := map[string]string{}
	scanner := bufio.NewScanner(reader)
	for scanner.Scan() {
		line := scanner.Text()
		kv := strings.SplitN(line, "=", 2)
		if len(kv) == 2 {
			env[kv[0]] = kv[1]
		}
	}
	return env
}

type fakeServiceTicketProvider struct{}

func (*fakeServiceTicketProvider) Ticket() (string, error) {
	return "mock-tvm-ticket", nil
}
