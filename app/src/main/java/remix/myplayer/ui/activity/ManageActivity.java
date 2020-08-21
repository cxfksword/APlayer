package remix.myplayer.ui.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.View;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.heytap.wearable.support.recycler.widget.LinearLayoutManager;
import com.heytap.wearable.support.widget.HeyBackTitleBar;
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem;
import com.heytap.wearable.support.widget.HeySingleDefaultItem;
import com.heytap.wearable.support.widget.HeySingleItemWithSummary;

import org.jetbrains.annotations.Nullable;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.db.room.DatabaseRepository;
import remix.myplayer.misc.MediaScanner;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.receiver.ExitReceiver;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.base.BaseMusicActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;
import timber.log.Timber;

import static remix.myplayer.service.MusicService.EXTRA_CONTROL;
import static remix.myplayer.util.Util.sendLocalBroadcast;

public class ManageActivity extends BaseMusicActivity {
    private static final int CLEAR_FINISH = 1;
    private static final int CACHE_SIZE = 2;

    @BindView(R.id.back_titlebar)
    HeyBackTitleBar mBackTitleBar;
    @BindView(R.id.manage_local_songs)
    HeyMultipleDefaultItem mManageLocalSongs;
    @BindView(R.id.manage_my_favorite)
    HeyMultipleDefaultItem mManageMyLove;
    @BindView(R.id.manage_scan_media)
    HeyMultipleDefaultItem mManageScanMedia;
    @BindView(R.id.manage_about)
    HeySingleDefaultItem mManageAbout;
    @BindView(R.id.manage_clear_cache)
    HeySingleItemWithSummary mManageClearCache;
    @BindView(R.id.manage_exit)
    HeySingleItemWithSummary mManageExit;

    //缓存大小
    private Long mCacheSize = 0L;
    private MsgHandler mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_manage);
        ButterKnife.bind(this);

        mBackTitleBar.setBackListener(view -> {
            finish();
        }, this);

        mHandler = new  MsgHandler(this);

        //计算缓存大小
        new Thread() {
            @Override
            public void run() {
                calCacheSize();
            }
        }.start();

    }

    @OnClick({R.id.manage_local_songs, R.id.manage_my_favorite, R.id.manage_scan_media, R.id.manage_about, R.id.manage_clear_cache, R.id.manage_exit})
    public void onItemClick(View v) {
        Intent intent = new Intent(MusicService.ACTION_CMD);
        switch (v.getId()) {
            case R.id.manage_local_songs:
                startActivity(new Intent(mContext, LocalSongActivity.class));
                break;
            case R.id.manage_my_favorite:
                ChildHolderActivity.start(mContext, Constants.MYFAVORITE, 0, getString(R.string.my_favorite));
                break;
            case R.id.manage_about:
                startActivity(new Intent(mContext, AboutActivity.class));
                break;
            case R.id.manage_scan_media:
                File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
                if (folder.exists() && folder.isDirectory() && folder.list() != null) {
                    new MediaScanner(mContext).scanFiles(folder);
                }
                break;
            case R.id.manage_clear_cache:
               new Thread() {
                   @Override
                   public void run() {
                       //清除歌词，封面等缓存
                       //清除配置文件、数据库等缓存
                       Util.deleteFilesByDirectory(getCacheDir());
                       Util.deleteFilesByDirectory(getExternalCacheDir());
                       DiskCache.init(mContext, "lyric");
                       //清除fresco缓存
                       Fresco.getImagePipeline().clearCaches();
                       mHandler.sendEmptyMessage(CLEAR_FINISH);
                       ImageUriRequest.clearUriCache();
                       calCacheSize();
                   }
               }.start();
               break;
            case R.id.manage_exit:
                Timber.v("发送Exit广播");
                sendBroadcast(new Intent(Constants.ACTION_EXIT)
                        .setComponent(new ComponentName(mContext, ExitReceiver.class)));
                break;
        }
    }

    @OnHandleMessage
    public void handleInternal(Message msg) {
        if (msg.what == CACHE_SIZE) {
            mManageClearCache.getSummaryTextView().setText(getString(R.string.cache_size, mCacheSize / 1024f / 1024f));
        }
        if (msg.what == CLEAR_FINISH) {
            ToastUtil.show(mContext, getString(R.string.clear_success));
            mManageClearCache.getSummaryTextView().setText(getString(R.string.cache_size, mCacheSize / 1024f / 1024f));
        }
    }

    private void calCacheSize() {
        //计算缓存大小
        mCacheSize = 0L;
        mCacheSize += Util.getFolderSize(getExternalCacheDir());
        mCacheSize += Util.getFolderSize(getCacheDir());
        mHandler.sendEmptyMessage(CACHE_SIZE);
    }
}
