package actions

import (
	"testing"

	"github.com/YandexClassifieds/cms/test"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
)

func TestAction_Run(t *testing.T) {
	test.InitTestEnv()
	log := logrus.New()

	scriptPrefix = "/usr/bin/"
	t.Cleanup(func() {
		scriptPrefix = "/usr/sbin/"
	})

	a := NewAction("whoami", log)
	require.NoError(t, a.Run())
}
