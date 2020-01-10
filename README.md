## 実機でバッグ

```
$ lsusb
Bus 001 Device 005: ID 2717:ff40 Xiaomi Inc. Mi/Redmi series (MTP)
```

```
$ sudo nvim /etc/udev/rules.d/51-android.rules
SUBSYSTEM=="usb", ATTR{idVendor}=="[調べたUSBベンダーID4桁]", MODE="0666", GROUP="plugdev"
```

上記の場合は'2717'

