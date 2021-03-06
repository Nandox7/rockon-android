package org.abrantes.filex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.BitmapFactory.Options;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.GradientDrawable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout.LayoutParams;


public class AlbumCursorAdapter extends SimpleCursorAdapter{
	String						FILEX_FILENAME_EXTENSION = "";
    String						FILEX_ALBUM_ART_PATH = "/sdcard/albumthumbs/RockOn/";
    String						FILEX_SMALL_ALBUM_ART_PATH = "/sdcard/albumthumbs/RockOn/small/";
    String						FILEX_CONCERT_PATH = "/sdcard/RockOn/concert/";
    double 						CURRENT_PLAY_SCREEN_FRACTION = 0.66;
    double 						CURRENT_PLAY_SCREEN_FRACTION_LANDSCAPE = 0.75;
    double 						NAVIGATOR_SCREEN_FRACTION = 1 - CURRENT_PLAY_SCREEN_FRACTION;
    double		 				NAVIGATOR_SCREEN_FRACTION_LANDSCAPE = 1 - CURRENT_PLAY_SCREEN_FRACTION_LANDSCAPE;

    // should go into constants class
    int							BITMAP_SIZE_SMALL = 0;
    int							BITMAP_SIZE_NORMAL = 1;
    int							BITMAP_SIZE_FULLSCREEN = 2;
    int							BITMAP_SIZE_XSMALL = 3;
    
    private final int 			NO_COVER_SAMPLING_INTERVAL = 1;
    private Cursor 				cursor;
    private Cursor				cursorSecondary;
    public Context	 			context;
    public int					viewWidth;
    public int					viewWidthNormal;
    public int					viewWidthBig;
    public int 					viewHeightBig;
    private LayoutParams 		params = null;
    private GradientDrawable	overlayGradient = null;
    public Bitmap				albumCoverBitmap;
    public BitmapFactory		bitmapDecoder = new BitmapFactory();
    public ImageView 			albumImage = null;
    public ImageView 			albumImageOverlay = null;
    public TextView				albumImageAlternative = null;
    public TextView				albumNameTextView = null;
    public TextView				albumArtistTextView = null;
	public String 				albumCoverPath = null;    
	public String 				artistName = null;
	public String 				albumName = null;
	public String 				path = null;
	public File 				albumCoverFilePath = null;
	public Options 				opts = null;
	public Bitmap[] 			albumImages;
	public int[] 				albumImagesIndexes;
	public int					albumImagesCenter = 0;
	public Bitmap				albumUndefinedCoverBitmap;
	public Bitmap				albumUndefinedCoverBigBitmap;
	public	boolean				PRELOAD = false;
	public boolean				isScrolling = false;
	public int					AVOID_PRELOAD_THRESHOLD = 10000;
	public boolean				showArtWhileScrolling = false;
	public boolean				showFrame = false;

	private int					HALF_IMAGES_IN_CACHE;
	

    public AlbumCursorAdapter(Context context, 
    							int layout, 
    							Cursor c,
    							String[] from,
    							int[] to,
    							Cursor cSecondary,
    							Bitmap[] albumImages,
    							int[] albumImagesIndexes,
    							boolean showArtWhileScrolling,
    							boolean showFrame,
    							int	imagesInCache){
        super(context, layout, c, from, to);
        this.cursor = c;
        this.cursorSecondary = cSecondary;
        this.context = context;
        this.showArtWhileScrolling = showArtWhileScrolling;
        this.showFrame = showFrame;
        this.HALF_IMAGES_IN_CACHE = imagesInCache/2;

        /*
         * Reload navigator width
         */
        reloadNavigatorWidth();
                
        /*
         * Preload the undefined album image
         */
		// TODO:
		// adjust sample size dynamically
		opts = new Options();
		opts.inSampleSize = NO_COVER_SAMPLING_INTERVAL;
		albumUndefinedCoverBitmap = this.createFancyAlbumCoverFromResource(
				R.drawable.albumart_mp_unknown,
				viewWidth,
				viewWidth);
    	
//		albumUndefinedCoverNormalBitmap = this.createFancyAlbumCoverFromResource(
//				R.drawable.albumart_mp_unknown,
//				viewWidthNormal,
//				viewWidthNormal);
		
		albumUndefinedCoverBigBitmap = this.createFancyAlbumCoverFromResource(
				R.drawable.albumart_mp_unknown,
				viewWidthBig,
				viewHeightBig);
    	
    	/*
         * Set up image cache variables
         */
    	this.albumImages = albumImages; 
    	this.albumImagesIndexes = albumImagesIndexes;
    	PRELOAD = true;

        

    }

