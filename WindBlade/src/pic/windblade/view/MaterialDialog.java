package pic.windblade.view;


import pic.windblade.R;
import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class MaterialDialog extends Dialog{
	public View mView;
	private Context context;
//	private int style = 0;
	private Button btn_cancel;
	private Button btn_confirm;
	private TextView tv_title;
	private TextView tv_content;
	private LinearLayout button_layout;
	private RelativeLayout content_Layout;
	public static final int STYLE_OTHER = 0;
//	private int rate = 2/3;   //dialog宽度占屏幕的比例
	/**
	 * @param context
	 */
	public MaterialDialog(Context context,boolean isOutSideCancel) {
		super(context,R.style.Dialog);
		this.context = context;
		initViews(isOutSideCancel);
	}
	/**
	 * 初始化函数
	 * @param context 
	 * @param resource 	R.layout文件
	 * @param rate 		dialog宽度占屏幕的比例(0为默认：占2/3)
	 */
//	public OrangeTitleDialog(Context context,int resource,int style){
//		super(context,R.style.Dialog);
//		this.context = context;
//		LayoutInflater mInflater = LayoutInflater.from(context);
//		mView = mInflater.inflate(resource, null);
//		this.style = style;
//	}
	
	private void initViews(boolean isOutSideCancel) {
		setCanceledOnTouchOutside(isOutSideCancel);//默认为点对话框消失
		LayoutInflater mInflater = LayoutInflater.from(context);
		mView = mInflater.inflate(R.layout.dialog_material, null);
		btn_cancel = (Button)mView.findViewById(R.id.dialog_btncancel);
		btn_confirm = (Button)mView.findViewById(R.id.dialog_btnconfirm);
		tv_title = (TextView)mView.findViewById(R.id.tv_title);
		tv_content = (TextView)mView.findViewById(R.id.tv_content);
		button_layout = (LinearLayout)mView.findViewById(R.id.button_layout);
		content_Layout = (RelativeLayout)mView.findViewById(R.id.rl_bg);
		
	}
	
	public void setTitle(String title){
		tv_title.setText(title);
	}
	public void setMessage(String msg){
		tv_content.setText(msg);
	}
	public void setContentPadding(int left,int top,int right,int bottom){
		tv_content.setPadding(left, top, right, bottom);
	}

	/**
	 * 初始化Dialog大小（宽度为屏幕尺寸2/3）
	 */
	private void initDialogSize() {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		android.view.WindowManager.LayoutParams p = getWindow().getAttributes();
		p.width = metrics.widthPixels * 4/5;
		getWindow().setAttributes(p);
		getWindow().setGravity(Gravity.CENTER);
	}
	
	public void setConfirmListener(android.view.View.OnClickListener clickListener){
		btn_confirm.setOnClickListener(clickListener);
	}
	public void setCancelListener(android.view.View.OnClickListener clickListener){
		btn_cancel.setOnClickListener(clickListener);
	}
	public void setItemClickListener(android.view.View.OnClickListener clickListener){
		content_Layout.setOnClickListener(clickListener);
	}
	public void setConfirmBtnText(String confirm){
		btn_confirm.setText(confirm);
	}
	public void setCancelBtnText(String cancel){
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
}