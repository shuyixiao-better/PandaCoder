package com.shuyixiao.gitstat.weekly.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Enumeration;

/**
 * 设备唯一标识工具类
 * 用于获取设备的唯一标识符，确保用户身份的唯一性
 * 
 * @author PandaCoder Team
 * @since 2.2.0
 */
public class DeviceIdentifierUtil {
    
    private static String cachedDeviceId = null;
    
    /**
     * 获取设备唯一标识
     * 优先使用MAC地址，如果获取失败则使用计算机名称和用户名的组合
     * 
     * @return 设备唯一标识（SHA-256哈希值）
     */
    public static String getDeviceId() {
        if (cachedDeviceId != null) {
            return cachedDeviceId;
        }
        
        try {
            // 尝试获取MAC地址
            String macAddress = getMacAddress();
            if (macAddress != null && !macAddress.isEmpty()) {
                cachedDeviceId = hashString(macAddress);
                return cachedDeviceId;
            }
            
            // 如果MAC地址获取失败，使用计算机名和用户名的组合
            String computerName = getComputerName();
            String userName = System.getProperty("user.name", "unknown");
            String combined = computerName + "_" + userName;
            cachedDeviceId = hashString(combined);
            return cachedDeviceId;
            
        } catch (Exception e) {
            System.err.println("[DeviceIdentifierUtil] 获取设备ID失败: " + e.getMessage());
            // 使用随机生成的ID作为后备方案
            cachedDeviceId = hashString("fallback_" + System.currentTimeMillis());
            return cachedDeviceId;
        }
    }
    
    /**
     * 获取MAC地址
     * 优先获取物理网卡的MAC地址，排除虚拟网卡
     * 
     * @return MAC地址字符串，格式：XX-XX-XX-XX-XX-XX
     */
    public static String getMacAddress() {
        try {
            // 尝试获取本地主机的网络接口
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            
            if (ni != null && !ni.isVirtual() && !ni.isLoopback()) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    return formatMacAddress(mac);
                }
            }
            
            // 如果上面的方法失败，遍历所有网络接口
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface network = networkInterfaces.nextElement();
                
                // 跳过虚拟网卡、回环接口和未启用的接口
                if (network.isVirtual() || network.isLoopback() || !network.isUp()) {
                    continue;
                }
                
                byte[] mac = network.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    return formatMacAddress(mac);
                }
            }
            
        } catch (SocketException | UnknownHostException e) {
            System.err.println("[DeviceIdentifierUtil] 获取MAC地址失败: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 格式化MAC地址
     * 
     * @param mac MAC地址字节数组
     * @return 格式化后的MAC地址字符串
     */
    private static String formatMacAddress(byte[] mac) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X", mac[i]));
            if (i < mac.length - 1) {
                sb.append("-");
            }
        }
        return sb.toString();
    }
    
    /**
     * 获取计算机名称
     * 
     * @return 计算机名称
     */
    public static String getComputerName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return System.getenv("COMPUTERNAME") != null ? 
                   System.getenv("COMPUTERNAME") : "unknown";
        }
    }
    
    /**
     * 使用SHA-256对字符串进行哈希
     * 
     * @param input 输入字符串
     * @return SHA-256哈希值（十六进制字符串）
     */
    private static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            System.err.println("[DeviceIdentifierUtil] 哈希计算失败: " + e.getMessage());
            return input; // 如果哈希失败，返回原始字符串
        }
    }
    
    /**
     * 清除缓存的设备ID（用于测试）
     */
    public static void clearCache() {
        cachedDeviceId = null;
    }
}

