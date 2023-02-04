package test

import (
	"github.com/YandexClassifieds/drills-helper/conf"
	"github.com/spf13/viper"
	"os"
	"sync"
)

var (
	once = &sync.Once{}
)

func InitTestEnv() {
	once.Do(func() {
		if os.Getenv("CI") != "" {
			viper.AutomaticEnv()
		}

		err := conf.LoadConfig("dev", "local")
		if err != nil {
			panic(err)
		}
	})
}
