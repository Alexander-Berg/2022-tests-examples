# m h  dom mon dow   command

# bring backup from production every monday
3 0 * * 1 /opt/broadmatching/scripts/utils/update_test_db.pl >> /opt/broadmatching/log/update_test_db.log  2>> /opt/broadmatching/log/update_test_db.err 

