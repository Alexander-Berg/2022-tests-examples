package database

import (
	"database/sql"
	"database/sql/driver"
	"fmt"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/h2p/cmd/h2p-idm/models"
	"github.com/YandexClassifieds/h2p/common/idm"
	"github.com/YandexClassifieds/h2p/test"
	"github.com/stretchr/testify/assert"
)

func TestIntegrate_Entries(t *testing.T) {
	tests := map[string]struct {
		Entry *models.Entry
	}{
		"service": {
			Entry: &models.Entry{
				Type:     models.Service,
				Name:     "test-service",
				Provides: "test-provides",
			},
		},
		"mysql": {
			Entry: &models.Entry{
				Type:     models.Mysql,
				Name:     "some-name",
				Cluster:  "mdb0000000",
				Database: "some-db",
				Mode:     "ro",
				Sox:      true,
			},
		},
		"postgresql": {
			Entry: &models.Entry{
				Type:     models.Postgresql,
				Name:     "some-name",
				Cluster:  "mdb0000000",
				Database: "some-db",
				Mode:     "rw",
				Sox:      false,
			},
		},
	}

	test.InitConfig(t)
	db := test.NewDB(t)

	m := Mngr{
		Ro:  db,
		Rw:  db,
		log: logrus.New("info"),
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			err := m.AddEntry(tc.Entry)
			assert.NoError(t, err)

			entries, err := m.GetEntries(tc.Entry.Type)
			assert.NoError(t, err)

			tc.Entry.Old = true
			assert.EqualValues(t, tc.Entry, entries.Get(tc.Entry.Id()))

			err = m.DeleteEntry(tc.Entry)
			assert.NoError(t, err)

			entries, err = m.GetEntries(tc.Entry.Type)
			assert.NoError(t, err)
			assert.Nil(t, entries[tc.Entry.Id()])
		})
	}
}

func TestIntegrate_Owners(t *testing.T) {
	test.InitConfig(t)
	db := test.NewDB(t)

	m := Mngr{
		Ro:  db,
		Rw:  db,
		log: logrus.New("info"),
	}

	assert.NoError(t, m.AddOwner("test-service", idm.Owner{Id: "11", Type: idm.GROUP}))
	assert.NoError(t, m.AddOwner("test-service", idm.Owner{Id: "some-login", Type: idm.USER}))
	assert.NoError(t, m.AddOwner("test-service2", idm.Owner{Id: "some-login2", Type: idm.USER}))

	owners, err := m.GetOwners()
	assert.NoError(t, err)
	assert.ElementsMatch(t, owners["test-service"], []idm.Owner{{Id: "11", Type: idm.GROUP}, {Id: "some-login", Type: idm.USER}})
	assert.ElementsMatch(t, owners["test-service2"], []idm.Owner{{Id: "some-login2", Type: idm.USER}})

	assert.NoError(t, m.DeleteOwner("test-service", idm.Owner{Id: "some-login", Type: idm.USER}))
	owners, err = m.GetOwners()
	assert.NoError(t, err)
	assert.ElementsMatch(t, owners["test-service"], []idm.Owner{{Id: "11", Type: idm.GROUP}})

	assert.NoError(t, m.DeleteOwner("test-service", idm.Owner{Id: "11", Type: idm.GROUP}))
	assert.NoError(t, m.DeleteOwner("test-service2", idm.Owner{Id: "some-login2", Type: idm.USER}))
	// double delete
	assert.NoError(t, m.DeleteOwner("test-service2", idm.Owner{Id: "some-login2", Type: idm.USER}))

	owners, err = m.GetOwners()
	assert.NoError(t, err)
	assert.Empty(t, owners["test-service"])
	assert.Empty(t, owners["test-service2"])
}

