package format_test

import (
	"testing"

	"github.com/YandexClassifieds/vtail/api/core"
	"github.com/YandexClassifieds/vtail/cmd/cli/commands/format"
	"github.com/sirupsen/logrus"
	"github.com/stretchr/testify/require"
)

func TestRest(t *testing.T) {
	logger := logrus.New()
	logger.Level = logrus.DebugLevel

	formatter := format.NewFormatter(format.Slim, []string{"rest"}, logger)
	rendered := formatter.Render(&core.LogMessage{
		Rest: `{"body":"\"-\""}`,
	})
	require.Equal(t, "\n  {body: '\"-\"'}", rendered)
}
