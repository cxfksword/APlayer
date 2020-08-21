package remix.myplayer.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.db.room.DatabaseRepository;
import remix.myplayer.db.room.model.PlayList;
import remix.myplayer.db.room.model.PlayQueue;
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.misc.interfaces.LoaderIds;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.adapter.PlayQueueAdapter;
import remix.myplayer.ui.widget.fastcroll_recyclerview.LocationRecyclerView;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.MusicUtil;
import remix.myplayer.util.Util;
import timber.log.Timber;


public class PlayQueueFragment extends LibraryFragment<Song, PlayQueueAdapter>  {
    @BindView(R.id.playqueue_recyclerview)
    LocationRecyclerView mRecyclerView;

    public static final String TAG = PlayQueueFragment.class.getSimpleName();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = TAG;

    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();
        if (mAdapter != null) {
            mAdapter.updatePlayingSong();
        }
    }

    @Override
    public void onPlayListChanged(@NotNull String name) {
        super.onPlayListChanged(name);
        if (name.equals(PlayQueue.TABLE_NAME)) {
            onMediaStoreChanged();
        }
    }


    @Override
    protected int getLayoutID() {
        return R.layout.fragment_playqueue;
    }

    @Override
    protected void initAdapter() {
        mAdapter = new PlayQueueAdapter(R.layout.item_playqueue, mRecyclerView);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Util.sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG).putExtra(MusicService.EXTRA_POSITION, position));
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
    }

    @Override
    protected void initView() {
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setHasFixedSize(true);
    }

    @Override
    protected Loader<List<Song>> getLoader() {
        return new AsyncSongLoader(mContext);
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.PLAYQUEUE_FRAGMENT;
    }

    private static class AsyncSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

        private AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return DatabaseRepository.getInstance()
                    .getPlayQueueSongs()
                    .onErrorReturn(throwable -> {
                        Timber.v(throwable);
                        return new ArrayList<Song>();
                    })
                    .blockingGet();
        }
    }
}
