package com.theta360.sample.view;

import android.view.View.OnClickListener;


/**
 * Line object for list when photo list is displayed
 */
public class ObjectRow {

	private int objectHandle;
	private boolean isPhoto;
	private byte[] thumbnail;
	private String fileName;
	private String captureDate;
	private OnClickListener onClickListener;

    /**
     * Handle value acquisition method for photo object
     * @return Handle value for photo object
     */
	public int getObjectHandle() {
		return objectHandle;
	}

    /**
     * Handle value setting method for photo object
     * @param objectHandle Handle value for photo object
     */
	public void setObjectHandle(int objectHandle) {
		this.objectHandle = objectHandle;
	}

    /**
     * Photo information feasibility value acquisition method
     * @return Photo information feasibility value
     */
	public boolean isPhoto() {
		return isPhoto;
	}

    /**
     * Photo information feasibility value setting method
     * @param isPhoto Photo information feasibility value
     */
	public void setIsPhoto(boolean isPhoto) {
		this.isPhoto = isPhoto;
	}

    /**
     * Thumbnail information acquisition method
     * @return Thumbnail information
     */
	public byte[] getThumbnail() {
		return thumbnail;
	}

    /**
     * Thumbnail information setting method
     * @param thumbnail Thumbnail information
     */
	public void setThumbnail(byte[] thumbnail) {
		this.thumbnail = thumbnail;
	}

    /**
     * File name acquisition method
     * @return File name
     */
	public String getFileName() {
		return fileName;
	}

    /**
     * File name setting method
     * @param fileName File name
     */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

    /**
     * Capture date and time acquisition method
     * @return Capture date and time
     */
	public String getCaptureDate() {
		return captureDate;
	}

    /**
     * Capture date and time setting method
     * @param captureDate Capture date and time
     */
	public void setCaptureDate(String captureDate) {
		this.captureDate = captureDate;
	}


    /**
     * Onclick Listener acquisition method
     * @return Onclick Listener
     */
	public OnClickListener getOnClickListener() {
		return onClickListener;
	}

    /**
     * Onclick Listener setting method
     * @param onClickListener Onclick Listener
     */
	public void setOnClickListener(OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
	}

}
