sudo systemctl stop vsftpd
rm -rd ~/check_ftp_storage
sudo apt purge -y vsftpd
echo 'done'
