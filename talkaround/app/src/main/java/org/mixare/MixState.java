/*
 * Copyright (C) 2010- Peer internet solutions
 * 
 * This file is part of mixare.
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.mixare;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mixare.data.DataSource;
import org.mixare.data.Json;
import org.mixare.reality.PhysicalPlace;
import org.mixare.render.Matrix;
import org.mixare.render.MixVector;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// 현재의 상태에 관한 클래스
public class MixState {

	// 각 상태에 대한 상수값 설정
	public static int NOT_STARTED = 0;
	public static int PROCESSING = 1;
	public static int READY = 2;
	public static int DONE = 3;

	int nextLStatus = MixState.NOT_STARTED;	// 다음 상태
	String downloadId;	// 다운로드할 ID
	String newurl;

	private float curBearing;	// 현재의 방위각
	private float curPitch;		// 현재의 장치각(?)

	private boolean detailsView;	// 디테일 뷰가 표시 중인지 여부

	private Context context;

	public MixState(){};

	/*public MixState(Context context){
		this.context = context;
	}*/

	// 이벤트 처리
	public boolean handleEvent(MixContext ctx, String onPress, String title, PhysicalPlace log) {
		/*CharSequence cs1 = "-";
		// 눌려진 스트링 값이 null 이 아니고, 웹페이지로 연결될 경우
		if (onPress != null && onPress.startsWith("webpage")) {
			try {
				// 내용을 파싱하고 디테일 뷰에 웹페이지를 띄운다
				String webpage = MixUtils.parseAction(onPress);
				this.detailsView = true;
				ctx.loadMixViewWebPage(webpage);
			} catch (Exception ex) {
			}
		}
		else{*/
		DialogSelectOption(ctx, title, log, onPress);

		return true;
	}

	public void DialogSelectOption(final MixContext ctx, final String markerTitle, final PhysicalPlace log, final String onPress) {
		final String items[] = {"메모 보기", "남겨진 추억"};
		AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
		ab.setTitle(markerTitle);
		ab.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// 프로그램을 종료한다
				Toast.makeText(ctx,
						items[id] + " 선택했습니다.",
						Toast.LENGTH_SHORT).show();
				dialog.dismiss();

				if (id == 1) {
					searchData sData = new searchData(ctx);
					sData.execute(onPress.substring(8,onPress.length()));
				} else if (id == 0) {
					try {
						Intent i1 = new Intent (ctx, ViewMemo.class);
						i1.putExtra("url", onPress); //키 - 보낼 값(밸류)
						ctx.startActivity(i1);
					} catch (Exception e) {}
				}
			}
		});

		// 다이얼로그 생성
		AlertDialog alertDialog = ab.create();
		// 다이얼로그 보여주기
		alertDialog.show();
	}

	private List<Marker> parseJSONtoMarker(String json) throws JSONException {
		JSONObject root = new JSONObject(json);
		Json jsonClass = new Json();
		List<Marker> markers = jsonClass.load(root, DataSource.DATAFORMAT.NAVER);

		return markers;
	}

	// 현재의 방위각을 리턴
	public float getCurBearing() {
		return curBearing;
	}

	// 현재의 장치각을 리턴
	public float getCurPitch() {
		return curPitch;
	}

	// 디테일 뷰의 표시 여부를 리턴
	public boolean isDetailsView() {
		return detailsView;
	}

	// 디테일 뷰의 표시 여부를 설정
	public void setDetailsView(boolean detailsView) {
		this.detailsView = detailsView;
	}

	// 장치각과 방위각을 계산
	public void calcPitchBearing(Matrix rotationM) {
		MixVector looking = new MixVector();
		rotationM.transpose();
		looking.set(1, 0, 0);
		looking.prod(rotationM);
		this.curBearing = (int) (MixUtils.getAngle(0, 0, looking.x, looking.z)  + 360 ) % 360 ;

		rotationM.transpose();
		looking.set(0, 1, 0);
		looking.prod(rotationM);
		this.curPitch = -MixUtils.getAngle(0, 0, looking.y, looking.z);
	}

	//DB관련 클래스-------------------------------------------------------------
	private class searchData extends AsyncTask<String, Void, String> {
		ProgressDialog progressDialog;
		private Context context2;

		public searchData (Context _context){
			context2 = _context;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}


		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			try{
				JSONArray QueryResult = new JSONArray(result);
				Log.e("dffd", result);
				for(int i = 0; i < QueryResult.length(); ++i){
					JSONObject tmp = QueryResult.getJSONObject(i);
					newurl = tmp.get("url2").toString();
				}
				Log.e("받은 url", newurl);
				Intent i2 = new Intent (context2, ViewPic.class);
				i2.putExtra("url", newurl); //키 - 보낼 값(밸류)
				context2.startActivity(i2);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		protected String doInBackground(String... params) {
			String URL1 = (String)params[0];

			String serverURL = "http://220.95.88.213:22223/findNewurl.php";
			String postParameters = "oldurl=" + URL1;
			Log.e("TAG", postParameters);

			try {
				URL url = new URL(serverURL);
				HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

				httpURLConnection.setReadTimeout(5000);
				httpURLConnection.setConnectTimeout(5000);
				httpURLConnection.setRequestMethod("POST");
				//httpURLConnection.setRequestProperty("content-type", "application/json");
				httpURLConnection.setDoInput(true);
				httpURLConnection.connect();

				OutputStream outputStream = httpURLConnection.getOutputStream();
				outputStream.write(postParameters.getBytes("UTF-8"));
				outputStream.flush();
				outputStream.close();

				int responseStatusCode = httpURLConnection.getResponseCode();
				Log.d("TAG", "POST response code - " + responseStatusCode);

				InputStream inputStream;
				if(responseStatusCode == HttpURLConnection.HTTP_OK) {
					inputStream = httpURLConnection.getInputStream();
				}
				else{
					inputStream = httpURLConnection.getErrorStream();
				}

				InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				StringBuilder sb = new StringBuilder();
				String line = null;

				while((line = bufferedReader.readLine()) != null){
					sb.append(line);
				}

				bufferedReader.close();
				return sb.toString();
			} catch (Exception e) {

				return new String("Error: " + e.getMessage());
			}
		}
	}
}
