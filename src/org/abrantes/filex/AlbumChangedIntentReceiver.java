package org.abrantes.filex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;


public class AlbumChangedIntentReceiver	extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("ALBUMINTENT", "albumChanged");
		Filex filex =((Filex) context);
		try {
			/*
			 * Destroy song Progress Dialog if on screen
			 */
			try{
				if(filex.songProgressAlertDialog != null){
					filex.dismissDialog(R.layout.song_progress_dialog);
					filex.removeDialog(R.layout.song_progress_dialog);
					filex.songProgressAlertDialog = null;
					filex.songProgressView = null;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			/* 
			 * move (album)cursor to the new album 
			 */
			filex.albumCursor.moveToPosition(
					filex.playerServiceIface.getAlbumCursorPosition());
			
			/*
			 * move (song)cursor to the new song
			 */
			filex.songCursor = filex.initializeSongCursor(filex.albumCursor.getString(
												filex.albumCursor.getColumnIndexOrThrow(
														MediaStore.Audio.Albums.ALBUM)));
			filex.songCursor.moveToPosition(
					filex.playerServiceIface.getSongCursorPosition());

			/*
			 * Update status variables 
			 */
			filex.albumCursorPositionPlaying = filex.playerServiceIface.getAlbumCursorPosition();
			filex.currentAlbumPosition = filex.playerServiceIface.getAlbumCursorPosition();
			filex.currentSongPosition = filex.playerServiceIface.getSongCursorPosition();
			
			/*
			 * Update the song progress bar
			 */
			filex.songProgressBar.setProgress(0);
			filex.songProgressBar.setMax((int) filex.songCursor.getDouble(
												filex.songCursor.getColumnIndexOrThrow(
														MediaStore.Audio.Media.DURATION)));
			
			/* to know whether this song change is being called 
			 * by the service or explicit user input
			 */
			filex.calledByService = true;
			
//			Message msg = new Message();
//			msg.obj = filex;
//			animationStarterHandler.sendMessageDelayed(msg, 100);
			
			/*
			 * Start the album fade out animation
			 * 	- album fade in is automatically started at the end of this animation
			 */
    		filex.currentAlbumPlayingLayoutOuter.startAnimation(filex.fadeAlbumOut);
    		/*
    		 * These calls are to ensure that the (whole) UI screen is refreshed
    		 */
    		filex.invalidateCurrentPlayingImageView.sendEmptyMessageDelayed(0, 50);
    		filex.invalidateCurrentPlayingImageView.sendEmptyMessageDelayed(0, 100);
    		filex.invalidateCurrentPlayingImageView.sendEmptyMessageDelayed(0, 150);
    		filex.invalidateCurrentPlayingImageView.sendEmptyMessageDelayed(0, 200);
    		filex.invalidateCurrentPlayingImageView.sendEmptyMessageDelayed(0, 250);
    		filex.invalidateCurrentPlayingImageView.sendEmptyMessageDelayed(0, 300);
    		filex.invalidateCurrentPlayingImageView.sendEmptyMessageDelayed(0, 350);
    		
    		/*
    		 * Trigger Song Progress
    		 */
			filex.triggerSongProgress();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	Handler animationStarterHandler = new Handler(){
//		@Override
//		public void handleMessage(Message msg){
//			Filex filex = (Filex) msg.obj;
//    		filex.currentAlbumPlayingLayoutOuter.startAnimation(filex.fadeAlbumOut);
//    		//msg.recycle();
//		}
//	};
	
}