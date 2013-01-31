package com.darrenmowat.gdcu.otto;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

;

public class DataBus {

	private static Bus mInstance = null;
	private static UploadStatusEventProducer statProducer = null;

	public static Bus getInstance() {
		if (mInstance == null) {
			mInstance = new Bus(ThreadEnforcer.ANY);
		}
		if (statProducer == null) {
			statProducer = new UploadStatusEventProducer();
			statProducer.register(mInstance);
		}
		return mInstance;
	}

	public static void postUploadStatusEventProducer(UploadStatusEvent event) {
		getInstance().post(event);
		statProducer.setLastServiceRefreshingEvent(event);
	}

}
