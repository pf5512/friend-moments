package com.anda.moments;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;

import com.anda.GlobalConfig;
import com.anda.moments.api.constant.ApiConstants;
import com.anda.moments.commons.Constant;
import com.anda.moments.constant.api.ReqUrls;
import com.anda.moments.entity.Images;
import com.anda.moments.entity.User;
import com.anda.moments.ui.ImagePagerActivity;
import com.anda.moments.ui.my.UserInfoActivity;
import com.anda.moments.utils.JsonUtils;
import com.anda.moments.utils.Log;
import com.anda.moments.utils.SharePreferenceManager;
import com.anda.moments.utils.StringUtils;
import com.anda.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.anda.universalimageloader.cache.memory.MemoryCacheAware;
import com.anda.universalimageloader.cache.memory.impl.LRULimitedMemoryCache;
import com.anda.universalimageloader.core.DisplayImageOptions;
import com.anda.universalimageloader.core.ImageLoader;
import com.anda.universalimageloader.core.ImageLoaderConfiguration;
import com.anda.universalimageloader.core.assist.ImageScaleType;
import com.anda.universalimageloader.core.assist.QueueProcessingType;
import com.squareup.leakcanary.LeakCanary;
import com.xiaomi.ad.AdSdk;
import com.zhy.http.okhttp.OkHttpUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.rong.imkit.RongIM;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.message.ImageMessage;

public class MyApplication extends Application {

	public static String TAG;
	

	private static MyApplication myApplication = null;
	private User user;


	//DIskLruCache中对文件的最大缓存值
//	private int maxSize = 10*1024*1024;//10M
//	private static DiskLruCache mDiskLruCache;

//	public static DiskLruCache getDiskLruCache(){
//		return mDiskLruCache;
//	}
//
//	private void initDiskLruCache(){
//		try {
//			File cacheDir= DiskLruCacheUtils.getDiskLruCacheDir(myApplication, "download");
//			if (!cacheDir.exists()) {
//				cacheDir.mkdirs();
//			}
//			int versionCode=DiskLruCacheUtils.getAppVersionCode(myApplication);
//			mDiskLruCache= DiskLruCache.open(cacheDir, versionCode, 1, maxSize);
//
////			DiskLruCacheUtils.saveDataToDiskLruCache();
//
//		} catch (Exception e) {
//			// TODO: handle exception
//		}
//	}



