package remix.myplayer.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.List;

import remix.myplayer.adapter.BaseAdapter;
import remix.myplayer.helper.MusicEventHelper;
import remix.myplayer.service.MusicService;

/**
 * Created by Remix on 2017/10/20.
 */

public abstract class PermissionActivity<D,A extends BaseAdapter> extends MultiChoiceActivity implements MusicEventHelper.MusicEventCallback, android.app.LoaderManager.LoaderCallbacks<List<D>>{
    protected boolean mHasPermission = false;
    protected A mAdapter;
    private final String[] mPermissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MusicEventHelper.addCallback(this);
        if(mHasPermission = hasPermissions()){
            getLoaderManager().initLoader(getLoaderId(), null, this);
        }
    }

    protected abstract int getLoaderId();

    @Override
    protected void onResume() {
        super.onResume();
        new RxPermissions(this)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(aBoolean -> {
                    if(aBoolean != mHasPermission){
                        Intent intent = new Intent(MusicService.ACTION_PERMISSION_CHANGE);
                        intent.putExtra("permission",aBoolean);
                        sendBroadcast(intent);
                    }
                });
    }

    protected boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mPermissions != null) {
            for (String permission : mPermissions) {
                if (mContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MusicEventHelper.removeCallback(this);
    }

    @Override
    public void onMediaStoreChanged() {
        if(mHasPermission)
            getLoaderManager().restartLoader(getLoaderId(), null, this);
        else{
            if(mAdapter != null)
                mAdapter.setDatas(null);
        }
    }

    @Override
    public void onPermissionChanged(boolean has) {
        if(has != mHasPermission){
            mHasPermission = has;
            onMediaStoreChanged();
        }
    }

    @Override
    public void onPlayListChanged() {

    }

    @Override
    public android.content.Loader<List<D>> onCreateLoader(int id, Bundle args) {
        return getLoader();
    }

    @Override
    public void onLoadFinished(android.content.Loader<List<D>> loader, List<D> data) {
        if(mAdapter != null)
            mAdapter.setDatas(data);
    }

    @Override
    public void onLoaderReset(android.content.Loader<List<D>> loader) {
        if(mAdapter != null)
            mAdapter.setDatas(null);
    }

    protected android.content.Loader<List<D>> getLoader(){
        return null;
    }

}
