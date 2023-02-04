package client

import (
	"testing"

	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/stretchr/testify/assert"
)

func Test(t *testing.T) {
	conf := viper.NewTestConf()
	assert.Equal(t, "test_service1", conf.Str("KEY1"))
	assert.Equal(t, "test_service1", conf.Str("KEY2"))
	assert.Equal(t, "local_service1", conf.Str("KEY3"))
	assert.Equal(t, "local", conf.Str("KEY4"))
	assert.Equal(t, "local_service1", conf.Str("KEY5"))
	assert.Equal(t, "test", conf.Str("KEY6"))
	assert.Equal(t, "local_service1", conf.Str("KEY7"))
	assert.Equal(t, "local", conf.Str("KEY8"))
}