	public static MyApplication getInstance() {
		return myApplication;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User getCurrentUser() {
		if(user==null){
			String userJson = (String)SharePreferenceManager.getSharePreferenceValue(myApplication,Constant.FILE_NAME,"user","");
			if(!StringUtils.isEmpty(userJson)){
				user = JsonUtils.fromJson(userJson,User.class);
				if(user==null){
					SharePreferenceManager.saveBatchSharedPreference(myApplication,Constant.FILE_NAME,"user","");
//					Intent intent = new Intent(myApplication, LoginActivity.class);
//					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//					myApplication.startActivity(intent);
				}
			}
		}
		return user;
	}
	@Override
	public void onCreate() {
		super.onCreate();
		TAG = this.getClass().getSimpleName();
		// 由于Application类本身已经单例，所以直接按以下处理即可。
		myApplication = this;
//		initImageLoader(this);

		/**
		 * OnCreate 会被多个进程重入，这段保护代码，确保只有您需要使用 RongIM 的进程和 Push 进程执行了 init。
		 * io.rong.push 为融云 push 进程名称，不可修改。
		 */
		if (getApplicationInfo().packageName.equals(getCurProcessName(getApplicationContext())) ||
				"io.rong.push".equals(getCurProcessName(getApplicationContext()))) {
			/**
			 * IMKit SDK调用第一步 初始化
			 */
			RongIM.init(this);

			/**
			 * 融云SDK事件监听处理
			 *
			 * 注册相关代码，只需要在主进程里做。
			 */
			if (getApplicationInfo().packageName.equals(getCurProcessName(getApplicationContext()))) {

				RongCloudEvent.init(this);
			}
		}

		try{

			//获取缓存的登陆对象
			String userJson = (String)SharePreferenceManager.getSharePreferenceValue(myApplication,Constant.FILE_NAME,"user","");
			if(!StringUtils.isEmpty(userJson)){
				user = JsonUtils.fromJson(userJson,User.class);
			}

			//获取缓存的后台jsessionId
			String jsessionId = (String) SharePreferenceManager.getSharePreferenceValue(myApplication, Constant.FILE_NAME, ReqUrls.JSESSION_ID, "");
			if(!StringUtils.isEmpty(jsessionId)){
				String tokens[] = jsessionId.split("_&_");
				long currentTime = System.currentTimeMillis();
				long saveCurrentTime = Long.parseLong(tokens[1]);
//				if(currentTime - saveCurrentTime < 1000*60*60*24*(2-0.2)){//未超过2天
					//判断token的有效期(设置了一个礼拜,这里设置2天)
					GlobalConfig.JSESSION_ID = tokens[0];
//				}

			}
			//获取融云token  token_rong_手机号
			if(user!=null) {
				String rongToken = (String) SharePreferenceManager.getSharePreferenceValue(myApplication, Constant.FILE_NAME, com.anda.moments.constant.api.ReqUrls.TOKEN_RONG + "_" +user.getPhoneNum(),"");
				if(!StringUtils.isEmpty(rongToken)) {
					String rongTokens[] = rongToken.split("_&_");
					long currentTime = System.currentTimeMillis();
					long saveCurrentTime = Long.parseLong(rongTokens[1]);
					if (currentTime - saveCurrentTime < 1000 * 60 * 60 * 24 * 5) {//未超过5天
						//判断token的有效期(设置了一个礼拜,这里设置5天)
						GlobalConfig.TOKEN_RONG = rongTokens[0];
					}
				}
			}

		}catch(Exception e){

		}

		setConversionListener();


		if(ApiConstants.ISDEBUG) {
			OkHttpUtils.getInstance().debug("OkHttpUtils").setConnectTimeout(100000, TimeUnit.MILLISECONDS);
		}else{
			OkHttpUtils.getInstance().setConnectTimeout(100000,TimeUnit.MICROSECONDS);
		}
//		initDiskLruCache();


		initLeakCanary();

		/**
		 * 调用统计SDK
		 *
		 * @param appKey
		 *            Bmob平台的Application ID
		 * @param channel
		 *            当前包所在渠道，可以为空
		 * @return 是否成功，如果失败请看logcat，可能是混淆或so文件未正确配置
		 */

		cn.bmob.statistics.AppStat.i("a127f2b26b0f921fded9d825d338fa8e", "bmob");


		//xiaomi
		AdSdk.setDebugOn(); // 打开调试，输出调试信息
		AdSdk.setMockOn();  // 调试时打开，正式发布时关闭
		AdSdk.initialize(this, "2882303761517481960");

	}

	private void initLeakCanary(){
		if (LeakCanary.isInAnalyzerProcess(this)) {
			// This process is dedicated to LeakCanary for
			// heap analysis.
			// You should not init your app in this process.
			return;
		}
		LeakCanary.install(this);
		Log.d(TAG,"leakCanary  init success....");
	}

	/**
	 * 初始化imageLoader
	 */
	public void initImageLoader(Context context) {
		int memoryCacheSize = (int) (Runtime.getRuntime().maxMemory() / 8);

		MemoryCacheAware<String, Bitmap> memoryCache;

		memoryCache = new LRULimitedMemoryCache(memoryCacheSize);

		// This configuration tuning is custom. You can tune every option, you
		// may tune some of them,
		// or you can create default configuration by
		// ImageLoaderConfiguration.createDefault(this);
		// method.
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				context).threadPriority(Thread.NORM_PRIORITY - 2)
				.memoryCache(memoryCache).denyCacheImageMultipleSizesInMemory()
				.discCacheFileNameGenerator(new Md5FileNameGenerator())
				.tasksProcessingOrder(QueueProcessingType.LIFO).build();
		// Initialize ImageLoader with configuration.
		ImageLoader.getInstance().init(config);
	}

