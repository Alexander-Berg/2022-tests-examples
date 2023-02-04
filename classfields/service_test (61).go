package validator

import (
	"errors"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/common/user_error"
	proto "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/layer"
	"github.com/YandexClassifieds/shiva/pkg/include"
	"github.com/YandexClassifieds/shiva/pkg/include/domain"
	"github.com/YandexClassifieds/shiva/pkg/manifest/model"
	"github.com/YandexClassifieds/shiva/pkg/param"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	"github.com/YandexClassifieds/shiva/pkg/service_map/validator"
	"github.com/YandexClassifieds/shiva/pkg/template"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
)

const (
	sMapStr = `{name: service-name, owner: https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra}`
)

func TestSuccessValidate(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service, sMapS, _ := newService(t)
	require.NoError(t, sMapS.ReadAndSave([]byte(sMapStr), 10, "maps/service-name.yml"))
	svcManifest := newM()
	errs, _ := service.Validate(layer.Layer_TEST, svcManifest, nil)
	test.AssertUserErrors(t, user_error.NewUserErrors(), errs)
}

func TestSuccessValidate_NewMap(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service, _, _ := newService(t)
	errs, _ := service.Validate(layer.Layer_TEST, newM(), newSMap())
	test.AssertUserErrors(t, user_error.NewUserErrors(), errs)
}

func TestValidateNoMap(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service, _, _ := newService(t)
	m := newM()
	errs, _ := service.Validate(layer.Layer_TEST, m, nil)
	test.AssertUserErrors(t, user_error.NewUserErrors(validator.ErrMapsNotFound(m.Path)), errs)
}

func TestValidateDC(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service, _, _ := newService(t)
	m := newM()
	m.DC = map[string]int{}
	errs, _ := service.Validate(layer.Layer_TEST, m, newSMap())
	test.AssertUserErrors(t, user_error.NewUserErrors(ErrEmptyDC), errs)
}

func TestValidateMyt(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service, _, _ := newService(t)
	m := newM()
	m.DC["myt"] = 2
	errs, _ := service.Validate(layer.Layer_TEST, m, newSMap())
	test.AssertUserErrors(t, user_error.NewUserErrors(ErrDCNotSupport("myt")), errs)
}

func TestValidateDCConflict(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service, _, _ := newService(t)
	m := newM()
	m.DC = map[string]int{
		"sas":    2,
		"yd_vla": 2,
	}
	errs, _ := service.Validate(layer.Layer_TEST, m, newSMap())
	test.AssertUserErrors(t, user_error.NewUserErrors(ErrConflictingDC), errs)
}

func TestValidateUpgrade(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service, _, _ := newService(t)
	m := newM()
	m.DC = map[string]int{
		"vla": 2,
		"sas": 5,
	}
	m.Upgrade.Parallel = 3
	errs, _ := service.Validate(layer.Layer_TEST, m, newSMap())
	test.AssertUserErrors(t, user_error.NewUserErrors(ErrHighUpgradeParallel), errs)
}

func TestValidateGeobase(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service, _, _ := newService(t)
	m := newM()
	m.GeobaseVersion = 42
	errs, _ := service.Validate(layer.Layer_TEST, m, newSMap())
	test.AssertUserErrors(t, user_error.NewUserErrors(ErrWrongGeobaseVersion), errs)
}

