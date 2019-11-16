# ForwardSms

作用: 自动转发收到的短信到tg指定用户
场景: 拥有多张卡,装在不同手机上时,随身携带多台手机可能不方便

## clone后修改 `local.properties` 文件,填入以下参数
```properties
# 钉钉公司和微应用信息
dd_corpid="ding***f"
dd_corpsecret="Bp1_***YvVQi-_Q"
dd_agent="80***11"

# tg bot token信息
tg_bottoken="9749****0NKAekxI"
tg_default_userName="u****d"
```

## 权限
除需要用户允许短信权限外, 如miui系统还需手动允许 **`通知类短信`** 权限,否则无法监听验证码新短信

## 保活
由用户自行通过系统设置允许app自启动及后台运行
其他措施:
1. 在application中通过 `bindService` 来启动;
3. 首页返回功能改为回到桌面功能,确保始终有activity在运行;
4. 各activity均设置 `android:excludeFromRecents="true"` 避免用户快速杀进程;

目前在nexus6P android8.1.0上使用正常

## 计划
1. app直接支持发送消息到 tg/钉钉, 可以不经过中转服务器
2. 添加微信消息转发渠道
3. 网络情况判断提示
4. 抽象封装 钉钉,tg 消息发送库
5. 缓存 chat 信息