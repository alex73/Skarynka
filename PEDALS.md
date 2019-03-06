Currently, usb4java doesn't support HID devices so good. 
I recommend to use Foot USB Pedal like http://www.amazon.com/Computer-Video-Racing-Pedal-Switch-Controller/dp/B0098PLPOI on Windows since it produces usual `B` chars under Windows.

If you want to use HIDScanController, you need to know:

For skip device loading as HID in Ubuntu, you need to add:
```
a) GRUB_CMDLINE_LINUX="usbhid.quirks=0x0c45:0x7403:0x0004" into /etc/default/grub,
b) ATTRS{idVendor}=="093a", ATTRS{idProduct}=="2510", MODE="600", OWNER="alex", OPTIONS=="ignore_device" in the /etc/udev/...
c) then run "update-grub",then restart.
```
For `0c45:7403`: press value is `1 0 0 5 0 0 0 0`, unpress value is `1 0 0 0 0 0 0 0`.
For `093a:2510`: left press value is `1 0 0 0`, right press value is `2 0 0 0`.


http://usb4java.org/faq.html
