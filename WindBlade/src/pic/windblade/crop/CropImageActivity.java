/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pic.windblade.crop;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import pic.windblade.R;
import pic.windblade.crop.HighlightView.GridMode;
import pic.windblade.crop.HighlightView.HandleMode;
import pic.windblade.crop.common.CropConst;
import pic.windblade.crop.intf.CropAreaListener;
import pic.windblade.view.InputCropAspectDialog;

/*
 * Modified from original in AOSP.
 */
public class CropImageActivity extends MonitoredActivity {

	private static final int SIZE_DEFAULT = 2048;
	private static final int SIZE_LIMIT = 4096;

	private final Handler handler = new Handler();

	private int aspectX;
	private int aspectY;

	// Output image
	private int maxX;
	private int maxY;
	private int exifRotation;

	private Uri sourceUri;
	private Uri saveUri;

	private boolean isSaving;

	private int sampleScaleSize;
	private RotateBitmap rotateBitmap;
	private CropImageView imageView;
	private HighlightView cropView;
	private TextView tv_aspect_ratio;
	private CropAreaOnChanged cropAreaListener;
	protected Cropper cropper;
	/** 已选择长宽比模式 */
	private int selectedAspectMode = CropConst.ASPECT_MODE_ONE_ONE;
	private RelativeLayout rl_aspect_unspecified;
	private ImageView img_aspect_unspecified;
	private TextView tv_aspect_unspecified;
	private RelativeLayout rl_aspect_oneone;
	private ImageView img_aspect_oneone;
	private TextView tv_aspect_oneone;
	private RelativeLayout rl_aspect_threetwo;
	private ImageView img_aspect_threetwo;
	private TextView tv_aspect_threetwo;
	private RelativeLayout rl_aspect_fourthree;
	private ImageView img_aspect_fourthree;
	private TextView tv_aspect_fourthree;
	private RelativeLayout rl_aspect_sixteennine;
	private ImageView img_aspect_sixteennine;
	private TextView tv_aspect_sixteennine;
	private RelativeLayout rl_aspect_custom;
	private ImageView img_aspect_custom;
	private TextView tv_aspect_custom;
	private LinearLayout llyt_aspect_mode;

	private boolean fixedAspectRatio = false;

	private int finalWidth;
	private int finalHeight;
	private InputCropAspectDialog customDialog;
	/** 原图宽度 */
	private int orignalImgWidth;
	/** 原图高度 */
	private int orignalImgHeight;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setupWindowFlags();
		setupViews();

