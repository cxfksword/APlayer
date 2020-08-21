package remix.myplayer.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.InsetDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.heytap.wearable.support.widget.HeyDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.OnClick;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.db.room.DatabaseRepository;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.menu.AudioPopupListener;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.GradientDrawableMaker;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.ui.dialog.PlayQueueDialog;
import remix.myplayer.ui.fragment.base.BaseMusicFragment;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

import static remix.myplayer.misc.ExtKt.isPortraitOrientation;
import static remix.myplayer.service.MusicService.EXTRA_CONTROL;
import static remix.myplayer.theme.ThemeStore.getAccentColor;
import static remix.myplayer.theme.ThemeStore.getPlayerNextSongBgColor;
import static remix.myplayer.theme.ThemeStore.isLightTheme;
import static remix.myplayer.util.Constants.MODE_LOOP;
import static remix.myplayer.util.Constants.MODE_REPEAT;
import static remix.myplayer.util.Constants.MODE_SHUFFLE;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;
import static remix.myplayer.util.Util.sendLocalBroadcast;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 专辑封面Fragment
 */
public class CoverFragment extends BaseMusicFragment {

  @BindView(R.id.cover_image)
  SimpleDraweeView mImage;
  @BindView(R.id.cover_container)
  View mCoverContainer;
  @BindView(R.id.song_title)
  TextView mSongTitle;
  @BindView(R.id.song_detail)
  TextView mSongDetail;

  @BindView(R.id.playbar_model)
  ImageButton mPlayModel;
  @BindView(R.id.playbar_toggle_play)
  DonutProgress mPlayToggle;
  @BindView(R.id.playbar_collect)
  ImageButton mPlayCollect;

  //上次选中的Fragment
  private int mPrevPosition = 0;
  //第一次启动的标志变量
  private boolean mFirstStart = true;

  private int mWidth;
  private Uri mUri = Uri.EMPTY;
  //当前播放的歌曲
  private Song mInfo;
  //当前是否播放
  private boolean mIsPlay;
  //当前播放时间
  private int mCurrentTime;
  //当前歌曲总时长
  private int mDuration;

  /**
   * 更新封面与背景的Handler
   */
  private static final int UPDATE_BG = 1;
  private static final int UPDATE_TIME_ONLY = 2;
  private static final int UPDATE_TIME_ALL = 3;
  private AudioManager mAudioManager;

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPageName = CoverFragment.class.getSimpleName();

    mInfo = MusicServiceRemote.getCurrentSong();
    mAudioManager = (AudioManager) getActivity().getSystemService(getContext().AUDIO_SERVICE);
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    mWidth = getResources().getDisplayMetrics().widthPixels;
    View rootView = inflater.inflate(R.layout.fragment_cover, container, false);
    mUnBinder = ButterKnife.bind(this, rootView);

    mImage.getHierarchy().setFailureImage(ThemeStore.isLightTheme() ?
            R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night);


    setUpSeekBar();
    setUpPlayBar();
    setUpViewColor();

