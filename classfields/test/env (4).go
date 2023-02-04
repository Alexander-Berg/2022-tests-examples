package test

import (
	"encoding/base64"
	"os"
	"sync"
	"time"

	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/spf13/viper"
)

var (
	mx = sync.Once{}
)

func InitTestEnv() {

	mx.Do(func() {
		var err error
		location, err := time.LoadLocation("Europe/Moscow")
		if err != nil {
			panic("init test fail: location")
		}
		time.Local = location

		if os.Getenv("CI") != "" {
			viper.AutomaticEnv()
			err = config.LoadConfig("", config.CommonTestCfg, config.TestCfg)
		} else {
			err = config.LoadConfig("", config.CommonTestCfg, config.TestCfg, config.LocalCfg)
		}
		if err != nil {
			panic(err)
		}
	})
}

func InitTestEnvGithubApp() {

	InitTestEnv()
	// use secret key
	envKey := base64.StdEncoding.EncodeToString([]byte("none"))
	viper.SetDefault("GITHUB_KEY", envKey)
}