		loadInput();
		if (rotateBitmap == null) {
			finish();
			return;
		}
		startCrop();
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	private void setupWindowFlags() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		}
	}

	private void setupViews() {
		setContentView(R.layout.crop_activity_crop);

		imageView = (CropImageView) findViewById(R.id.crop_image);
		imageView.context = this;
		imageView.setRecycler(new ImageViewTouchBase.Recycler() {
			@Override
			public void recycle(Bitmap b) {
				b.recycle();
				System.gc();
			}
		});
		cropAreaListener = new CropAreaOnChanged();
		imageView.setCropAreaListener(cropAreaListener);

		// findViewById(R.id.btn_cancel).setOnClickListener(
		// new View.OnClickListener() {
		// public void onClick(View v) {
		// setResult(RESULT_CANCELED);
		// finish();
		// }
		// });
		findViewById(R.id.btn_rotate).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onRotateClicked();
					}
				});
		findViewById(R.id.btn_done).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View v) {
						onSaveClicked();
					}
				});
		tv_aspect_ratio = (TextView) findViewById(R.id.tv_aspect_ratio);
		rl_aspect_unspecified = (RelativeLayout) findViewById(R.id.rl_aspect_unspecified);
		img_aspect_unspecified = (ImageView) findViewById(R.id.img_aspect_unspecified);
		tv_aspect_unspecified = (TextView) findViewById(R.id.tv_aspect_unspecified);
		rl_aspect_oneone = (RelativeLayout) findViewById(R.id.rl_aspect_oneone);
		img_aspect_oneone = (ImageView) findViewById(R.id.img_aspect_oneone);
		tv_aspect_oneone = (TextView) findViewById(R.id.tv_aspect_oneone);
		rl_aspect_threetwo = (RelativeLayout) findViewById(R.id.rl_aspect_threetwo);
		img_aspect_threetwo = (ImageView) findViewById(R.id.img_aspect_threetwo);
		tv_aspect_threetwo = (TextView) findViewById(R.id.tv_aspect_threetwo);
		rl_aspect_fourthree = (RelativeLayout) findViewById(R.id.rl_aspect_fourthree);
		img_aspect_fourthree = (ImageView) findViewById(R.id.img_aspect_fourthree);
		tv_aspect_fourthree = (TextView) findViewById(R.id.tv_aspect_fourthree);
		rl_aspect_sixteennine = (RelativeLayout) findViewById(R.id.rl_aspect_sixteennine);
		img_aspect_sixteennine = (ImageView) findViewById(R.id.img_aspect_sixteennine);
		tv_aspect_sixteennine = (TextView) findViewById(R.id.tv_aspect_sixteennine);
		rl_aspect_custom = (RelativeLayout) findViewById(R.id.rl_aspect_custom);
		img_aspect_custom = (ImageView) findViewById(R.id.img_aspect_custom);
		tv_aspect_custom = (TextView) findViewById(R.id.tv_aspect_custom);
		llyt_aspect_mode = (LinearLayout) findViewById(R.id.llyt_aspect_mode);

		rl_aspect_unspecified.setOnClickListener(new SwitchAspectOnClick());
		rl_aspect_oneone.setOnClickListener(new SwitchAspectOnClick());
		rl_aspect_threetwo.setOnClickListener(new SwitchAspectOnClick());
		rl_aspect_fourthree.setOnClickListener(new SwitchAspectOnClick());
		rl_aspect_sixteennine.setOnClickListener(new SwitchAspectOnClick());
		rl_aspect_custom.setOnClickListener(new SwitchAspectOnClick());
	}

	private void loadInput() {
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			aspectX = extras.getInt(Crop.Extra.ASPECT_X);
			aspectY = extras.getInt(Crop.Extra.ASPECT_Y);
			maxX = extras.getInt(Crop.Extra.MAX_X);
			maxY = extras.getInt(Crop.Extra.MAX_Y);
			saveUri = extras.getParcelable(MediaStore.EXTRA_OUTPUT);
		}

		sourceUri = intent.getData();
		if (sourceUri != null) {
			exifRotation = CropUtil.getExifRotation(CropUtil.getFromMediaUri(
					this, getContentResolver(), sourceUri));

			InputStream is = null;
			try {
				sampleScaleSize = calculateBitmapSampleSize(sourceUri);
				is = getContentResolver().openInputStream(sourceUri);
				BitmapFactory.Options option = new BitmapFactory.Options();
				option.inSampleSize = sampleScaleSize;
				rotateBitmap = new RotateBitmap(BitmapFactory.decodeStream(is,
						null, option), exifRotation);
			} catch (IOException e) {
				Log.e("Error reading image: " + e.getMessage(), e);
				setResultException(e);
			} catch (OutOfMemoryError e) {
				Log.e("OOM reading image: " + e.getMessage(), e);
				setResultException(e);
			} finally {
				CropUtil.closeSilently(is);
			}
		}
	}

	private int calculateBitmapSampleSize(Uri bitmapUri) throws IOException {
		InputStream is = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		try {
			is = getContentResolver().openInputStream(bitmapUri);
			BitmapFactory.decodeStream(is, null, options); // Just get image
															// size
		} finally {
			CropUtil.closeSilently(is);
		}

		int maxSize = getMaxImageSize();
		int sampleSize = 1;
		while (options.outHeight / sampleSize > maxSize
				|| options.outWidth / sampleSize > maxSize) {
			sampleSize = sampleSize << 1;
		}
		return sampleSize;
	}

	private int getMaxImageSize() {
		int textureLimit = getMaxTextureSize();
		if (textureLimit == 0) {
			return SIZE_DEFAULT;
		} else {
			return Math.min(textureLimit, SIZE_LIMIT);
		}
	}

	private int getMaxTextureSize() {
		// The OpenGL texture size is the maximum size that can be drawn in an
		// ImageView
		int[] maxSize = new int[1];
		GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
		return maxSize[0];
	}

	private void startCrop() {
		if (isFinishing()) {
			return;
		}
		imageView.setImageRotateBitmapResetBase(rotateBitmap, true);
		CropUtil.startBackgroundJob(this, null,
				getResources().getString(R.string.crop_wait), new Runnable() {
					public void run() {
						final CountDownLatch latch = new CountDownLatch(1);
						handler.post(new Runnable() {
							public void run() {
								if (imageView.getScale() == 1F) {
									imageView.center();
								}
								latch.countDown();
							}
						});
						try {
							latch.await();
						} catch (InterruptedException e) {
							throw new RuntimeException(e);
						}
						cropper = new Cropper();
						cropper.crop();
					}
				}, handler);
	}

	private class Cropper {

		private void makeDefault() {
			if (rotateBitmap == null) {
				return;
			}
			HighlightView hv = new HighlightView(imageView);
			final int width = rotateBitmap.getWidth();
			final int height = rotateBitmap.getHeight();
			orignalImgWidth = width * sampleScaleSize;
			orignalImgHeight = height * sampleScaleSize;
			Rect imageRect = new Rect(0, 0, width, height);

			// Make the default size about 4/5 of the width or height
			int cropWidth = Math.min(width, height) * 4 / 5;
			int cropHeight = cropWidth;

			if (aspectX != 0 && aspectY != 0) {
				if (aspectX > aspectY) {
					cropHeight = cropWidth * aspectY / aspectX;
				} else {
					cropWidth = cropHeight * aspectX / aspectY;
				}
			}

			int x = (width - cropWidth) / 2;
			int y = (height - cropHeight) / 2;

			RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
			hv.setStyles(GridMode.Changing, false, 0, HandleMode.Never);
			hv.setup(imageView.getUnrotatedMatrix(), imageRect, cropRect,
					fixedAspectRatio);
			imageView.add(hv);
		}

		/** 重设裁剪框 */
		public void resetAspect(boolean isRotate) {
			/** 由于只需要当前比例（缩放后），所以比例传1 */
			Rect scaledCropRect = cropView.getScaledCropRect(1);
			int cropWidth = scaledCropRect.width();
			int cropHeight = scaledCropRect.height();
			Rect scaledSrcRect = cropView.getScaledImageRect(1);

			imageView.clear();

			HighlightView hv = new HighlightView(imageView);
			RectF cropRect;
			if (isRotate) {
				// 判断旋转后裁剪框的宽或长是否超过原图长宽
				int tempWidth = cropWidth;
				cropWidth = cropHeight;
				cropHeight = tempWidth;
				boolean needResize = false;// 是否需要重置裁剪框位置和大小
				if (scaledCropRect.left + cropWidth > scaledSrcRect.right) {
					// 右侧宽度超出
					needResize = true;
				} else if (scaledCropRect.top + cropHeight > scaledSrcRect.bottom) {
					// 底部高度超出
					needResize = true;
				}
				if (needResize) {
					cropWidth = Math.min(scaledSrcRect.bottom,
							scaledSrcRect.right) * 4 / 5;
					cropHeight = cropWidth;
					if (aspectX != 0 && aspectY != 0) {
						if (aspectX >= aspectY) {
							cropHeight = cropWidth * aspectY / aspectX;
						} else {
							cropWidth = cropHeight * aspectX / aspectY;
						}
					}
					int x = (scaledSrcRect.right - cropWidth) / 2;
					int y = (scaledSrcRect.bottom - cropHeight) / 2;
					cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
				} else {
					cropRect = new RectF(scaledCropRect.left,
							scaledCropRect.top,
							scaledCropRect.left + cropWidth, scaledCropRect.top
									+ cropHeight);
				}
			} else {
				if (aspectX != 0 && aspectY != 0) {
					if (aspectX >= aspectY) {
						cropHeight = cropWidth * aspectY / aspectX;
					} else {
						cropWidth = cropHeight * aspectX / aspectY;
					}
				}
				cropRect = new RectF(scaledCropRect.left, scaledCropRect.top,
						scaledCropRect.left + cropWidth, scaledCropRect.top
								+ cropHeight);
			}

			hv.setStyles(GridMode.Changing, false, 0, HandleMode.Never);
			// hv.setup(imageView.getUnrotatedMatrix(), imageRect, cropRect,
			// aspectX != 0 && aspectY != 0);
			hv.setup(imageView.getUnrotatedMatrix(), scaledSrcRect, cropRect,
					fixedAspectRatio);
			imageView.add(hv);
			imageView.invalidate();
			if (imageView.highlightViews.size() == 1) {
				cropView = imageView.highlightViews.get(0);
				cropView.setFocus(true);
			}
			cropAreaListener.areaChangedAfterInit();
		}

		/** 用自定义值重设裁剪框 */
		public void resetCustomAspect() {
			// TODO 需要重新缩放原图，必要时候更改裁剪框位置（不需要根据长宽比重算宽高值）
			Rect orignalSrcRect = cropView.getScaledImageRect(sampleScaleSize);
			Rect scaledSrcRect = cropView.getScaledImageRect(1);
			int cropWidth = finalWidth;
			int cropHeight = finalHeight;
//			imageView.clear();
			HighlightView hv = new HighlightView(imageView);
			int x = (orignalSrcRect.right - cropWidth) / 2;
			int y = (orignalSrcRect.bottom - cropHeight) / 2;

			RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
			hv.setStyles(GridMode.Changing, false, 0, HandleMode.Never);
			hv.setup(imageView.getUnrotatedMatrix(), scaledSrcRect, cropRect,
					fixedAspectRatio);
			imageView.clear();
			imageView.add(hv);
			imageView.invalidate();
			if (imageView.highlightViews.size() == 1) {
				cropView = imageView.highlightViews.get(0);
				cropView.setFocus(true);
			}
			cropAreaListener.areaChangedAfterInit();
		}

		public void crop() {
			handler.post(new Runnable() {
				public void run() {
					makeDefault();
					imageView.invalidate();
					if (imageView.highlightViews.size() == 1) {
						cropView = imageView.highlightViews.get(0);
						cropView.setFocus(true);
					}
					cropAreaListener.areaChangedAfterInit();
					setAspectBtnSelected(CropConst.ASPECT_MODE_UNSPECIFIED);// 默认无限制
				}
			});
		}
	}

	private void onSaveClicked() {
		if (cropView == null || isSaving) {
			return;
		}
		isSaving = true;

		Bitmap croppedImage;
		Rect r = cropView.getScaledCropRect(sampleScaleSize);
		int width = r.width();
		int height = r.height();

		int outWidth = width;
		int outHeight = height;
		if (maxX > 0 && maxY > 0 && (width > maxX || height > maxY)) {
			float ratio = (float) width / (float) height;
			if ((float) maxX / (float) maxY > ratio) {
				outHeight = maxY;
				outWidth = (int) ((float) maxY * ratio + .5f);
			} else {
				outWidth = maxX;
				outHeight = (int) ((float) maxX / ratio + .5f);
			}
		}

		try {
			croppedImage = decodeRegionCrop(r, outWidth, outHeight);
		} catch (IllegalArgumentException e) {
			setResultException(e);
			finish();
			return;
		}

		if (croppedImage != null) {
			imageView.setImageRotateBitmapResetBase(new RotateBitmap(
					croppedImage, exifRotation), true);
			imageView.center();
			imageView.highlightViews.clear();
		}
		saveImage(croppedImage);
	}

	private void saveImage(Bitmap croppedImage) {
		if (croppedImage != null) {
			final Bitmap b = croppedImage;
			CropUtil.startBackgroundJob(this, null,
					getResources().getString(R.string.crop_saving),
					new Runnable() {
						public void run() {
							saveOutput(b);
						}
					}, handler);
		} else {
			finish();
		}
	}

	private Bitmap decodeRegionCrop(Rect rect, int outWidth, int outHeight) {
		// Release memory now
		clearImageView();

		InputStream is = null;
		Bitmap croppedImage = null;
		try {
			is = getContentResolver().openInputStream(sourceUri);
			BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is,
					false);
			final int width = decoder.getWidth();
			final int height = decoder.getHeight();

			if (exifRotation != 0) {
				// Adjust crop area to account for image rotation
				Matrix matrix = new Matrix();
				matrix.setRotate(-exifRotation);

				RectF adjusted = new RectF();
				matrix.mapRect(adjusted, new RectF(rect));

				// Adjust to account for origin at 0,0
				adjusted.offset(adjusted.left < 0 ? width : 0,
						adjusted.top < 0 ? height : 0);
				rect = new Rect((int) adjusted.left, (int) adjusted.top,
						(int) adjusted.right, (int) adjusted.bottom);
			}

			try {
				croppedImage = decoder.decodeRegion(rect,
						new BitmapFactory.Options());
				if (croppedImage != null
						&& (rect.width() > outWidth || rect.height() > outHeight)) {
					Matrix matrix = new Matrix();
					matrix.postScale((float) outWidth / rect.width(),
							(float) outHeight / rect.height());
					croppedImage = Bitmap.createBitmap(croppedImage, 0, 0,
							croppedImage.getWidth(), croppedImage.getHeight(),
							matrix, true);
				}
			} catch (IllegalArgumentException e) {
				// Rethrow with some extra information
				throw new IllegalArgumentException("Rectangle " + rect
						+ " is outside of the image (" + width + "," + height
						+ "," + exifRotation + ")", e);
			}

		} catch (IOException e) {
			Log.e("Error cropping image: " + e.getMessage(), e);
			setResultException(e);
		} catch (OutOfMemoryError e) {
			Log.e("OOM cropping image: " + e.getMessage(), e);
			setResultException(e);
		} finally {
			CropUtil.closeSilently(is);
		}
		return croppedImage;
	}

	private void clearImageView() {
		imageView.clear();
		if (rotateBitmap != null) {
			rotateBitmap.recycle();
		}
		System.gc();
	}

	private void saveOutput(Bitmap croppedImage) {
		if (saveUri != null) {
			OutputStream outputStream = null;
			try {
				outputStream = getContentResolver().openOutputStream(saveUri);
				if (outputStream != null) {
					croppedImage.compress(Bitmap.CompressFormat.JPEG, 90,
							outputStream);
				}
			} catch (IOException e) {
				setResultException(e);
				Log.e("Cannot open file: " + saveUri, e);
			} finally {
				CropUtil.closeSilently(outputStream);
			}

			CropUtil.copyExifRotation(CropUtil.getFromMediaUri(this,
					getContentResolver(), sourceUri), CropUtil.getFromMediaUri(
					this, getContentResolver(), saveUri));

			setResultUri(saveUri);
		}

		final Bitmap b = croppedImage;
		handler.post(new Runnable() {
			public void run() {
				imageView.clear();
				b.recycle();
			}
		});

		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (rotateBitmap != null) {
			rotateBitmap.recycle();
		}
	}

	@Override
	public boolean onSearchRequested() {
		return false;
	}

	public boolean isSaving() {
		return isSaving;
	}

	private void setResultUri(Uri uri) {
		setResult(RESULT_OK,
				new Intent().putExtra(MediaStore.EXTRA_OUTPUT, uri));
	}

	private void setResultException(Throwable throwable) {
		setResult(Crop.RESULT_ERROR,
				new Intent().putExtra(Crop.Extra.ERROR, throwable));
	}

	/**
	 * 计算裁剪区域图片大小并刷新回显
	 */
	private void calculateCropSizeAndReview() {
		Rect r = cropView.getScaledCropRect(sampleScaleSize);
		int width = r.width();
		int height = r.height();

		int outWidth = width;
		int outHeight = height;
		if (maxX > 0 && maxY > 0 && (width > maxX || height > maxY)) {
			float ratio = (float) width / (float) height;
			if ((float) maxX / (float) maxY > ratio) {
				outHeight = maxY;
				outWidth = (int) ((float) maxY * ratio + .5f);
			} else {
				outWidth = maxX;
				outHeight = (int) ((float) maxX / ratio + .5f);
			}
		}

		finalWidth = outWidth;
		finalHeight = outHeight;
		tv_aspect_ratio.setText("选中区域宽高：" + outWidth + " x " + outHeight);
		if (!fixedAspectRatio) {
			// 未限制裁剪框比例时，需要及时刷新当前长宽比值
			int gcd = CropUtil.getGreatestCommonDivisor(outWidth, outHeight);
			aspectX = outWidth / gcd;
			aspectY = outHeight / gcd;
		}
	}

	/**
	 * 裁剪区域监听器实现类
	 */
	private class CropAreaOnChanged implements CropAreaListener {
		@Override
		public void areaChangedAfterInit() {
			calculateCropSizeAndReview();
		}

		@Override
		public void areaChangedAfterTouch() {
			calculateCropSizeAndReview();
		}

	}

	private class SwitchAspectOnClick implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.rl_aspect_unspecified:
				setAspectBtnSelected(CropConst.ASPECT_MODE_UNSPECIFIED);
				break;
			case R.id.rl_aspect_oneone:
				setAspectBtnSelected(CropConst.ASPECT_MODE_ONE_ONE);
				break;
			case R.id.rl_aspect_threetwo:
				setAspectBtnSelected(CropConst.ASPECT_MODE_THREE_TWO);
				break;
			case R.id.rl_aspect_fourthree:
				setAspectBtnSelected(CropConst.ASPECT_MODE_FOUR_THREE);
				break;
			case R.id.rl_aspect_sixteennine:
				setAspectBtnSelected(CropConst.ASPECT_MODE_SIXTEEN_NINE);
				break;
			case R.id.rl_aspect_custom:
				setAspectBtnSelected(CropConst.ASPECT_MODE_CUSTOM);
				break;
			}
		}

	}

	private void setAspectBtnSelected(int selectedMode) {
		selectedAspectMode = selectedMode;
		setAspectBtnImage(selectedMode);
		setAspectBtnText(selectedMode);
	}

	private void setAspectBtnText(int selectedMode) {
		setAspectBtnTextColor(tv_aspect_unspecified, false);
		setAspectBtnTextColor(tv_aspect_oneone, false);
		setAspectBtnTextColor(tv_aspect_threetwo, false);
		setAspectBtnTextColor(tv_aspect_fourthree, false);
		setAspectBtnTextColor(tv_aspect_sixteennine, false);
		setAspectBtnTextColor(tv_aspect_custom, false);
		switch (selectedMode) {
		case CropConst.ASPECT_MODE_UNSPECIFIED:
			setAspectBtnTextColor(tv_aspect_unspecified, true);
			fixedAspectRatio = false;
			setAspectAndRefresh(aspectX, aspectY, false);
			break;
		case CropConst.ASPECT_MODE_ONE_ONE:
			setAspectBtnTextColor(tv_aspect_oneone, true);
			fixedAspectRatio = true;
			setAspectAndRefresh(1, 1, false);
			break;
		case CropConst.ASPECT_MODE_THREE_TWO:
			setAspectBtnTextColor(tv_aspect_threetwo, true);
			fixedAspectRatio = true;
			setAspectAndRefresh(3, 2, false);
			break;
		case CropConst.ASPECT_MODE_FOUR_THREE:
			setAspectBtnTextColor(tv_aspect_fourthree, true);
			fixedAspectRatio = true;
			setAspectAndRefresh(4, 3, false);
			break;
		case CropConst.ASPECT_MODE_SIXTEEN_NINE:
			setAspectBtnTextColor(tv_aspect_sixteennine, true);
			fixedAspectRatio = true;
			setAspectAndRefresh(16, 9, false);
			break;
		case CropConst.ASPECT_MODE_CUSTOM:
			setAspectBtnTextColor(tv_aspect_custom, true);
			showCustomDialog();
			break;
		}
	}

	private void setAspectBtnTextColor(TextView tv, boolean isSelected) {
		if (isSelected) {
			tv.setTextColor(Color.parseColor(CropConst.ASPECT_COLOR_SELECTED));
		} else {
			tv.setTextColor(Color.parseColor(CropConst.ASPECT_COLOR_UNSELECTED));
		}
	}

	private void setAspectBtnImage(int mode) {
		// 用于清空选择
		img_aspect_unspecified
				.setImageResource(R.drawable.ic_cut_unspecified_x);
		img_aspect_oneone.setImageResource(R.drawable.ic_cut_oneone_x);
		img_aspect_threetwo.setImageResource(R.drawable.ic_cut_threetwo_x);
		img_aspect_fourthree.setImageResource(R.drawable.ic_cut_fourthree_x);
		img_aspect_sixteennine
				.setImageResource(R.drawable.ic_cut_sixteennine_x);
		img_aspect_custom.setImageResource(R.drawable.ic_cut_custom_x);

		switch (mode) {
		case CropConst.ASPECT_MODE_UNSPECIFIED:
			img_aspect_unspecified
					.setImageResource(R.drawable.ic_cut_unspecified_pressed);
			break;
		case CropConst.ASPECT_MODE_ONE_ONE:
			img_aspect_oneone
					.setImageResource(R.drawable.ic_cut_oneone_pressed);
			break;
		case CropConst.ASPECT_MODE_THREE_TWO:
			img_aspect_threetwo
					.setImageResource(R.drawable.ic_cut_threetwo_pressed);
			break;
		case CropConst.ASPECT_MODE_FOUR_THREE:
			img_aspect_fourthree
					.setImageResource(R.drawable.ic_cut_fourthree_pressed);
			break;
		case CropConst.ASPECT_MODE_SIXTEEN_NINE:
			img_aspect_sixteennine
					.setImageResource(R.drawable.ic_cut_sixteennine_pressed);
			break;
		case CropConst.ASPECT_MODE_CUSTOM:
			img_aspect_custom
					.setImageResource(R.drawable.ic_cut_custom_pressed);
			break;
		}
	}

	private void setAspectAndRefresh(int x, int y, boolean isRotate) {
		aspectX = x;
		aspectY = y;
		cropper.resetAspect(isRotate);
	}

	private void setCustomAspectAndRefresh(int x, int y) {
		aspectX = x;
		aspectY = y;
		cropper.resetCustomAspect();
	}

	private void onRotateClicked() {
		if (selectedAspectMode == CropConst.ASPECT_MODE_CUSTOM) {
			int tempFinalWidth = finalWidth;
			finalWidth = finalHeight;
			finalHeight = tempFinalWidth;
			setCustomAspectAndRefresh(aspectY, aspectX);
		} else {
			setAspectAndRefresh(aspectY, aspectX, true);
		}
	}

	private void showCustomDialog() {
		customDialog = new InputCropAspectDialog(CropImageActivity.this, true,
				finalWidth, finalHeight, aspectX, aspectY);
		customDialog.setCancelListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				customDialog.dismiss();
			}
		});
		customDialog.setConfirmListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// 获取修改后的宽高、宽高比例，并刷新裁剪框
				int width = customDialog.getWidth();
				int height = customDialog.getHeight();
				if (width > 0 && height > 0) {
					if (width > orignalImgWidth) {
						showToast("宽不能大于" + orignalImgWidth);
						return;
					}
					if (height > orignalImgHeight) {
						showToast("高不能大于" + orignalImgHeight);
						return;
					}
					finalWidth = width;
					finalHeight = height;
					int gcd = CropUtil.getGreatestCommonDivisor(width, height);
					aspectX = width / gcd;
					aspectY = height / gcd;
					customDialog.dismiss();
					// 当设置的长宽大于当前缩放后显示原图大小，需要进行缩放操作（考虑自定义的缩放单独用一个方法）
					setCustomAspectAndRefresh(aspectX, aspectY);
				} else {
					showToast("宽和高必须都大于零");
				}
			}
		});
		customDialog.showDialog();
	}

	private void showToast(String msg) {
		Toast.makeText(CropImageActivity.this, msg, Toast.LENGTH_SHORT).show();
	}
}
