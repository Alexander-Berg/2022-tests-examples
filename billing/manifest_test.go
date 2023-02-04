package manifest

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestManifest_Check(t *testing.T) {
	tests := []struct {
		testname  string
		manifest  Manifest
		withError bool
	}{
		{
			"good manifest",
			Manifest{
				Endpoints: map[string]Endpoint{
					"good_1": {
						Actions: Actions{
							{Name: "A", Stage: "before_lock", Deps: []string{}},
							{Name: "B", Stage: "before_lock", Deps: []string{"A"}},
						},
					},
					"good_2": {
						Actions: Actions{
							{Name: "C", Stage: "before_lock", Deps: []string{}},
							{Name: "D", Stage: "before_lock", Deps: []string{"C"}},
						},
					},
				},
			},
			false,
		},
		{
			"error if cycle in manifest's endpoint",
			Manifest{
				Endpoints: map[string]Endpoint{
					"good_1": {
						Actions: Actions{
							{Name: "A", Stage: "before_lock", Deps: []string{}},
							{Name: "B", Stage: "before_lock", Deps: []string{"A"}},
						},
					},
					"bad_2": {
						Actions: Actions{
							{Name: "A", Stage: "before_lock", Deps: []string{"B"}},
							{Name: "B", Stage: "before_lock", Deps: []string{"A"}},
						},
					},
				},
			},
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			err := test.manifest.Check()
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
			}
		})
	}
}

func TestManifest_GetEndpoint(t *testing.T) {
	tests := []struct {
		testname  string
		manifest  Manifest
		name      string
		wantError error
	}{
		{
			"get existing point",
			Manifest{
				Endpoints: map[string]Endpoint{
					"1": {
						Actions: Actions{
							{Name: "A", Stage: "before_lock", Deps: []string{}},
						},
					},
				},
			},
			"1",
			nil,
		},
		{
			"error if unknown endpoint",
			Manifest{
				Endpoints: map[string]Endpoint{},
			},
			"1",
			ErrorUnknownEndpoint,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			e, err := test.manifest.GetEndpoint(test.name)
			if test.wantError != nil {
				require.ErrorIs(t, test.wantError, err)
			} else {
				require.NoError(t, err)
				require.Equal(t, test.manifest.Endpoints[test.name], *e)
			}
		})
	}
}

func TestManifests_Check(t *testing.T) {
	tests := []struct {
		testname  string
		manifests Manifests
		withError bool
	}{
		{
			"good (empty) manifests",
			Manifests{{Namespace: "taxi"}, {Namespace: "drive"}},
			false,
		},
		{
			"error if one of manifests is bad",
			Manifests{
				{
					Namespace: "taxi",
					Endpoints: map[string]Endpoint{
						"1": {
							Actions: Actions{
								{Name: "A", Stage: "before_lock", Deps: []string{}},
								{Name: "A", Stage: "before_lock", Deps: []string{}},
							},
						},
					},
				},
				{Namespace: "drive"},
			},
			true,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			err := test.manifests.Check()
			if test.withError {
				require.Error(t, err)
			} else {
				require.NoError(t, err)
			}
		})
	}
}

func TestManifests_GetManifest(t *testing.T) {
	tests := []struct {
		testname  string
		manifests Manifests
		namespace string
		wantError error
	}{
		{
			"get existing manifest",
			Manifests{{Namespace: "taxi"}},
			"taxi",
			nil,
		},
		{
			"error if unknown manifest",
			Manifests{{Namespace: "taxi"}},
			"drive",
			ErrorUnknownNamespace,
		},
	}

	for _, test := range tests {
		t.Run(test.testname, func(t *testing.T) {
			m, err := test.manifests.GetManifest(test.namespace)
			if test.wantError != nil {
				require.ErrorIs(t, test.wantError, err)
			} else {
				require.NoError(t, err)
				require.NotEmpty(t, m)
			}
		})
	}
}
