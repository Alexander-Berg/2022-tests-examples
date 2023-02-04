package app

import (
	"testing"

	"github.com/YandexClassifieds/drills-helper/test"
	"github.com/sirupsen/logrus"
)

func TestApp_InitCommands(_ *testing.T) {
	test.InitTestEnv()

	app := New(logrus.New())
	app.InitCommands()
}