    /*
     * reloadNavigatorWidth
     */
    public void reloadNavigatorWidth(){

    	try{
	        WindowManager windowManager 		= (WindowManager) 
				context.getSystemService(Context.WINDOW_SERVICE);
	
	        Display display						= windowManager.getDefaultDisplay();
			
			if(display.getOrientation() == 0){
				viewWidth = (int) Math.floor(
					display.getWidth() * 
					NAVIGATOR_SCREEN_FRACTION
					);
			
				viewWidthNormal = (int) Math.floor(
					display.getWidth() * 
					(1-NAVIGATOR_SCREEN_FRACTION)
					);
				viewWidthBig = (int) Math.min(
					display.getWidth(),
					display.getHeight()
					);
			}else{    
				viewWidth = (int) Math.floor(
					display.getWidth() * 
					NAVIGATOR_SCREEN_FRACTION_LANDSCAPE
					);
				viewWidthNormal = (int) Math.floor(
					display.getWidth() * 
					(1-NAVIGATOR_SCREEN_FRACTION_LANDSCAPE) / 2
					);
				viewWidthBig = (int) Math.min(
					display.getWidth(),
					display.getHeight()
					);
			}
			viewWidthBig = display.getWidth() - 30;
			viewHeightBig = display.getHeight() - 60;
			
			viewWidthBig = 320;
			viewHeightBig = 320;
			
			params = new LayoutParams(viewWidth, viewWidth);
    	} catch(Exception e) {
    		viewWidth = 120;
    		
    		viewWidthNormal = 200;
			
    		viewWidthBig = 320;
			viewHeightBig = 320;
    	}
    }
    
    /* (non-Javadoc)
     * This is where you actually create the item view of the list
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
//    	Log.i("BINDVIEW", "Position: " + cursor.getPosition());
    	    	
    	try{
	    	/*
	    	 * Get the item list image component set its height right
	    	 */
	    	albumImage = (ImageView)
	    		view.findViewById(R.id.navigator_albumart_image);
	    	albumImage.setLayoutParams(params);
	    	
	    	albumImageOverlay = (ImageView)
	    		view.findViewById(R.id.navigator_albumart_overlay);
	    	//albumImageOverlay.setImageDrawable(overlayGradient);
	    	albumImageOverlay.setLayoutParams(params);
	    	if(this.showFrame){
	    		albumImageOverlay.setVisibility(View.VISIBLE);
	    	} else {
	    		albumImageOverlay.setVisibility(View.GONE);
	    	}

