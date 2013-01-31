package com.darrenmowat.gdcu.service.helpers;

public interface ThreadCallbacks {

	public void onThreadFinished();

	public void onThreadError(Exception e);

}
