import os
import time
import uiautomator2 as u2
import xml.etree.ElementTree as ET

# device_url = input('Введите URL девайса в формате "f2oxmzft75yrr6x4.sas.yp-c.yandex.net:7401":\n')
# os.system('adb connect ' + device_url)



#Open Play Market
os.system('adb shell am start -a android.intent.action.VIEW -d \'market://details?id=ru.yandex.disk\'')
time.sleep(2)
#Press "Install"
os.system('adb shell input tap 500 1100')
time.sleep(17)
#Press "Open"
os.system('adb shell input tap 750 880')

# Install Disk
#os.system('adb install 500.apk')
time.sleep(3)
# Open Disk
#os.system('adb shell am start ru.yandex.disk')
time.sleep(3)
# Log in
os.system('adb shell input tap 600 1700')
time.sleep(3)
# Close onboarding
os.system('adb shell input tap 120 180')
time.sleep(3)
# Press "More" button
os.system('adb shell input tap 950 1980')
time.sleep(2)
# Press "Notes" button
os.system('adb shell input tap 550 1900')
time.sleep(2)
# Update Disk
os.system('adb install trunk.apk')
time.sleep(1)
# Open disk
os.system('adb shell am start ru.yandex.disk')
time.sleep(2)
# Open profile
os.system('adb shell input tap 100 150')
# Open profile
os.system('adb shell input tap 550 1550')
# Swipe to "About" line
os.system('adb shell input swipe 500 1700 500 800 300')
time.sleep(1)
# Press "About"
os.system('adb shell input tap 500 2000')

#os.system('adb disconnect ' + device_url)