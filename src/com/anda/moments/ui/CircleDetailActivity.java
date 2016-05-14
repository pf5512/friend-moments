package com.anda.moments.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.anda.GlobalConfig;
import com.anda.moments.MyApplication;
import com.anda.moments.R;
import com.anda.moments.apdater.CommentRecyclerViewAdapter;
import com.anda.moments.apdater.HomeAdapter;
import com.anda.moments.apdater.PraiseRecyclerViewAdapter;
import com.anda.moments.api.ApiMomentsUtils;
import com.anda.moments.api.constant.ApiConstants;
import com.anda.moments.api.constant.ReqUrls;
import com.anda.moments.commons.AppManager;
import com.anda.moments.entity.CircleMessage;
import com.anda.moments.entity.CommentConfig;
import com.anda.moments.entity.CommentInfo;
import com.anda.moments.entity.CommentUser;
import com.anda.moments.entity.Images;
import com.anda.moments.entity.Media;
import com.anda.moments.entity.ParseModel;
import com.anda.moments.entity.PraiseUser;
import com.anda.moments.entity.PraisedInfo;
import com.anda.moments.entity.User;
import com.anda.moments.ui.base.BaseActivity;
import com.anda.moments.utils.CommonHelper;
import com.anda.moments.utils.DateUtils;
import com.anda.moments.utils.DeviceInfo;
import com.anda.moments.utils.HttpConnectionUtil;
import com.anda.moments.utils.Log;
import com.anda.moments.utils.StringUtils;
import com.anda.moments.utils.ToastUtils;
import com.anda.moments.views.ActionBar;
import com.anda.moments.views.CustomSingleImageView;
import com.anda.moments.views.NineGridlayout;
import com.anda.moments.views.audio.MediaManager;
import com.anda.moments.views.popup.ActionItem;
import com.anda.moments.views.popup.TitlePopup;
import com.squareup.picasso.Picasso;
import com.yqritc.scalablevideoview.ScalableVideoView;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.FileCallBack;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import sz.itguy.utils.FileUtil;

/**
 * 动态明细
 * @author pengweiqiang
 *
 */
public class CircleDetailActivity extends BaseActivity implements CommentRecyclerViewAdapter.OnItemClickListener,PraiseRecyclerViewAdapter.OnItemClickListener{

	ActionBar mActionbar;
	private static final int ITEM_VIEW_TYPE_IMAGES = 0;
	private static final int ITEM_VIEW_TYPE_AUDIO = 1;
	private static final int ITEM_VIEW_TYPE_VIDEO = 2;
	private static final int ITEM_VIEW_TYPE_TEXT = 3;

	int headWidth =70;
	int itemViewType;//显示类型  文字、图片、音频、视频
	private CircleMessage circleMessage;


	public View mViewComment;//萌化了
	//图片类型 start
	public NineGridlayout ivMore;//图片九宫格
	public CustomSingleImageView ivOne;//单张图片
	//图片类型 end

	//视频类型 start
	public ScalableVideoView mScalableVideoView;
	public ImageView mPlayImageView;//
	public ImageView mThumbnailImageView;//缩略图
	public ProgressBar mProgressBar;
	//视频类型 end

	//音频类型 start
	public View mViewAudio;//语音背景
	public View mViewAnim;//语音动画
	public TextView mTvAudioSecond;//时长

	//音频类型 end

	public ImageView mIvUserHead;//头像
	public TextView mTvUserName;//昵称
	public TextView mTvContent;//评论内容
	public TextView mTvPublishTime;//发表时间

	public View digCommentBody;
	//评论列表控件
//        public CommentListView commentListView;
	public RecyclerView commentListView;
	public TextView mTvCommentCount;//评论总数
	//评论列表适配器
//        public CommentAdapter commentAdapter;

	public CommentRecyclerViewAdapter commentAdapter;
	public View mViewPraiseCommentLine;

	//点赞列表
	public RecyclerView praiseListView;
	public TextView mTvPraiseCount;
	public PraiseRecyclerViewAdapter praiseRecyclerViewAdapter;




