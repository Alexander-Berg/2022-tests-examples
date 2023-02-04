package client

import (
	"fmt"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestNewFlags(t *testing.T) {
	type env struct {
		WithHTTP      string
		WithLogbroker string
	}

	setEnv := func(env env) {
		_ = os.Setenv("PAYOUT_HTTP_CPF_SENDER", env.WithHTTP)
		_ = os.Setenv("PAYOUT_LOGBROKER_CPF_SENDER", env.WithLogbroker)
	}

	testCases := []struct {
		Env                   env
		ExpectedWithHTTP      bool
		ExpectedWithLogbroker bool
	}{
		{
			ExpectedWithHTTP:      false,
			ExpectedWithLogbroker: false,
		},
		{
			Env: env{
				WithHTTP: "+",
			},
			ExpectedWithHTTP:      true,
			ExpectedWithLogbroker: false,
		},
		{
			Env: env{
				WithLogbroker: "+",
			},
			ExpectedWithHTTP:      false,
			ExpectedWithLogbroker: true,
		},
		{
			Env: env{
				WithHTTP:      "+",
				WithLogbroker: "true",
			},
			ExpectedWithHTTP:      true,
			ExpectedWithLogbroker: true,
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(fmt.Sprint(i), func(t *testing.T) {
			setEnv(c.Env)

			flags := NewFlags()

			t.Run("with_http", func(t *testing.T) {
				assert.Equal(t, c.ExpectedWithHTTP, flags.WithHTTP())
			})

			t.Run("with_logbroker", func(t *testing.T) {
				assert.Equal(t, c.ExpectedWithLogbroker, flags.WithLogbroker())
			})
		})
	}
}
