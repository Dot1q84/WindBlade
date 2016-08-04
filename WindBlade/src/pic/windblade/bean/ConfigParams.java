package pic.windblade.bean;

public class ConfigParams {
	/**是否显示比例值（调试用）*/
	private boolean isShowProportionValue;
	/**是否锁定长宽比*/
	private boolean fixedAspectRatio;
	/**比例是否可选*/
	private boolean isShowProportionOption;
	/**是否允许比例自定义*/
	private boolean isProportionCustom;
	/**是否可旋转*/
	private boolean canRotate;
	/**选择区域是否显示网格线*/
	private int showGuidelines;
	/**长宽比·x轴*/
	private int aspectRatioX;
	/**长宽比·y轴*/
	private int aspectRatioY;
	
	/**网格线·什么时候都显示*/
	public static final int GUIDELINE_ON = 1;
	/**网格线·touch的时候显示*/
	public static final int GUIDELINE_ON_TOUCH = 2;
	/**网格线·不显示*/
	public static final int GUIDELINE_OFF = 3;
	

}