func TestIntegrate_Roles(t *testing.T) {
	test.InitConfig(t)
	db := test.NewDB(t)

	m := Mngr{
		Ro:  db,
		Rw:  db,
		log: logrus.New("info"),
	}

	role1 := models.Role{
		Login:   "test-login",
		Service: "test-service",
		Sox:     true,
		Ticket:  "VOID-1",
	}
	role2 := models.Role{
		Login:   "test-login2",
		Service: "test-service",
		Sox:     false,
	}

	assert.NoError(t, m.AddRole(role1))
	assert.NoError(t, m.AddRole(role2))

	roles, err := m.GetRoles()
	assert.NoError(t, err)
	assert.ElementsMatch(t, roles, []models.Role{role1, role2})

	assert.NoError(t, m.DeleteRole(role2))
	roles, err = m.GetRoles()
	assert.NoError(t, err)
	assert.ElementsMatch(t, roles, []models.Role{role1})

	assert.NoError(t, m.DeleteRole(role1))
	// double delete
	assert.NoError(t, m.DeleteRole(role1))

	roles, err = m.GetRoles()
	assert.NoError(t, err)
	assert.Empty(t, roles)
}

func TestMngr_AddEntry(t *testing.T) {
	tests := map[string]struct {
		Entry       *models.Entry
		SelectQuery string
		SelectCount int
		InsertQuery string
		InsertError error
		Args        []driver.Value
		ExpectedErr error
	}{
		"service": {
			Entry: &models.Entry{
				Name:     "service",
				Provides: "provides",
				Type:     models.Service,
			},
			SelectQuery: "SELECT COUNT(.*) FROM services",
			SelectCount: 0,
			InsertQuery: "INSERT INTO services",
			Args:        []driver.Value{"service", "provides", false},
		},
		"service-already": {
			Entry: &models.Entry{
				Name:     "service",
				Provides: "provides",
				Type:     models.Service,
			},
			SelectQuery: "SELECT COUNT(.*) FROM services",
			SelectCount: 1,
			Args:        []driver.Value{"service", "provides", false},
		},
		"service-error": {
			Entry: &models.Entry{
				Name:     "service",
				Provides: "provides",
				Type:     models.Service,
			},
			SelectQuery: "SELECT COUNT(.*) FROM services",
			SelectCount: 0,
			InsertQuery: "INSERT INTO services",
			InsertError: fmt.Errorf("test"),
			Args:        []driver.Value{"service", "provides", false},
			ExpectedErr: fmt.Errorf("can't insert to database: test"),
		},
		"mysql": {
			Entry: &models.Entry{
				Name:     "cluster-name",
				Cluster:  "cluster",
				Database: "db",
				Mode:     "ro",
				Type:     models.Mysql,
			},
			SelectQuery: "SELECT COUNT(.*) FROM mysql",
			SelectCount: 0,
			InsertQuery: "INSERT INTO mysql",
			Args:        []driver.Value{"cluster", "cluster-name", "db", "ro", false},
		},
		"mysql-already": {
			Entry: &models.Entry{
				Name:     "cluster-name",
				Cluster:  "cluster",
				Database: "db",
				Mode:     "ro",
				Type:     models.Mysql,
			},
			SelectQuery: "SELECT COUNT(.*) FROM mysql",
			SelectCount: 1,
			Args:        []driver.Value{"cluster", "cluster-name", "db", "ro", false},
		},
		"mysql-error": {
			Entry: &models.Entry{
				Name:     "cluster-name",
				Cluster:  "cluster",
				Database: "db",
				Mode:     "ro",
				Type:     models.Mysql,
			},
			SelectQuery: "SELECT COUNT(.*) FROM mysql",
			SelectCount: 0,
			InsertQuery: "INSERT INTO mysql",
			InsertError: fmt.Errorf("test"),
			Args:        []driver.Value{"cluster", "cluster-name", "db", "ro", false},
			ExpectedErr: fmt.Errorf("can't insert to database: test"),
		},
		"postgresql": {
			Entry: &models.Entry{
				Name:     "cluster-name",
				Cluster:  "cluster",
				Database: "db",
				Mode:     "ro",
				Type:     models.Postgresql,
			},
			SelectQuery: "SELECT COUNT(.*) FROM postgresql",
			SelectCount: 0,
			InsertQuery: "INSERT INTO postgresql",
			Args:        []driver.Value{"cluster", "cluster-name", "db", "ro", false},
		},
		"postgresql-already": {
			Entry: &models.Entry{
				Name:     "cluster-name",
				Cluster:  "cluster",
				Database: "db",
				Mode:     "ro",
				Type:     models.Postgresql,
			},
			SelectQuery: "SELECT COUNT(.*) FROM postgresql",
			SelectCount: 1,
			Args:        []driver.Value{"cluster", "cluster-name", "db", "ro", false},
		},
		"postgresql-error": {
			Entry: &models.Entry{
				Name:     "cluster-name",
				Cluster:  "cluster",
				Database: "db",
				Mode:     "ro",
				Type:     models.Postgresql,
			},
			SelectQuery: "SELECT COUNT(.*) FROM postgresql",
			SelectCount: 0,
			InsertQuery: "INSERT INTO postgresql",
			InsertError: fmt.Errorf("test"),
			Args:        []driver.Value{"cluster", "cluster-name", "db", "ro", false},
			ExpectedErr: fmt.Errorf("can't insert to database: test"),
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock := prepareDbInsertMock(t, tc.SelectQuery, tc.SelectCount, tc.InsertQuery, tc.InsertError, tc.Args)

			m := Mngr{
				log: logrus.New("info"),
				Rw:  db,
			}

			if tc.ExpectedErr != nil {
				assert.EqualError(t, m.AddEntry(tc.Entry), tc.ExpectedErr.Error())
			} else {
				assert.NoError(t, m.AddEntry(tc.Entry))
			}
			assert.NoError(t, mock.ExpectationsWereMet())
		})
	}
}