    return rootView;
  }

  private void setUpSeekBar() {
    if (mInfo == null) {
      return;
    }
    //初始化已播放时间与剩余时间
    mDuration = (int) mInfo.getDuration();
    final int temp = MusicServiceRemote.getProgress();
    mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;

  }

  private void setUpPlayBar() {
    updateCollectStatus();
  }

  /**
   * 根据主题颜色修改按钮颜色
   */
  private void setUpViewColor() {

    //播放模式与播放队列
    int playMode = SPUtil.getValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL,
            MODE_LOOP);
    switch (playMode) {
      case MODE_LOOP:
        mPlayModel.setImageResource(R.drawable.play_btn_loop);
        break;
      case MODE_SHUFFLE:
        mPlayModel.setImageResource(R.drawable.play_btn_shuffle);
        break;
      default:
        mPlayModel.setImageResource(R.drawable.play_btn_loop_one);
        break;
    }

  }

  private void updateCollectStatus() {
    if (mInfo == null) {
      return;
    }

    if (MusicServiceRemote.getIsLove()) {
      mPlayCollect.setImageResource(R.drawable.icon_uncollect);
    } else {
      mPlayCollect.setImageResource(R.drawable.icon_collect);
    }

  }


  @Override
  public void onResume() {
    super.onResume();

    onMetaChanged();
    onPlayStateChange();
  }

  private void updateCover(boolean withAnimation) {
    updateCover(mInfo, mUri, withAnimation);
    mFirstStart = false;
  }

  /**
   * 操作为上一首歌曲时，显示往左侧消失的动画 下一首歌曲时，显示往右侧消失的动画
   *
   * @param info 需要更新的歌曲
   * @param withAnim 是否需要动画
   */
  public void updateCover(Song info, Uri uri, boolean withAnim) {
    if (!isAdded()) {
      return;
    }
    if (mImage == null || info == null) {
      return;
    }
    mUri = uri;
    updateSongStatus(info);
    if (withAnim) {
      int operation = MusicServiceRemote.getOperation();

      int offsetX = (mWidth + mImage.getWidth()) >> 1;
      final double startValue = 0;
      final double endValue = operation == Command.PREV ? offsetX : -offsetX;

      //封面移动动画
      final Spring outAnim = SpringSystem.create().createSpring();
      outAnim.addListener(new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
          if (mCoverContainer == null || spring == null) {
            return;
          }
          mCoverContainer.setTranslationX((float) spring.getCurrentValue());
        }

        @Override
        public void onSpringAtRest(Spring spring) {
          //显示封面的动画
          if (mImage == null || spring == null) {
            return;
          }
          mCoverContainer.setTranslationX((float) startValue);
          setImageUriInternal();

          float endVal = 1;
          final Spring inAnim = SpringSystem.create().createSpring();
          inAnim.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
              if (mImage == null || spring == null) {
                return;
              }
              mCoverContainer.setScaleX((float) spring.getCurrentValue());
              mCoverContainer.setScaleY((float) spring.getCurrentValue());
            }

            @Override
            public void onSpringActivate(Spring spring) {

            }

          });
          inAnim.setCurrentValue(0.85);
          inAnim.setEndValue(endVal);
        }
      });
      outAnim.setOvershootClampingEnabled(true);
      outAnim.setCurrentValue(startValue);
      outAnim.setEndValue(endValue);
    } else {
      setImageUriInternal();
    }
  }

  private void setImageUriInternal() {
    mImage.setImageURI(mUri);
  }


  public void updateSongStatus(Song song) {
    if (song == null) {
      return;
    }
    String title = song.getTitle() == null ? "" : song.getTitle();
    String artist = song.getArtist() == null ? "" : song.getArtist();
    String album = song.getAlbum() == null ? "" : song.getAlbum();

    if (title.equals("")) {
      mSongTitle.setText(getString(R.string.unknown_song));
    } else {
      mSongTitle.setText(title);
    }
    if (artist.equals("")) {
      mSongDetail.setText(song.getAlbum());
    } else if (album.equals("")) {
      mSongDetail.setText(song.getArtist());
    } else {
      mSongDetail.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));
    }
  }



  @Override
  public void onMetaChanged() {
    super.onMetaChanged();
    mInfo = MusicServiceRemote.getCurrentSong();
    if (mInfo.getId() < 0) {
      return;
    }
    //当操作不为播放或者暂停且正在运行时，更新所有控件
    final int operation = MusicServiceRemote.getOperation();
    if ((operation != Command.TOGGLE || mFirstStart)) {
      //更新顶部信息
      updateSongStatus(mInfo);
      //更新歌词
//      mHandler.postDelayed(() -> mLyricFragment.updateLrc(mInfo), 500);
      //更新进度条
      int temp = MusicServiceRemote.getProgress();
      mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;
      mDuration = (int) mInfo.getDuration();
      mPlayToggle.setMax(mDuration);
      updateProgress(0);
      //更新收藏状态
      updateCollectStatus();

      requestCover(false);
    }
  }

  /**
   * 更新封面
   */
  private void requestCover(boolean withAnimation) {
    //更新封面
    if (mInfo == null) {
      mUri = Uri.parse("res://" + mContext.getPackageName() + "/" + (isLightTheme()
              ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night));
      updateCover(withAnimation);
    } else {
      new ImageUriRequest<String>() {
        @Override
        public void onError(Throwable throwable) {
          mUri = Uri.EMPTY;
          updateCover(withAnimation);
        }

        @Override
        public void onSuccess(String result) {
          mUri = Uri.parse(result);
          updateCover(withAnimation);
        }

        @Override
        public Disposable load() {
          return getCoverObservable(getSearchRequestWithAlbumType(mInfo))
                  .compose(RxUtil.applyScheduler())
                  .subscribe(this::onSuccess, this::onError);
        }
      }.load();
    }
  }



  @Override
  public void onPlayStateChange() {
    super.onPlayStateChange();
    //更新按钮状态
    final boolean isPlay = MusicServiceRemote.isPlaying();
    if (mIsPlay != isPlay) {
      updatePlayButton(isPlay);
    }
  }

  @Override
  public void onLoveStateChange() {
    updateCollectStatus();
  }

  /**
   * 更新播放、暂停按钮
   */
  public void updatePlayButton(final boolean isPlay) {
    mIsPlay = isPlay;
    if (isPlay) {
      mPlayToggle.setAttributeResourceId(R.drawable.play_btn_pause);
    } else {
      mPlayToggle.setAttributeResourceId(R.drawable.play_btn_play);
    }
    mPlayToggle.invalidate();
  }


  /**
   * 上一首 下一首 播放、暂停
   */
  @OnClick({R.id.playbar_next, R.id.playbar_prev, R.id.playbar_toggle_play})
  public void onCtrlClick(View v) {
    Intent intent = new Intent(MusicService.ACTION_CMD);
    switch (v.getId()) {
      case R.id.playbar_prev:
        intent.putExtra(EXTRA_CONTROL, Command.PREV);
        break;
      case R.id.playbar_next:
        intent.putExtra(EXTRA_CONTROL, Command.NEXT);
        break;
      case R.id.playbar_toggle_play:
        intent.putExtra(EXTRA_CONTROL, Command.TOGGLE);
        break;
    }
    sendLocalBroadcast(intent);
  }

  /**
   * 播放模式 播放列表 关闭 隐藏
   */
  @OnClick({R.id.playbar_model, R.id.playbar_volume, R.id.playbar_collect})
  public void onOtherClick(View v) {
    switch (v.getId()) {
      //设置播放模式
      case R.id.playbar_model:
        int currentModel = MusicServiceRemote.getPlayModel();
        currentModel = (currentModel == MODE_REPEAT ? MODE_LOOP : ++currentModel);
        MusicServiceRemote.setPlayModel(currentModel);
        mPlayModel.setImageDrawable(getActivity().getDrawable(currentModel == MODE_LOOP ? R.drawable.play_btn_loop :
                        currentModel == MODE_SHUFFLE ? R.drawable.play_btn_shuffle : R.drawable.play_btn_loop_one));

        String msg = currentModel == MODE_LOOP ? getString(R.string.model_normal)
                : currentModel == MODE_SHUFFLE ? getString(R.string.model_random)
                : getString(R.string.model_repeat);
        //刷新下一首
        ToastUtil.show(getContext(), msg);
        break;
      //打开正在播放列表
      case R.id.playbar_playinglist:
        PlayQueueDialog.newInstance()
                .show(getFragmentManager(), PlayQueueDialog.class.getSimpleName());
        break;
      case R.id.playbar_volume:
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
        break;
      case R.id.playbar_collect:
        Intent intent = new Intent(MusicService.ACTION_CMD);
        intent.putExtra(EXTRA_CONTROL, Command.LOVE);
        sendLocalBroadcast(intent);
        break;
    }
  }

  public void updateProgress(int current) {
    if (mPlayToggle == null) {
      return;
    }
    if (current > 0 && current < mDuration) {
      mPlayToggle.setDonut_progress((String.valueOf(current)));
    } else if (current >= mDuration) {
      mPlayToggle.setDonut_progress(String.valueOf(mDuration));
    } else {
      mPlayToggle.setDonut_progress("0");
    }
  }

}