	    	// TODO: needs a if(albums in full screen)
	    	albumNameTextView = (TextView)
	    									view.findViewById(R.id.navigator_albumname);
	    	albumArtistTextView = (TextView)
	    									view.findViewById(R.id.navigator_albumartist);
	    	albumNameTextView.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)));
	    	albumArtistTextView.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)));
	    	
	    	
    		/*
	    	 * if preload is in use, get the preloaded bitmap, otherwise go get it
	    	 */
	    	if(PRELOAD){
	    		if(albumImage != null && cursor != null){
//		    			Log.i("SHOW", "cursor.getPosition " + cursor.getPosition() + " -- Center " + albumImagesCenter + " -- HALF_CACHESIZE "+ HALF_IMAGES_IN_CACHE);
//		    			Log.i("SHOW", "Cached Idx: " + albumImagesIndexes[HALF_IMAGES_IN_CACHE + cursor.getPosition() - albumImagesCenter] +
//		    					" == CursorPosition: " + cursor.getPosition());
	    			if(Math.abs(cursor.getPosition()-albumImagesCenter) < HALF_IMAGES_IN_CACHE && 
	    					albumImagesIndexes[HALF_IMAGES_IN_CACHE + cursor.getPosition() - albumImagesCenter] == cursor.getPosition()){
//		    				Log.i("SHOW", "Cached Cover");
	    				albumImage.setImageBitmap(albumImages[HALF_IMAGES_IN_CACHE + cursor.getPosition() - albumImagesCenter]);
	    				/* Hide Artist Name */
	    		    	albumImageAlternative = (TextView)
				    		view.findViewById(R.id.navigator_albumart_alternative);
				    	albumImageAlternative.setVisibility(View.GONE);
	    			}
	    			else{
//		    				Log.i("SHOW", "Uncached cover");
	    				albumImage.setImageBitmap(getAlbumBitmap(cursor.getPosition(), BITMAP_SIZE_SMALL));
//		    				albumImage.setImageBitmap(albumUndefinedCoverBitmap);
//		    				/* Show Artist Name */
//		    				if(!cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)).equals("<unknown>")){
//			    		    	albumImageAlternative = (TextView)
//						    		view.findViewById(R.id.navigator_albumart_alternative);
//						    	albumImageAlternative.setLayoutParams(params);
//						    	albumImageAlternative.setVisibility(View.VISIBLE);
//						    	albumImageAlternative.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)));
//		    				}
	    			}
	    		}
	    		return;
	    	} else {
	    		albumImage.setImageBitmap(getAlbumBitmap(cursor.getPosition(), BITMAP_SIZE_SMALL));
	    		return;
	    	}
		} catch(Exception e) {
    		e.printStackTrace();
    		albumImage.setImageBitmap(albumUndefinedCoverBitmap);
    		return;
    	}
    }
    
    /*********************************
     * 
     * PreloadAlbumImages
     * 
     *********************************/
    public void	preloadAlbumImages(Bitmap[] albumImages){
//    	cursor.moveToFirst();
    	for(int i=0; i< cursor.getCount(); i++){
//    	while(!cursor.isAfterLast()){
    		cursor.moveToPosition(i);
    		Log.i("PRELOAD", ""+cursor.getPosition());
    		albumImages[cursor.getPosition()] = getAlbumBitmap(cursor.getPosition(), BITMAP_SIZE_XSMALL);
//    		cursor.moveToNext();
    	}
    }
    
    /*********************************
     * 
     * albumArtExists
     * 
     *********************************/
    public boolean albumArtExists(int position){
    	cursor.moveToPosition(position);
    	
    	/*
    	 * Get the path to the album art
    	 */
    	albumCoverPath = null;
		artistName = cursor.getString(
								cursor.getColumnIndexOrThrow(
										MediaStore.Audio.Albums.ARTIST));
		albumName = cursor.getString(
								cursor.getColumnIndexOrThrow(
										MediaStore.Audio.Albums.ALBUM));
		path = FILEX_SMALL_ALBUM_ART_PATH+
						validateFileName(artistName)+
						" - "+
						validateFileName(albumName)+
						FILEX_FILENAME_EXTENSION;
		albumCoverFilePath = new File(path);
		if(albumCoverFilePath.exists() && albumCoverFilePath.length() > 0){
			albumCoverPath = path;
			return true;
		} else {
			return false;
		}
    }
    
    /*********************************
     * 
     * getAlbumBitmap
     *
     *********************************/
    int dim = 1;
    public Bitmap getAlbumBitmap(int position, int bitmapFuzzySize){
    	if(position == -1 || position >= cursor.getCount()){
    		return albumUndefinedCoverBitmap;
    	}
    	
    	cursorSecondary.moveToPosition(position);
    		
    	/*
    	 * Get the path to the album art
    	 */
    	albumCoverPath = null;
		artistName = cursorSecondary.getString(
						cursorSecondary.getColumnIndexOrThrow(
										MediaStore.Audio.Albums.ARTIST));
		albumName = cursorSecondary.getString(
						cursorSecondary.getColumnIndexOrThrow(
										MediaStore.Audio.Albums.ALBUM));
		if(bitmapFuzzySize == BITMAP_SIZE_SMALL){
			path = FILEX_SMALL_ALBUM_ART_PATH+
				validateFileName(artistName)+
				" - "+
				validateFileName(albumName)+
				FILEX_FILENAME_EXTENSION;
    	} else {
    		path = cursorSecondary.getString(
    				cursorSecondary.getColumnIndexOrThrow(
    							MediaStore.Audio.Albums.ALBUM_ART));
    		
        	/* check if the embedded mp3 is valid (or big enough)*/
    		if(path != null){
    			FileInputStream pathStream = null;
				try {
					pathStream = new FileInputStream(path);
					BitmapFactory.Options opts = new BitmapFactory.Options();
		    		opts.inJustDecodeBounds = true;
		    		Bitmap bmTmp = BitmapFactory.decodeStream(pathStream, null, opts);
		    		if(opts == null || opts.outHeight < 300 || opts.outWidth < 300)
		    			path = null;
		    		if(bmTmp != null)
		    			bmTmp.recycle();
	    			pathStream.close();
				} catch (Exception e) {
					e.printStackTrace();
					path = null;
				}
    		}
    		
    		if(path == null){
	    		path = FILEX_ALBUM_ART_PATH+
					validateFileName(artistName)+
					" - "+
					validateFileName(albumName)+
					FILEX_FILENAME_EXTENSION;
    		}
    	}
    
		albumCoverFilePath = new File(path);
		if(albumCoverFilePath.exists() && albumCoverFilePath.length() > 0){
			albumCoverPath = path;
		}
		
		try{
			/*
			 * If the album art exists put it in the preloaded array, otherwise
			 * just use the default image
			 */
			if(albumCoverPath != null){
				if(bitmapFuzzySize == BITMAP_SIZE_XSMALL){
//					if(cursor.getCount() > 1 && cursor.getCount() < AVOID_PRELOAD_THRESHOLD){
//					dim = viewWidth*4;
					dim = Math.min(
							Math.max(Math.round(viewWidth * (10000.0f/cursor.getCount())), Math.round(viewWidth/1.8f)),
							viewWidth);
					//Log.i("performance", ""+dim);
					albumCoverBitmap = createFancyAlbumCoverFromFilePath(albumCoverPath, dim, dim);
//					} else {
//						albumCoverBitmap = createFancyAlbumCoverFromFilePath(albumCoverPath, viewWidth, viewWidth);
//					}
				}
				else if(bitmapFuzzySize == BITMAP_SIZE_SMALL)
					albumCoverBitmap = createFancyAlbumCoverFromFilePath(albumCoverPath, viewWidth, viewWidth);
				else if(bitmapFuzzySize == BITMAP_SIZE_NORMAL)
					albumCoverBitmap = createFancyAlbumCoverFromFilePath(albumCoverPath, viewWidthNormal, viewWidthNormal);
				else if(bitmapFuzzySize == BITMAP_SIZE_FULLSCREEN)
					albumCoverBitmap = createFancyAlbumCoverFromFilePath(albumCoverPath, viewWidthBig, viewHeightBig);
				return albumCoverBitmap;
			} else {
				// TODO:
				// adjust sample size dynamically
//				opts = new Options();
//				opts.inSampleSize = NO_COVER_SAMPLING_INTERVAL;
//				albumCoverBitmap = bitmapDecoder.decodeResource(this.context.getResources(),
//																R.drawable.albumart_mp_unknown, opts);
//				albumCoverBitmap = createFancyAlbumCoverFromResource(R.drawable.albumart_mp_unknown, viewWidth, viewWidth);
				if(bitmapFuzzySize == BITMAP_SIZE_XSMALL)
					albumCoverBitmap = createFancyAlbumCover(albumUndefinedCoverBitmap, viewWidth, viewWidth);
				else if(bitmapFuzzySize == BITMAP_SIZE_SMALL)
					albumCoverBitmap = createFancyAlbumCover(albumUndefinedCoverBitmap, viewWidth, viewWidth);
				else if(bitmapFuzzySize == BITMAP_SIZE_NORMAL)
					albumCoverBitmap = createFancyAlbumCover(albumUndefinedCoverBigBitmap, viewWidthNormal, viewWidthNormal);
				else if(bitmapFuzzySize == BITMAP_SIZE_FULLSCREEN)
					albumCoverBitmap = createFancyAlbumCover(albumUndefinedCoverBigBitmap, viewWidthBig, viewWidthBig);
				return albumCoverBitmap;
			}
		} catch(Exception e) {
			e.printStackTrace();
			if(bitmapFuzzySize == BITMAP_SIZE_XSMALL)
				return albumUndefinedCoverBitmap;
			else if(bitmapFuzzySize == BITMAP_SIZE_SMALL)
				return albumUndefinedCoverBitmap;
			else if(bitmapFuzzySize == BITMAP_SIZE_NORMAL)
				return albumUndefinedCoverBigBitmap;
			else if(bitmapFuzzySize == BITMAP_SIZE_FULLSCREEN)
				return albumUndefinedCoverBigBitmap;
			else
				return albumUndefinedCoverBigBitmap;
		}
    }
    
    
    Shader shader;
    RectF	rect = new RectF();
    Paint	paint= new Paint();
    float	round = 0.f;

    
    /*********************************
     * 
     * createFancyAlbumCover
     * 
     *********************************/
