package test

import (
	"database/sql"
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"github.com/spf13/viper"
	"github.com/stretchr/testify/assert"
	"testing"
)

func NewDB(t *testing.T) *sql.DB {
	t.Helper()

	dsn := fmt.Sprintf("%v:%v@tcp(%v)/h2p_idm",
		viper.GetString("MYSQL_LOGIN"),
		viper.GetString("MYSQL_PASSWORD"),
		viper.GetString("MYSQL_MASTER"))
	db, err := sql.Open("mysql", dsn)
	assert.NoError(t, err)

	t.Cleanup(func() {
		tables := []string{"acl", "services", "mysql", "owners", "postgresql"}

		for _, table := range tables {
			_, err := db.Exec(fmt.Sprintf("TRUNCATE TABLE %s", table))
			assert.NoError(t, err, "db truncate failed")
		}
	})

	return db
}
