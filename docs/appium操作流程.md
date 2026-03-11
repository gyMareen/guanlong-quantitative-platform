检查模拟器端：adb devices -l
安装Appium：npm i -g appium
安装安卓驱动：appium driver install uiautomator2
安装Inspector插件：appium plugin install inspector
确认Inspector插件已安装：appium plugin list
启动Appium服务，不要网页端：appium
启动Appium，开启Web管理端：appium --use-plugins=inspector --allow-cors
管理端地址：http://127.0.0.1:4723/inspector