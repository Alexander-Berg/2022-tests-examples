/*
В данном пакете тесты должны использовать реальный staff client, в остальных местах лучше применить моки
*/
package staff

import (
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	storage2 "github.com/YandexClassifieds/shiva/common/storage"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSuccessValidateByGroup(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	sMap := &proto.ServiceMap{
		Name: "golp-log-generator",
		Owners: []string{
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_auto_autoru_serv",
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra",
			"https://staff.yandex-team.ru/alexander-s",
		},
	}
	user, err := service.GetByLogin("danevge")
	require.NoError(t, err)
	require.NoError(t, service.Validate(user, sMap))
}

func TestSuccessLegacyOwnerValidateByGroup(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	//goland:noinspection GoDeprecation
	sMap := &proto.ServiceMap{
		Name:  "golp-log-generator",
		Owner: "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra",
	}
	user, err := service.GetByLogin("danevge")
	require.NoError(t, err)
	require.NoError(t, service.Validate(user, sMap))
}

func TestSuccessValidateByUser(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	sMap := &proto.ServiceMap{
		Name: "golp-log-generator",
		Owners: []string{
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_auto_autoru_serv",
			"https://staff.yandex-team.ru/danevge",
			"https://staff.yandex-team.ru/spooner",
		},
	}
	user, err := service.GetByLogin("spooner")
	require.NoError(t, err)
	require.NoError(t, service.Validate(user, sMap))
}

//goland:noinspection GoDeprecation
func TestSuccessLegacyOwnerValidateByUser(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	sMap := &proto.ServiceMap{
		Name:  "golp-log-generator",
		Owner: "https://staff.yandex-team.ru/spooner",
	}
	user, err := service.GetByLogin("spooner")
	require.NoError(t, err)
	require.NoError(t, service.Validate(user, sMap))
}

func TestValidateLogin(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	_, err := service.GetByLogin("danevge_not_exist")
	assert.Errorf(t, err, "login 'danevge_not_exist' not found")
}

func TestValidateOwner(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	sMap := &proto.ServiceMap{
		Name:   "golp-log-generator",
		Owners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_auto_autoru_serv"},
	}
	user, err := service.GetByLogin("danevge")
	require.NoError(t, err)
	err = service.Validate(user, sMap)
	assert.Error(t, err, "user by login 'danevge' is not owner")
}

func TestSuccessValidateByEndSlash(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	sMap := &proto.ServiceMap{
		Name:   "golp-log-generator",
		Owners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt_teamyaml/"},
	}
	user, err := service.GetByLogin("danevge")
	require.NoError(t, err)
	require.NoError(t, service.Validate(user, sMap))
}

func TestService_IsUserFromVertis(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)

	vertisUser := "danevge"
	NoneVertisUser := "dshtan"

	isValid, err := service.IsUserFromVertis(vertisUser)
	require.NoError(t, err)
	require.True(t, isValid)

	isValid, err = service.IsUserFromVertis(NoneVertisUser)
	require.NoError(t, err)
	require.False(t, isValid)
}

func TestGetByLogin(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	user, err := service.GetByLogin("danevge")
	require.NoError(t, err)
	assert.NotNil(t, user)
	assert.True(t, user.ID > 0)
	user2, err := service.Get(user.ID)
	require.NoError(t, err)
	assert.NotNil(t, user2)
	assert.Equal(t, user.ID, user2.ID)
	assert.Equal(t, user.Login, user2.Login)
}

func TestGetByTelegram(t *testing.T) {
	testCases := []struct {
		name  string
		login string
	}{
		{
			name:  "lowerCase",
			login: "danevge",
		},
		{
			name:  "mixedCase",
			login: "DaneVgE",
		},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			test.InitTestEnv()
			db := test_db.NewSeparatedDb(t)

			service := newService(t, staffapi.NewConf(), db)
			user, err := service.GetByTelegram(tc.login)
			require.NoError(t, err)
			assert.NotNil(t, user)
			assert.True(t, user.ID > 0)
			user2, err := service.Get(user.ID)
			require.NoError(t, err)
			assert.NotNil(t, user2)
			assert.Equal(t, user.ID, user2.ID)
			assert.Equal(t, user.Login, user2.Login)
		})
	}
}

