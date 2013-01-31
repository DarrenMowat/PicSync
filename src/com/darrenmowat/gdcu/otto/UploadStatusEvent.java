package com.darrenmowat.gdcu.otto;

public class UploadStatusEvent {

	public String title;
	public String message;

	public UploadStatusEvent(String title, String message) {
		this.title = title;
		this.message = message;
	}

}
