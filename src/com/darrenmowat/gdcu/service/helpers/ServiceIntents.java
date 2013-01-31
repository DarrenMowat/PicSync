package com.darrenmowat.gdcu.service.helpers;

import android.content.Intent;

public class ServiceIntents {

	public static class Upload {

		private static final String ACTIONPREFIX = "com.darrenmowat.gdcu.service.UploadService";

		public static final String UPLOAD_MEDIA = ACTIONPREFIX + ".upload";

		public static final Intent UPLOAD_MEDIA_INTENT = new Intent(UPLOAD_MEDIA);

	}

	public static class Gallery {

		private static final String ACTIONPREFIX = "com.darrenmowat.gdcu.service.GalleryService";

		public static final String FIND_MEDIA = ACTIONPREFIX + ".find_media";

		public static final Intent FIND_MEDIA_INTENT = new Intent(FIND_MEDIA);

		public static final String ADD_FROM_OBSERVER = ACTIONPREFIX + ".add_from_observer";

		public static final Intent ADD_FROM_OBSERVER_INTENT = new Intent(ADD_FROM_OBSERVER);

		public static final String PATH_EXTRA = "_path";

		public static final String MIME_EXTRA = "_mime";

	}

}