	@Override
	@SuppressLint("InlinedApi")
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_circle_detail);
		super.onCreate(savedInstanceState);
		headWidth = DeviceInfo.dp2px(mContext,70);
		circleMessage = (CircleMessage)this.getIntent().getSerializableExtra("circleMessage");

		showData();
	}

	@Override
	public void initView() {
		mActionbar = (ActionBar)findViewById(R.id.actionBar);
		mActionbar.setTitle("动态详情");
		mActionbar.setLeftActionButtonListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AppManager.getAppManager().finishActivity();
			}
		});

		long startTime = System.currentTimeMillis();

		mIvUserHead = (ImageView)findViewById(R.id.iv_user_head);
		mTvUserName = (TextView)findViewById(R.id.tv_user_name);
		mTvContent = (TextView)findViewById(R.id.tv_content);
		mTvPublishTime = (TextView)findViewById(R.id.tv_create_time);

		mViewComment = findViewById(R.id.iv_comment);//评论弹出框

		digCommentBody = findViewById(R.id.digCommentBody);

		commentListView = (RecyclerView) findViewById(R.id.commentList);
		mTvCommentCount = (TextView)findViewById(R.id.tv_comment_count);


		mViewPraiseCommentLine  = findViewById(R.id.line_praise_comment);

		//点赞列表
		praiseListView = (RecyclerView) findViewById(R.id.praiseList);
		mTvPraiseCount = (TextView)findViewById(R.id.tv_praise_count);





		mViewComment.setOnClickListener(onClickListener);//评论
		mIvUserHead.setOnClickListener(onClickListener);
		mTvUserName.setOnClickListener(onClickListener);


		// 停止计时
		long endTime = System.nanoTime();
		// 计算耗时
		long val = (endTime - startTime) / 1000L;
		Log.e(TAG," cirlceDetail:"+val);

	}

	private void showData(){

		commentAdapter = new CommentRecyclerViewAdapter(mContext,null);
		commentAdapter.setOnItemClickListener(this);

		praiseRecyclerViewAdapter = new PraiseRecyclerViewAdapter(mContext,null);
		praiseRecyclerViewAdapter.setOnItemClickListener(this);

		//设置固定大小
		praiseListView.setHasFixedSize(true);
		//创建线性布局
		LinearLayoutManager mLayoutManagerPraise = new LinearLayoutManager(mContext);
		//垂直方向
		mLayoutManagerPraise.setOrientation(OrientationHelper.HORIZONTAL);
		//给RecyclerView设置布局管理器
		praiseListView.setLayoutManager(mLayoutManagerPraise);



		//设置固定大小
		commentListView.setHasFixedSize(true);
		//创建线性布局
		LinearLayoutManager mLayoutManagerComment = new LinearLayoutManager(mContext);
		//垂直方向
		mLayoutManagerComment.setOrientation(OrientationHelper.VERTICAL);
		//给RecyclerView设置布局管理器
		commentListView.setLayoutManager(mLayoutManagerComment);


		praiseListView.setAdapter(praiseRecyclerViewAdapter);
		commentListView.setAdapter(commentAdapter);



		itemViewType = getItemViewType();

		ViewStub mediaViewStub = (ViewStub) findViewById(R.id.media_view_stub);
		switch (itemViewType){
			case ITEM_VIEW_TYPE_TEXT:
//                    mediaViewStub.setLayoutResource(R.layout.);
				break;
			case ITEM_VIEW_TYPE_IMAGES://图片
				mediaViewStub.setLayoutResource(R.layout.home_view_stub_images);
				mediaViewStub.inflate();

				ivMore = (NineGridlayout) findViewById(R.id.iv_ngrid_layout);
				ivOne = (CustomSingleImageView) findViewById(R.id.iv_oneimage);
				break;
			case ITEM_VIEW_TYPE_AUDIO://音频
				mediaViewStub.setLayoutResource(R.layout.home_view_stub_audios);
				mediaViewStub.inflate();

				mViewAudio = findViewById(R.id.view_record);
				mViewAnim = findViewById(R.id.voice_anim);
				mTvAudioSecond = (TextView)findViewById(R.id.tv_audio_second);


				break;
			case ITEM_VIEW_TYPE_VIDEO://视频
				mediaViewStub.setLayoutResource(R.layout.home_view_stub_videos);
				mediaViewStub.inflate();

				mScalableVideoView = (ScalableVideoView)findViewById(R.id.video_view);
				mPlayImageView = (ImageView)findViewById(R.id.playImageView);
				mThumbnailImageView = (ImageView)findViewById(R.id.thumbnailImageView);
				mProgressBar = (ProgressBar)findViewById(R.id.progressBar);

				try {
					mScalableVideoView.setDataSource("");
				} catch (IOException e) {
					// e.printStackTrace();
				}

				mScalableVideoView.setOnClickListener(onClickListener);

				break;
		}

		//展示不同的类型
		showItemTypeData(itemViewType);


		//发表内容
		mTvContent.setText(StringUtils.ToDBC(circleMessage.getInfoText()));
		User publishUser = circleMessage.getPublishUser();
		if(publishUser!=null) {
			mTvUserName.setText(publishUser.getUserName());
			String headUrl = publishUser.getIcon();
			Picasso.with(mContext).load(headUrl).resize(headWidth,headWidth).centerCrop().placeholder(R.drawable.default_useravatar).into(mIvUserHead);
//            Picasso.with(context).load(headUrl).placeholder(R.drawable.default_useravatar).into(mIvUserHead);
		}
		//发表时间
		mTvPublishTime.setText(DateUtils.getTimestampString(circleMessage.getCreateTime()));


		//点赞列表
		PraisedInfo praisedInfo = circleMessage.getPraisedInfo();
		int praseInfoCount = praisedInfo.getPraiseNum();

		//回复列表
		CommentInfo commentInfo = circleMessage.getCommentInfo();
		int commentNum = commentInfo.getCommentNum();
		if(commentNum>0 || praseInfoCount > 0) {
			digCommentBody.setVisibility(View.VISIBLE);
			//评论列表不为空
			if(commentNum>0) {
				mTvCommentCount.setText(String.valueOf(commentNum));
				final List<CommentUser> commentUsers = commentInfo.getCommentUsers();
				commentAdapter.setDatas(commentUsers);
//            commentAdapter.notifyDataSetChanged();
				calRecycleViewHeight(commentListView, commentNum);
				mTvCommentCount.setVisibility(View.VISIBLE);
				commentListView.setVisibility(View.VISIBLE);
				digCommentBody.setVisibility(View.VISIBLE);
				mViewPraiseCommentLine.setVisibility(View.VISIBLE);
			}else{
				mTvCommentCount.setVisibility(View.GONE);
				commentListView.setVisibility(View.GONE);
				mViewPraiseCommentLine.setVisibility(View.GONE);
			}
			//点赞列表不为空
			if(praseInfoCount>0){
				mTvPraiseCount.setText(String.valueOf(praseInfoCount));
				final List<PraiseUser> praiseUsers = praisedInfo.getPraiseUsers();
				praiseRecyclerViewAdapter.setDatas(praiseUsers);
//            praiseRecyclerViewAdapter.notifyDataSetChanged();
				praiseListView.setVisibility(View.VISIBLE);
				mTvPraiseCount.setVisibility(View.VISIBLE);
			}else{
				praiseListView.setVisibility(View.GONE);
				mTvPraiseCount.setVisibility(View.GONE);
				mViewPraiseCommentLine.setVisibility(View.GONE);
			}



//            commentListView.setOnItemClick(new CommentListView.OnItemClickListener() {
//                @Override
//                public void onItemClick(int commentPosition) {
//                    //当前的评论
//                    CommentUser commentUser = commentUsers.get(commentPosition);
//
//                    if(commentUser.getUserId().equals(circleMessage.getPublishUser().getUserId())){//自己评论自己的
//
//                    }else{
//                        CommentConfig commentConfig = new CommentConfig();
//                        commentConfig.circlePosition = position;
//                        commentConfig.commentPosition = commentPosition;
//                        commentConfig.commentType = CommentConfig.Type.REPLY;
//                        commentConfig.replyUser = commentUser;
//
//                        homeFragment.updateEditTextBodyVisible(View.VISIBLE,commentConfig);
//                    }
//
//                }
//            });
		}else{
			digCommentBody.setVisibility(View.GONE);
			commentListView.setVisibility(View.GONE);
		}


	}

	@Override
	public void initListener() {

	}

	OnClickListener onClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if(CommonHelper.isFastClick()){
				return;
			}
			switch (v.getId()){
				case R.id.iv_comment://评论
					popup(v);
					break;
				case R.id.iv_user_head://头像
					startUserInfoActivity(circleMessage.getPublishUser());
					break;
				case R.id.tv_user_name://昵称
					startUserInfoActivity(circleMessage.getPublishUser());
					break;
				case R.id.video_view://点击进入视频详情
					Intent intent = new Intent(mContext, VideoDetailActivity.class);
					intent.putExtra(VideoDetailActivity.KEY_FILE_PATH,circleMessage.getVideos().get(0).getPath());
					intent.putExtra("firstPicture","");
					startActivity(intent);
					break;
			}
		}
	};


	private void calRecycleViewHeight(RecyclerView recyclerView,int size){
		ViewGroup.LayoutParams mParams = recyclerView.getLayoutParams();
		mParams.height = DeviceInfo.dp2px(mContext,42) * size+DeviceInfo.dp2px(mContext,3)+1;
		mParams.width = DeviceInfo.getScreenWidth(mContext);
		recyclerView.setLayoutParams(mParams);
	}


	private void showItemTypeData(int itemViewType){
		switch (itemViewType){
			case ITEM_VIEW_TYPE_TEXT:

				break;
			case ITEM_VIEW_TYPE_IMAGES:
				//图片展示  start
				final List<Images> imagesList = circleMessage.getImages();
				if (imagesList==null || imagesList.isEmpty()) {
					ivMore.setVisibility(View.GONE);
					ivOne.setVisibility(View.GONE);
				} else if (imagesList.size() == 1) {
					ivMore.setVisibility(View.GONE);
					ivOne.setVisibility(View.VISIBLE);

					handlerOneImage(imagesList.get(0));
				} else {
					ivMore.setVisibility(View.VISIBLE);
					ivOne.setVisibility(View.GONE);

					ivMore.setImagesData(imagesList);
					ivMore.setOnItemClickListener(new NineGridlayout.OnItemClickListener() {
						@Override
						public void onItemClick(View view, int position) {
							ImagePagerActivity.imageSize = new int[]{view.getMeasuredWidth(), view.getMeasuredHeight()};
							ImagePagerActivity.startImagePagerActivity(mContext, imagesList, position);
						}
					});
//            ivMore.setOnClickListener();
				}
				//图片展示  end
				break;
			case ITEM_VIEW_TYPE_AUDIO://语音

				mViewAudio.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if(circleMessage.getAudios()!=null && !circleMessage.getAudios().isEmpty()) {
							downloadMedia(circleMessage.getAudios().get(0).getPath(), ReqUrls.MEDIA_TYPE_AUDIO);
						}
					}
				});

				break;
			case ITEM_VIEW_TYPE_VIDEO://视频