func TestMngr_DeleteEntry(t *testing.T) {
	tests := map[string]struct {
		Entry       *models.Entry
		DeleteQuery string
		DeleteError error
		Args        []driver.Value
		ExpectedErr error
	}{
		"service": {
			Entry: &models.Entry{
				Name:     "service",
				Provides: "provides",
				Type:     models.Service,
			},
			DeleteQuery: "DELETE FROM services",
			Args:        []driver.Value{"service", "provides", false},
		},
		"service-error": {
			Entry: &models.Entry{
				Name:     "service",
				Provides: "provides",
				Type:     models.Service,
			},
			DeleteQuery: "DELETE FROM services",
			DeleteError: fmt.Errorf("test"),
			Args:        []driver.Value{"service", "provides", false},
			ExpectedErr: fmt.Errorf("can't delete from database: test"),
		},
		"mysql": {
			Entry: &models.Entry{
				Name:     "cluster-name",
				Cluster:  "cluster",
				Database: "db",
				Mode:     "ro",
				Type:     models.Mysql,
			},
			DeleteQuery: "DELETE FROM mysql",
			Args:        []driver.Value{"cluster", "cluster-name", "db", "ro", false},
		},
		"mysql-error": {
			Entry: &models.Entry{
				Name:     "cluster-name",
				Cluster:  "cluster",
				Database: "db",
				Mode:     "ro",
				Type:     models.Mysql,
			},
			DeleteQuery: "DELETE FROM mysql",
			DeleteError: fmt.Errorf("test"),
			Args:        []driver.Value{"cluster", "cluster-name", "db", "ro", false},
			ExpectedErr: fmt.Errorf("can't delete from database: test"),
		},
		"postgresql": {
			Entry: &models.Entry{
				Name:     "cluster-name",
				Cluster:  "cluster",
				Database: "db",
				Mode:     "ro",
				Type:     models.Postgresql,
			},
			DeleteQuery: "DELETE FROM postgresql",
			Args:        []driver.Value{"cluster", "cluster-name", "db", "ro", false},
		},
		"postgresql-error": {
			Entry: &models.Entry{
				Name:     "cluster-name",
				Cluster:  "cluster",
				Database: "db",
				Mode:     "ro",
				Type:     models.Postgresql,
			},
			DeleteQuery: "DELETE FROM postgresql",
			DeleteError: fmt.Errorf("test"),
			Args:        []driver.Value{"cluster", "cluster-name", "db", "ro", false},
			ExpectedErr: fmt.Errorf("can't delete from database: test"),
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock := prepareDbDeleteMock(t, tc.DeleteQuery, tc.DeleteError, tc.Args)
			m := Mngr{
				log: logrus.New("info"),
				Rw:  db,
			}

			if tc.ExpectedErr != nil {
				assert.EqualError(t, m.DeleteEntry(tc.Entry), tc.ExpectedErr.Error())
			} else {
				assert.NoError(t, m.DeleteEntry(tc.Entry))
			}
			assert.NoError(t, mock.ExpectationsWereMet())
		})
	}
}

