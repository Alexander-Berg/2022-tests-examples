package service1

import (
	"testing"

	"github.com/YandexClassifieds/go-common/conf/viper"
)

func Test(t *testing.T) {
	viper.NewConf("service1")
}
