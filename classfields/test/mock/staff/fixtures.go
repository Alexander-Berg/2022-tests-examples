package staff

import staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"

const (
	Owner = "danevge"
)

var groupWithoutLeader = staffapi.DepartmentGroup{
	ID: 1,
	Department: staffapi.Department{
		Heads: []staffapi.Head{},
	},
	URL: "yandex_personal_vertserv_without_leader",
}

var groupCompAuto = staffapi.DepartmentGroup{
	ID: 2,
	Department: staffapi.Department{
		Heads: []staffapi.Head{
			staffapi.Head{
				HeadPerson: staffapi.HeadPerson{
					Login: "goodfella",
				},
				Role: "chief",
			},
		},
	},
	URL: "yandex_personal_vertserv_comp_auto",
}

var groupInfraService = staffapi.DepartmentGroup{
	ID: 3,
	Department: staffapi.Department{
		Heads: []staffapi.Head{
			staffapi.Head{
				HeadPerson: staffapi.HeadPerson{
					Login: "spooner",
				},
				Role: "chief",
			},
		},
	},
	URL: "yandex_personal_vertserv_infra_service",
}

var groupInfraMnt = staffapi.DepartmentGroup{
	ID: 4,
	Department: staffapi.Department{
		Heads: []staffapi.Head{},
	},
	Ancestors: []staffapi.DepartmentGroup{
		groupInfraService,
	},
	URL:   "yandex_personal_vertserv_infra_mnt",
	Level: 1,
}

var personGoodfella = staffapi.Person{
	ID:    1,
	Login: "goodfella",
	Accounts: []staffapi.Account{
		staffapi.Account{
			Id:    1,
			Type:  "github",
			Value: "ya-goodfella",
		},
	},
	DepartmentGroup: groupCompAuto,
}

var personSpooner = staffapi.Person{
	ID:    2,
	Login: "spooner",
	Accounts: []staffapi.Account{
		staffapi.Account{
			Id:    1,
			Type:  "github",
			Value: "bogdanov1609",
		},
	},
	DepartmentGroup: groupInfraService,
}

var personCoderoc = staffapi.Person{
	ID:    3,
	Login: "coderoc",
	Accounts: []staffapi.Account{
		staffapi.Account{
			Id:    1,
			Type:  "github",
			Value: "coderoc",
		},
	},
	DepartmentGroup: groupWithoutLeader,
}

var personIbiryulin = staffapi.Person{
	ID:    4,
	Login: "ibiryulin",
	Accounts: []staffapi.Account{
		staffapi.Account{
			Id:    1,
			Type:  "github",
			Value: "merryjane",
		},
	},
	DepartmentGroup: groupInfraService,
}