func TestMngr_GetEntries(t *testing.T) {
	tests := map[string]struct {
		Type            models.Type
		Query           string
		Rows            *sqlmock.Rows
		ExpectedEntries models.Entries
	}{
		"services": {
			Type:  models.Service,
			Query: "SELECT service, provides, sox FROM services",
			Rows: sqlmock.NewRows([]string{"service", "provides", "sox"}).
				AddRow("service1", "provides1", false).
				AddRow("service2", "provides1", true),
			ExpectedEntries: map[string]*models.Entry{
				"/service1/provides1/false": {
					Name:     "service1",
					Provides: "provides1",
					Sox:      false,
					Old:      true,
				},
				"/service2/provides1/true": {
					Name:     "service2",
					Provides: "provides1",
					Sox:      true,
					Old:      true,
				},
			},
		},
		"mysql": {
			Type:  models.Mysql,
			Query: "SELECT instance, cluster, db, mode, sox FROM mysql",
			Rows: sqlmock.NewRows([]string{"instance", "cluster", "db", "mode", "sox"}).
				AddRow("mdb00000", "instance1", "db1", "ro", false).
				AddRow("mdb11111", "instance1", "db2", "rw", true),
			ExpectedEntries: map[string]*models.Entry{
				"/mdb00000/db1/ro/false": {
					Name:     "instance1",
					Cluster:  "mdb00000",
					Database: "db1",
					Mode:     "ro",
					Sox:      false,
					Type:     models.Mysql,
					Old:      true,
				},
				"/mdb11111/db2/rw/true": {
					Name:     "instance1",
					Cluster:  "mdb11111",
					Database: "db2",
					Mode:     "rw",
					Sox:      true,
					Type:     models.Mysql,
					Old:      true,
				},
			},
		},
		"postgresql": {
			Type:  models.Postgresql,
			Query: "SELECT cluster, name, db, mode, sox FROM postgresql",
			Rows: sqlmock.NewRows([]string{"cluster", "name", "db", "mode", "sox"}).
				AddRow("mdb00000", "instance1", "db1", "ro", false).
				AddRow("mdb11111", "instance1", "db2", "rw", true),
			ExpectedEntries: map[string]*models.Entry{
				"/mdb00000/db1/ro/false": {
					Name:     "instance1",
					Cluster:  "mdb00000",
					Database: "db1",
					Mode:     "ro",
					Sox:      false,
					Type:     models.Postgresql,
					Old:      true,
				},
				"/mdb11111/db2/rw/true": {
					Name:     "instance1",
					Cluster:  "mdb11111",
					Database: "db2",
					Mode:     "rw",
					Sox:      true,
					Type:     models.Postgresql,
					Old:      true,
				},
			},
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock := prepareDbGetMock(t, tc.Query, tc.Rows)
			m := Mngr{
				log: logrus.New("info"),
				Ro:  db,
			}

			entries, err := m.GetEntries(tc.Type)
			assert.NoError(t, err)
			assert.EqualValues(t, tc.ExpectedEntries, entries)
			assert.NoError(t, mock.ExpectationsWereMet())
		})
	}
}