func TestUpdateByTelegram(t *testing.T) {

	testCases := []struct {
		name  string
		login string
	}{
		{
			name:  "lowerCase",
			login: "danevge",
		},
		{
			name:  "mixedCase",
			login: "DaneVgE",
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			test.InitTestEnv()
			db := test_db.NewSeparatedDb(t)

			service := newService(t, staffapi.NewConf(), db)
			user1, err := service.create(&staffapi.Person{
				Login: "danevge",
				Accounts: []staffapi.Account{
					{
						Id:    0,
						Type:  "telegram",
						Value: "danevge_not_exist",
					},
				},
				DepartmentGroup: staffapi.DepartmentGroup{},
			})
			require.NoError(t, err)
			user2, err := service.GetByTelegram(tc.login)
			require.NoError(t, err)
			assert.Equal(t, user2.Telegram, "danevge")
			assert.NotEqual(t, user1.Telegram, user2.Telegram)
			assert.Equal(t, user1.Login, user2.Login)
			assert.Equal(t, user1.ID, user2.ID)
		})
	}
}

func TestValidateByShadowHead(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	user, err := service.GetByLogin("ibiryulin")
	require.NoError(t, err)
	err = service.Validate(user, &proto.ServiceMap{
		Name: "service-name",
		Owners: []string{
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt"},
	})
	require.NoError(t, err)
}

func TestFilterByOwnerByShadowHead(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	user, err := service.GetByLogin("ibiryulin")
	require.NoError(t, err)

	result, err := service.FilterByOwner(user, []*proto.ServiceMap{
		{
			Name: "service-name",
			Owners: []string{
				"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt"},
		},
	})
	require.NoError(t, err)
	assert.Len(t, result, 1)
}

func TestStaffPriority(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	service := newService(t, staffapi.NewConf(), db)
	_, err := service.create(&staffapi.Person{
		Login: "danevge",
		Accounts: []staffapi.Account{
			{
				Id:    0,
				Type:  "telegram",
				Value: "danevge_not_exist",
			},
		},
		DepartmentGroup: staffapi.DepartmentGroup{},
	})
	require.NoError(t, err)
	_, err = service.GetByTelegram("danevge_not_exist")
	require.Equal(t, staffapi.NewErrUserNotFound("danevge_not_exist"), err)
}

func TestStaffTimeout_FallbackToDb(t *testing.T) {
	testCases := []struct {
		name  string
		login string
	}{
		{
			name:  "lowerCase",
			login: "danevge_not_exists",
		},
		{
			name:  "mixedCase",
			login: "daNevge_not_exiSts",
		},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			test.InitTestEnv()
			db := test_db.NewSeparatedDb(t)

			conf := staffapi.NewConf()
			conf.Timeout = 1
			service := newService(t, conf, db)

			user1, err := service.create(&staffapi.Person{
				Login: "danevge",
				Accounts: []staffapi.Account{
					{
						Id:    0,
						Type:  "telegram",
						Value: "danevge_nOt_exists",
					},
				},
				DepartmentGroup: staffapi.DepartmentGroup{},
			})
			require.NoError(t, err)
			user2, err := service.GetByTelegram(tc.login)
			require.NoError(t, err)
			assert.Equal(t, user1.Telegram, user2.Telegram)
			assert.Equal(t, user1.ID, user2.ID)
			assert.Equal(t, user1.ID, user2.ID)
		})
	}

}

func TestStaffReturn400(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(400)
	}))
	defer srv.Close()

	conf := staffapi.NewConf()
	conf.Endpoint = srv.URL
	service := newService(t, staffapi.NewConf(), db)

	_, err := service.create(&staffapi.Person{
		Login: "danevge",
		Accounts: []staffapi.Account{
			{
				Id:    0,
				Type:  "telegram",
				Value: "danevge_not_exists",
			},
		},
		DepartmentGroup: staffapi.DepartmentGroup{},
	})
	require.NoError(t, err)
	_, err = service.GetByTelegram("danevge_not_exists")
	require.Error(t, err)
}

