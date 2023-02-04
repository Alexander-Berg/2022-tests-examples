package test

import (
	"github.com/spf13/viper"
	"testing"
)

func InitConfig(t *testing.T) {
	t.Helper()

	viper.SetConfigType("env")
	viper.AddConfigPath(".")
	viper.AddConfigPath("..")
	viper.AddConfigPath("../..")
	viper.AddConfigPath("../../..")
	viper.AddConfigPath("../../../..")
	viper.AddConfigPath("../../../../..")

	names := []string{"test", "local"}

	for _, name := range names {
		viper.SetConfigName(name)
		err := viper.MergeInConfig()
		if err != nil {
			t.Fatal(err)
		}
	}

	viper.Set("listen_deadline", "5s")
}