func TestMngr_AddOwner(t *testing.T) {
	tests := map[string]struct {
		Service     string
		Owner       idm.Owner
		SelectQuery string
		SelectCount int
		InsertQuery string
		InsertError error
		Args        []driver.Value
		ExpectedErr error
	}{
		"new": {
			Service: "service",
			Owner: idm.Owner{
				Id:   "1",
				Type: idm.GROUP,
			},
			SelectQuery: "SELECT COUNT(.*) FROM owners",
			SelectCount: 0,
			InsertQuery: "INSERT INTO owners",
			Args:        []driver.Value{"1", idm.GROUP, "service"},
		},
		"exists": {
			Service: "service",
			Owner: idm.Owner{
				Id:   "1",
				Type: idm.GROUP,
			},
			SelectQuery: "SELECT COUNT(.*) FROM owners",
			SelectCount: 1,
			Args:        []driver.Value{"1", idm.GROUP, "service"},
		},
		"error": {
			Service: "service",
			Owner: idm.Owner{
				Id:   "1",
				Type: idm.GROUP,
			},
			SelectQuery: "SELECT COUNT(.*) FROM owners",
			SelectCount: 0,
			InsertQuery: "INSERT INTO owners",
			InsertError: fmt.Errorf("test"),
			Args:        []driver.Value{"1", idm.GROUP, "service"},
			ExpectedErr: fmt.Errorf("can't insert to database: test"),
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock := prepareDbInsertMock(t, tc.SelectQuery, tc.SelectCount, tc.InsertQuery, tc.InsertError, tc.Args)

			m := Mngr{
				log: logrus.New("info"),
				Rw:  db,
			}

			if tc.ExpectedErr != nil {
				assert.EqualError(t, m.AddOwner(tc.Service, tc.Owner), tc.ExpectedErr.Error())
			} else {
				assert.NoError(t, m.AddOwner(tc.Service, tc.Owner))
			}
			assert.NoError(t, mock.ExpectationsWereMet())
		})
	}
}

func TestMngr_DeleteOwner(t *testing.T) {
	tests := map[string]struct {
		Service     string
		Owner       idm.Owner
		DeleteQuery string
		DeleteError error
		Args        []driver.Value
		ExpectedErr error
	}{
		"success": {
			Service: "service",
			Owner: idm.Owner{
				Id:   "1",
				Type: idm.GROUP,
			},
			DeleteQuery: "DELETE FROM owners",
			Args:        []driver.Value{"1", idm.GROUP, "service"},
		},
		"error": {
			Service: "service",
			Owner: idm.Owner{
				Id:   "1",
				Type: idm.GROUP,
			},
			DeleteQuery: "DELETE FROM owners",
			DeleteError: fmt.Errorf("test"),
			Args:        []driver.Value{"1", idm.GROUP, "service"},
			ExpectedErr: fmt.Errorf("can't delete from database: test"),
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock := prepareDbDeleteMock(t, tc.DeleteQuery, tc.DeleteError, tc.Args)
			m := Mngr{
				log: logrus.New("info"),
				Rw:  db,
			}

			if tc.ExpectedErr != nil {
				assert.EqualError(t, m.DeleteOwner(tc.Service, tc.Owner), tc.ExpectedErr.Error())
			} else {
				assert.NoError(t, m.DeleteOwner(tc.Service, tc.Owner))
			}
			assert.NoError(t, mock.ExpectationsWereMet())
		})
	}
}

func TestMngr_GetOwners(t *testing.T) {
	db, mock, err := sqlmock.New()
	assert.NoError(t, err)
	t.Cleanup(func() {
		db.Close()
	})

	mock.ExpectQuery("SELECT service, type, id FROM owners").
		WillReturnRows(sqlmock.NewRows([]string{"service", "type", "id"}).
			AddRow("service1", idm.GROUP, "1337").
			AddRow("service1", idm.USER, "other-login").
			AddRow("service2", idm.USER, "some-login"))
	m := Mngr{
		log: logrus.New("info"),
		Ro:  db,
	}

	owners, err := m.GetOwners()
	assert.NoError(t, err)
	assert.EqualValues(t, map[string][]idm.Owner{
		"service1": {idm.Owner{Type: idm.GROUP, Id: "1337"}, idm.Owner{Type: idm.USER, Id: "other-login"}},
		"service2": {idm.Owner{Type: idm.USER, Id: "some-login"}},
	}, owners)
	assert.NoError(t, mock.ExpectationsWereMet())
}

