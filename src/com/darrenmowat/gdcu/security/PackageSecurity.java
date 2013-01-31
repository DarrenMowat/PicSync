package com.darrenmowat.gdcu.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

public class PackageSecurity {

	/**
	 * This method will check that the package name & singature of this apk are valid
	 * 
	 * If they are not a PackageSecurityException is thrown. The provided userIdentifier will be included as part
	 * of the exception message. 
	 * 
	 * You can catch the exception and so as you please or allow it to be caught and logged by whatever exception tracker you are using.
	 * 
	 * @param context - A reference to Android Context
	 * @param packageName - The name you expect the package to be
	 * @param packageSignature - One of the signature you expect the package to be signed with
	 * @param userIdentifier - A way to identify this user. This will be included in the PackageSecurityException thrown
	 * @throws PackageSecurityException
	 */
	public static void checkPackageCertificate(Context context, String packageName, String packageSignature, String userIdentifier) throws PackageSecurityException {
		if (!context.getPackageName().equals(packageName)) {
			throw new PackageSecurityException("Package has been renamed to " + context.getPackageName() + " " + userIdentifier, userIdentifier);
		}
		PackageInfo packageInfo;
		try {
			packageInfo = context.getPackageManager().getPackageInfo(packageName,
					PackageManager.GET_SIGNATURES);
		} catch (PackageManager.NameNotFoundException e) {
			throw new PackageSecurityException("Package name not found - " + packageName + " " + userIdentifier, userIdentifier);
		}
		boolean signed = false;
		for (Signature signature : packageInfo.signatures) {
			if (signature.toCharsString().equals(packageSignature)) {
				signed = true;
			}
		}
		if (!signed) {
			throw new PackageSecurityException("Package is not signed with correct key" + " " + userIdentifier, userIdentifier);
		}
	}
}
