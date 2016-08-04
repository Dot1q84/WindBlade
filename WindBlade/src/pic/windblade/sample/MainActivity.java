package pic.windblade.sample;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import pic.windblade.R;
import pic.windblade.crop.Crop;
import pic.windblade.view.MaterialDialog;

public class MainActivity extends Activity {

	private ImageView resultView;
	private TextView action_pick;
	private TextView action_camera;
	public MaterialDialog materialDlg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		resultView = (ImageView) findViewById(R.id.result_image);
		action_pick = (TextView) findViewById(R.id.action_pick);
		action_pick.setOnClickListener(new PickOnClick());
		action_camera = (TextView) findViewById(R.id.action_camera);
		action_camera.setOnClickListener(new CameraOnClick());
	}

	private class PickOnClick implements OnClickListener {

		@Override
		public void onClick(View v) {
//			resultView.setImageDrawable(null);
			Crop.pickImage(MainActivity.this);
		}
	}
	
	private class CameraOnClick implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO 拍照待实现
//			Toast.makeText(MainActivity.this, "拍照待实现", Toast.LENGTH_SHORT).show();
//			resultView.setImageDrawable(null);
			materialDlg = new MaterialDialog(MainActivity.this, true);
			materialDlg.setTitle("提示信息");
			materialDlg.setMessage("拍照待实现");
			materialDlg.setConfirmListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					materialDlg.dismiss();
				}
			});
			materialDlg.setCancelListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					materialDlg.dismiss();
				}
			});
			materialDlg.showDialog();
		}
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent result) {
		if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
			beginCrop(result.getData());
		} else if (requestCode == Crop.REQUEST_CROP) {
			handleCrop(resultCode, result);
		}
	}

	private void beginCrop(Uri source) {
		/**destUri不可重复，否则会造成下次切图无法正常返回，最好是带有时间戳*/
		Uri destination = Uri.fromFile(new File(getCacheDir(), System.currentTimeMillis()+"_cropped"));
		Crop.of(source, destination).asSquare().start(this);
//		Crop.of(source, destination).withAspect(16, 9).start(this);
	}

	private void handleCrop(int resultCode, Intent result) {
		if (resultCode == RESULT_OK) {
			resultView.setImageURI(Crop.getOutput(result));
		} else if (resultCode == Crop.RESULT_ERROR) {
			Toast.makeText(this, Crop.getError(result).getMessage(),
					Toast.LENGTH_SHORT).show();
		}
	}

}
