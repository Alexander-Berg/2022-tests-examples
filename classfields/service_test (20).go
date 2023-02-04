package yql

import (
	"context"
	"testing"
	"time"

	"github.com/YandexClassifieds/go-common/conf/viper"
	"github.com/stretchr/testify/require"
)

const (
	testQuery = `USE hahn;

DECLARE $x AS List<String>;
SELECT ListConcat($x, ",") as result;`
	testQuery2 = `SELECT "4" as result;`
)

func TestService_Run(t *testing.T) {
	c := viper.NewTestConf()

	ctx, cancel := context.WithTimeout(context.Background(), 1*time.Minute)
	defer cancel()

	s := NewService(c)
	err := s.Run(ctx, testQuery, map[string]interface{}{"x": []string{"42"}})
	require.NoError(t, err)

	err = s.Run(ctx, testQuery2, nil)
	require.NoError(t, err)
}
