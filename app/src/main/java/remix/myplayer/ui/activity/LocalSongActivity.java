package remix.myplayer.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.heytap.wearable.support.widget.HeyBackTitleBar;
import com.heytap.wearable.support.widget.HeyMultipleDefaultItem;

import org.jetbrains.annotations.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.ui.activity.base.BaseMusicActivity;
import remix.myplayer.ui.fragment.SongFragment;

public class LocalSongActivity extends BaseMusicActivity {
    @BindView(R.id.back_titlebar)
    HeyBackTitleBar mBackTitleBar;
    @BindView(R.id.fragment_container)
    LinearLayout mFragmentContainer;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_song);
        ButterKnife.bind(this);

        mBackTitleBar.setBackListener(view -> {
            finish();
        }, this);


        if (savedInstanceState != null) {
            return;
        }

        SongFragment songFragment = new SongFragment();
        songFragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, songFragment).commit();
    }
}
