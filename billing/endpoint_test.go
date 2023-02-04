package manifest

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestEndpoint_Check(t *testing.T) {
	tests := []struct {
		testname  string
		endpoint  Endpoint
		withError bool
	}{
		{
			"good endpoint",
			Endpoint{
				Actions: Actions{
					{Name: "A", Stage: "before_lock", Deps: []string{}},
					{Name: "B", Stage: "before_lock", Deps: []string{"A"}},
				},
			},
			false,
		},
		{
			"error if cycle in endpoint",
			Endpoint{
				Actions: Actions{
					{Name: "A", Stage: "before_lock", Deps: []string{"B"}},
					{Name: "B", Stage: "before_lock", Deps: []string{"A"}},
				},
			},
			true,
		},
		{
			"error if multiple identical names are in endpoint",
			Endpoint{
				Actions: Actions{
					{Name: "A", Stage: "before_lock", Deps: []string{}},
					{Name: "A", Stage: "before_lock", Deps: []string{}},
				},
			},
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			err := test.endpoint.Check()
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
			}
		})
	}
}