func TestValidateResource(t *testing.T) {

	type testCase struct {
		layer layer.Layer
		name  string
		rs    model.Resources
		urs   *user_error.UserErrors
		warns *user_error.UserErrors
		sMap  *proto.ServiceMap
	}

	test.RunUp(t)
	defer test.Down(t)
	service, _, _ := newService(t)

	ts := []testCase{
		{
			layer: layer.Layer_PROD,
			name:  "empty_cpu",
			rs: model.Resources{
				Memory: 256,
			},
			urs:   user_error.NewUserErrors(),
			warns: user_error.NewUserErrors(),
		},
		{
			layer: layer.Layer_PROD,
			name:  "empty_memory",
			rs: model.Resources{
				CPU: 1000,
			},
			urs:   user_error.NewUserErrors(ErrEmptyMemory),
			warns: user_error.NewUserErrors(),
		},
		{
			layer: layer.Layer_PROD,
			name:  "high memory",
			rs: model.Resources{
				Memory: maxMemory + 1,
			},
			urs:   user_error.NewUserErrors(ErrMemoryLimit),
			warns: user_error.NewUserErrors(),
		},
		{
			layer: layer.Layer_PROD,
			name:  "empty",
			rs:    model.Resources{},
			urs:   user_error.NewUserErrors(ErrEmptyMemory),
			warns: user_error.NewUserErrors(),
		},
		{
			layer: layer.Layer_PROD,
			name:  "success_cpu",
			rs: model.Resources{
				CPU:    1000,
				Memory: 256,
			},
			urs:   user_error.NewUserErrors(),
			warns: user_error.NewUserErrors(),
		},
		{
			layer: layer.Layer_TEST,
			name:  "warn_cpu_in_test",
			rs: model.Resources{
				CPU:    1000,
				Memory: 256,
			},
			urs:   user_error.NewUserErrors(),
			warns: user_error.NewUserErrors(WarnCPUOnTestNotRecommend),
		},
		{
			layer: layer.Layer_TEST,
			name:  "success_batch_cpu",
			rs: model.Resources{
				CPU:    1000,
				Memory: 256,
			},
			urs:   user_error.NewUserErrors(ErrBatchDC),
			warns: user_error.NewUserErrors(),
			sMap:  newBatchSMap(),
		},
		{
			layer: layer.Layer_PROD,
			name:  "success_auto_cpu",
			rs: model.Resources{
				AutoCpu: true,
				Memory:  256,
			},
			urs:   user_error.NewUserErrors(),
			warns: user_error.NewUserErrors(),
		},
		{
			layer: layer.Layer_PROD,
			name:  "warn_cpu_and_auto_cpu",
			rs: model.Resources{
				AutoCpu: true,
				CPU:     100,
				Memory:  256,
			},
			urs:   user_error.NewUserErrors(),
			warns: user_error.NewUserErrors(WarnCPUAndAutoCpu),
		},
		{
			name:  "auto_cpu_batch",
			layer: layer.Layer_PROD,
			rs: model.Resources{
				AutoCpu: true,
				Memory:  256,
			},
			urs:   user_error.NewUserErrors(ErrBatchResources, ErrAutoCpuAndBatchNotSupport, ErrBatchDC),
			warns: user_error.NewUserErrors(),
			sMap:  newBatchSMap(),
		},
	}

	for _, c := range ts {
		t.Run(c.name, func(t *testing.T) {
			if c.sMap == nil {
				c.sMap = newSMap()
			}
			m := newM()
			m.Resources = c.rs
			errs, warns := service.Validate(c.layer, m, c.sMap)
			test.AssertUserErrors(t, c.urs, errs)
			test.AssertUserErrors(t, c.warns, warns)
		})
	}
}

func TestValidateGeneral(t *testing.T) {
	type TestCase struct {
		name  string
		path  string
		place param.Place
		err   *user_error.UserError
	}

	test.RunUp(t)
	defer test.Down(t)
	service, _, _ := newService(t)
	cases := []TestCase{
		{
			name:  "ConfigParams_Overridden",
			path:  param.ConfigParams.Path,
			place: param.Overridden,
			err:   ErrConfigUsedInGeneral,
		},
		{
			name:  "ConfigParams_General",
			path:  param.ConfigParams.Path,
			place: param.General,
			err:   ErrConfigUsedInGeneral,
		},
		{
			name:  "ConfigParams_Layer",
			path:  param.ConfigParams.Path,
			place: param.Layer,
		},
		{
			name:  "ConfigFiles_Overridden",
			path:  param.ConfigFiles.Path,
			place: param.Overridden,
			err:   ErrConfigUsedInGeneral,
		},
		{
			name:  "ConfigFiles_General",
			path:  param.ConfigFiles.Path,
			place: param.General,
			err:   ErrConfigUsedInGeneral,
		},
		{
			name:  "ConfigFiles_Layer",
			path:  param.ConfigFiles.Path,
			place: param.Layer,
		},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			m := newM()
			m.Place = map[string]param.Place{c.path: c.place}
			errs, _ := service.Validate(layer.Layer_TEST, m, newSMap())
			test.AssertUserErrors(t, user_error.NewUserErrors(c.err), errs)
		})
	}
}

