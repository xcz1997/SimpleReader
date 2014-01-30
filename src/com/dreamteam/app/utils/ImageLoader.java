package com.dreamteam.app.utils;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;


/**
 * @description 
 * @author zcloud
 * @date 2013/11/13
 */
public class ImageLoader
{
	public static final String tag = "ImageLoader";
	
	private static HashMap<String, SoftReference<Bitmap>> cache;
	private static Map<ImageView, String> imageViews;
	private static ExecutorService pool;
	private Bitmap defBitmap;
	
	static
	{
		cache = new HashMap<String, SoftReference<Bitmap>>();
		pool = Executors.newFixedThreadPool(5);
		imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
	}
	
	public Bitmap getCacheImage(String url)
	{
		Bitmap bmp = null;
		
		if(cache.containsKey(url))
		{
			bmp = cache.get(url).get();
		}
		return bmp;
	}

	public void loadImage(String url, ImageView imageView, int width, int height)
	{
		imageViews.put(imageView, url);
		Bitmap bmp = getCacheImage(url);
		
		if(bmp != null)
		{
			imageView.setImageBitmap(bmp);
		}
		else
		{
			Log.d(tag, "loadfromFile");
			File file = FileUtils.getImageSDFile(url);
			if(file.exists())
			{
				bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
				imageView.setImageBitmap(bmp);
			}
			else
			{
				//下载网络图片
				imageView.setImageBitmap(defBitmap);
				loadNetImage(url, imageView, width, height);
			}
		}
	}
	
	private void loadNetImage(final String url, final ImageView imageView, final int width, final int height)
	{
		final Handler handler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				String tag = imageViews.get(imageViews);
				if(tag != null && tag.equals(url))
				{
					if(msg.obj != null)
					{
						imageView.setImageBitmap((Bitmap) msg.obj);
					}
				}
			}
		};
		
		pool.execute(new Runnable(){
			@Override
			public void run()
			{
				try
				{
					InputStream is = HttpUtils.getInputStream(url);
					Bitmap bmp = BitmapFactory.decodeStream(is);
					Bitmap mBmp = ImageUtils.zoomBitmap(bmp, width, height);
					cache.put(url, new SoftReference<Bitmap>(mBmp));
					
					Message msg = handler.obtainMessage();
					msg.obj = bmp;
					handler.sendMessage(msg);
					ImageUtils.saveImageToSD(bmp, url);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	public void setDefBitmap(Bitmap defBitmap)
	{
		this.defBitmap = defBitmap;
	}
}