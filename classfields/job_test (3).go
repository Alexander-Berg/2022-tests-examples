package job

import (
	"context"
	"fmt"
	"testing"

	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/stretchr/testify/require"
	"service-log-keeper/pkg/shiva"
	"service-log-keeper/pkg/yql"
)

const testTable = "2022-07-26"

func TestJob_Run(t *testing.T) {
	c := viper.NewTestConf()
	l := logrus.NewLogger()

	yqlSvc := yql.NewService(c)
	shivaSvc := shiva.NewService(c, l)

	t.Cleanup(func() {
		err := yqlSvc.Run(context.Background(), `USE hahn;
PRAGMA yt.TmpFolder = "home/verticals/.tmp";
DROP TABLE `+fmt.Sprintf("`%s/%s`", c.Str("DST_PATH"), testTable)+`;`, nil)
		require.NoError(t, err)
	})

	j := New(c, yqlSvc, shivaSvc, l)
	j.Run()

	err := yqlSvc.Run(context.Background(), `USE hahn;
SELECT Ensure(COUNT(*), COUNT(*) == 100)
FROM `+fmt.Sprintf("`%s/%s`", c.Str("DST_PATH"), testTable)+`
WHERE _service="shiva-test-pcidss"`, nil)
	require.NoError(t, err)
}