func TestMngr_AddRole(t *testing.T) {
	tests := map[string]struct {
		Role        models.Role
		SelectQuery string
		SelectCount int
		InsertQuery string
		InsertError error
		Args        []driver.Value
		ExpectedErr error
	}{
		"new": {
			Role: models.Role{
				Login:   "login",
				Service: "service",
				Sox:     false,
				Ticket:  "VOID-1",
			},
			SelectQuery: "SELECT COUNT(.*) FROM acl",
			SelectCount: 0,
			InsertQuery: "INSERT INTO acl",
			Args:        []driver.Value{"login", "service", false, "VOID-1"},
		},
		"exists": {
			Role: models.Role{
				Login:   "login",
				Service: "service",
				Sox:     false,
				Ticket:  "VOID-1",
			},
			SelectQuery: "SELECT COUNT(.*) FROM acl",
			SelectCount: 1,
			Args:        []driver.Value{"login", "service", false, "VOID-1"},
		},
		"error": {
			Role: models.Role{
				Login:   "login",
				Service: "service",
				Sox:     false,
				Ticket:  "VOID-1",
			},
			SelectQuery: "SELECT COUNT(.*) FROM acl",
			SelectCount: 0,
			InsertQuery: "INSERT INTO acl",
			InsertError: fmt.Errorf("test"),
			Args:        []driver.Value{"login", "service", false, "VOID-1"},
			ExpectedErr: fmt.Errorf("can't insert to database: test"),
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock := prepareDbInsertMock(t, tc.SelectQuery, tc.SelectCount, tc.InsertQuery, tc.InsertError, tc.Args)

			m := Mngr{
				log: logrus.New("info"),
				Rw:  db,
			}

			if tc.ExpectedErr != nil {
				assert.EqualError(t, m.AddRole(tc.Role), tc.ExpectedErr.Error())
			} else {
				assert.NoError(t, m.AddRole(tc.Role))
			}
			assert.NoError(t, mock.ExpectationsWereMet())
		})
	}
}

func TestMngr_DeleteRole(t *testing.T) {
	tests := map[string]struct {
		Service     string
		Owner       idm.Owner
		DeleteQuery string
		DeleteError error
		Args        []driver.Value
		ExpectedErr error
	}{
		"success": {
			Service: "service",
			Owner: idm.Owner{
				Id:   "1",
				Type: idm.GROUP,
			},
			DeleteQuery: "DELETE FROM owners",
			Args:        []driver.Value{"1", idm.GROUP, "service"},
		},
		"error": {
			Service: "service",
			Owner: idm.Owner{
				Id:   "1",
				Type: idm.GROUP,
			},
			DeleteQuery: "DELETE FROM owners",
			DeleteError: fmt.Errorf("test"),
			Args:        []driver.Value{"1", idm.GROUP, "service"},
			ExpectedErr: fmt.Errorf("can't delete from database: test"),
		},
	}

	for name, tc := range tests {
		t.Run(name, func(t *testing.T) {
			db, mock := prepareDbDeleteMock(t, tc.DeleteQuery, tc.DeleteError, tc.Args)
			m := Mngr{
				log: logrus.New("info"),
				Rw:  db,
			}

			if tc.ExpectedErr != nil {
				assert.EqualError(t, m.DeleteOwner(tc.Service, tc.Owner), tc.ExpectedErr.Error())
			} else {
				assert.NoError(t, m.DeleteOwner(tc.Service, tc.Owner))
			}
			assert.NoError(t, mock.ExpectationsWereMet())
		})
	}
}

