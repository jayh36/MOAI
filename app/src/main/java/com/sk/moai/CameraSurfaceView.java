package com.sk.moai;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by HONG on 2017-08-18.
 */
@SuppressWarnings("deprecation")
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder ;
    private Camera camera = null ;

    public Camera getCamera() {
        return camera;
    }

    public CameraSurfaceView(Context context){
        super(context);

        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder){
        camera = Camera.open();
        camera.setDisplayOrientation(90);

        try{
            camera.setPreviewDisplay(mHolder);
        }catch(Exception e){
            Log.e("CameraSurfaceVeiw", "Fail to set camera preview.",e);
        }
    }
    public void surfaceChanged(SurfaceHolder holder, int fromat, int width, int height){
        camera.startPreview();
    }
    public void surfaceDestroyed(SurfaceHolder holder){
        camera.stopPreview();
        camera.release();
        camera = null;
    }

    public boolean capture(Camera.PictureCallback handler){
        if(camera != null){
            camera.takePicture(null, null, handler);
            return true ;

        }else{
            return false ;
        }
    }
}