	public DisplayImageOptions getOptions(int drawableId) {
		return new DisplayImageOptions.Builder().showImageOnLoading(drawableId)
				.showImageForEmptyUri(drawableId).showImageOnFail(drawableId)
				.resetViewBeforeLoading(true).cacheInMemory(true)
				.cacheOnDisc(true).imageScaleType(ImageScaleType.EXACTLY)
				.bitmapConfig(Bitmap.Config.RGB_565).build();
	}
	/**
	 * 获取导航页
	 */
	/*public void getBoot(final Handler handler){
		
		ApiHomeUtils.getBoot(myApplication, new RequestCallback() {
			
			@Override
			public void execute(ParseModel parseModel) {
				if (!ApiConstants.RESULT_SUCCESS.equals(parseModel
						.getStatus())) {
					String dataStr = (String)SharePreferenceManager.getSharePreferenceValue(myApplication, Constant.FILE_NAME, "boot", "");
					if(!StringUtils.isEmpty(dataStr)){
						mImageView = JsonUtils.fromJson(dataStr, List.class);
					}
				} else {
					mImageView = JsonUtils.fromJson(parseModel.getData().toString(), List.class);
					SharePreferenceManager.saveBatchSharedPreference(myApplication, Constant.FILE_NAME, "boot", parseModel.getData().toString());
				}
				handler.sendEmptyMessage(1);
			}
		});
		
	}
*/

	/**
	 * 获得当前进程的名字
	 *
	 * @param context
	 * @return 进程号
	 */
	public static String getCurProcessName(Context context) {

		int pid = android.os.Process.myPid();

		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);

		for (ActivityManager.RunningAppProcessInfo appProcess : activityManager
				.getRunningAppProcesses()) {

			if (appProcess.pid == pid) {
				return appProcess.processName;
			}
		}
		return null;
	}

	/**
	 * 会话聊天点击
	 */
	private void setConversionListener(){

		RongIM.setConversationBehaviorListener(new RongIM.ConversationBehaviorListener() {
			//头像点击
			@Override
			public boolean onUserPortraitClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo) {

				Intent intent = new Intent(context, UserInfoActivity.class);
				User user = new User();
				user.setPhoneNum(userInfo.getUserId());
				user.setUserName(userInfo.getName());
				if(userInfo.getPortraitUri()!=null) {
					user.setIcon(userInfo.getPortraitUri().toString());
				}
				user.setFlag(1);
				intent.putExtra("user",user);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				return false;
			}

			@Override
			public boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo) {
				return false;
			}

			@Override
			public boolean onMessageClick(Context context, View view, Message message) {

				if (message.getContent() instanceof ImageMessage) {
					ImageMessage imageMessage = (ImageMessage) message.getContent();
//					Intent intent = new Intent(context, PhotoActivity.class);
//
//					intent.putExtra("photo", imageMessage.getLocalUri() == null ? imageMessage.getRemoteUri() : imageMessage.getLocalUri());
//					if (imageMessage.getThumUri() != null)
//						intent.putExtra("thumbnail", imageMessage.getThumUri());
//
//					context.startActivity(intent);
					Uri uri = imageMessage.getLocalUri() == null ? imageMessage.getRemoteUri() : imageMessage.getLocalUri();
					List<Images> itemList = new ArrayList<Images>();
					Images images = new Images();
					images.setImgPath(uri.toString());
					images.setFileOrder("1");
					itemList.add(images);
					ImagePagerActivity.imageSize = new int[]{view.getMeasuredWidth(), view.getMeasuredHeight()};
					ImagePagerActivity.startImagePagerActivity(context, itemList, 0);
				}

				return false;
			}

			@Override
			public boolean onMessageLinkClick(Context context, String s) {
				return false;
			}

			@Override
			public boolean onMessageLongClick(Context context, View view, Message message) {
				return false;
			}
		});

//		RongIM.setConversationBehaviorListener(new RongIM.ConversationBehaviorListener() {
//
//			@Override
//			public boolean onClickUserPortrait(Context context, RongIMClient.ConversationType conversationType, RongIMClient.UserInfo user) {
//
//				//在这里处理你想要跳转的activity，示例代码为YourAcitivy
//
////				Intent in = new Intent(context, YourAcitivy.class);
////				context.startActivity(in);
//				return false;
//			}
//
//			@Override
//			public boolean onClickMessage(Context context, RongIMClient.Message message) {
//
//				//点击消息处理事件，示例代码展示了如何获得消息内容
//				if (message.getContent() instanceof LocationMessage) {
//					Intent intent = new Intent(context, LocationActivity.class);
//					intent.putExtra("location", message.getContent());
//					context.startActivity(intent);
//
//				}else  if(message.getContent() instanceof RichContentMessage){
//					RichContentMessage mRichContentMessage = (RichContentMessage) message.getContent();
//					Log.d("Begavior",  "extra:"+mRichContentMessage.getExtra());
//
//				}
//
//				Log.d("Begavior", message.getObjectName() + ":" + message.getMessageId());
//
//				return false;
//			}
//		});
	}





}
