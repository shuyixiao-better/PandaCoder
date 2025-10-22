package com.shuyixiao.gitstat.email.util;

import com.intellij.openapi.project.Project;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Copyright © 2025 PandaCoder. All rights reserved.
 * ClassName PasswordEncryptor.java
 * author 舒一笑不秃头
 * version 2.0.0
 * Description SMTP密码加密工具类，使用AES加密算法对邮箱SMTP密码进行加密存储和解密使用，基于项目路径生成唯一密钥确保安全性
 * createTime 2025-10-22
 * 技术分享 · 公众号：舒一笑的架构笔记
 */
public class PasswordEncryptor {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    /**
     * 生成项目唯一密钥
     */
    private static SecretKey getKey(Project project) {
        String seed = project.getBasePath() + "GitStatEmail";
        byte[] keyBytes = Arrays.copyOf(
            seed.getBytes(StandardCharsets.UTF_8), 
            16
        );
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * 加密密码
     */
    public static String encrypt(String password, Project project) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        
        try {
            SecretKey key = getKey(project);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(
                password.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }
    
    /**
     * 解密密码
     */
    public static String decrypt(String encryptedPassword, Project project) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return "";
        }
        
        try {
            SecretKey key = getKey(project);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(
                Base64.getDecoder().decode(encryptedPassword)
            );
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }
}

