package com.theta360.sample;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.theta360.lib.PtpipInitiator;
import com.theta360.lib.ThetaException;
import com.theta360.lib.ptpip.entity.*;
import com.theta360.lib.ptpip.eventlistener.PtpipEventListener;
import com.theta360.sample.view.LogView;
import com.theta360.sample.view.ObjectListArrayAdapter;
import com.theta360.sample.view.ObjectRow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Activity that displays the photo list
 */
public class ObjectListActivity extends Activity {
	private ListView objectList;
	private LogView logViewer;
	private String cameraIpAddress;

	private LinearLayout layoutCameraArea;
	private Button btnShoot;
	private TextView textCameraStatus;


    /**
     * onCreate Method
     * @param savedInstanceState onCreate Status value
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_object_list);

		logViewer = (LogView) findViewById(R.id.log_view);
		cameraIpAddress = getResources().getString(R.string.theta_ip_address);
		getActionBar().setTitle(cameraIpAddress);

		layoutCameraArea = (LinearLayout) findViewById(R.id.shoot_area);
		textCameraStatus = (TextView) findViewById(R.id.camera_status);
		btnShoot = (Button) findViewById(R.id.btn_shoot);
		btnShoot.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				btnShoot.setEnabled(false);
				textCameraStatus.setText(R.string.text_camera_synthesizing);
				new ShootTask().execute();
			}
		});

	}


    /**
     * onCreateOptionsMenu Method
     * @param menu Menu initialization object
     * @return Menu display feasibility status value
     */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.connection, menu);

		Switch connectionSwitch = (Switch) menu.findItem(R.id.connection).getActionView().findViewById(R.id.connection_switch);
		connectionSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			LoadObjectListTask sampleTask = new LoadObjectListTask();

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				objectList = (ListView) findViewById(R.id.object_list);
				ObjectListArrayAdapter empty = new ObjectListArrayAdapter(ObjectListActivity.this, R.layout.listlayout_object, new ArrayList<ObjectRow>());
				objectList.setAdapter(empty);

				if (isChecked) {
					layoutCameraArea.setVisibility(View.VISIBLE);
					sampleTask.execute();
				} else {
					layoutCameraArea.setVisibility(View.INVISIBLE);
					sampleTask.cancel(true);
					sampleTask = new LoadObjectListTask();
					new DisConnectTask().execute();
				}
			}
		});
		return true;
	}

	private void changeCameraStatus(final int resid) {
		runOnUiThread(new Runnable() {
			public void run() {
				textCameraStatus.setText(resid);
			}
		});
	}

	private void appendLogView(final String log) {
		runOnUiThread(new Runnable() {
			public void run() {
				logViewer.append(log);
			}
		});
	}

	private class LoadObjectListTask extends AsyncTask<Void, String, List<ObjectRow>> {

		private ProgressBar progressBar;

		public LoadObjectListTask() {
			progressBar = (ProgressBar) findViewById(R.id.loading_object_list_progress_bar);
		}

		@Override
		protected void onPreExecute() {
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected List<ObjectRow> doInBackground(Void... params) {
			try {
				publishProgress("------");
				publishProgress("connecting to " + cameraIpAddress + "...");
				PtpipInitiator camera = new PtpipInitiator(cameraIpAddress);
				publishProgress("connected.");
				changeCameraStatus(R.string.text_camera_standby);

				DeviceInfo deviceInfo = camera.getDeviceInfo();
				publishProgress(deviceInfo.getClass().getSimpleName() + ":<" + deviceInfo.getModel() + ", " + deviceInfo.getDeviceVersion() + ", " + deviceInfo.getSerialNumber() + ">");

				ObjectHandles objectHandles = camera.getObjectHandles(PtpipInitiator.PARAMETER_VALUE_DEFAULT, PtpipInitiator.PARAMETER_VALUE_DEFAULT, PtpipInitiator.PARAMETER_VALUE_DEFAULT);
				int objectHandleCount = objectHandles.size();
				publishProgress("getObjectHandles() received " + objectHandleCount + " handles.");

				List<ObjectRow> objectRows = new ArrayList<ObjectRow>();

				StorageIds storageIDs = camera.getStorageIDs();
				int storageCount = storageIDs.size();
				for (int i = 0; i < storageCount; i++) {
					int storageId = storageIDs.getStorageId(i);

					StorageInfo storage = camera.getStorageInfo(storageId);
					ObjectRow storageCapacity = new ObjectRow();
					int freeSpaceInImages = storage.getFreeSpaceInImages();
					int megaByte = 1024 * 1024;
					long freeSpace = storage.getFreeSpaceInBytes() / megaByte;
					long maxSpace = storage.getMaxCapacity() / megaByte;
					storageCapacity.setFileName("Free space: " + freeSpaceInImages + "[shots] (" + freeSpace + "/" + maxSpace + "[MB])");
					objectRows.add(storageCapacity);
				}

				for (int i = 0; i < objectHandleCount; i++) {
					ObjectRow objectRow = new ObjectRow();

					final int objectHandle = objectHandles.getObjectHandle(i);
					ObjectInfo object = camera.getObjectInfo(objectHandle);
					objectRow.setObjectHandle(objectHandle);
					objectRow.setFileName(object.getFilename());
					objectRow.setCaptureDate(object.getCaptureDate());

					if (object.getObjectFormat() == ObjectInfo.OBJECT_FORMAT_CODE_EXIF_JPEG) {
						objectRow.setIsPhoto(true);
						PtpObject thumbnail = camera.getThumb(objectHandle);
						final byte[] thumbnailImage = thumbnail.getDataObject();
						objectRow.setThumbnail(thumbnailImage);
					} else {
						objectRow.setIsPhoto(false);
					}
					objectRows.add(objectRow);

					publishProgress("getObjectInfo: " + (i + 1) + "/" + objectHandleCount);
				}
				return objectRows;

			} catch (Throwable throwable) {
				String errorLog = Log.getStackTraceString(throwable);
				publishProgress(errorLog);
				return null;
			}
		}

		@Override
		protected void onProgressUpdate(String... values) {
			for (String log : values) {
				logViewer.append(log);
			}
		}

		@Override
		protected void onPostExecute(List<ObjectRow> objectRows) {
			if (objectRows != null) {
				ObjectListArrayAdapter objectListArrayAdapter = new ObjectListArrayAdapter(ObjectListActivity.this, R.layout.listlayout_object, objectRows);
				objectList.setAdapter(objectListArrayAdapter);
				objectList.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						ObjectRow selectedItem = (ObjectRow) parent.getItemAtPosition(position);
						if (selectedItem.isPhoto()) {
							byte[] thumbnail = selectedItem.getThumbnail();
							int objectHandle = selectedItem.getObjectHandle();
							GLPhotoActivity.startActivity(ObjectListActivity.this, cameraIpAddress, objectHandle, thumbnail);
						} else {
							Toast.makeText(getApplicationContext(), "This isn't a photo.", Toast.LENGTH_SHORT).show();
						}
					}
				});
			}

			progressBar.setVisibility(View.GONE);
		}

		@Override
		protected void onCancelled() {
			progressBar.setVisibility(View.GONE);
		}
	}

	private class DisConnectTask extends AsyncTask<Void, String, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {

			try {
				PtpipInitiator.close();
				publishProgress("disconnected.");
				return true;

			} catch (Throwable throwable) {
				String errorLog = Log.getStackTraceString(throwable);
				publishProgress(errorLog);
				return false;
			}
		}

		@Override
		protected void onProgressUpdate(String... values) {
			for (String log : values) {
				logViewer.append(log);
			}
		}
	}

	private static enum ShootResult {
		SUCCESS, FAIL_CAMERA_DISCONNECTED, FAIL_STORE_FULL, FAIL_DEVICE_BUSY
	}

	private class ShootTask extends AsyncTask<Void, Void, ShootResult> {

		@Override
		protected void onPreExecute() {
			logViewer.append("initiateCapture");
		};

		@Override
		protected ShootResult doInBackground(Void... params) {
			CaptureListener postviewListener = new CaptureListener();
			try {
				PtpipInitiator camera = new PtpipInitiator(getResources().getString(R.string.theta_ip_address));
				camera.initiateCapture(postviewListener);
				return ShootResult.SUCCESS;

			} catch (IOException e) {
				return ShootResult.FAIL_CAMERA_DISCONNECTED;
			} catch (ThetaException e) {
				if (Response.RESPONSE_CODE_STORE_FULL == e.getStatus()) {
					return ShootResult.FAIL_STORE_FULL;
				} else if (Response.RESPONSE_CODE_DEVICE_BUSY == e.getStatus()) {
					return ShootResult.FAIL_DEVICE_BUSY;
				} else {
					return ShootResult.FAIL_CAMERA_DISCONNECTED;
				}
			}
		}

		@Override
		protected void onPostExecute(ShootResult result) {
			if (result == ShootResult.FAIL_CAMERA_DISCONNECTED) {
				logViewer.append("initiateCapture:FAIL_CAMERA_DISCONNECTED");
			} else if (result == ShootResult.FAIL_STORE_FULL) {
				logViewer.append("initiateCapture:FAIL_STORE_FULL");
			} else if (result == ShootResult.FAIL_DEVICE_BUSY) {
				logViewer.append("initiateCapture:FAIL_DEVICE_BUSY");
			} else if (result == ShootResult.SUCCESS) {
				logViewer.append("initiateCapture:SUCCESS");
			}
		}

		private class CaptureListener extends PtpipEventListener {
			private int latestCapturedObjectHandle;
			private boolean objectAdd = false;

			@Override
			public void onObjectAdded(int objectHandle) {
				this.objectAdd = true;
				this.latestCapturedObjectHandle = objectHandle;
				appendLogView("objectAdd:ObjectHandle " + latestCapturedObjectHandle);
			}

			@Override
			public void onCaptureComplete(int transactionId) {
				appendLogView("CaptureComplete");
				if (objectAdd) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							btnShoot.setEnabled(true);
							textCameraStatus.setText(R.string.text_camera_standby);
							new GetThumbnailTask(latestCapturedObjectHandle).execute();
						}
					});
				}
			}
		}
	}

	private class GetThumbnailTask extends AsyncTask<Void, Void, Void> {

		private int objectHandle;

		public GetThumbnailTask(int objectHandle) {
			this.objectHandle = objectHandle;
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				PtpipInitiator camera = new PtpipInitiator(getResources().getString(R.string.theta_ip_address));
				PtpObject thumbnail = camera.getThumb(objectHandle);
				byte[] thumbnailImage = thumbnail.getDataObject();
				GLPhotoActivity.startActivity(ObjectListActivity.this, cameraIpAddress, objectHandle, thumbnailImage);
				return null;

			} catch (IOException e) {
				String errorLog = Log.getStackTraceString(e);
				logViewer.append(errorLog);
			} catch (ThetaException e) {
				String errorLog = Log.getStackTraceString(e);
				logViewer.append(errorLog);
			}
			return null;
		}
	}
}
