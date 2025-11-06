package com.shuyixiao.gitstat.email.util;

import com.intellij.openapi.project.Project;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * PasswordEncryptor 单元测试
 * 测试密码加密解密功能，包括新格式、旧格式和明文密码的兼容性
 */
public class PasswordEncryptorTest {
    
    private Project mockProject;
    
    @Before
    public void setUp() {
        // 创建模拟的 Project 对象
        mockProject = Mockito.mock(Project.class);
        Mockito.when(mockProject.getBasePath()).thenReturn("/test/project/path");
    }
    
    /**
     * 测试基本的加密解密功能
     */
    @Test
    public void testBasicEncryptDecrypt() {
        String originalPassword = "mySecretPassword123";
        
        // 加密
        String encrypted = PasswordEncryptor.encrypt(originalPassword, mockProject);
        
        // 验证加密后的格式
        assertTrue("加密后应该以 ENC: 开头", encrypted.startsWith("ENC:"));
        assertNotEquals("加密后不应该等于原密码", originalPassword, encrypted);
        
        // 解密
        String decrypted = PasswordEncryptor.decrypt(encrypted, mockProject);
        
        // 验证解密结果
        assertEquals("解密后应该等于原密码", originalPassword, decrypted);
    }
    
    /**
     * 测试空密码处理
     */
    @Test
    public void testEmptyPassword() {
        String emptyPassword = "";
        
        String encrypted = PasswordEncryptor.encrypt(emptyPassword, mockProject);
        assertEquals("空密码加密后应该还是空", "", encrypted);
        
        String decrypted = PasswordEncryptor.decrypt(emptyPassword, mockProject);
        assertEquals("空密码解密后应该还是空", "", decrypted);
    }
    
    /**
     * 测试 null 密码处理
     */
    @Test
    public void testNullPassword() {
        String encrypted = PasswordEncryptor.encrypt(null, mockProject);
        assertEquals("null密码加密后应该是空字符串", "", encrypted);
        
        String decrypted = PasswordEncryptor.decrypt(null, mockProject);
        assertEquals("null密码解密后应该是空字符串", "", decrypted);
    }
    
    /**
     * 测试明文密码兼容性（旧版本配置）
     */
    @Test
    public void testPlainTextPasswordCompatibility() {
        String plainPassword = "plainTextPassword";
        
        // 直接解密明文密码（模拟旧版本配置）
        String decrypted = PasswordEncryptor.decrypt(plainPassword, mockProject);
        
        // 应该返回原密码（兼容模式）
        assertEquals("明文密码应该直接返回", plainPassword, decrypted);
    }
    
    /**
     * 测试损坏的 Base64 数据
     */
    @Test
    public void testCorruptedBase64Data() {
        String corruptedData = "这不是有效的Base64数据!!!";
        
        // 解密损坏的数据不应该抛出异常
        String decrypted = PasswordEncryptor.decrypt(corruptedData, mockProject);
        
        // 应该返回原数据（兼容模式）
        assertEquals("损坏的数据应该直接返回", corruptedData, decrypted);
    }
    
    /**
     * 测试旧格式的 Base64 加密数据（无 ENC: 前缀）
     */
    @Test
    public void testOldFormatBase64() {
        // 先用新格式加密
        String password = "testPassword";
        String newFormatEncrypted = PasswordEncryptor.encrypt(password, mockProject);
        
        // 移除 ENC: 前缀，模拟旧格式
        String oldFormatEncrypted = newFormatEncrypted.substring(4); // 去掉 "ENC:"
        
        // 解密旧格式
        String decrypted = PasswordEncryptor.decrypt(oldFormatEncrypted, mockProject);
        
        // 应该能正确解密
        assertEquals("旧格式应该能正确解密", password, decrypted);
    }
    
    /**
     * 测试重复加密（已加密的密码不应该再次加密）
     */
    @Test
    public void testDoubleEncryption() {
        String password = "testPassword";
        
        // 第一次加密
        String encrypted1 = PasswordEncryptor.encrypt(password, mockProject);
        
        // 第二次加密（传入已加密的密码）
        String encrypted2 = PasswordEncryptor.encrypt(encrypted1, mockProject);
        
        // 应该返回相同的结果（不重复加密）
        assertEquals("已加密的密码不应该重复加密", encrypted1, encrypted2);
    }
    
    /**
     * 测试 isEncrypted 方法
     */
    @Test
    public void testIsEncrypted() {
        String plainPassword = "plainPassword";
        String encryptedPassword = PasswordEncryptor.encrypt(plainPassword, mockProject);
        
        assertFalse("明文密码应该返回 false", PasswordEncryptor.isEncrypted(plainPassword));
        assertTrue("加密密码应该返回 true", PasswordEncryptor.isEncrypted(encryptedPassword));
        assertFalse("空密码应该返回 false", PasswordEncryptor.isEncrypted(""));
        assertFalse("null应该返回 false", PasswordEncryptor.isEncrypted(null));
    }
    
    /**
     * 测试 canDecrypt 方法
     */
    @Test
    public void testCanDecrypt() {
        String password = "testPassword";
        String encrypted = PasswordEncryptor.encrypt(password, mockProject);
        
        assertTrue("有效的加密密码应该能解密", PasswordEncryptor.canDecrypt(encrypted, mockProject));
        assertTrue("空密码应该返回 true", PasswordEncryptor.canDecrypt("", mockProject));
        assertTrue("明文密码应该返回 true", PasswordEncryptor.canDecrypt("plainText", mockProject));
    }
}