func TestMngr_GetRoles(t *testing.T) {
	db, mock, err := sqlmock.New()
	assert.NoError(t, err)
	t.Cleanup(func() {
		db.Close()
	})

	mock.ExpectQuery("SELECT login, service, sox, ticket FROM acl").
		WillReturnRows(sqlmock.NewRows([]string{"login", "service", "sox", "type"}).
			AddRow("login1", "service1", false, "").
			AddRow("login2", "service2", true, "VOID-1"))
	m := Mngr{
		log: logrus.New("info"),
		Ro:  db,
	}

	roles, err := m.GetRoles()
	assert.NoError(t, err)
	assert.EqualValues(t, []models.Role{
		{Login: "login1", Service: "service1", Sox: false, Ticket: ""},
		{Login: "login2", Service: "service2", Sox: true, Ticket: "VOID-1"},
	}, roles)
	assert.NoError(t, mock.ExpectationsWereMet())
}

func TestMngr_GetOwnerRolesMap(t *testing.T) {
	db, mock, err := sqlmock.New()
	assert.NoError(t, err)
	t.Cleanup(func() {
		db.Close()
	})

	mock.ExpectQuery("SELECT login, service, sox, ticket FROM acl").
		WillReturnRows(sqlmock.NewRows([]string{"login", "service", "sox", "type"}).
			AddRow("login1", "/service1/test/provides/test/", false, "").
			AddRow("login2", "/service1/test/provides/owner/", false, "").
			AddRow("login3", "/service/mysql/instance/mdb000/database/owner/", false, ""))
	m := Mngr{
		log: logrus.New("info"),
		Ro:  db,
	}

	roles, err := m.GetOwnerRolesMap()
	assert.NoError(t, err)
	t.Log(roles)
	assert.EqualValues(t, map[string][]idm.Owner{
		"/service/mysql/instance/mdb000/database/owner/": {{Id: "login3", Type: idm.USER}},
		"/service1/test/provides/owner/":                 {{Id: "login2", Type: idm.USER}},
	}, roles)
	assert.NoError(t, mock.ExpectationsWereMet())
}

func prepareDbInsertMock(t *testing.T, selectQ string, selectC int, insertQ string, insertE error, args []driver.Value) (*sql.DB, sqlmock.Sqlmock) {
	t.Helper()

	db, mock, err := sqlmock.New()
	assert.NoError(t, err)
	t.Cleanup(func() {
		db.Close()
	})

	mock.ExpectBegin()
	mock.ExpectQuery(selectQ).WithArgs(args...).WillReturnRows(
		sqlmock.NewRows([]string{"count"}).AddRow(selectC))

	if selectC == 0 {
		if insertE != nil {
			mock.ExpectExec(insertQ).WithArgs(args...).WillReturnError(insertE)
			mock.ExpectRollback()
		} else {
			mock.ExpectExec(insertQ).WithArgs(args...).WillReturnResult(
				sqlmock.NewResult(1, 1))
			mock.ExpectCommit()
		}
	}
	mock.MatchExpectationsInOrder(true)

	return db, mock
}

func prepareDbDeleteMock(t *testing.T, deleteQ string, deleteE error, args []driver.Value) (*sql.DB, sqlmock.Sqlmock) {
	t.Helper()

	db, mock, err := sqlmock.New()
	assert.NoError(t, err)
	t.Cleanup(func() {
		db.Close()
	})

	mock.ExpectBegin()
	if deleteE != nil {
		mock.ExpectExec(deleteQ).WithArgs(args...).WillReturnError(deleteE)
		mock.ExpectRollback()
	} else {
		mock.ExpectExec(deleteQ).WithArgs(args...).WillReturnResult(
			sqlmock.NewResult(1, 1))
		mock.ExpectCommit()
	}
	mock.MatchExpectationsInOrder(true)

	return db, mock
}

func prepareDbGetMock(t *testing.T, query string, rows *sqlmock.Rows) (*sql.DB, sqlmock.Sqlmock) {
	t.Helper()

	db, mock, err := sqlmock.New()
	assert.NoError(t, err)
	t.Cleanup(func() {
		db.Close()
	})

	mock.ExpectQuery(query).WillReturnRows(rows)

	return db, mock
}
