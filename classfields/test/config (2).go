package test

import (
	"github.com/YandexClassifieds/grafana-infra-sync/config"
	"github.com/spf13/viper"
	"log"
)

func InitTestConfig() {
	viper.AddConfigPath(".")
	viper.AddConfigPath("..")
	viper.AddConfigPath("../..")
	viper.AddConfigPath("../../..")
	viper.AddConfigPath("../../../..")
	viper.AddConfigPath("../../../../..")
	viper.AutomaticEnv()
	err := config.LoadConfig("test.env")
	if err != nil {
		log.Fatal(err)
	}
}
