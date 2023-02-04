mkdir "$HOME/check_ftp_storage"
mkdir "$HOME/check_ftp_storage/subdir"
touch "$HOME/check_ftp_storage/file1.txt"
touch "$HOME/check_ftp_storage/subdir/file2.txt"
sudo apt install -y vsftpd
sudo systemctl stop vsftpd
echo 'write_enable=YES' | sudo tee -a /etc/vsftpd.conf
sudo systemctl start vsftpd
echo 'done'
