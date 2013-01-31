package com.darrenmowat.gdcu.otto;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

public class UploadStatusEventProducer {

	private UploadStatusEvent lastEvent = new UploadStatusEvent("", "");
	private boolean registered = false;

	public void register(Bus bus) {
		if (!registered) {
			bus.register(this);
			registered = true;
		}
	}

	public void unregister(Bus bus) {
		if (registered) {
			bus.unregister(this);
			registered = false;
		}
	}

	@Produce
	public UploadStatusEvent produceUploadStatusEvent() {
		return lastEvent;
	}

	public void setLastServiceRefreshingEvent(UploadStatusEvent lastEvent) {
		this.lastEvent = lastEvent;
	}

}
