cnt = 2
name = "hotbilldb_test"
env = "PRESTABLE"
resource_preset = "s2.micro"
disk_size = 20
dbname = "accountsdb"
dbowner = "accounts"
username = "accounts"
hosts = [
    {
        zone = "vla",
        priority = 5
    },
    {
        zone = "sas",
        priority = 5
    },
    // igogor: убираю, т.к. нам не хватает 1го ядра для шарда прода.
//    {
//        zone = "man",
//        priority = 0
//    },
]
