package template

import (
	_ "embed"
	"errors"
	"testing"

	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/storage"
	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/user_error"
	spb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/env"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	tmp_errors "github.com/YandexClassifieds/shiva/pkg/template/errors"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

var (
	//go:embed test_data/test-service.yml
	testSvcMap string
)

func Test_Validate(t *testing.T) {
	const (
		env = "ENV"
	)

	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	mapSvc := service_map.NewService(db, log, service_change.NewNotificationMock())
	envSvc := reader.NewService(db, log)

	svc := NewService(mapSvc, envSvc)

	require.NoError(t, mapSvc.ReadAndSave([]byte(testSvcMap), 10, spb.ToFullPath("test-service")))

	cases := []struct {
		template    string
		expectedErr *user_error.UserError
	}{
		{
			template:    "${url:qwer:api}",
			expectedErr: tmp_errors.NewErrUnknownService(env, "qwer"),
		},
		{
			template:    "${port:test-service:admin}",
			expectedErr: tmp_errors.NewErrInvalidProvider(env, "test-service", "admin"),
		},
		{
			template:    "${url:test-service:api}",
			expectedErr: nil,
		},
		{
			template:    "${tvm-id:test-service}",
			expectedErr: nil,
		},
		{
			template:    "${tvm-id:shiva-tg}",
			expectedErr: tmp_errors.NewErrUnknownService(env, "shiva-tg"),
		},
		{
			template:    "${port:shiva:http-admin}",
			expectedErr: tmp_errors.NewErrUnknownService(env, "shiva"),
		},
		{
			template:    "${tvm-id:shiva}",
			expectedErr: tmp_errors.NewErrInvalidTvmID(env),
		},
		{
			template:    "${tvm-id}",
			expectedErr: tmp_errors.NewErrParseTemplate("${tvm-id}"),
		},
		{
			template:    "",
			expectedErr: nil,
		},
		{
			template:    "value",
			expectedErr: nil,
		},
	}

	for _, c := range cases {
		t.Run(c.template, func(t *testing.T) {
			err := svc.ValidateTemplate("shiva", env, c.template)

			if c.expectedErr == nil {
				assert.NoError(t, err)
			} else {
				var userError *user_error.UserError
				require.True(t, errors.As(err, &userError))

				assert.Equal(t, *c.expectedErr, *userError)
			}
		})
	}
}

func Test_Resolve_ValidSvcAddressTemplates(t *testing.T) {
	var (
		templates = map[string]string{
			"port": "${port:test-service:api}",
			"host": "${host:test-service:api}",
			"url":  "${url:test-service:api}",
			"tvm":  "tvm",
		}
	)

	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	mapSvc := service_map.NewService(db, log, service_change.NewNotificationMock())
	envSvc := reader.NewService(db, log)

	svc := NewService(mapSvc, envSvc)

	require.NoError(t, mapSvc.ReadAndSave([]byte(testSvcMap), 10, spb.ToFullPath("test-service")))

	envs, err := svc.Resolve("shiva", templates, common.Prod)
	require.NoError(t, err)

	assert.Equal(t, "80", envs["port"])
	assert.Equal(t, "test-service-api.vrts-slb.prod.vertis.yandex.net", envs["host"])
	assert.Equal(t, "http://test-service-api.vrts-slb.prod.vertis.yandex.net:80", envs["url"])
	assert.Equal(t, "", envs["tvm"])
}

func Test_Resolve_ValidTvmIDTemplate(t *testing.T) {
	var (
		templates = map[string]string{
			"tvm": "${tvm-id:test-service}",
		}
	)

	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	mapSvc := service_map.NewService(db, log, service_change.NewNotificationMock())
	envSvc := reader.NewService(db, log)
	envStorage := storage.NewStorage(db, log)

	svc := NewService(mapSvc, envSvc)

	require.NoError(t, mapSvc.ReadAndSave([]byte(testSvcMap), 10, spb.ToFullPath("test-service")))

	cases := []struct {
		layer common.Layer
		tvmID string
	}{
		{layer: common.Test, tvmID: "10"},
		{layer: common.Prod, tvmID: "20"},
	}

	for _, c := range cases {
		t.Run(c.layer.String(), func(t *testing.T) {
			require.NoError(t, envStorage.Save(&storage.ExternalEnv{
				Service: "test-service",
				Layer:   c.layer,
				Type:    env.EnvType_GENERATED_TVM_ID,
				Key:     "tvm-id",
				Value:   c.tvmID,
			}))

			envs, err := svc.Resolve("shiva", templates, c.layer)
			require.NoError(t, err)

			assert.Equal(t, c.tvmID, envs["tvm"])
		})
	}
}

func Test_Resolve_InvalidTemplates(t *testing.T) {
	const (
		env     = "ENV"
		service = "shiva"
	)

	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	svcMap := service_map.NewService(db, log, service_change.NewNotificationMock())
	envSvc := reader.NewService(db, log)
	svc := NewService(svcMap, envSvc)

	require.NoError(t, svcMap.ReadAndSave([]byte(testSvcMap), 10, spb.ToFullPath("test-service")))

	cases := []struct {
		template string
		err      *user_error.UserError
	}{
		{
			template: "${tvm-id:shiva}",
			err:      tmp_errors.NewErrInvalidTvmID(env),
		},
		{
			template: "${tvm-id:gh-app}",
			err:      tmp_errors.NewErrUnknownService(env, "gh-app"),
		},
		{
			template: "${url:gh-app:api}",
			err:      tmp_errors.NewErrUnknownService(env, "gh-app"),
		},
		{
			template: "${url:test-service:public}",
			err:      tmp_errors.NewErrInvalidProvider(env, "test-service", "public"),
		},
		{
			template: "${tvm-id",
			err:      tmp_errors.NewErrInvalidTvmIDTemplate("${tvm-id"),
		},
	}

	for _, c := range cases {
		t.Run(c.template, func(t *testing.T) {
			envs := map[string]string{
				env: c.template,
			}

			_, err := svc.Resolve(service, envs, common.Prod)

			var userError *user_error.UserErrors
			require.True(t, errors.As(err, &userError))

			assert.Equal(t, *user_error.NewUserErrors(c.err), *userError)
		})
	}
}
