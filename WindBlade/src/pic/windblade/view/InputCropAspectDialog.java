package pic.windblade.view;

import pic.windblade.R;
import pic.windblade.crop.CropUtil;
import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class InputCropAspectDialog extends Dialog {
	public View mView;
	private Context context;
	private Button btn_cancel;
	private Button btn_confirm;
	private TextView tv_title;
	private LinearLayout button_layout;
	private RelativeLayout content_Layout;
	private EditText edt_width;
	private EditText edt_height;
	private ImageView img_lock;
	public static final int STYLE_OTHER = 0;
	/** 是否锁定长宽比 */
	private boolean isLocked = false;
	private int width;
	private int height;
	private int aspectX;
	private int aspectY;

	private boolean changedByHeight = false;
	private boolean changedByWidth = false;

	/**
	 * @param context
	 */
	public InputCropAspectDialog(Context context, boolean isOutSideCancel,
			int width, int height, int aspectX, int aspectY) {
		super(context, R.style.Dialog);
		this.context = context;
		this.width = width;
		this.height = height;
		this.aspectX = aspectX;
		this.aspectY = aspectY;

		initViews(isOutSideCancel);
		initData();

	}

	private void initViews(boolean isOutSideCancel) {
		setCanceledOnTouchOutside(isOutSideCancel);// 默认为点对话框消失
		LayoutInflater mInflater = LayoutInflater.from(context);
		mView = mInflater.inflate(R.layout.dialog_input_cropaspect, null);
		btn_cancel = (Button) mView.findViewById(R.id.dialog_btncancel);
		btn_confirm = (Button) mView.findViewById(R.id.dialog_btnconfirm);
		tv_title = (TextView) mView.findViewById(R.id.tv_title);
		button_layout = (LinearLayout) mView.findViewById(R.id.button_layout);
		content_Layout = (RelativeLayout) mView.findViewById(R.id.rl_bg);
		edt_width = (EditText) mView.findViewById(R.id.edt_width);
		edt_height = (EditText) mView.findViewById(R.id.edt_height);
		img_lock = (ImageView) mView.findViewById(R.id.img_lock);
		img_lock.setOnClickListener(new LockOnClick());
		edt_width.addTextChangedListener(new EdtWidthWatcher());
		edt_height.addTextChangedListener(new EdtHeightWatcher());
	}

	private void initData() {
		edt_width.setText(Integer.toString(width));
		edt_height.setText(Integer.toString(height));
	}

	public void setTitle(String title) {
		tv_title.setText(title);
	}

	/**
	 * 初始化Dialog大小（宽度为屏幕尺寸2/3）
	 */
	private void initDialogSize() {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		android.view.WindowManager.LayoutParams p = getWindow().getAttributes();
		p.width = metrics.widthPixels * 4 / 5;
		getWindow().setAttributes(p);
		getWindow().setGravity(Gravity.CENTER);
	}

	public void setConfirmListener(
			android.view.View.OnClickListener clickListener) {
		btn_confirm.setOnClickListener(clickListener);
	}

	public void setCancelListener(
			android.view.View.OnClickListener clickListener) {
		btn_cancel.setOnClickListener(clickListener);
	}

	public void setItemClickListener(
			android.view.View.OnClickListener clickListener) {
		content_Layout.setOnClickListener(clickListener);
	}

	public void setConfirmBtnText(String confirm) {
		btn_confirm.setText(confirm);
	}

	public void setCancelBtnText(String cancel) {
		btn_cancel.setText(cancel);
	}

	public void showDialog() {
		setContentView(mView);
		initDialogSize();
		try {
			this.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setConfirmBtnVisible(boolean isVisible) {
		if (isVisible) {
			btn_confirm.setVisibility(View.VISIBLE);
		} else {
			btn_confirm.setVisibility(View.GONE);
		}
	}

	public void setCancelBtnVisible(boolean isVisible) {
		if (isVisible) {
			btn_cancel.setVisibility(View.VISIBLE);
		} else {
			btn_cancel.setVisibility(View.GONE);
		}
	}

	/**
	 * 长宽比是否锁定
	 * 
	 * @return
	 */
	public boolean isLockedAspect() {
		return isLocked;
	}

	private class LockOnClick implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			isLocked = !isLocked;
			if (isLocked) {
				img_lock.setImageResource(R.drawable.btn_lock_on);
				updateAspect();
			} else {
				img_lock.setImageResource(R.drawable.btn_lock_off);
			}
		}
	}

	private class EdtWidthWatcher implements TextWatcher {

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			if (!changedByHeight) {
				try {
					width = Integer.parseInt(s.toString().trim());
					if (isLocked) {
						if (aspectX != 0 && aspectY != 0) {
							int tempHeight = width * aspectY / aspectX;
							if (height != tempHeight) {
								height = tempHeight;
								changedByWidth = true;
								edt_height.setText(Integer.toString(height));
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					width = 0;
					if (isLocked) {
						int tempHeight = 0;
						if (height != tempHeight) {
							height = tempHeight;
							changedByWidth = true;
							edt_height.setText(Integer.toString(height));
						}
					}
				}
			} else {
				changedByHeight = false;
			}
		}

	}

	private class EdtHeightWatcher implements TextWatcher {

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
		}

		@Override
		public void afterTextChanged(Editable s) {
			if (!changedByWidth) {
				try {
					height = Integer.parseInt(s.toString().trim());
					if (isLocked) {
						if (aspectX != 0 && aspectY != 0) {
							int tempWidth = height * aspectX / aspectY;
							if (width != tempWidth) {
								width = tempWidth;
								changedByHeight = true;
								edt_width.setText(Integer.toString(width));
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					height = 0;
					if (isLocked) {
						int tempWidth = 0;
						if (width != tempWidth) {
							width = tempWidth;
							changedByHeight = true;
							edt_width.setText(Integer.toString(width));
						}
					}
				}
			} else {
				changedByWidth = false;
			}
		}

	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	/**
	 * 更新长宽比值
	 */
	private void updateAspect() {
		if (width > 0 && height > 0) {
			int gcd = CropUtil.getGreatestCommonDivisor(width, height);
			aspectX = width / gcd;
			aspectY = height / gcd;
		}
	}

}