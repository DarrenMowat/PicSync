package com.darrenmowat.gdcu.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {

	public static String getFileMd5(java.io.File file) throws NoSuchAlgorithmException, IOException {
		byte[] md5_buffer = new byte[32768];
		MessageDigest digest = MessageDigest.getInstance("MD5");
		InputStream fis = new FileInputStream(file);
		BufferedInputStream bf = new BufferedInputStream(fis);
		int len = 0;
		int n = 0;
		while (n != -1) {
			n = bf.read(md5_buffer);
			if (n > 0) {
				len = len + n;
				digest.update(md5_buffer, 0, n);
			}
		}
		byte[] digestResult = digest.digest();
		bf.close();
		fis.close();
		return HexConversions.bytesToHex(digestResult).toLowerCase();
	}

	public static String getMd5String(String in) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
			digest.reset();
			digest.update(in.getBytes());
			byte[] a = digest.digest();
			int len = a.length;
			StringBuilder sb = new StringBuilder(len << 1);
			for (int i = 0; i < len; i++) {
				sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
				sb.append(Character.forDigit(a[i] & 0x0f, 16));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
}
