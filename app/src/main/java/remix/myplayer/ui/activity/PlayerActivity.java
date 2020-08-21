package remix.myplayer.ui.activity;

import static remix.myplayer.misc.ExtKt.isPortraitOrientation;
import static remix.myplayer.service.MusicService.EXTRA_CONTROL;
import static remix.myplayer.theme.ThemeStore.getAccentColor;
import static remix.myplayer.theme.ThemeStore.getPlayerNextSongBgColor;
import static remix.myplayer.theme.ThemeStore.getPlayerProgressColor;
import static remix.myplayer.theme.ThemeStore.isLightTheme;
import static remix.myplayer.theme.ThemeStore.sColoredNavigation;
import static remix.myplayer.util.Constants.MODE_LOOP;
import static remix.myplayer.util.Constants.MODE_REPEAT;
import static remix.myplayer.util.Constants.MODE_SHUFFLE;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;
import static remix.myplayer.util.SPUtil.SETTING_KEY.BOTTOM_OF_NOW_PLAYING_SCREEN;
import static remix.myplayer.util.Util.registerLocalReceiver;
import static remix.myplayer.util.Util.sendLocalBroadcast;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Palette.Swatch;
import android.support.v7.widget.PopupMenu;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.heytap.wearable.support.widget.HeyAppTitleBar;
import com.heytap.wearable.support.widget.pageindicator.HeyPageIndicator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.lyric.LrcView;
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
import remix.myplayer.ui.activity.base.BaseMusicActivity;
import remix.myplayer.ui.adapter.PagerAdapter;
import remix.myplayer.ui.dialog.FileChooserDialog;
import remix.myplayer.ui.dialog.PlayQueueDialog;
import remix.myplayer.ui.fragment.CoverFragment;
import remix.myplayer.ui.fragment.LyricFragment;
import remix.myplayer.ui.fragment.PlayQueueFragment;
import remix.myplayer.ui.fragment.RecordFragment;
import remix.myplayer.ui.widget.AudioViewPager;
import remix.myplayer.ui.widget.playpause.PlayPauseView;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.MusicUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;
import timber.log.Timber;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放界面
 */
public class PlayerActivity extends BaseMusicActivity implements FileChooserDialog.FileCallback {

  private static final String TAG = "PlayerActivity";
//  public static final String EXTRA_SHOW_ANIMATION = "ShowAnimation";
//  public static final String EXTRA_FROM_NOTIFY = "FromNotify";
//  public static final String EXTRA_FROM_ACTIVITY = "FromActivity";
//  public static final String EXTRA_ANIM_URL = "AnimUrl";
//  public static final String EXTRA_RECT = "Rect";

  //上次选中的Fragment
  private int mPrevPosition = 0;
  //第一次启动的标志变量
  private boolean mFirstStart = true;

  //顶部信息
  @BindView(R.id.top_titlebar)
  HeyAppTitleBar mTopTitleBar;

  //背景
  @BindView(R.id.audio_holder_container)
  ViewGroup mContainer;
  @BindView(R.id.holder_pager)
  AudioViewPager mPager;

  //翻页指示
  @BindView(R.id.indicator)
  HeyPageIndicator mPageIndicator;
  //歌词控件
  private LrcView mLrcView;

  //当前播放的歌曲
  private Song mInfo;

  //当前播放时间
  private int mCurrentTime;
  //当前歌曲总时长
  private int mDuration;

  //需要高斯模糊的高度与宽度
  public int mWidth;
  public int mHeight;

  //Fragment
  private LyricFragment mLyricFragment;
  private CoverFragment mCoverFragment;
  private PlayQueueFragment mPlayQueueFragment;

  /**
   * 下拉关闭
   */
  private float mEventY1;
  private float mEventY2;
  private float mEventX1;
  private float mEventX2;

  /**
   * 更新Handler
   */
  private MsgHandler mHandler;

  /**
   * 更新封面与背景的Handler
   */
  private Uri mUri;
  private static final int UPDATE_BG = 1;
  private static final int UPDATE_TIME_ONLY = 2;
  private static final int UPDATE_TIME_ALL = 3;
  private AudioManager mAudioManager;

  //底部显示控制
  private int mBottomConfig;
  public static final int BOTTOM_SHOW_NEXT = 0;
  public static final int BOTTOM_SHOW_VOLUME = 1;
  public static final int BOTTOM_SHOW_BOTH = 2;
  public static final int BOTTOM_SHOW_NONE = 3;

