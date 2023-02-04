package services

import (
	_ "embed"
	"testing"

	"github.com/YandexClassifieds/shiva/common"
	"github.com/YandexClassifieds/shiva/common/user_error"
	spb "github.com/YandexClassifieds/shiva/pb/shiva/service_map"
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

func Test_Match(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	mapSvc := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())

	svc := NewResolver(mapSvc)

	cases := []struct {
		env        string
		isTemplate bool
	}{
		{
			env:        "${pOrt:shiva:ci}",
			isTemplate: true,
		},
		{
			env:        "{host:shiva:ci}",
			isTemplate: false,
		},
		{
			env:        "${pOrT:shiva-tg:admin}",
			isTemplate: true,
		},
		{
			env:        "${url:shiva1:main}",
			isTemplate: true,
		},
		{
			env:        "${url:shiva}",
			isTemplate: true,
		},
	}

	for _, c := range cases {
		t.Run(c.env, func(t *testing.T) {
			assert.Equal(t, c.isTemplate, svc.Match(c.env))
		})
	}
}

func Test_Validate(t *testing.T) {
	const (
		env = "TEST_SERVICE"
	)

	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	mapSvc := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())

	require.NoError(t, mapSvc.ReadAndSave([]byte(testSvcMap), 10, spb.ToFullPath("test-service")))

	svc := NewResolver(mapSvc)

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
			template:    "${port:shiva:http-admin}",
			expectedErr: tmp_errors.NewErrUnknownService(env, "shiva"),
		},
		{
			template:    "${port:shiva:api:qwerty}",
			expectedErr: tmp_errors.NewErrInvalidSvcAddressTemplate("${port:shiva:api:qwerty}"),
		},
		{
			template:    "${tvm-id:shiva}",
			expectedErr: tmp_errors.NewErrParseTemplate("${tvm-id:shiva}"),
		},
	}

	for _, c := range cases {
		t.Run(c.template, func(t *testing.T) {
			err := svc.Validate("shiva", env, c.template)

			assert.Equal(t, c.expectedErr, err)
		})
	}
}

func Test_Resolve(t *testing.T) {
	test.RunUp(t)
	defer test.Down(t)

	db := test_db.NewDb(t)
	mapSvc := service_map.NewService(db, test.NewLogger(t), service_change.NewNotificationMock())

	require.NoError(t, mapSvc.ReadAndSave([]byte(testSvcMap), 10, spb.ToFullPath("test-service")))

	svc := NewResolver(mapSvc)

	cases := []struct {
		template string
		result   string
		err      *user_error.UserError
	}{
		{
			template: "${port:test-service:api}",
			err:      nil,
			result:   "80",
		},
		{
			template: "${host:test-service:api}",
			err:      nil,
			result:   "test-service-api.vrts-slb.test.vertis.yandex.net",
		},
		{
			template: "${url:test-service:api}",
			err:      nil,
			result:   "http://test-service-api.vrts-slb.test.vertis.yandex.net:80",
		},
		{
			template: "${url:test-service:grpc}",
			result:   "",
			err:      tmp_errors.NewErrInvalidProvider("ENV", "test-service", "grpc"),
		},
		{
			template: "${url:shiva-tg:grpc}",
			result:   "",
			err:      tmp_errors.NewErrUnknownService("ENV", "shiva-tg"),
		},
		{
			template: "{url:shiva-tg:grpc}",
			result:   "",
			err:      tmp_errors.NewErrParseTemplate("{url:shiva-tg:grpc}"),
		},
	}

	for _, c := range cases {
		t.Run(c.template, func(t *testing.T) {
			res, err := svc.Resolve("shiva", "ENV", c.template, common.Test)

			assert.Equal(t, c.err, err)
			assert.Equal(t, c.result, res)
		})
	}
}