//    Bitmap cBitmap = Bitmap.createBitmap(460, 460, Bitmap.Config.ARGB_8888);
    public Bitmap createFancyAlbumCover(Bitmap bitmap, int width, int height){

    	try{

    		/*
    		 * Adjust the aspect ratio of the incoming bitmap if needed
    		 */
    		float aspectRatio = (float)width/(float)height; 
    		if(Math.abs(aspectRatio - ((float)bitmap.getWidth()/(float)bitmap.getHeight())) > 0.1){
        		
    			if(aspectRatio > 1){ // width is larger
	    			bitmap = Bitmap.createBitmap(
		        				bitmap,
		        				0,
		        				(int)(bitmap.getHeight()-(bitmap.getHeight()/aspectRatio))/2,
		        				bitmap.getWidth(),
		        				(int)(bitmap.getHeight()/aspectRatio));
    			} else {
					bitmap = Bitmap.createBitmap(
	        				bitmap,
	        				(int)(bitmap.getWidth()-(bitmap.getWidth()*aspectRatio))/2,
	        				0,
	        				(int)(bitmap.getWidth()*aspectRatio),
	        				bitmap.getHeight());
    			}
    		}
    		
	//    	Bitmap tBitmap;
	    	paint.setAntiAlias(true);
	    	paint.setDither(true);
	    	//BlurMaskFilter blurFilter = new BlurMaskFilter(viewWidth/20.0f, BlurMaskFilter.Blur.INNER);
	    	
	        Bitmap cBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
	        Canvas canvas = new Canvas();
	        canvas.setBitmap(cBitmap); 
	        
	//        //paint.setXfermode(new PorterDuffXfermode(Mode.));
	//        paint.setMaskFilter(blurFilter);
	//	  	//paint.setAlpha(100);
	//	  	tBitmap = Bitmap.createScaledBitmap(bitmapDecoder.decodeFile(path),
	//	  		width, height, false);
	//	  	canvas.drawBitmap(tBitmap, 0, 0, paint);
	//	
	//	    
	//		paint.setXfermode(new PorterDuffXfermode(Mode.LIGHTEN)); 
	//		paint.setAlpha(150);
	//		tBitmap = Bitmap.createScaledBitmap(bitmapDecoder.decodeFile(path),
	//				width, height, false);
	//		canvas.drawBitmap(tBitmap, 0, 0, paint);
	
			//paint.setXfermode(new PorterDuffXfermode(Mode.DARKEN));
	        
	        paint.setXfermode(null);
	        if(bitmap != null){
		        Shader bmpShader = new BitmapShader(
		        		Bitmap.createScaledBitmap(
		        				bitmap,
		        				width, 
		        				height, 
		        				false),
	//	        		bitmap,
		        		TileMode.CLAMP,
		        		TileMode.CLAMP);
		        paint.setShader(bmpShader);
	        }
	        
	//        rect = new RectF();
	        rect.left = 1.0f;
	        rect.top = 1.0f;
	        rect.right = width - 1.0f;
	        rect.bottom = height - 1.0f;
	        if(width > 300)
	        	round = 0.f; // fullscreen
	        else{
	        	// other sizes
	        	if(showFrame)
	        		round = width/40.f;
	        	else
	        		round = width/30.f;
	        }
	        if(showFrame){
	        	canvas.drawRoundRect(rect, round, round, paint);
	        } else {
	        	canvas.drawRoundRect(rect, round, round, paint);
	        }
	        
	
	        
	//        int[] gradColors = {0x44222222, 0x44EEEEEE, 0x44FFFFFF};
	//        float[] gradColorPositions = {0, 0.75f, 1.0f};
	//		//paint.setXfermode(new PorterDuffXfermode(Mode.LIGHTEN)); 
	//        Shader gradientShader = new LinearGradient(
	//        		0,0,
	//                0,(int) (height/3),
	//                gradColors, 
	//                gradColorPositions,
	//                TileMode.CLAMP);
	        
	
	//      int[] gradColors = {0x33333333, 0x55333333, 0x33333333};
	//      float[] gradColorPositions = {0, 0.85f, 1.0f};
	//      paint.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY)); 
	//      Shader gradientShader = new LinearGradient(
	//      		0,0,
	//              0,(int) (height/3),
	//              gradColors, 
	//              gradColorPositions,
	//              TileMode.CLAMP);
	      
	        
	//        paint.setShader(gradientShader);
	// 
	////        rect = new RectF();
	//        rect.left = (float) -width;
	//        rect.top = (float) -height/2;
	//        rect.right = (float) 2*width;
	//        rect.bottom = (float) height/3; 
	//        canvas.drawOval(rect, paint);
	        
	//        paint  = new Paint();
	//        paint.setShadowLayer(6.0f, 0, 0, Color.WHITE);
	//        paint.setStyle(Paint.Style.STROKE);
	//        paint.setStrokeWidth(2.0f);
	//        paint.setColor(Color.WHITE);
	//        canvas.drawRoundRect(rect, 128.0f, 12.0f, paint);
	
	//        paint.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY));
	//        //paint.setMaskFilter(new BlurMaskFilter(viewWidth/2.0f, BlurMaskFilter.Blur.NORMAL));
	//        //paint.setAlpha(100);
	//        
	//        tBitmap = Bitmap.createScaledBitmap(bitmapDecoder.decodeFile(path),
	//        		width, height, false);
	//        canvas.drawBitmap(tBitmap, 0, 0, paint);
    	
	        return cBitmap;
    	} catch (Exception e) {
    		e.printStackTrace();
    		return albumUndefinedCoverBitmap;
    	} catch (Error e) {
    		e.printStackTrace();
    		return albumUndefinedCoverBigBitmap;
    	}
    	
    }
    
    
    BitmapFactory.Options bitOpts = new BitmapFactory.Options();
    byte[] tmpBitmapAlloc = new byte[1024*8];
    //byte[] tmpBitmapAlloc2 = new byte[1024*64];
    /*********************************
     * 
     * createFancyAlbumCoverFromFilePath
     * 
     *********************************/
    public Bitmap createFancyAlbumCoverFromFilePath(String path, int width, int height){
    	bitOpts.inTempStorage = tmpBitmapAlloc; 
    	
      	if (false)
    		return bitmapDecoder.decodeFile(path, bitOpts);
    	
    	
    	try{
    		FileInputStream pathStream = new FileInputStream(path);
    		Bitmap tmpBm = bitmapDecoder.decodeStream(pathStream, null, bitOpts);
    		Bitmap bm = createFancyAlbumCover(tmpBm, width,height);
    		tmpBm.recycle();
    		pathStream.close();
    		return bm;
    	} catch(Exception e) {
    		return createFancyAlbumCover(albumUndefinedCoverBitmap, width,height);
    	}catch (Error err){
    		err.printStackTrace();
    		return albumUndefinedCoverBitmap;
    	}
    	
    }
    
    /*********************************
     * 
     * createFancyAlbumCoverFromResource
     * 
     *********************************/
    public Bitmap createFancyAlbumCoverFromResource(int res, int width, int height){
    	bitOpts.inTempStorage = tmpBitmapAlloc; 

    	if (false)
    		return bitmapDecoder.decodeResource(context.getResources(), res, bitOpts);
    	try{
	    	Bitmap bmpTmp = bitmapDecoder.decodeResource(context.getResources(), res, bitOpts);
	    	Bitmap bm = createFancyAlbumCover(bmpTmp, width,height);
	    	bmpTmp.recycle();
	    	return bm;
    	}catch(Error err){
    		err.printStackTrace();
    		return albumUndefinedCoverBitmap;
    	}
    }
    
	/*********************************
	 * 
	 * ValidateFilename
	 * 
	 *********************************/
	private String validateFileName(String fileName) {
		if(fileName == null)
			return "";
		fileName = fileName.replace('/', '_');
		fileName = fileName.replace('<', '_');
		fileName = fileName.replace('>', '_');
		fileName = fileName.replace(':', '_');
		fileName = fileName.replace('\'', '_');
		fileName = fileName.replace('?', '_');
		fileName = fileName.replace('"', '_');
		fileName = fileName.replace('|', '_');
		return fileName;
	}
}