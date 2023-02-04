package soxUpdater

import (
	"github.com/YandexClassifieds/h2p/common/idm"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestGetSoxFields(t *testing.T) {
	fields := GetSoxFields(true)
	assert.Equal(t, &[]idm.RoleField{{
		Type: idm.BooleanField,
		Slug: "sox",
		Name: idm.RoleHelp{
			Ru: soxNameRU,
			En: soxNameEN,
		},
	},
		{
			Type: idm.CharField,
			Slug: "ticket",
			Name: idm.RoleHelp{
				Ru: ticketNameRU,
				En: ticketNameEN,
			},
			Required: true,
		}}, fields)

	fields = GetSoxFields(false)
	assert.Nil(t, fields)
}