  private static final int FRAGMENT_COUNT = 2;

  private static final int DELAY_SHOW_NEXT_SONG = 3000;


  @Override
  protected void setUpTheme() {
//    if (ThemeStore.isLightTheme()) {
//      super.setUpTheme();
//    } else {
//      setTheme(R.style.AudioHolderStyle_Night);
//    }
    final int superThemeRes = ThemeStore.getThemeRes();
    int themeRes;
    switch (superThemeRes) {
      case R.style.Theme_APlayer_Black:
        themeRes = R.style.PlayerActivityStyle_Black;
        break;
      case R.style.Theme_APlayer_Dark:
        themeRes = R.style.PlayerActivityStyle_Dark;
        break;
      default:
        themeRes = R.style.PlayerActivityStyle;
    }
    setTheme(themeRes);
  }

  @Override
  protected void setNavigationBarColor() {
    super.setNavigationBarColor();
    //导航栏变色
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && sColoredNavigation) {
      final int navigationColor = ThemeStore.getBackgroundColorMain(this);
      getWindow().setNavigationBarColor(navigationColor);
      Theme.setLightNavigationbarAuto(this, ColorUtil.isColorLight(navigationColor));
    }
  }

  @Override
  protected void setStatusBarMode() {
    StatusBarUtil.setStatusBarMode(this, ThemeStore.getBackgroundColorMain(this));
  }

  @Override
  protected void setStatusBarColor() {
    StatusBarUtil.setColorNoTranslucent(this, ThemeStore.getBackgroundColorMain(this));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_player);
    ButterKnife.bind(this);

    mHandler = new MsgHandler(this);

    mInfo = MusicServiceRemote.getCurrentSong();
    mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

    setUpSize();
    setUpTop();
    setUpFragments();
    setUpSeekBar();

  }

  @Override
  public void onResume() {
    super.onResume();
    if (isPortraitOrientation(this)) {
      mPager.setCurrentItem(0);
    }
    //更新进度条
    startProgressThread();
  }

  @Override
  public void onServiceConnected(@NotNull MusicService service) {
    super.onServiceConnected(service);
    onMetaChanged();
    onPlayStateChange();
  }

  @Override
  protected void onStart() {
    super.onStart();
    overridePendingTransition(R.anim.audio_in, 0);
  }


  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(0, R.anim.audio_out);
  }




  private AlphaAnimation makeAnimation(View view, boolean show) {
    AlphaAnimation alphaAnimation = new AlphaAnimation(show ? 0 : 1, show ? 1 : 0);
    alphaAnimation.setDuration(300);
    alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
        view.setVisibility(View.VISIBLE);
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
      }

      @Override
      public void onAnimationRepeat(Animation animation) {

      }
    });
    return alphaAnimation;
  }

  /**
   * 获得屏幕大小
   */
  private void setUpSize() {
    WindowManager wm = getWindowManager();
    Display display = wm.getDefaultDisplay();
    DisplayMetrics metrics = new DisplayMetrics();
    display.getMetrics(metrics);
    mWidth = metrics.widthPixels;
    mHeight = metrics.heightPixels;
  }





  /**
   * 更新顶部歌曲信息
   */
  public void updateTopStatus(Song song) {
    if (song == null) {
      return;
    }

  }


  /**
   * 初始化顶部信息
   */
  private void setUpTop() {
    updateTopStatus(mInfo);

    mTopTitleBar.getLeftIcon().setVisibility(View.GONE);
    mTopTitleBar.getRightIcon().setOnClickListener(view -> {
      Intent intent = new Intent(mContext, ManageActivity.class);
      startActivity(intent);
    });
    mTopTitleBar.getRightIcon().setBackground(getDrawable(R.drawable.icon_background_selector));
    mTopTitleBar.setOnClickListener(view -> {
      Intent intent = new Intent(mContext, ManageActivity.class);
      startActivity(intent);
    });
  }

  /**
   * 初始化seekbar
   */
  @SuppressLint("CheckResult")
  private void setUpSeekBar() {
    if (mInfo == null) {
      return;
    }
    //初始化已播放时间与剩余时间
    mDuration = (int) mInfo.getDuration();
    final int temp = MusicServiceRemote.getProgress();
    mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;
  }

    /**
     * 初始化viewpager
     */
  @SuppressLint("ClickableViewAccessibility")
  private void setUpFragments() {
    final FragmentManager fragmentManager = getSupportFragmentManager();

    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    fragmentManager.executePendingTransactions();
    final List<Fragment> fragments = fragmentManager.getFragments();
    if (fragments != null) {
      for (Fragment fragment : fragments) {
        if (fragment instanceof LyricFragment ||
            fragment instanceof CoverFragment ||
            fragment instanceof PlayQueueFragment) {
          fragmentManager.beginTransaction().remove(fragment).commitNow();
        }
      }
    }

    mCoverFragment = new CoverFragment();
    setUpCoverFragment();
    mLyricFragment = new LyricFragment();
    setUpLyricFragment();
    mPlayQueueFragment = new PlayQueueFragment();

    if (isPortraitOrientation(this)) {
//      mRecordFragment = new RecordFragment();

      //Viewpager
      PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
//      adapter.addFragment(mRecordFragment);
      adapter.addFragment(mCoverFragment);
      adapter.addFragment(mLyricFragment);
      adapter.addFragment(mPlayQueueFragment);

      mPager.setAdapter(adapter);
      mPager.setOffscreenPageLimit(adapter.getCount() - 1);
      mPager.setCurrentItem(0);

      mPageIndicator.setDotsCount(adapter.getCount(), true);
      mPageIndicator.setOnDotClickListener(position -> {
        mPager.setCurrentItem(position);
      });


      final int THRESHOLD_Y = DensityUtil.dip2px(mContext, 40);
      final int THRESHOLD_X = DensityUtil.dip2px(mContext, 60);
      //下滑关闭
      mPager.setOnTouchListener((v, event) -> {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          mEventX1 = event.getX();
          mEventY1 = event.getY();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
          mEventX2 = event.getX();
          mEventY2 = event.getY();
          if (mEventY2 - mEventY1 > THRESHOLD_Y && Math.abs(mEventX1 - mEventX2) < THRESHOLD_X) {
            onBackPressed();
          }
        }
        return false;
      });
      mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
          mPageIndicator.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
          mPrevPosition = position;
          //歌词界面常亮
          if (position == 1 && SPUtil
              .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCREEN_ALWAYS_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          }
          mPageIndicator.onPageSelected(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
          mPageIndicator.onPageScrollStateChanged(state);
        }
      });
    } else {
      //歌词界面常亮
      if (SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCREEN_ALWAYS_ON,
          false)) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      }
      mCoverFragment = new CoverFragment();
      setUpCoverFragment();
      mLyricFragment = new LyricFragment();
      setUpLyricFragment();
      fragmentManager
          .beginTransaction()
          .replace(R.id.container_cover, mCoverFragment)
          .replace(R.id.container_lyric, mLyricFragment)
          .commit();
    }

  }

  private void setUpLyricFragment() {
    mLyricFragment.setOnInflateFinishListener(view -> {
      mLrcView = (LrcView) view;
      mLrcView.setOnLrcClickListener(new LrcView.OnLrcClickListener() {
        @Override
        public void onClick() {
        }

        @Override
        public void onLongClick() {
        }
      });
      mLrcView.setOnSeekToListener(progress -> {
        if (progress > 0 && progress < MusicServiceRemote.getDuration()) {
          MusicServiceRemote.setProgress(progress);
          mCurrentTime = progress;
          mHandler.sendEmptyMessage(UPDATE_TIME_ALL);
        }
      });
      mLrcView.setHighLightColor(ThemeStore.getTextColorPrimary());
      mLrcView.setOtherColor(ThemeStore.getTextColorSecondary());
      mLrcView.setTimeLineColor(ThemeStore.getTextColorSecondary());
    });
  }

  private void setUpCoverFragment() {
//    mCoverFragment.setOnFirstLoadFinishListener(() -> mAnimationCover.setVisibility(View.INVISIBLE));
//    mCoverFragment.setInflateFinishListener(view -> {
//      //不启动动画 直接显示
//      if (!mShowAnimation) {
//        mCoverFragment.showImage();
//        //隐藏动画用的封面并设置位置信息
//        mAnimationCover.setVisibility(View.GONE);
//        return;
//      }
//
//      if (mOriginRect == null || mOriginRect.width() <= 0 || mOriginRect.height() <= 0) {
//        //获取传入的界面信息
//        mOriginRect = getIntent().getParcelableExtra(EXTRA_RECT);
//      }
//
//      if (mOriginRect == null) {
//        return;
//      }
//      // 获取上一个界面中，图片的宽度和高度
//      mOriginWidth = mOriginRect.width();
//      mOriginHeight = mOriginRect.height();
//
//      // 设置 view 的位置，使其和上一个界面中图片的位置重合
//      FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(mOriginWidth, mOriginHeight);
//      params.setMargins(mOriginRect.left,
//          mOriginRect.top - StatusBarUtil.getStatusBarHeight(mContext), mOriginRect.right,
//          mOriginRect.bottom);
//      mAnimationCover.setLayoutParams(params);
//
//      //获得终点控件的位置信息
//      view.getGlobalVisibleRect(mDestRect);
//      // 计算图片缩放比例和位移距离
//      getMoveInfo(mDestRect);
//
//      mAnimationCover.setPivotX(0);
//      mAnimationCover.setPivotY(0);
//
//      final float transitionX = mTransitionBundle.getFloat(TRANSITION_X);
//      final float transitionY = mTransitionBundle.getFloat(TRANSITION_Y);
//      final float scaleX = mScaleBundle.getFloat(SCALE_WIDTH) - 1;
//      final float scaleY = mScaleBundle.getFloat(SCALE_HEIGHT) - 1;
//
//      final Spring spring = SpringSystem.create().createSpring();
//      spring.setSpringConfig(COVER_IN_SPRING_CONFIG);
//      spring.addListener(new SimpleSpringListener() {
//        @Override
//        public void onSpringUpdate(Spring spring) {
//          if (mAnimationCover == null) {
//            return;
//          }
//          final double currentVal = spring.getCurrentValue();
//          mAnimationCover.setTranslationX((float) (transitionX * currentVal));
//          mAnimationCover.setTranslationY((float) (transitionY * currentVal));
//          mAnimationCover.setScaleX((float) (1 + scaleX * currentVal));
//          mAnimationCover.setScaleY((float) (1 + scaleY * currentVal));
//        }
//
//        @Override
//        public void onSpringAtRest(Spring spring) {
//          //入场动画结束时显示fragment中的封面
//          mCoverFragment.showImage();
////                    mHandler.postDelayed(() -> {
////                        //隐藏动画用的封面
////                        mAnimationCover.setVisibility(View.INVISIBLE);
////                    },24);
//
//        }
//
//        @Override
//        public void onSpringActivate(Spring spring) {
//          overridePendingTransition(0, 0);
//        }
//      });
//      spring.setOvershootClampingEnabled(true);
//      spring.setCurrentValue(0);
//      spring.setEndValue(1);
//
//    });
  }

  @Override
  public void onMediaStoreChanged() {
    super.onMediaStoreChanged();

    final Song newSong = MusicServiceRemote.getCurrentSong();
    updateTopStatus(newSong);
    mLyricFragment.updateLrc(newSong);
    mInfo = newSong;
    requestCover(false);
  }

  @Override
  public void onMetaChanged() {
    super.onMetaChanged();
    Song oldSong = mInfo;
    mInfo = MusicServiceRemote.getCurrentSong();
    if (mInfo.getId() < 0) {
      return;
    }
    //当操作不为播放或者暂停且正在运行时，更新所有控件
    final int operation = MusicServiceRemote.getOperation();
    if ((operation != Command.TOGGLE || (mInfo.getId() != oldSong.getId()))) {
      //更新歌词
      mHandler.postDelayed(() -> mLyricFragment.updateLrc(mInfo), 500);
      //更新进度条
      int temp = MusicServiceRemote.getProgress();
      mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;
      mDuration = (int) mInfo.getDuration();
//      mProgressSeekBar.setMax(mDuration);
//      //更新下一首歌曲
//      mNextSong.setText(getString(R.string.next_song, MusicServiceRemote.getNextSong().getTitle()));
//      updateBackground();
//      requestCover(operation != Command.TOGGLE && !mFirstStart);
    }
  }

  @Override
  public void onPlayStateChange() {
    super.onPlayStateChange();
  }

  private void startProgressThread() {
    new ProgressThread().start();
  }

  //更新进度条线程
  private class ProgressThread extends Thread {

    @Override
    public void run() {
      while (mIsForeground) {
        //音量
//        if (mVolumeSeekbar.getVisibility() == View.VISIBLE) {
//          final int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//          final int current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//          runOnUiThread(() -> mVolumeSeekbar.setProgress((int) (current * 1.0 / max * 100)));
//        }
        if (MusicServiceRemote.isPlaying()) {
          int progress = MusicServiceRemote.getProgress();
          if (progress > 0 && progress < mDuration) {
            mCurrentTime = progress;
            mHandler.sendEmptyMessage(UPDATE_TIME_ALL);
          }
        }

        try {
          //1000ms时间有点长
          sleep(500);
        } catch (Exception e) {
          e.printStackTrace();
        }

      }
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return super.onKeyDown(keyCode, event);
  }




  private void setProgressDrawable(SeekBar seekBar, int accentColor) {
    LayerDrawable progressDrawable = (LayerDrawable) seekBar.getProgressDrawable();
    //修改progress颜色
    ((GradientDrawable) progressDrawable.getDrawable(0)).setColor(getPlayerProgressColor());
    (progressDrawable.getDrawable(1)).setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
    seekBar.setProgressDrawable(progressDrawable);
  }

  //更新背景
  private void updateBackground() {
//    new RemoteUriRequest(getSearchRequestWithAlbumType(mInfo),
//        new RequestConfig.Builder(200, 200).build()) {
//
//      @Override
//      public void onError(Throwable throwable) {
//        Timber.v(throwable);
//        updateSwatch(null);
//      }
//
//      @Override
//      public void onSuccess(@Nullable Bitmap result) {
//        updateSwatch(result);
//      }
//    }.load();
  }

  @SuppressLint("CheckResult")
  private void updateSwatch(final Bitmap bitmap) {
    Single
        .fromCallable(() -> bitmap == null ?
            BitmapFactory.decodeResource(getResources(),
                isLightTheme() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night)
            : bitmap)
        .map(result -> {
          final Palette palette = Palette.from(result).generate();
          return palette.getMutedSwatch();
        })
        .onErrorReturnItem(new Swatch(Color.GRAY, 100))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(swatch -> {
//          int colorFrom = ColorUtil.adjustAlpha(swatch.getRgb(), 0.3f);
//          int colorTo = ColorUtil.adjustAlpha(swatch.getRgb(), 0.05f);
          final int colorFrom = swatch.getRgb();
          final int colorTo = swatch.getRgb();
          mContainer.setBackground(
              new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{colorFrom, colorTo}));
        }, Timber::v);

  }

  private void updateCover(boolean withAnimation) {
    mCoverFragment.updateCover(mInfo, mUri, withAnimation);
    mFirstStart = false;
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
  protected void onDestroy() {
    super.onDestroy();
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    mHandler.remove();
  }

  /**
   * 选择歌词文件
   */
  @Override
  public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
//        //如果之前忽略过该歌曲的歌词，取消忽略
//        Set<String> ignoreLrcId = new HashSet<>(SPUtil.getStringSet(this,SPUtil.SETTING_KEY.NAME,"IgnoreLrcID"));
//        if(ignoreLrcId.size() > 0){
//            for (String id : ignoreLrcId){
//                if((mInfo.getID() + "").equals(id)){
//                    ignoreLrcId.remove(mInfo.getID() + "");
//                    SPUtil.putStringSet(context,SPUtil.SETTING_KEY.NAME,"IgnoreLrcID",ignoreLrcId);
//                }
//            }
//        }
    SPUtil.putValue(mContext, SPUtil.LYRIC_KEY.NAME, mInfo.getId() + "",
        SPUtil.LYRIC_KEY.LYRIC_MANUAL);
    mLyricFragment.updateLrc(file.getAbsolutePath());

    sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.CHANGE_LYRIC));
  }

  @Override
  public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {
  }


  private void updateProgressByHandler() {
    if (mCoverFragment != null) {
      mCoverFragment.updateProgress(mCurrentTime);
    }
    if (mLrcView != null) {
      mLrcView.seekTo(mCurrentTime, false, false);
    }
  }

  @OnHandleMessage
  public void handleInternal(Message msg) {
//        if(msg.what == UPDATE_BG){
//            int colorFrom = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.3f);
//            int colorTo = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.05f);
//            mContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{colorFrom, colorTo}));
//        }
    if (msg.what == UPDATE_TIME_ONLY) {
      updateProgressByHandler();
    }
    if (msg.what == UPDATE_TIME_ALL) {
      updateProgressByHandler();
    }
  }

  public LyricFragment getLyricFragment() {
    return mLyricFragment;
  }

  public void showLyricOffsetView() {
    //todo
    if (mPager.getCurrentItem() != 2) {
      mPager.setCurrentItem(2, true);
    }
    if (getLyricFragment() != null) {
      getLyricFragment().showLyricOffsetView();
    }
  }

  public static final String ACTION_UPDATE_NEXT = "remix.myplayer.update.next_song";
}
