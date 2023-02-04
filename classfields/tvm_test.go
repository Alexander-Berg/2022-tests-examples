package handler

import (
	"sort"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/generator/task"
	change "github.com/YandexClassifieds/shiva/pb/shiva/events/change_conf"
	privateApi "github.com/YandexClassifieds/shiva/pb/shiva/private_api"
	shivaM "github.com/YandexClassifieds/shiva/pb/shiva/private_api/mocks"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pkg/abc/domain"
	abcM "github.com/YandexClassifieds/shiva/pkg/abc/mocks"
	ssTokens "github.com/YandexClassifieds/shiva/pkg/secrets/tokens"
	ssTokensM "github.com/YandexClassifieds/shiva/pkg/secrets/tokens/mocks"
	"github.com/YandexClassifieds/shiva/test"
	mock2 "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

const (
	secID = "sec-01efxmtecv69t6zmqcn2gj8kd6"
)

func TestUpdate(t *testing.T) {

	test.RunUp(t)
	h, m := newTvmHandler(t)
	taskModel := &task.Task{
		Service:    "shiva_test",
		ChangeType: change.ChangeType_UPDATE,
		Handler:    h.Name(),
	}
	newSM := &proto.ServiceMap{
		Name: "shiva_test",
		Owners: []string{
			"https://staff.yandex-team.ru/spooner",
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/",
		},
		Type: proto.ServiceType_service,
	}
	oldSM := &proto.ServiceMap{
		Name: "shiva_test",
		Owners: []string{
			"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/",
		},
		Type: proto.ServiceType_service,
	}
	owners := newSM.ActualOwners()
	sort.Strings(owners)
	m.On("RewriteReaders", secID, owners).Return(nil)
	require.NoError(t, h.OnUpdate(taskModel, newSM, oldSM))
}

func TestSkipUpdater(t *testing.T) {
	test.RunUp(t)
	h, _ := newTvmHandler(t)
	for _, st := range []proto.ServiceType{proto.ServiceType_external, proto.ServiceType_mysql, proto.ServiceType_mdb_mysql,
		proto.ServiceType_conductor, proto.ServiceType_jenkins} {
		t.Run(st.String(), func(t *testing.T) {
			newSM := &proto.ServiceMap{
				Name: "shiva_test",
				Owners: []string{
					"https://staff.yandex-team.ru/spooner",
					"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/",
				},
				Type: proto.ServiceType_conductor,
			}
			// assert by yav mock
			require.NoError(t, h.OnUpdate(nil, newSM, nil))
		})
	}
}

func TestSkipByEnv(t *testing.T) {
	test.RunUp(t)
	h, _ := newTvmHandler(t)
	taskModel := &task.Task{
		Service: "shiva_test",
		Handler: h.Name(),
		EnvKey:  "value",
	}

	require.NoError(t, h.Updated(taskModel, nil))
	require.NoError(t, h.Updated(taskModel, nil))
}

func TestLegacyOwner(t *testing.T) {
	test.RunUp(t)
	h, m := newTvmHandler(t)
	taskModel := &task.Task{
		Service:    "shiva_test",
		ChangeType: change.ChangeType_UPDATE,
		Handler:    h.Name(),
	}
	newSM := &proto.ServiceMap{
		Name:  "shiva_test",
		Owner: "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/",
		Type:  proto.ServiceType_service,
	}
	oldSM := &proto.ServiceMap{
		Name:  "shiva_test",
		Owner: "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/",
		Type:  proto.ServiceType_service,
	}
	owners := newSM.ActualOwners()
	sort.Strings(owners)
	m.On("RewriteReaders", secID, owners).Return(nil)
	require.NoError(t, h.OnUpdate(taskModel, newSM, oldSM))
}

func TestOnForceUpdate(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	taskS := task.NewService(log, db)

	abcS := &abcM.AbcService{}
	shivaC := &shivaM.PrivateServiceClient{}
	ssTokenS := &ssTokensM.TokensService{}

	abcS.On("GetTvm", mock.Anything).Return(&domain.TvmResource{
		Resource: &domain.Resource{},
		Secret: &domain.Secret{
			UUID: secID,
		},
	}, nil)
	abcS.On("RegenerateTvmSecret", "autogen/TEST/shiva_test").Return(nil)

	shivaC.On("SetEnvs", mock.Anything, mock.Anything).Return(nil, nil)

	ssTokenS.On("Delegate", "shiva_test", secID).Return(nil)

	h := NewTvmHandler(layer.Layer_TEST, taskS, abcS, shivaC, ssTokenS, log, nil)

	taskModel := &task.Task{
		Service:    "shiva_test",
		ChangeType: change.ChangeType_UPDATE,
		Handler:    h.Name(),
		EnvKey:     "_DEPLOY_G_TVM_SECRET",
	}
	sMap := &proto.ServiceMap{
		Name:  "shiva_test",
		Owner: "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/",
		Type:  proto.ServiceType_service,
	}

	err := h.OnForceUpdate(taskModel, sMap)

	require.NoError(t, err)
	abcS.AssertExpectations(t)
	shivaC.AssertExpectations(t)
	ssTokenS.AssertExpectations(t)
}

func TestOnEnvUpdated(t *testing.T) {
	test.RunUp(t)
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	taskS := task.NewService(log, db)

	abcS := &abcM.AbcService{}
	abcS.On("DeleteOldTvmSecret", "autogen/TEST/shiva_test").Return(nil)

	h := NewTvmHandler(layer.Layer_TEST, taskS, abcS, nil, nil, log, nil)

	taskModel := &task.Task{
		Service:    "shiva_test",
		ChangeType: change.ChangeType_ENV_UPDATED,
		Handler:    h.Name(),
		EnvKey:     "_DEPLOY_G_TVM_SECRET",
	}
	sMap := &proto.ServiceMap{
		Name:  "shiva_test",
		Owner: "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/",
		Type:  proto.ServiceType_service,
	}

	err := h.Updated(taskModel, sMap)

	require.NoError(t, err)
	abcS.AssertExpectations(t)

}
func newTvmHandler(t *testing.T) (Handler, *mock2.YavService) {
	log := test.NewLogger(t)
	db := test_db.NewDb(t)
	taskS := task.NewService(log, db)
	abcM := &abcM.AbcService{}
	abcM.On("GetTvm", mock.Anything).Return(&domain.TvmResource{
		Resource: &domain.Resource{},
		Secret: &domain.Secret{
			UUID: secID,
		},
	}, nil)
	//don't need
	var shivaC privateApi.PrivateServiceClient
	var tokensS *ssTokens.Service
	// mocks
	yavM := &mock2.YavService{}

	return NewTvmHandler(layer.Layer_TEST, taskS, abcM, shivaC, tokensS, log, yavM), yavM
}
