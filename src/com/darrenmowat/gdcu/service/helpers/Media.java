package com.darrenmowat.gdcu.service.helpers;

import java.io.File;

public class Media {

	private File file;
	private String type;
	private String md5;

	public Media(File file, String type) {
		this.file = file;
		this.type = type;
		this.md5 = "";
	}

	public String getType() {
		return type;
	}

	public File getFile() {
		return file;
	}

	public void setMD5(String md5) {
		this.md5 = md5;
	}

	public String getMD5() {
		return md5;
	}

}