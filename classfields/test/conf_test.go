package test

import (
	"testing"

	"github.com/YandexClassifieds/go-common/conf"
	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/stretchr/testify/assert"
)

func TestInterface(t *testing.T) {
	var c conf.Conf
	c =  &viper.Conf{}
	assert.NotNil(t, c)
}

