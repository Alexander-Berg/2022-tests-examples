package testutil

import (
	"github.com/YandexClassifieds/logs/pkg/config"
	"github.com/spf13/viper"
	"testing"
)

func Init(t testing.TB) {
	t.Helper()
	viper.AddConfigPath(".")
	viper.AddConfigPath("..")
	viper.AddConfigPath("../..")
	viper.AddConfigPath("../../..")
	viper.AddConfigPath("../../../..")
	viper.AddConfigPath("../../../../..")

	err := config.LoadConfig("test")
	if err != nil {
		t.Fatal(err)
	}
}
