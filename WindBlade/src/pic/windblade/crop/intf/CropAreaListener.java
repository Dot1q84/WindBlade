package pic.windblade.crop.intf;
/**
 * 裁剪区域监听器接口
 */
public interface CropAreaListener {
	/**裁剪区域初始化时监听*/
	public void areaChangedAfterInit();
	/**裁剪区域点击时（Touch）变化监听*/
	public void areaChangedAfterTouch(); 
}