func TestStaffReturn502_FallbackToDb(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)

	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(502)
	}))
	defer srv.Close()

	conf := staffapi.NewConf()
	conf.Endpoint = srv.URL
	service := newService(t, conf, db)
	user1, err := service.create(&staffapi.Person{
		Login: "danevge",
		Accounts: []staffapi.Account{
			{
				Id:    0,
				Type:  "telegram",
				Value: "danevge_not_exists",
			},
		},
		DepartmentGroup: staffapi.DepartmentGroup{},
	})
	require.NoError(t, err)
	user2, err := service.GetByTelegram("danevge_not_exists")
	require.NoError(t, err)
	assert.Equal(t, user1.Telegram, user2.Telegram)
	assert.Equal(t, user1.ID, user2.ID)
	assert.Equal(t, user1.ID, user2.ID)
}

func TestLoginValidation(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	service := newService(t, staffapi.NewConf(), db)
	goodLogins := []string{"A_BC", "abc1", "AbC2"}
	badLogins := []string{"A_B,C", "ab.c1", "AbC2()"}

	for _, login := range goodLogins {
		t.Run(login, func(t *testing.T) {
			_, err := service.GetByTelegram(login)
			assert.NotEqual(t, err, ErrTgLoginNotValid)
		})
	}

	for _, login := range badLogins {
		t.Run(login, func(t *testing.T) {
			_, err := service.GetByTelegram(login)
			assert.Equal(t, err, ErrTgLoginNotValid)
		})
	}
}

func TestGetPersonsByGithubs(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	staffService := newService(t, staffapi.NewConf(), db)
	persons, err := staffService.GetPersonsByGithubs("danevge", "bogdanov1609")
	require.NoError(t, err)
	assert.Equal(t, len(persons), 2)
	p, err := staffService.GetPersonsByGithubs()
	require.NoError(t, err)
	assert.Equal(t, len(p), 0)
}

func TestGetPersonsByLogins(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	staffService := newService(t, staffapi.NewConf(), db)
	persons, err := staffService.GetPersonsByLogins("danevge", "niklogvinenko")
	require.NoError(t, err)
	assert.Equal(t, len(persons), 2)
	p, err := staffService.GetPersonsByLogins()
	require.NoError(t, err)
	assert.Equal(t, len(p), 0)
}

func TestGetGroupByUrl(t *testing.T) {

	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	staffService := newService(t, staffapi.NewConf(), db)
	_, err := staffService.GetGroupByName("yandex_personal_vertserv_infra_mnt")
	require.NoError(t, err)
	_, err = staffService.GetGroupByName("sdgfsdgf")
	assert.True(t, errors.Is(err, staffapi.ErrGroupNotFound))
}

func TestCheckMembership(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	staffService := newService(t, staffapi.NewConf(), db)

	testCases := []struct {
		name     string
		login    string
		staffUrl string
		result   bool
	}{
		{"belongs to group", "danevge",
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt_teamyaml", true},
		{"in child group", "danevge",
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra", true},
		{"direct link to person", "danevge",
			"https://staff.yandex-team.ru/danevge", true},
		{"wrong direct link to person", "danevge",
			"https://staff.yandex-team.ru/alexander-s", false},
		{"sibling group", "danevge",
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_test", false},
		{"bootcamp group without head, use closest ancestor group head", "svyatoslav",
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_dep20083", true},
		{"filter out hr partner", "v-kuznetsova",
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_dep20083", false},
		{"head from parent group", "svyatoslav",
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_comp_infra_bigdata", true},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			person, err := staffService.GetPersonByLogin(tc.login)
			require.NoError(t, err)

			match, err := staffService.MatchPerson(person, []string{tc.staffUrl})
			require.NoError(t, err)
			require.Equal(t, tc.result, match)
		})
	}
}

func TestCheckMembershipError(t *testing.T) {
	test.InitTestEnv()
	db := test_db.NewSeparatedDb(t)
	staffService := newService(t, staffapi.NewConf(), db)

	person, err := staffService.GetPersonByLogin("danevge")
	require.NoError(t, err)

	match, err := staffService.MatchPerson(person, []string{"https://staff.yandex-team.ru/departments/sdfasdf"})
	require.False(t, match)
	require.Error(t, err)
	require.True(t, errors.Is(err, staffapi.ErrGroupNotFound))
}

func newService(t *testing.T, config staffapi.Conf, db *storage2.Database) *Service {
	log := test.NewLogger(t)
	staffApi := staffapi.NewApi(config, log)

	return NewService(db, staffApi, log)
}