func TestValidateConfigCollision(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)
	service, _, _ := newService(t)
	confSrv := include.NewService(test_db.NewDb(t), test.NewLogger(t))
	require.NoError(t, confSrv.ReadAndSave([]byte("param: p1"), 10, "path/f1.yml"))
	require.NoError(t, confSrv.ReadAndSave([]byte("param: p2"), 10, "path/f2.yml"))
	m := newM()
	i1, err := confSrv.GetByPath("path/f1.yml")
	require.NoError(t, err)
	i2, err := confSrv.GetByPath("path/f2.yml")
	require.NoError(t, err)
	m.Config.Files = []*domain.Include{i1, i2}
	errs, _ := service.Validate(layer.Layer_TEST, m, newSMap())
	test.AssertUserErrors(t, user_error.NewUserErrors(model.CollisionError("param", []string{"path/f1.yml", "path/f2.yml"})), errs)
}

func TestValidateSecrets(t *testing.T) {
	test.RunUp(t)
	cases := []struct {
		name     string
		ssError  error
		expected *user_error.UserErrors
	}{
		{
			name:     "go_error",
			ssError:  errors.New("just err"),
			expected: user_error.NewUserErrors(user_error.NewInternalError(errors.New("just err"))),
		},
		{
			name: "multi_err",
			ssError: user_error.NewUserErrors(
				user_error.NewInternalError(errors.New("e1")),
				user_error.NewInternalError(errors.New("e2")),
			),
			expected: user_error.NewUserErrors(
				user_error.NewInternalError(errors.New("e1")),
				user_error.NewInternalError(errors.New("e2")),
			),
		},
		{
			name:     "user_err",
			ssError:  user_error.NewUserError(errors.New("user_err"), ""),
			expected: user_error.NewUserErrors(user_error.NewUserError(errors.New("user_err"), "")),
		},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			svc, _, ssMock := newService(t)
			svcMap := newSMap()
			svcMap.Name = "secret_test"
			svcManifest := newM()
			svcManifest.Name = "secret_test"
			ssMock.On("ValidateSecret", "secret_test", "${sec-one:ver-one:k1}", 0).Return(tc.ssError)
			ssMock.On("ValidateSecret", "secret_test", mock.Anything, 0).Return(nil)
			errs, _ := svc.validateConfig(&validateContext{
				manifest: svcManifest,
				sMap:     svcMap,
				newMap:   false,
			})
			require.NotEmpty(t, errs)
			assert.Equal(t, tc.expected, errs)
		})
	}
}

func TestValidate_Batch(t *testing.T) {
	test.RunUp(t)
	service, _, _ := newService(t)
	t.Run("periodic_ok", func(t *testing.T) {
		svcMap := newSMap()
		svcManifest := newM()
		svcManifest.Resources.CPU = 100
		svcMap.Type = proto.ServiceType_batch // should error that we have dc set for periodic job
		svcManifest.Periodic = "0 * * * *"
		svcManifest.DC = nil
		errs, _ := service.Validate(layer.Layer_TEST, svcManifest, svcMap)
		require.True(t, errs.Empty(), "has unexpected errors: %+v", errs.Get())
	})
	t.Run("errors", func(t *testing.T) {
		svcMap := newSMap()
		svcManifest := newM()
		svcManifest.Resources.CPU = 0         // should warn we have default cpu limits
		svcMap.Type = proto.ServiceType_batch // should error that we have dc set for periodic job
		svcManifest.Periodic = "0 * * * *"
		errs, warns := service.Validate(layer.Layer_TEST, svcManifest, svcMap)
		test.AssertUserErrors(t, user_error.NewUserErrors(ErrBatchDC, ErrBatchResources), errs)
		test.AssertUserErrors(t, user_error.NewUserErrors(), warns)
	})
	t.Run("empty_cron", func(t *testing.T) {
		svcMap := newSMap()
		svcManifest := newM()
		svcMap.Type = proto.ServiceType_batch
		svcManifest.DC = nil
		svcManifest.Periodic = ""
		errs, _ := service.Validate(layer.Layer_TEST, svcManifest, svcMap)
		require.True(t, errs.Len() == 0, "expected 0 err, got:\n%v", errs.Error())
	})
	t.Run("invalid_format", func(t *testing.T) {
		svcMap := newSMap()
		svcManifest := newM()
		svcMap.Type = proto.ServiceType_batch
		svcManifest.DC = nil
		svcManifest.Periodic = "not_valid"
		errs, _ := service.Validate(layer.Layer_TEST, svcManifest, svcMap)
		require.True(t, errs.Len() == 1, "expected 1 err, got:\n%v", errs.Error())
		assert.Contains(t, errs.Get()[0].Error(), "invalid cron format")
		assert.Contains(t, errs.Get()[0].RusMessage, "cron-формат")
	})
}

