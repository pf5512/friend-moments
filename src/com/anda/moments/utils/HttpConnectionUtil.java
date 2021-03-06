package com.anda.moments.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.anda.GlobalConfig;
import com.anda.moments.MyApplication;
import com.anda.moments.api.ApiUtils;
import com.anda.moments.api.constant.ApiConstants;
import com.anda.moments.api.constant.MethodType;
import com.anda.moments.api.constant.ReqUrls;
import com.anda.moments.commons.Constant;
import com.anda.moments.entity.ParseModel;
import com.anda.moments.ui.LoginActivity;

/**
 * http协议网络请求
 * 
 */
public class HttpConnectionUtil {
	
	private static String TAG = "HttpConnectionUtil";
	
	public static enum HttpMethod {
		GET, POST
	}
	
	/**
	 * 异步连接
	 * 
	 * @param url 网址
	 * @param method Http方法,POST跟GET
	 * @param callback 回调方法,返回给页面或其他的数据
	 */
	public static void asyncConnect(final String url, final HttpMethod method, final RequestCallback callback,
			final MethodType methodType, final Context context) {
		asyncConnect(url, null, method, callback, methodType, context);
	}
	
	/**
	 * 同步方法
	 * 
	 * @param url 网址
	 * @param method Http方法,POST跟GET
	 * @param callback 回调方法,返回给页面或其他的数据
	 */
	public static void syncConnect(final String url, final HttpMethod method, final RequestCallback callback,
			final MethodType methodType, Context context) {
		syncConnect(url, null, method, callback, methodType, context);
	}
	
	/**
	 * 请求不等待响应
	 */
	public static void reqNoWaitResponse(final String url, Map<String, Object> params, final HttpMethod method) {
		HttpClient client = new DefaultHttpClient();
		HttpUriRequest request = getRequest(url, params, method);
		try {
			@SuppressWarnings("unused")
			HttpResponse response = client.execute(request);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 异步带参数方法
	 * 
	 * @param url 网址
	 * @param params POST或GET要传递的参数
	 * @param method 方法,POST或GET
	 * @param callback 回调方法
	 */
	public static void asyncConnect(final String url, final Map<String, Object> params, final HttpMethod method,
			final RequestCallback callback, final MethodType methodType, final Context context) {
//		Handler handler = new Handler();
//		Runnable runnable = new Runnable() {
//			public void run() {
//				syncConnect(url, params, method, callback, methodType, context);
//			}
//		};
//		handler.post(runnable);
		syncConnect(url, params, method, callback, methodType, context);
	}
	
	/**
	 * 同步带参数方法
	 * 
	 * @param url 网址
	 * @param params POST或GET要传递的参数
	 * @param method 方法,POST或GET
	 * @param callback 回调方法
	 */
	private static void syncConnect(final String url, final Map<String, Object> params, final HttpMethod method,
			final RequestCallback callback, final MethodType methodType, final Context context) {
		Log.i(TAG, url+"接口传入参数："+JsonUtils.toJson(params));
		final Handler handler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				switch (message.what) {
				case ApiConstants.RESULT_CODE: {
					String backStr = message.getData().getString(ApiConstants.RESULT_BACK_STR);
					Log.i(TAG, url+"接口返回结果："+backStr);
					if (callback != null && null != backStr)
						callback.execute(getParseModel(backStr, methodType, context));
					break;
				}
				}
			}
		};
		ThreadUtil.getTheadPool(true).submit(new Runnable() {
			@Override
			public void run() {
				String backStr = "";
				try {
					HttpParams httpParameters = new BasicHttpParams();
					HttpConnectionParams.setConnectionTimeout(httpParameters, ApiConstants.MAX_CONNECTION_TIME);
					HttpConnectionParams.setSoTimeout(httpParameters, ApiConstants.MAX_CONNECTION_TIME);
					HttpClient client = new DefaultHttpClient(httpParameters);
					
					HttpUriRequest request = getRequest(url, params, method);
					if(!StringUtils.isEmpty(GlobalConfig.JSESSION_ID)){
						request.addHeader("JSESSIONID",GlobalConfig.JSESSION_ID);
					}
					HttpResponse response = client.execute(request);
					int statusCode = response.getStatusLine().getStatusCode();
					backStr = String.valueOf(statusCode);
					Log.i(TAG, backStr);
					if (statusCode == HttpStatus.SC_OK || statusCode == NetUtil.NET_QUERY_SUCC   || statusCode == NetUtil.FAIL_CODE_400) {
						backStr = EntityUtils.toString(response.getEntity(),HTTP.UTF_8);
//						backStr = handleEntity(response.getEntity(),HTTP.UTF_8);
						sendMessage(backStr, handler, ApiConstants.RESULT_CODE);
					}else if(statusCode== NetUtil.FAIL_CODE){//500
						sendMessage(backStr,handler,ApiConstants.RESULT_CODE);
					}
				} catch (Exception e) {
					
					Thread.yield();
					sendMessage(backStr, handler, ApiConstants.RESULT_CODE);
					e.printStackTrace();
				}
			}
		});
	}
	
	private static void sendMessage(String result, Handler handler, int what) {
		Message message = Message.obtain(handler, what);
		Bundle data = new Bundle();
		data.putString(ApiConstants.RESULT_BACK_STR, result);
		message.setData(data);
		handler.sendMessage(message);
	}
	