//                mIvPlay.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        if(circleMessage.getVideos()!=null && !circleMessage.getVideos().isEmpty()) {
//                            downloadMedia(circleMessage.getVideos().get(0).getPath());
//                        }
//                    }
//                });
				mPlayImageView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if(circleMessage.getVideos()!=null && !circleMessage.getVideos().isEmpty()) {
//                            playerVideo(circleMessage.getVideos().get(0).getPath(),viewHolder);
							downloadMedia(circleMessage.getVideos().get(0).getPath(), ReqUrls.MEDIA_TYPE_VIDEO);
						}
					}
				});

				String url = circleMessage.getVideos().get(0).getPath();
				String downLoadPath =  FileUtil.createFile(FileUtil.DOWNLOAD_MEDIA_FILE_DIR);
				String fileName = url.substring(url.lastIndexOf("/")+1);
//                mThumbnailImageView.setImageBitmap(getVideoThumbnail(downLoadPath+"/"+fileName));


				break;
		}
	}

	private void handlerOneImage(final Images image) {
		int totalWidth;
		int imageWidth;
		int imageHeight;
//        ScreenTools screentools = ScreenTools.instance(context);
//        totalWidth = screentools.getScreenWidth() - screentools.dip2px(100);
//        imageWidth = screentools.dip2px(image.getWidth());
//        imageWidth = totalWidth;
//        imageHeight = screentools.dip2px(image.getHeight());
//        if (image.getWidth() <= image.getHeight()) {
//            if (imageHeight > totalWidth) {
//                imageHeight = totalWidth;
//                imageWidth = (imageHeight * image.getWidth()) / image.getHeight();
//            }
//        } else {
//            if (imageWidth > totalWidth) {
//                imageWidth = totalWidth;
//                imageHeight = (imageWidth * image.getHeight()) / image.getWidth();
//            }
//        }
//        ViewGroup.LayoutParams layoutparams = ivOne.getLayoutParams();
//        layoutparams.height = imageHeight;
//        layoutparams.width = imageWidth;
//        ivOne.setLayoutParams(layoutparams);
		ivOne.setClickable(true);
//        ivOne.setScaleType(ImageView.ScaleType.CENTER_CROP);
		ivOne.setImageUrl(image.getImgPath());
		ivOne.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 因为单张图片时，图片实际大小是自适应的，imageLoader缓存时是按测量尺寸缓存的
				ImagePagerActivity.imageSize = new int[]{v.getMeasuredWidth(), v.getMeasuredHeight()};
				List<Images> urls = new ArrayList<Images>();
				urls.add(image);
				ImagePagerActivity.startImagePagerActivity(mContext, urls, 0);
			}
		});


	}


	/**
	 * 萌化啦
	 */
	private void addLoveSth(){
		User user = MyApplication.getInstance().getCurrentUser();
		if(user==null){
			ToastUtils.showToast(mContext,"请先登录");
			return;
		}
		String infoId = String.valueOf(circleMessage.getInfoId());
		ApiMomentsUtils.addLoveSth(mContext,infoId,"1",user.getPhoneNum(),new HttpConnectionUtil.RequestCallback(){

			@Override
			public void execute(ParseModel parseModel) {
				if(ApiConstants.RESULT_SUCCESS.equals(parseModel.getRetFlag())){
					ToastUtils.showToast(mContext,parseModel.getInfo());
				}else{
					ToastUtils.showToast(mContext,parseModel.getInfo());
				}
			}
		});
	}

	private String TAG = "HomeAdapter";
	public void downloadMedia(String url,final int type){
		if(CommonHelper.isFastClick()){
			return;
		}
		String downLoadPath =  FileUtil.createFile(FileUtil.DOWNLOAD_MEDIA_FILE_DIR);
		String fileName = url.substring(url.lastIndexOf("/")+1);

		if(new File(downLoadPath,fileName).exists()){//是否下载过
			String filePath = downLoadPath+"/"+fileName;
			switch (type){
				case  ReqUrls.MEDIA_TYPE_AUDIO://音频
					playAudioRecord(filePath);
					break;
				case  ReqUrls.MEDIA_TYPE_VIDEO://视频
					playerVideo(filePath);
					break;
			}
			return;
		}

		OkHttpUtils//
				.get()//
//                .tag(this)
				.url(url)//
				.build()//
				.execute(new FileCallBack(downLoadPath+"/", fileName) {
					@Override
					public void inProgress(float progress, long total) {
						if(type == ReqUrls.MEDIA_TYPE_VIDEO) {
							int progressInt = (int) (100 * progress);
							mProgressBar.setProgress(progressInt);
							Log.e(TAG,progressInt+"  total "+total);
						}
					}

					@Override
					public void onError(Call call, Exception e) {
					}

					@Override
					public void onResponse(File file) {
						if(file.exists()){
							switch (type){
								case  ReqUrls.MEDIA_TYPE_AUDIO://音频
									playAudioRecord(file.getPath());
									break;
								case  ReqUrls.MEDIA_TYPE_VIDEO://视频
									playerVideo(file.getPath());
									break;
							}
						}
						Log.e(TAG, "onResponse :" + file.getAbsolutePath());
					}
				});

	}


	/**
	 * 播放录音
	 */
	AnimationDrawable animationDrawable;
	private void playAudioRecord(String filePath){
		mViewAnim.setBackgroundResource(R.drawable.anim_play_audio);
		if(animationDrawable!=null && animationDrawable.isRunning()){
			animationDrawable.selectDrawable(2);
			animationDrawable.stop();
		}
//		if(animation==null) {
		animationDrawable = (AnimationDrawable) mViewAnim.getBackground();
//		}

		animationDrawable.start();
		MediaManager.playSound(filePath,
				new MediaPlayer.OnCompletionListener() {
					@Override
					public void onCompletion(MediaPlayer mp) {
						animationDrawable.stop();
						mViewAnim
								.setBackgroundResource(R.drawable.icon_voice_anim_3);
					}
				});
	}

	private void playerVideo(String filePath){

		try {
			mScalableVideoView.setDataSource(filePath);
			mScalableVideoView.setVolume(0,0);
			mScalableVideoView.setLooping(true);
			mScalableVideoView.prepare(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mp) {
					circleMessage.setPlay(true);
					mPlayImageView.setVisibility(View.GONE);
					mThumbnailImageView.setVisibility(View.GONE);
					mScalableVideoView.start();
				}
			});


		} catch (IOException e) {
			android.util.Log.e(TAG, e.getLocalizedMessage());
			ToastUtils.showToast(mContext, "播放视频异常");
		}


	}

	public Bitmap getVideoThumbnail(String filePath) {
		if(!new File(filePath).exists()){
			return null;
		}
		Bitmap bitmap = null;
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		try {
			retriever.setDataSource(filePath);
			bitmap = retriever.getFrameAtTime(TimeUnit.SECONDS.toMicros(1));
		}
		catch(IllegalArgumentException e) {
			e.printStackTrace();
		}
		finally {
			try {
				retriever.release();
			}
			catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
		return bitmap;
	}

	public int getItemViewType() {
		int itemType = ITEM_VIEW_TYPE_TEXT;

		List<Media> videos = circleMessage.getVideos();
		List<Media> audios = circleMessage.getAudios();
		List<Images> images = circleMessage.getImages();

		if (images!=null && !images.isEmpty()) {//图片
			itemType = ITEM_VIEW_TYPE_IMAGES;
		} else if (audios!=null && !audios.isEmpty()) {//音频
			itemType = ITEM_VIEW_TYPE_AUDIO;
		} else if(videos!=null && !videos.isEmpty()){//视频
			itemType = ITEM_VIEW_TYPE_VIDEO;
		}else{
			itemType = ITEM_VIEW_TYPE_TEXT;
		}
		return itemType;
	}

	@Override
	public void onItemClick(View view, int commentPosition) {
		//当前的评论
		CommentUser commentUser = circleMessage.getCommentInfo().getCommentUsers().get(commentPosition);

		if(commentUser.getUserId().equals(circleMessage.getPublishUser().getUserId())){//自己评论自己的

		}else{
			CommentConfig commentConfig = new CommentConfig();
//			commentConfig.circlePosition = position;
			commentConfig.commentPosition = commentPosition;
			commentConfig.commentType = CommentConfig.Type.REPLY;
			commentConfig.replyUser = commentUser;
		}
	}

	@Override
	public void onItemLongClick(View view, int commentPosition) {
		//当前的评论
		CommentUser commentUser = circleMessage.getCommentInfo().getCommentUsers().get(commentPosition);
		User user = MyApplication.getInstance().getCurrentUser();
		if(commentUser.getUserId().equals(user.getUserId())) {//自己评论自己的
			showDeleteWindow(commentPosition);
		}

	}

	@Override
	public void onItemPraiseClick(View view, int praisePosition) {
		PraiseUser praiseUser = circleMessage.getPraisedInfo().getPraiseUsers().get(praisePosition);
		User user = new User();
		user.setIcon(praiseUser.getIcon());
		user.setUserId(praiseUser.getUserId());
		user.setUserName(praiseUser.getUserName());
		user.setPhoneNum(praiseUser.getPhoneNum());
		startUserInfoActivity(user);
	}

	@Override
	public void onItemPraiseLongClick(View view, int position) {

	}

	//进入个人主页
	private void startUserInfoActivity(User user){
		Intent intent = new Intent(mContext,UserHomeActivity.class);
//        User user = datalist.get(position).getPublishUser();
		user.setFlag(1);//已接受好友
		intent.putExtra("user",user);
		startActivity(intent);
	}


	private TitlePopup titlePopup;
	/**
	 * 弹出评论,赞菜单加载
	 */
	private void popup(View view) {


		titlePopup = new TitlePopup(mContext, DeviceInfo.dp2px(mContext,265),DeviceInfo.dp2px(mContext,36));
		PraisedInfo praisedInfo = circleMessage.getPraisedInfo();
//        boolean isPraise = false;//自己是否赞过
		String praiseText = "赞";
		int praiseIndex = 0;//当前点赞位置
		if(praisedInfo.getPraiseNum()>0 ){
			List<PraiseUser> praiseUsers = praisedInfo.getPraiseUsers();
			User user = MyApplication.getInstance().getCurrentUser();
			for (int i = 0;i<praiseUsers.size();i++) {
				if(praiseUsers.get(i).getPhoneNum().equals(user.getPhoneNum())){
//                    isPraise = true;
					praiseIndex = i;
					praiseText = "取消";
					break;
				}
			}
		}
		titlePopup.addAction(new ActionItem(mContext, praiseText, R.drawable.circle_praise,praiseIndex));
		titlePopup.addAction(new ActionItem(mContext, "萌化啦~",R.drawable.btn_comment_meng,praiseIndex));
		titlePopup.addAction(new ActionItem(mContext, "评论",R.drawable.circle_comment,praiseIndex));

		titlePopup.setItemOnClickListener(onItemOnClickListener);


		titlePopup.setAnimationStyle(R.style.social_pop_anim);
		titlePopup.show(view);

	}

	public TitlePopup.OnItemOnClickListener onItemOnClickListener = new TitlePopup.OnItemOnClickListener(){

		@Override
		public void onItemClick(ActionItem item, int position) {
			switch (position){
				case  0:
					if(item.mTitle.equals("赞")) {
						praise(item.position);
					}else{//取消赞
						cancelPraise(item.position);
					}
					break;
				case 1:
					addLoveSth();
					break;
				case 2://直接回复楼主的评论
//                    String content = "评论"+positionParent;
//                    ToastUtils.showToast(context,"评论 "+positionParent);
//                    addComment(positionParent,content);
					CommentConfig commentConfig = new CommentConfig();
//					commentConfig.circlePosition = positionParent;
					commentConfig.commentType = CommentConfig.Type.PUBLIC;
//					homeFragment.updateEditTextBodyVisible(View.VISIBLE,commentConfig);
					break;
			}
		}

		@Override
		public void setParentPosition(int parentPosition) {

		}

		@Override
		public void setViewHolder(HomeAdapter.ViewHolder viewHolder) {

		}


	};

	/**
	 * 点赞
	 */
	private void praise(final int praisePosition){
		final User user = MyApplication.getInstance().getCurrentUser();
		if(user==null){
			ToastUtils.showToast(mContext,"请先登录");
			return;
		}
		String infoId = String.valueOf(circleMessage.getInfoId());
		ApiMomentsUtils.praise(mContext, infoId,user.getPhoneNum(), new HttpConnectionUtil.RequestCallback() {
			@Override
			public void execute(ParseModel parseModel) {
				if(ApiConstants.RESULT_SUCCESS.equals(parseModel.getRetFlag())){
					ToastUtils.showToast(mContext,parseModel.getInfo());
//					notifyPraiseData(1,viewHolder,circlePosition,praisePosition);
				}else{
					ToastUtils.showToast(mContext,parseModel.getInfo());
				}
			}
		});
	}

	/**
	 * 取消点赞
	 * @param praisePosition 取消赞位置
	 */
	private void cancelPraise(final int praisePosition){
		User user = MyApplication.getInstance().getCurrentUser();
		if(user==null){
			ToastUtils.showToast(mContext,"请先登录");
			return;
		}
		String url = ReqUrls.DEFAULT_REQ_HOST_IP + ReqUrls.REQUEST_CANCEL_PRAISE;
		String infoId = String.valueOf(circleMessage.getInfoId());
		OkHttpUtils//
				.get()//
				.addHeader("JSESSIONID", GlobalConfig.JSESSION_ID)
				.addParams("infoId",infoId)
				.addParams("phoneNum",user.getPhoneNum())
				.url(url)//
				.build()//
				.execute(new StringCallback() {
					@Override
					public void onError(Call call, Exception e) {
						ToastUtils.showToast(mContext,"取消点赞失败");
					}

					@Override
					public void onResponse(String response) {
						JSONObject jsonObject = null;
						try {
							jsonObject = new JSONObject(response);
							if(ApiConstants.RESULT_SUCCESS.equals(jsonObject.getString("rectFlag"))) {
//								notifyPraiseData(0, viewHolder, circlePostion, praisePosition);
							}else{
								ToastUtils.showToast(mContext,jsonObject.getString("info"));
							}
						} catch (JSONException e) {
							e.printStackTrace();
							ToastUtils.showToast(mContext,"取消点赞失败,稍后再试");
						}

					}
				});
	}

	/**
	 * 弹出删除评论对话框
	 * @param commentPosition
	 */
	private void showDeleteWindow(final int commentPosition){
		final AlertDialog dlg = new AlertDialog.Builder(mContext).create();
		dlg.show();
		Window window = dlg.getWindow();
		window.setContentView(R.layout.alertdialog);
		TextView tv_paizhao = (TextView) window.findViewById(R.id.tv_content1);
		tv_paizhao.setText("删除");
		tv_paizhao.setOnClickListener(new View.OnClickListener() {
			@SuppressLint("SdCardPath")
			public void onClick(View v) {
				deleteComment(commentPosition);
				dlg.cancel();
			}
		});
		window.findViewById(R.id.ll_content2).setVisibility(View.GONE);


	}
	/**
	 * 删除评论
	 * @param commentPosition 评论位置
	 */
	private void deleteComment(final int commentPosition){
		User user = MyApplication.getInstance().getCurrentUser();
		if(user==null){
			ToastUtils.showToast(mContext,"请先登录");
			return;
		}
		String url = ReqUrls.DEFAULT_REQ_HOST_IP + ReqUrls.REQUEST_DELETE_COMMENT;
		String infoId = String.valueOf(circleMessage.getInfoId());
		OkHttpUtils//
				.get()//
				.addHeader("JSESSIONID",GlobalConfig.JSESSION_ID)
				.addParams("infoId",infoId)
				.addParams("phoneNum",user.getPhoneNum())
				.url(url)//
				.build()//
				.execute(new StringCallback() {
					@Override
					public void onError(Call call, Exception e) {
						ToastUtils.showToast(mContext,"删除评论失败");
					}

					@Override
					public void onResponse(String response) {
						JSONObject jsonObject = null;
						try {
							jsonObject = new JSONObject(response);
							if(ApiConstants.RESULT_SUCCESS.equals(jsonObject.getString("rectFlag"))) {

							}else{
								ToastUtils.showToast(mContext,jsonObject.getString("info"));
							}
						} catch (JSONException e) {
							e.printStackTrace();
							ToastUtils.showToast(mContext,"删除评论失败");
						}

					}
				});
	}
}