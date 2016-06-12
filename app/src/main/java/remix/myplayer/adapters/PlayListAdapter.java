package remix.myplayer.adapters;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import remix.myplayer.R;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.infos.PlayListItem;
import remix.myplayer.listeners.PopupListener;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 播放列表的适配器
 */
public class PlayListAdapter extends RecyclerView.Adapter<PlayListAdapter.ViewHolder> {
    private Context mContext;
    public PlayListAdapter(Context context) {
        this.mContext = context;
    }

    private OnItemClickLitener mOnItemClickLitener;
    public interface OnItemClickLitener {
        void onItemClick(View view, int position);
        void onItemLongClick(View view , int position);
    }
    public void setOnItemClickLitener(OnItemClickLitener mOnItemClickLitener) {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_recycle_item, null, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        try {
            //根据当前索引，获得歌曲列表
            Iterator it = PlayListActivity.getPlayList().keySet().iterator();
            String name = "";
            for(int i = 0 ; i<= position ;i++) {
                it.hasNext();
                name = it.next().toString();
            }
            //设置播放列表名字
            holder.mName.setText(name);

            //设置专辑封面
            new AsynLoadImage(holder.mImage).execute(name);
//            ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get(name);
//            if(list != null && list.size() > 0) {
//                for(PlayListItem item : list){
//                    String url = DBUtil.getImageUrl(item.getAlbumId() + "",Constants.URL_ALBUM);
//                    if(url != null && !url.equals("")) {
//                        File file = new File(url);
//                        if(!file.exists())
//                            continue;
//                        holder.mImage.setImageURI(Uri.parse(url));
//                        break;
//                    }
//                }
//            }

            if(mOnItemClickLitener != null) {
                holder.mImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnItemClickLitener.onItemClick(holder.mImage,position);
                    }
                });
                holder.mImage.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mOnItemClickLitener.onItemLongClick(holder.mImage,position);
                        return true;
                    }
                });
            }

            if(holder.mButton != null) {
                //我的收藏列表，不能删除
                if(name.equals(mContext.getString(R.string.my_favorite))){
                    holder.mButton.setImageResource(R.drawable.rcd_icn_love);
                    holder.mButton.setClickable(false);
                    holder.mButton.setBackgroundColor(mContext.getResources().getColor(R.color.transparent));
                } else {
                    holder.mButton.setImageResource(R.drawable.list_icn_more);
                    holder.mButton.setClickable(true);
                    holder.mButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Context wrapper = new ContextThemeWrapper(MainActivity.mInstance, R.style.MyPopupMenu);
                            final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton,Gravity.END);
                            popupMenu.getMenuInflater().inflate(R.menu.alb_art_menu, popupMenu.getMenu());
                            popupMenu.setOnMenuItemClickListener(new PopupListener(mContext, position, Constants.PLAYLIST_HOLDER, ""));
                            popupMenu.show();
                        }
                    });
                }

            }
        } catch (Exception e){
            e.toString();
        }

    }

    @Override
    public int getItemCount() {
        return PlayListActivity.getPlayList() == null ? 0 : PlayListActivity.getPlayList().size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mName;
        public final SimpleDraweeView mImage;
        public final ImageView mButton;
        public ViewHolder(View itemView) {
            super(itemView);
            mName = (TextView) itemView.findViewById(R.id.playlist_item_name);
            mImage = (SimpleDraweeView)itemView.findViewById(R.id.recycleview_simpleiview);
            mButton = (ImageView)itemView.findViewById(R.id.recycleview_button);
        }
    }

    class AsynLoadImage extends AsyncTask<String,Integer,String> {
        private final SimpleDraweeView mImage;
        public AsynLoadImage(SimpleDraweeView imageView)
        {
            mImage = imageView;
        }
        @Override
        protected String doInBackground(String... params) {
            ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get(params[0]);
            String url = null;
            if(list != null && list.size() > 0) {
                for(PlayListItem item : list){
                    url = DBUtil.getImageUrl(item.getAlbumId() + "",Constants.URL_ALBUM);
                    if(url != null && !url.equals("")) {
                        File file = new File(url);
                        if(file.exists()) {
                            break;
                        }
                    }
                }
            }
            return url;
        }
        @Override
        protected void onPostExecute(String url) {
            Uri uri = Uri.parse("file:///" + url);
            if(mImage != null)
                mImage.setImageURI(uri);
        }
    }

}
