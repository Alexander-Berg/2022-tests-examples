package mongo

import (
	"fmt"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
	"go.mongodb.org/mongo-driver/mongo/options"
)

func TestSetWriteConcern(t *testing.T) {
	ws := []any{"majority", "error", 0, 1, 2}
	js := []bool{true, false}
	wTimeouts := []time.Duration{0, 1, 500}
	for _, w := range ws {
		for _, j := range js {
			for _, wTimeout := range wTimeouts {
				t.Run(strings.Join([]string{
					fmt.Sprintf("%v", w),
					fmt.Sprintf("%v", j),
					fmt.Sprintf("%v", wTimeout)},
					"-"), func(t *testing.T) {
					clientOptions := options.Client()
					writeConcern := WriteConcern{W: &w, J: &j, WTimeout: &wTimeout}
					err := setWriteConcern(clientOptions, &writeConcern)
					if w != "error" {
						require.NoError(t, err)
						require.Equal(t, w, clientOptions.WriteConcern.GetW())
						require.Equal(t, j, clientOptions.WriteConcern.GetJ())
						require.Equal(t, wTimeout, clientOptions.WriteConcern.GetWTimeout())
					} else {
						require.Error(t, err)
					}
				})
			}
		}
	}
	t.Run("nil-nil-nil", func(t *testing.T) {
		clientOptions := options.Client()
		writeConcern := WriteConcern{}
		err := setWriteConcern(clientOptions, &writeConcern)
		require.NoError(t, err)
		require.Nil(t, clientOptions.WriteConcern)
	})
	t.Run("1-nil-nil", func(t *testing.T) {
		clientOptions := options.Client()
		w := any(1)
		writeConcern := WriteConcern{W: &w}
		err := setWriteConcern(clientOptions, &writeConcern)
		require.NoError(t, err)
		require.Equal(t, 1, clientOptions.WriteConcern.GetW())
		require.Equal(t, false, clientOptions.WriteConcern.GetJ())
		require.Equal(t, time.Duration(0), clientOptions.WriteConcern.GetWTimeout())
	})
}
