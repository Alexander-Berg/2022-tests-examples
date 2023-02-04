package utils

import (
	"fmt"
	"testing"

	sq "github.com/Masterminds/squirrel"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/billing/hot/accounts/pkg/core/entities"
)

func TestGetLocFilter(t *testing.T) {
	loc := entities.Location{
		Namespace:  "qqq",
		Type:       "abyrvalg",
		Attributes: [5]string{"1", "2", "3", "4", "5"},
	}

	res := GetLocFilter(loc)

	assert.Equal(t, res, sq.And{
		sq.Eq{"namespace": "qqq"},
		sq.Eq{"type": "abyrvalg"},
		sq.Eq{"attribute_1": "1"},
		sq.Eq{"attribute_2": "2"},
		sq.Eq{"attribute_3": "3"},
		sq.Eq{"attribute_4": "4"},
		sq.Eq{"attribute_5": "5"},
	})
}

func TestGetLocRefs(t *testing.T) {
	loc := entities.Location{
		Namespace:  "abyrvalg1",
		Type:       "abyrvalg2",
		ShardKey:   "abyrvalg3",
		Attributes: [5]string{"1", "2", "3", "4", "5"},
	}

	res := GetLocRefs(&loc)

	assert.Equal(t, res, []any{
		&loc.Namespace,
		&loc.Type,
		&loc.ShardKey,
		&loc.Attributes[0],
		&loc.Attributes[1],
		&loc.Attributes[2],
		&loc.Attributes[3],
		&loc.Attributes[4],
	})
}

func TestGetAccountLocRefs(t *testing.T) {
	loc := entities.AccountLocation{
		Loc: entities.Location{
			Namespace:  "abyrvalg0",
			Type:       "abyrvalg1",
			ShardKey:   "abyrvalg2",
			Attributes: [5]string{"1", "2", "3", "4", "5"},
		},
		AddAttributes: [5]string{"6", "7", "8", "9", "10"},
	}

	res := GetAccountLocRefs(&loc)

	assert.Equal(t, res, []any{
		&loc.Loc.Namespace,
		&loc.Loc.Type,
		&loc.Loc.ShardKey,
		&loc.Loc.Attributes[0],
		&loc.Loc.Attributes[1],
		&loc.Loc.Attributes[2],
		&loc.Loc.Attributes[3],
		&loc.Loc.Attributes[4],
		&loc.AddAttributes[0],
		&loc.AddAttributes[1],
		&loc.AddAttributes[2],
		&loc.AddAttributes[3],
		&loc.AddAttributes[4],
	})
}

func TestGetSelectQuery(t *testing.T) {
	loc := entities.Location{
		Namespace:  "abyrvalg0",
		Type:       "abyrvalg2",
		Attributes: [5]string{"1", "2", "3", "4", "5"},
	}
	tableName := "some_table"
	cols := []string{"column1", "column2"}
	res := GetSelectQuery([]entities.Location{loc}, cols, tableName)

	for i := 1; i <= entities.AttributesCount; i++ {
		cols = append(cols, fmt.Sprintf("attribute_%d", i))
	}

	assert.Equal(t, res, sq.Select(cols...).From(tableName).Where(
		sq.Or{
			sq.And{
				sq.Eq{"namespace": "abyrvalg0"},
				sq.Eq{"type": "abyrvalg2"},
				sq.Eq{"attribute_1": "1"},
				sq.Eq{"attribute_2": "2"},
				sq.Eq{"attribute_3": "3"},
				sq.Eq{"attribute_4": "4"},
				sq.Eq{"attribute_5": "5"},
			},
		}))
}
