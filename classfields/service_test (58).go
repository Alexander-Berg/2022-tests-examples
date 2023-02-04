package validator

import (
	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/YandexClassifieds/shiva/pkg/include/domain"
	"github.com/YandexClassifieds/shiva/test"
	"testing"
)

const (
	commonYml = "common.yml"
	testYml   = "test.yml"
)

var commonConf = &domain.Include{
	Path: commonYml,
	Value: map[string]string{
		"PARAM_1": "param 1",
		"PARAM_2": "param 2",
	},
}
var testConf = &domain.Include{
	Path: testYml,
	Value: map[string]string{
		"PARAM_3": "param 3",
		"PARAM_4": "param 4",
		"PARAM_5": "param 5",
	},
}

func TestSuccessValidate(t *testing.T) {
	s := NewService(test.NewLogger(t))
	errors := s.Validate(testConf)
	test.AssertUserErrors(t, errors, user_error.NewUserErrors())
	errors = s.Validate(commonConf)
	test.AssertUserErrors(t, errors, user_error.NewUserErrors())
}

func TestKeyFormatError(t *testing.T) {

	s := NewService(test.NewLogger(t))
	cases := []string{"param", "PArAM", "PARAM-1", "param", "param.param", "PARAM.PARAM", "PARAM/PARAM"}
	for _, c := range cases {
		t.Run(c, func(t *testing.T) {
			test.AssertUserErrors(t, s.Validate(getConf(c)), user_error.NewUserErrors(KeyFormatError(c)))
		})
	}
}

func getConf(key string) *domain.Include {
	value := map[string]string{}
	for k, v := range testConf.Value {
		value[k] = v
	}
	value[key] = key
	return &domain.Include{
		Path:  testYml,
		Value: value,
	}
}

func getJoin(key string) []*domain.Include {
	return []*domain.Include{commonConf, getConf(key)}
}
