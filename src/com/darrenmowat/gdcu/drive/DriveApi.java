package com.darrenmowat.gdcu.drive;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.JSONException;

import android.util.Log;

import com.darrenmowat.gdcu.GDCU;
import com.darrenmowat.gdcu.utils.Preconditions;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

/*
 * Wrapper for the Google Drive API
 * 
 * To hopefully simplify things...
 * 
 */
public class DriveApi {

	private Drive drive;

	private String rootFolderId;

	public static final String DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive";

	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();

	private String uploadsDirName;
	private File uploadsDir;

	private static final int RESPONSE_ITEM_LIMIT = 30;

	public DriveApi(String userToken, String uploadsDirName) {
		Preconditions.checkNotNull(userToken, "User Token passed into DriveApi is null");
		Preconditions.checkNotNull(uploadsDirName, "uploadsDirName passed into DriveApi is null");
		Credential user = new Credential(BearerToken.authorizationHeaderAccessMethod())
				.setAccessToken(userToken);
		drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, user)
				.setApplicationName(GDCU.APP_NAME + " (gzip)")
				.setJsonHttpRequestInitializer(new GzipRequestInitializer()).build();

		uploadsDir = GDCU.getCachedUploadsDir();
		rootFolderId = GDCU.getCachedRootDir();
		this.uploadsDirName = uploadsDirName;
	}

	private ArrayList<File> retrieveAllFiles(String query, String fields) throws IOException {
		ArrayList<File> result = new ArrayList<File>();
		Files.List request = drive.files().list();
		if (query != null && !query.trim().equals("")) {
			request.setQ(query);
		}
		if (fields != null && !fields.trim().equals("")) {
			request.setFields(fields);
		}
		request.setMaxResults(RESPONSE_ITEM_LIMIT);
		do {
			try {
				FileList files = request.execute();
				log("retrieveAllFiles [" + query + "]: " + files.getItems().size() + " items");
				result.addAll(files.getItems());
				request.setPageToken(files.getNextPageToken());
			} catch (IOException e) {
				log("An error occurred: " + e);
				request.setPageToken(null);
			}
		} while (request.getPageToken() != null && request.getPageToken().length() > 0);
		return result;
	}

	
	
	private File findUploadDirectory() throws IOException, JSONException {
		if (uploadsDir == null) {
			if (GDCU.getCachedUploadsDir() == null) {
				String query = "title = '" + uploadsDirName + "' " + "and trashed = false "
						+ "and mimeType = 'application/vnd.google-apps.folder'" + " and '"
						+ getRootId() + "' in parents";
				String fields = "kind,etag,nextPageToken,items(kind,id,title,createdDate,modifiedDate,parents/id,parents/isRoot,fileSize,labels/trashed)";
				ArrayList<File> files = retrieveAllFiles(query, fields);
				File uploads = null;
				if (files.size() == 0) {
					// We must create the uploads directory
					File ud = new File();
					ud.setMimeType("application/vnd.google-apps.folder");
					ud.setTitle(uploadsDirName);
					ParentReference root = new ParentReference();
					root.setId(getRootId());
					root.setIsRoot(true);
					ArrayList<ParentReference> parents = new ArrayList<ParentReference>();
					parents.add(root);
					ud.setParents(parents);
					Insert up = drive.files().insert(ud);
					uploads = up.execute();
				} else {
					uploads = files.get(0);
				}
				uploadsDir = uploads;
				GDCU.setCachedUploadsDir(uploadsDir);
			} else {
				uploadsDir = GDCU.getCachedUploadsDir();
			}
		}
		return uploadsDir;
	}

	private String getRootId() throws JSONException, IOException {
		if (rootFolderId == null) {
			rootFolderId = about().getRootFolderId();
			GDCU.setCachedRootDir(rootFolderId);
		}
		return rootFolderId;
	}

	public ArrayList<File> retrieveAllUploadedFiles() throws IOException, JSONException {
		File uploads = findUploadDirectory();
		String query = "trashed = false and '" + uploads.getId() + "' in parents";
		String fields = "kind,etag,nextPageToken,items(kind,id,title,mimeType,createdDate,modifiedDate,parents/id,parents/isRoot,md5Checksum,fileSize,labels/trashed)";
		return retrieveAllFiles(query, fields);
	}

	public ArrayList<File> retrieveAllUploadedFilesSince(long time) throws IOException,
			JSONException {
		DateTime since = new DateTime(time);
		File uploads = findUploadDirectory();
		String query = "trashed = false and '" + uploads.getId()
				+ "' in parents and modifiedDate > '" + since.toStringRfc3339() + "'";
		String fields = "kind,etag,nextPageToken,items(kind,id,title,mimeType,createdDate,modifiedDate,parents/id,parents/isRoot,md5Checksum,fileSize,labels/trashed)";
		return retrieveAllFiles(query, fields);
	}

	public File renameUploadsDir(String newTitle) throws IOException, JSONException {
		File current = findUploadDirectory();
		String query = "title = '" + newTitle + "' " + "and trashed = false "
				+ "and mimeType = 'application/vnd.google-apps.folder'" + " and '"
				+ getRootId() + "' in parents";
		String fields = "kind,etag,nextPageToken,items(kind,id,title,createdDate,modifiedDate,parents/id,parents/isRoot,fileSize,labels/trashed)";
		ArrayList<File> files = retrieveAllFiles(query, fields);
		if (files.size() == 0) {
			// There isn't an existing folder with this name
			// Rename the current one
			current.setTitle(newTitle);
			File updatedFile = drive.files().update(current.getId(), current).execute();
			uploadsDirName = newTitle;
			uploadsDir = updatedFile;
			GDCU.setCachedUploadsDir(uploadsDir);
		} else {
			File updatedFile = files.get(0);
			uploadsDirName = newTitle;
			uploadsDir = updatedFile;
			GDCU.setCachedUploadsDir(uploadsDir);
		}
		return uploadsDir;
	}

	private About about() throws JSONException, IOException {
		About about = drive.about().get().setFields("rootFolderId").execute();
		return about;
	}

	public File uploadFile(String name, String mimeType, java.io.File mediaFile, boolean onWifi)
			throws IOException, JSONException {
		// File Metadata
		File fileMetadata = new File();
		fileMetadata.setTitle(name);
		fileMetadata.setMimeType(mimeType);

		// Set the parent folder.
		ParentReference uploadDir = new ParentReference();
		uploadDir.setId(findUploadDirectory().getId());
		fileMetadata.setParents(Arrays.asList(uploadDir));

		InputStreamContent mediaContent = new InputStreamContent(mimeType, new BufferedInputStream(
				new FileInputStream(mediaFile)));
		mediaContent.setLength(mediaFile.length());

		Drive.Files.Insert insert = drive.files().insert(fileMetadata, mediaContent);
		insert.getMediaHttpUploader().setProgressListener(new ProgressListener(mediaFile));
		insert.getMediaHttpUploader().setBackOffPolicyEnabled(true);
		int chunkSize = onWifi ? MediaHttpUploader.MINIMUM_CHUNK_SIZE * 2
				: MediaHttpUploader.MINIMUM_CHUNK_SIZE;
		insert.getMediaHttpUploader().setChunkSize(chunkSize);
		return insert.execute();

	}

	public class GzipRequestInitializer implements JsonHttpRequestInitializer {
		@Override
		public void initialize(JsonHttpRequest request) {
			HttpHeaders headers = request.getRequestHeaders();
			headers.setAccept("gzip");
			headers.setUserAgent(GDCU.APP_NAME + " (gzip)");
			request.setRequestHeaders(headers);
		}
	}

	private class ProgressListener implements MediaHttpUploaderProgressListener {

		private java.io.File file;

		public ProgressListener(java.io.File file) {
			this.file = file;
		}

		@Override
		public void progressChanged(MediaHttpUploader uploader) throws IOException {
			switch (uploader.getUploadState()) {
			case INITIATION_STARTED:
				if (GDCU.DEVEL_BUILD) {
					log("Upload Progress [" + file + "]: Initiation has started!");
				}
				break;
			case INITIATION_COMPLETE:
				if (GDCU.DEVEL_BUILD) {
					log("Upload Progress [" + file + "]: Initiation is complete!");
				}
				break;
			case MEDIA_IN_PROGRESS:
				if (GDCU.DEVEL_BUILD) {
					log("Upload Progress [" + file + "]: " + uploader.getProgress() + "%");
				}
				break;
			case MEDIA_COMPLETE:
				if (GDCU.DEVEL_BUILD) {
					log("Upload Progress [" + file + "]: Upload Complete!");
				}
			case NOT_STARTED:
				if (GDCU.DEVEL_BUILD) {
					log("Upload Progress [" + file + "]: Not Started!");
				}
				break;
			default:
				break;
			}
		}
	}

	private void log(String msg) {
		Log.v("GDCU::DriveApi", msg);
	}
}
