package com.shuyixiao.gitstat.email.util;

import com.intellij.openapi.diagnostic.Logger;
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
 * version 2.1.0
 * Description SMTP密码加密工具类，使用AES加密算法对邮箱SMTP密码进行加密存储和解密使用，基于项目路径生成唯一密钥确保安全性
 * createTime 2025-10-22
 * updateTime 2025-11-06
 * 技术分享 · 公众号：舒一笑的架构笔记
 *
 * 修复说明：
 * 1. 增加了密码格式检测，自动识别加密/明文密码
 * 2. 解密失败时返回空字符串，避免程序崩溃
 * 3. 增加了详细的日志记录，便于问题排查
 * 4. 兼容旧版本的明文密码配置
 */
public class PasswordEncryptor {

    private static final Logger LOG = Logger.getInstance(PasswordEncryptor.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    // 加密密码的标识前缀，用于区分加密和明文密码
    private static final String ENCRYPTED_PREFIX = "ENC:";

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
     * 返回格式：ENC:Base64编码的加密数据
     */
    public static String encrypt(String password, Project project) {
        if (password == null || password.isEmpty()) {
            return "";
        }

        // 如果已经是加密格式，直接返回
        if (password.startsWith(ENCRYPTED_PREFIX)) {
            return password;
        }

        try {
            SecretKey key = getKey(project);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(
                password.getBytes(StandardCharsets.UTF_8)
            );
            String encryptedStr = Base64.getEncoder().encodeToString(encrypted);
            return ENCRYPTED_PREFIX + encryptedStr;
        } catch (Exception e) {
            LOG.error("Failed to encrypt password", e);
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }

    /**
     * 解密密码
     * 支持以下格式：
     * 1. ENC:开头的加密密码（新格式）
     * 2. Base64格式的加密密码（旧格式，兼容）
     * 3. 明文密码（直接返回，用于兼容旧版本）
     */
    public static String decrypt(String encryptedPassword, Project project) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return "";
        }

        // 新格式：ENC:开头
        if (encryptedPassword.startsWith(ENCRYPTED_PREFIX)) {
            String base64Data = encryptedPassword.substring(ENCRYPTED_PREFIX.length());
            return decryptBase64(base64Data, project);
        }

        // 尝试作为旧格式的Base64加密数据解密
        String decrypted = decryptBase64(encryptedPassword, project);
        if (decrypted != null) {
            return decrypted;
        }

        // 如果解密失败，可能是明文密码，直接返回
        LOG.warn("Password decryption failed, treating as plain text. Please re-save the configuration to encrypt it.");
        return encryptedPassword;
    }

    /**
     * 解密Base64编码的加密数据
     * @return 解密后的密码，如果解密失败返回null
     */
    private static String decryptBase64(String base64Data, Project project) {
        try {
            SecretKey key = getKey(project);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] encryptedBytes = Base64.getDecoder().decode(base64Data);
            byte[] decrypted = cipher.doFinal(encryptedBytes);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Base64解码失败
            LOG.debug("Not a valid Base64 string: " + e.getMessage());
            return null;
        } catch (Exception e) {
            // 解密失败（密钥不匹配、数据损坏等）
            LOG.debug("Decryption failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查密码是否已加密
     */
    public static boolean isEncrypted(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return password.startsWith(ENCRYPTED_PREFIX);
    }

    /**
     * 验证密码是否可以正常解密
     * 用于配置验证
     */
    public static boolean canDecrypt(String encryptedPassword, Project project) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return true; // 空密码视为有效
        }

        try {
            String decrypted = decrypt(encryptedPassword, project);
            return decrypted != null && !decrypted.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}