func TestValidateAnyDC(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	service, _, _ := newService(t)
	svcMap := newSMap()
	svcManifest := newM()
	svcManifest.DC = map[string]int{
		"any": 1,
	}
	svcManifest.Upgrade = model.Upgrade{
		Parallel: 1,
	}

	errs, _ := service.Validate(layer.Layer_TEST, svcManifest, svcMap)

	require.True(t, errs.Empty(), "has unexpected errors: %+v", errs.Get())
}

func TestValidateAnyDCWithAnotherDC(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	service, _, _ := newService(t)
	svcMap := newSMap()
	svcManifest := newM()
	svcManifest.DC = map[string]int{
		"any": 1,
		"vla": 1,
	}
	svcManifest.Upgrade = model.Upgrade{
		Parallel: 1,
	}

	errs, _ := service.Validate(layer.Layer_TEST, svcManifest, svcMap)

	test.AssertUserErrors(t, user_error.NewUserErrors(ErrAnyDCNotAlone), errs)
}

func TestValidateParams(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	service, _, _ := newService(t)
	svcMap := newSMap()
	svcManifest := newM()
	c := model.NewConfig()
	c.Params = map[string]string{
		"_DEPLOY_TRACING_ADDR": "jaeger-agent.service.consul",
		"HOSTNAME":             "${node.unique.name}",
		"DC":                   "${node.datacenter}",
		"JAEGER_HOST":          "jaeger-agent.service.consul",
	}
	svcManifest.Config = c
	svcManifest.Resources = model.Resources{
		AutoCpu: true,
		Memory:  128,
	}

	errs, warns := service.Validate(layer.Layer_TEST, svcManifest, svcMap)

	expectedErrs := user_error.NewUserErrors(
		ErrExplicitBaseEnv,
	)
	test.AssertUserErrors(t, expectedErrs, errs)

	expectedWarns := user_error.NewUserErrors(
		WarnUnusedBaseEnv("_DEPLOY_HOSTNAME", "HOSTNAME"),
		WarnUnusedBaseEnv("_DEPLOY_DC", "DC"),
		WarnUnusedBaseEnv("_DEPLOY_TRACING_ADDR", "JAEGER_HOST"),
	)
	test.AssertUserErrors(t, expectedWarns, warns)
}

func newM() *model.Manifest {
	c := model.NewConfig()
	c.Params = map[string]string{
		"p1":         "v1",
		"p2":         "v2",
		"p3":         "v3",
		"SECRET_KEY": "${sec-one:ver-one:k1}",
	}
	return &model.Manifest{
		Name:  "service-name",
		Image: "service-name",
		Resources: model.Resources{
			CPU:    1000,
			Memory: 128,
		},
		DC: map[string]int{
			"sas": 2,
			"vla": 2,
		},
		Upgrade: model.Upgrade{
			Parallel: 2,
		},
		Config: c,
	}
}

func newSMap() *proto.ServiceMap {

	return &proto.ServiceMap{
		Name:   "service-name",
		Type:   proto.ServiceType_service,
		Owners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra"},
		Path:   "maps/service-name.yml",
	}
}

func newBatchSMap() *proto.ServiceMap {

	return &proto.ServiceMap{
		Name:   "service-name",
		Type:   proto.ServiceType_batch,
		Owners: []string{"https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra"},
		Path:   "maps/service-name.yml",
	}
}

func newService(t *testing.T) (*Service, *service_map.Service, *ssMock) {
	db := test_db.NewDb(t)
	SMapS := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())
	envSvc := reader.NewService(db, test.NewLogger(t))
	secretSvc := new(ssMock)
	secretSvc.On("ValidateSecret", "service-name", mock.Anything, 0).Return(nil)
	return NewService(SMapS, secretSvc, template.NewService(SMapS, envSvc), test.NewLogger(t)), SMapS, secretSvc
}

type ssMock struct {
	mock.Mock
}

func (m *ssMock) ValidateSecret(name, value string, tvmId int) error {
	return m.Called(name, value, tvmId).Error(0)
}
