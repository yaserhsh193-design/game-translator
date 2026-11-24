package com.gametranslator.tv;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

final class Net {
    private Net() {
    }

    /** Returns the TV's address on the home network (Wi-Fi or Ethernet), or null. */
    static String localIp() {
        String fallback = null;
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                String name = ni.getName() == null ? "" : ni.getName();
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!(a instanceof Inet4Address) || a.isLoopbackAddress()) continue;
                    String ip = a.getHostAddress();
                    if (a.isSiteLocalAddress() && (name.startsWith("wlan") || name.startsWith("eth"))) {
                        return ip;
                    }
                    if (a.isSiteLocalAddress() && fallback == null) {
                        fallback = ip;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
