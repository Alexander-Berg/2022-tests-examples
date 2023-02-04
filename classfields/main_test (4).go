package handlers

import (
	"github.com/YandexClassifieds/h2p/common/idm"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestServiceToRole(t *testing.T) {
	cases := map[string]struct {
		Service string
		Role    idm.RoleData
	}{
		"service": {
			Service: "/service/shiva/provides/public/",
			Role: idm.RoleData{
				Service:  "shiva",
				Provides: "public",
			},
		},
		"serviceWOSlashes": {
			Service: "service/shiva/provides/public",
			Role: idm.RoleData{
				Service:  "shiva",
				Provides: "public",
			},
		},
		"mysqlOwner": {
			Service: "/service/mysql/instance/mdb0000000/database/owner/",
			Role: idm.RoleData{
				Service:  "mysql",
				Instance: "mdb0000000",
				Database: "owner",
			},
		},
		"mysqlMode": {
			Service: "/service/mysql/instance/mdb0000000/database/h2p_idm/mode/rw/",
			Role: idm.RoleData{
				Service:  "mysql",
				Instance: "mdb0000000",
				Database: "h2p_idm",
				Mode:     "rw",
			},
		},
	}

	for name, tc := range cases {
		t.Run(name, func(t *testing.T) {
			role := serviceToRole(tc.Service)
			assert.Equal(t, tc.Role, *role)
		})
	}
}

func TestFieldsToRole(t *testing.T) {
	cases := map[string]struct {
		Sox    bool
		Ticket string
		Role   idm.FieldsData
	}{
		"sox": {
			Sox:    true,
			Ticket: "TICKET-1",
			Role: idm.FieldsData{
				Sox:    true,
				Ticket: "TICKET-1",
			},
		},
		"notSox": {
			Role: idm.FieldsData{},
		},
	}

	for name, tc := range cases {
		t.Run(name, func(t *testing.T) {
			role := fieldsToRole(tc.Sox, tc.Ticket)
			assert.Equal(t, tc.Role, *role)
		})
	}
}
