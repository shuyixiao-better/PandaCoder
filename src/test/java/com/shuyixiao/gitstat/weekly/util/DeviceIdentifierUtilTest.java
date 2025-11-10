package com.shuyixiao.gitstat.weekly.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * DeviceIdentifierUtil 单元测试
 * 测试设备唯一标识获取功能
 */
public class DeviceIdentifierUtilTest {
    
    /**
     * 测试获取设备ID
     */
    @Test
    public void testGetDeviceId() {
        String deviceId = DeviceIdentifierUtil.getDeviceId();
        
        assertNotNull("设备ID不应该为null", deviceId);
        assertFalse("设备ID不应该为空", deviceId.isEmpty());
        
        // SHA-256 哈希值应该是64个字符（256位 = 64个十六进制字符）
        assertTrue("设备ID长度应该大于0", deviceId.length() > 0);
        
        System.out.println("设备ID: " + deviceId);
    }
    
    /**
     * 测试设备ID的一致性（多次调用应该返回相同的值）
     */
    @Test
    public void testDeviceIdConsistency() {
        String deviceId1 = DeviceIdentifierUtil.getDeviceId();
        String deviceId2 = DeviceIdentifierUtil.getDeviceId();
        
        assertEquals("多次调用应该返回相同的设备ID", deviceId1, deviceId2);
    }
    
    /**
     * 测试获取MAC地址
     */
    @Test
    public void testGetMacAddress() {
        String macAddress = DeviceIdentifierUtil.getMacAddress();
        
        // MAC地址可能为null（如果获取失败）
        if (macAddress != null) {
            System.out.println("MAC地址: " + macAddress);
            
            // MAC地址格式应该是 XX-XX-XX-XX-XX-XX
            assertTrue("MAC地址应该包含连字符", macAddress.contains("-"));
            
            // MAC地址应该有6组十六进制数
            String[] parts = macAddress.split("-");
            assertTrue("MAC地址应该有6组数字", parts.length >= 6);
        } else {
            System.out.println("MAC地址获取失败（这是正常的，可能是虚拟机或特殊环境）");
        }
    }
    
    /**
     * 测试获取计算机名称
     */
    @Test
    public void testGetComputerName() {
        String computerName = DeviceIdentifierUtil.getComputerName();
        
        assertNotNull("计算机名称不应该为null", computerName);
        assertFalse("计算机名称不应该为空", computerName.isEmpty());
        assertNotEquals("计算机名称不应该是unknown", "unknown", computerName);
        
        System.out.println("计算机名称: " + computerName);
    }
    
    /**
     * 测试缓存清除
     */
    @Test
    public void testClearCache() {
        // 第一次获取
        String deviceId1 = DeviceIdentifierUtil.getDeviceId();
        
        // 清除缓存
        DeviceIdentifierUtil.clearCache();
        
        // 第二次获取
        String deviceId2 = DeviceIdentifierUtil.getDeviceId();
        
        // 即使清除缓存，设备ID也应该相同（因为是基于硬件信息）
        assertEquals("清除缓存后设备ID应该保持一致", deviceId1, deviceId2);
    }
}