	/**
	 * POST跟GET传递参数不同,POST是隐式传递,GET是显式传递
	 * 
	 * @param url 网址
	 * @param params 参数
	 * @param method 方法
	 * @return
	 */
	private static HttpUriRequest getRequest(String url, Map<String, Object> params, HttpMethod method) {
//		Log.d(TAG, "-------发出的url请求-----------" + url);
		if (method.equals(HttpMethod.POST)) {
			List<NameValuePair> listParams = new ArrayList<NameValuePair>();
			if (params != null) {
				for (String name : params.keySet()) {
					Log.d(TAG, "========" + name + "=" + params.get(name));
//					if(ReqUrls.IMG_FILE.equals(name)){
//						listParams.add(new BasicNameValuePair(name, ));
//					}
					listParams.add(new BasicNameValuePair(name, params.get(name).toString()));
				}
			}
			try {
				UrlEncodedFormEntity entity = new UrlEncodedFormEntity(listParams, HTTP.UTF_8);
				HttpPost request = new HttpPost(url);
				request.setEntity(entity);
				return request;
			} catch (UnsupportedEncodingException e) {
				// Should not come here, ignore me.
				throw new java.lang.RuntimeException(e.getMessage(), e);
			}
		} else {
			if (url.indexOf("?") < 0) {
				url += "?";
			}
			if (params != null) {
				int i =0;
				for (String name : params.keySet()) {
					try {
						if(i==0){
							url +=  name + "=" + URLEncoder.encode(params.get(name).toString(), HTTP.UTF_8);
						}else{
							url += "&" + name + "=" + URLEncoder.encode(params.get(name).toString(), HTTP.UTF_8);
						}
						i++;
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
			}
			Log.i(TAG, url);
			HttpGet request = new HttpGet(url);
			return request;
		}
	}
	
	/**
	 * 回调接口
	 * 
	 * 
	 */
	public interface RequestCallback {
		void execute(ParseModel parseModel);
	}
	
	/**
	 * 转换parseModel对象
	 * 
	 * @param backStr
	 */
	private static synchronized ParseModel getParseModel(String backStr, MethodType methodType, Context context) {
//		if(MethodType.GET_INFO_DETAILS.getIndex() == methodType.getIndex()){//我的列表
//			ParseModel pm = new ParseModel();
//			if(!String.valueOf(NetUtil.FAIL_CODE).equals(backStr)){
//				pm.setRetFlag(ApiConstants.RESULT_SUCCESS);
//				pm.setInfo(NetUtil.SERVICE_SUCCESS_MSG);
//			}else{
//				pm.setRetFlag(String.valueOf(NetUtil.FAIL_CODE));
//				pm.setInfo(NetUtil.SERVICE_ERR_MSG);
//			}
//			return pm;
//		}
		ParseModel pm = ApiUtils.parse2ParseModel(backStr);
		if (pm == null) {
			pm = new ParseModel();
			if(String.valueOf(NetUtil.FAIL_CODE).equals(backStr)){//服务器异常
				pm.setRetFlag(String.valueOf(NetUtil.FAIL_CODE));
				pm.setInfo(NetUtil.SERVICE_ERR_MSG);
				return pm;
			}
			
			pm.setRetFlag(String.valueOf(NetUtil.NET_ERR));
			pm.setInfo(NetUtil.NET_ERR_MSG);
			return pm;
		}
		String retFlag = pm.getRetFlag();
		if (retFlag!=null && retFlag.equals(NetUtil.SUCCESS_CODE)) {
			Object apiResult = null;
//			if (MethodType.GET_MAINPAGE_AD.getIndex() == methodType.getIndex()) { // 广告
////				apiResult = ApiUtils.getAd(pm.getData());
//			}else if (MethodType.SUBMIT_ORDER.getIndex() == methodType.getIndex()) { // 提交订单
//                apiResult = pm.getData();
//            }
//			if(MethodType.LOGIN.getIndex() == methodType.getIndex()){//登录
//				apiResult = JsonUtils.fromJson(pm.getData().toString(), User.class);
//			}
			apiResult = pm.getData();
			String jsessionId = pm.getJsessionid();
			//做的token刷新处理  start
			if (!StringUtils.isEmpty(jsessionId)) {
				GlobalConfig.JSESSION_ID = jsessionId;
				long currentTime = System.currentTimeMillis();
				SharePreferenceManager.saveBatchSharedPreference(context,
						Constant.FILE_NAME, com.anda.moments.constant.api.ReqUrls.JSESSION_ID, pm.getJsessionid() + "_&_"
								+ currentTime);
			}
			//做的token刷新处理 end
			
			pm.setApiResult(apiResult);
		}else if(retFlag!=null && retFlag.equals(NetUtil.FAIL_CODE_401)){//会话超时，重新登陆
			GlobalConfig.JSESSION_ID = "";
			SharePreferenceManager.saveBatchSharedPreference(context,
					Constant.FILE_NAME, com.anda.moments.constant.api.ReqUrls.JSESSION_ID,"");

			Intent loginIntent = new Intent(context,LoginActivity.class);

			loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		loginIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(loginIntent);
		}
		return pm;
	}
	
	public static String handleEntity(HttpEntity entity, String charset)
			throws IOException {
		if (entity == null)
			return null;

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];

		long count = entity.getContentLength();
		long curCount = 0;
		int len = -1;
		InputStream is = entity.getContent();
		while ((len = is.read(buffer)) != -1) {
			outStream.write(buffer, 0, len);
			curCount += len;
		}

		byte[] data = outStream.toByteArray();
		outStream.close();
		is.close();
		return new String(data, charset);
	}
}
