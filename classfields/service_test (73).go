package api

import (
	"errors"
	"testing"

	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestGetPersonByTelegram(t *testing.T) {
	test.InitTestEnv()
	staffApi := NewApi(NewConf(), test.NewLogger(t))
	person, err := staffApi.GetPersonByTelegram("danevge")
	require.NoError(t, err)
	assert.Equal(t, person.Login, "danevge")
	_, err = staffApi.GetPersonByTelegram("danevge_yandex")
	assert.True(t, errors.Is(err, ErrUserNotFound))
}

func TestService_GetPersonByTelegramMixedCase(t *testing.T) {
	test.InitTestEnv()
	staffApi := NewApi(NewConf(), test.NewLogger(t))
	person, err := staffApi.GetPersonByTelegram("DaNeVGe")
	require.NoError(t, err)
	assert.Equal(t, person.Login, "danevge")
}

func TestGetPersonByLogin(t *testing.T) {

	test.InitTestEnv()
	staffApi := NewApi(NewConf(), test.NewLogger(t))
	person, err := staffApi.GetPersonByLogin("danevge")
	require.NoError(t, err)
	assert.Equal(t, person.Login, "danevge")
	_, err = staffApi.GetPersonByLogin("asfdasdf")
	assert.True(t, errors.Is(err, ErrUserNotFound))
}

func TestGetPersonsByLogins(t *testing.T) {
	test.InitTestEnv()
	staffApi := NewApi(NewConf(), test.NewLogger(t))
	persons, err := staffApi.GetPersonsByLogins("danevge", "niklogvinenko")
	require.NoError(t, err)
	assert.Equal(t, len(persons), 2)
	p, err := staffApi.GetPersonsByLogins()
	require.NoError(t, err)
	assert.Equal(t, len(p), 0)
}

func TestGetPersonsByGithubs(t *testing.T) {
	test.InitTestEnv()
	staffApi := NewApi(NewConf(), test.NewLogger(t))
	persons, err := staffApi.GetPersonsByGithubs("danevge", "bogdanov1609")
	require.NoError(t, err)
	assert.Equal(t, len(persons), 2)
	p, err := staffApi.GetPersonsByGithubs()
	require.NoError(t, err)
	assert.Equal(t, len(p), 0)
}

func TestGetGroupByUrl(t *testing.T) {
	test.InitTestEnv()
	staffApi := NewApi(NewConf(), test.NewLogger(t))
	_, err := staffApi.GetGroupByUrl("yandex_personal_vertserv_infra")
	require.NoError(t, err)
	_, err = staffApi.GetGroupByUrl("sdgfsdgf")
	assert.True(t, errors.Is(err, ErrGroupNotFound))
}

func TestGetGroupsByParent(t *testing.T) {
	test.InitTestEnv()
	staffApi := NewApi(NewConf(), test.NewLogger(t))
	groups, err := staffApi.GetGroupsByParent("yandex_personal_vertserv_infra_service")
	require.NoError(t, err)
	assertHasGroup(t, groups, "yandex_personal_vertserv_infra_mnt_teamyaml")
	groups, err = staffApi.GetGroupsByParent("fdgffdg")
	require.NoError(t, err)
	assert.Equal(t, len(groups), 0)
}

func TestExtractGroup(t *testing.T) {
	test.InitTestEnv()
	assert.Equal(t, ExtractName("https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"), "yandex_personal_vertserv_infra_mnt")
}

func assertHasGroup(t *testing.T, lst []*DepartmentGroup, url string) bool {
	t.Helper()
	found := false
	for _, v := range lst {
		if v.URL == url {
			found = true
			break
		}
	}
	return assert.True(t, found, "group '%s' not found", url)
}
