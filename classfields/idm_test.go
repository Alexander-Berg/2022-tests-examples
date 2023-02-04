package app

import (
	"github.com/YandexClassifieds/h2p/common/idm"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestGetSlugPath(t *testing.T) {
	tests := map[string]struct {
		Kind      Kind
		ValuePath string

		ExpectedSlugPath string
	}{
		"serviceListen": {
			Kind:             Service,
			ValuePath:        "/service/provides/",
			ExpectedSlugPath: "/service/service/provides/provides/",
		},
		"serviceQuery": {
			Kind:             Service,
			ValuePath:        "/service/provides/",
			ExpectedSlugPath: "/service/service/provides/provides/",
		},
		"mysqlListen": {
			Kind:             Mysql,
			ValuePath:        "/mysql/vertis/some-database/ro/",
			ExpectedSlugPath: "/service/mysql/instance/vertis/database/some-database/mode/ro/",
		},
		"mysqlQuery": {
			Kind:             Mysql,
			ValuePath:        "/mysql/vertis/some-database/rw/",
			ExpectedSlugPath: "/service/mysql/instance/vertis/database/some-database/mode/rw/",
		},
		"postgresqlListen": {
			Kind:             Postgresql,
			ValuePath:        "/postgresql/vertis/some-database/ro/",
			ExpectedSlugPath: "/service/postgresql/instance/vertis/database/some-database/mode/ro/",
		},
		"postgresqlQuery": {
			Kind:             Postgresql,
			ValuePath:        "/postgresql/vertis/some-database/rw/",
			ExpectedSlugPath: "/service/postgresql/instance/vertis/database/some-database/mode/rw/",
		},
		"serviceTrimmed": {
			Kind:             Service,
			ValuePath:        "service/provides",
			ExpectedSlugPath: "/service/service/provides/provides/",
		},
		"mysqlTrimmed": {
			Kind:             Mysql,
			ValuePath:        "mysql/vertis/some-database/ro",
			ExpectedSlugPath: "/service/mysql/instance/vertis/database/some-database/mode/ro/",
		},
		"postgresqlTrimmed": {
			Kind:             Postgresql,
			ValuePath:        "postgresql/vertis/some-database/ro",
			ExpectedSlugPath: "/service/postgresql/instance/vertis/database/some-database/mode/ro/",
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			a := App{
				s: state{
					kind: tc.Kind,
				},
			}
			slugPath, err := a.getSlugPath(tc.ValuePath)
			assert.NoError(t, err)
			assert.Equal(t, slugPath, tc.ExpectedSlugPath)
		})
	}
}

func TestHasSoxField(t *testing.T) {
	tests := map[string]struct {
		Node *idm.APINodeDescription

		ExpectedResult bool
	}{
		"has": {
			Node: &idm.APINodeDescription{
				Fields: []idm.RoleField{
					{
						Type: idm.BooleanField,
						Slug: "sox",
					},
				},
			},
			ExpectedResult: true,
		},
		"has not": {
			Node: &idm.APINodeDescription{
				Fields: []idm.RoleField{
					{
						Type: idm.BooleanField,
						Slug: "notASox",
					},
				},
			},
			ExpectedResult: false,
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			a := App{}
			has := a.hasSoxField(tc.Node)
			assert.Equal(t, has, tc.ExpectedResult)
		})
	}
}
